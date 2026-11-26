package com.example.investsmart.security;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SecurityEvents {
    private final JdbcTemplate db; private final SecurityCrypto crypto; private final TransactionTemplate independent;
    private final int loginLimit,sourceLimit,denialLimit,mfaLimit,eventDays,auditDays;
    @Value("${investsmart.security.trusted-proxies:}") private String trustedProxies="";
    public SecurityEvents(JdbcTemplate db,SecurityCrypto crypto,SecuritySchema schema,PlatformTransactionManager tx,
        @Value("${investsmart.security.login-failures:5}") int loginLimit,@Value("${investsmart.security.source-failures:30}") int sourceLimit,
        @Value("${investsmart.security.access-denials:5}") int denialLimit,@Value("${investsmart.security.mfa-failures:5}") int mfaLimit,
        @Value("${investsmart.security.event-retention-days:90}") int eventDays,@Value("${investsmart.security.audit-retention-days:365}") int auditDays){
        this.db=db;this.crypto=crypto;this.loginLimit=loginLimit;this.sourceLimit=sourceLimit;this.denialLimit=denialLimit;this.mfaLimit=mfaLimit;this.eventDays=eventDays;this.auditDays=auditDays;
        independent=new TransactionTemplate(tx);independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public String subject(String value){return crypto.fingerprint(value==null?"anonymous":value.trim().toLowerCase(Locale.ROOT));}
    private String address(HttpServletRequest request){String remote=request.getRemoteAddr();if(Arrays.stream(trustedProxies.split(",")).map(String::trim).anyMatch(ip->!ip.isBlank()&&ip.equals(remote))){String header=request.getHeader("X-Forwarded-For");if(header!=null){String ip=header.split(",")[0].trim();if(ip.matches("[0-9a-fA-F:.]{3,45}"))return ip;}}return remote;}
    public String source(HttpServletRequest request){return subject(address(request));}
    public String masked(HttpServletRequest request){String ip=address(request);if(ip==null)return "Unknown";if(ip.contains(":"))return ip.substring(0,Math.min(9,ip.length()))+"…";int dot=ip.lastIndexOf('.');return dot<0?"Unknown":ip.substring(0,dot)+".*";}
    public void record(String type,Long userId,String identity,HttpServletRequest request,String details){
        independent.executeWithoutResult(status->{
            db.queryForObject("SELECT id FROM security_mutex WHERE id=1 FOR UPDATE",Integer.class);
            Instant now=Instant.now();String key=subject(identity),src=source(request);
            db.update("INSERT INTO security_events(occurred_at,event_type,user_id,subject_key,source_key,source_label,route,details) VALUES(?,?,?,?,?,?,?,?)",Timestamp.from(now),type,userId,key,src,masked(request),bounded(request.getRequestURI(),160),bounded(details,300));
            if("LOGIN_FAILURE".equals(type)){
                int n=count(type,"subject_key",key,900);if(n>=loginLimit)alert("Repeated login failures",key,userId,"MEDIUM",n,now);
                int all=count(type,"source_key",src,900);if(all>=sourceLimit)alert("Login failures from one source",src,null,"HIGH",all,now);
            }
            if("ACCESS_DENIED".equals(type)){int n=count(type,"subject_key",key,600);if(n>=denialLimit)alert("Repeated access denials",key,userId,"HIGH",n,now);}
            if("MFA_FAILURE".equals(type)){int n=count(type,"subject_key",key,600);if(n>=mfaLimit)alert("Repeated MFA failures",key,userId,"HIGH",n,now);}
            if("REQUEST_RESET".equals(type)||"REQUEST_VERIFY".equals(type)){int n=count(type,"subject_key",key,900);if(n>=3)alert("Repeated account recovery requests",key,userId,"MEDIUM",n,now);}
            if("ORDER_REJECTED".equals(type)){int n=count(type,"subject_key",key,900);if(n>=5)alert("Repeated rejected orders",key,userId,"HIGH",n,now);}
            if("ADMIN_ACTION".equals(type)){int n=count(type,"subject_key",key,600);if(n>=5)alert("Unusual administrator activity",key,userId,"HIGH",n,now);}
        });
    }
    private int count(String type,String column,String key,int seconds){return db.queryForObject("SELECT COUNT(*) FROM security_events WHERE event_type=? AND "+column+"=? AND occurred_at>?",Integer.class,type,key,Timestamp.from(Instant.now().minusSeconds(seconds)));}
    private void alert(String rule,String key,Long userId,String severity,int count,Instant now){
        var ids=db.queryForList("SELECT id FROM security_alerts WHERE rule_name=? AND subject_key=? AND last_at>? ORDER BY id DESC LIMIT 1",Long.class,rule,key,Timestamp.from(now.minusSeconds(1800)));
        if(ids.isEmpty())db.update("INSERT INTO security_alerts(rule_name,subject_key,user_id,severity,status,occurrences,first_at,last_at,seen) VALUES(?,?,?,?,'OPEN',?,?,?,FALSE)",rule,key,userId,severity,count,Timestamp.from(now),Timestamp.from(now));
        else db.update("UPDATE security_alerts SET occurrences=?,last_at=?,seen=FALSE,status=CASE WHEN status='RESOLVED' THEN 'OPEN' ELSE status END WHERE id=?",count,Timestamp.from(now),ids.getFirst());
    }
    public void loginAllowed(String email,HttpServletRequest request){
        if(count("LOGIN_FAILURE","subject_key",subject(email),60)>=loginLimit||count("LOGIN_FAILURE","source_key",source(request),60)>=sourceLimit)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many attempts. Try again in 1 minute.");
    }
    public void mfaAllowed(Long id){if(count("MFA_FAILURE","subject_key",subject("mfa:"+id),600)>=mfaLimit)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many authenticator attempts. Try again in 10 minutes.");}
    public void limit(String action,String email,HttpServletRequest request,int perIdentity,int perSource){limit(action,email,request,perIdentity,perSource,null);}
    public void limit(String action,String email,HttpServletRequest request,int perIdentity,int perSource,Long userId){
        String type="REQUEST_"+action;
        Instant now=Instant.now();
        int wait=Math.max(requestWait(type,"subject_key",subject(email),perIdentity,now),requestWait(type,"source_key",source(request),perSource,now));
        if(wait>0)throw new com.example.investsmart.exception.RequestCooldownException("PASSKEY_REGISTER".equals(action)?"passkey setup":action.toLowerCase(Locale.ROOT).replace('_',' '),wait);
        record(type,userId,email,request,"");
    }
    private int requestWait(String type,String column,String key,int maximum,Instant now){
        var times=db.queryForList("SELECT occurred_at FROM security_events WHERE event_type=? AND "+column+"=? AND occurred_at>? ORDER BY occurred_at DESC LIMIT ?",Timestamp.class,type,key,Timestamp.from(now.minusSeconds(900)),maximum);
        if(times.size()<maximum)return 0;
        return Math.max(1,(int)Math.ceil(Duration.between(now,times.getLast().toInstant().plusSeconds(900)).toMillis()/1000.0));
    }
    public void audit(Long actor,Long target,String action,String reason){db.update("INSERT INTO admin_audit(occurred_at,actor_id,target_id,action_name,reason) VALUES(?,?,?,?,?)",Timestamp.from(Instant.now()),actor,target,action,bounded(reason,1200));}
    @Transactional public int archiveStaleAlerts(){
        Instant cutoff=Instant.now().minus(Duration.ofDays(1));
        var stale=db.queryForList("SELECT id,user_id,rule_name,severity,occurrences,subject_key FROM security_alerts WHERE status<>'RESOLVED' AND last_at<? FOR UPDATE",Timestamp.from(cutoff));
        int archived=0;
        for(var alert:stale){
            Long id=((Number)alert.get("id")).longValue();
            int updated=db.update("UPDATE security_alerts SET status='RESOLVED',seen=TRUE,notes=CASE WHEN notes IS NULL OR notes='' THEN 'Automatically moved to logs after 24 hours without new activity.' ELSE notes END WHERE id=? AND status<>'RESOLVED' AND last_at<?",id,Timestamp.from(cutoff));
            if(updated==0)continue;
            Long userId=alert.get("user_id")==null?null:((Number)alert.get("user_id")).longValue();
            String email=userId==null?null:db.query("SELECT email FROM users WHERE id=?",(rs,row)->rs.getString(1),userId).stream().findFirst().orElse(null);
            String details="Alert #"+id+" ("+alert.get("rule_name")+", "+alert.get("severity")+", "+alert.get("occurrences")+" occurrences) automatically archived after 24 hours without new activity; account email: "+(email==null||email.isBlank()?"unavailable / source-wide":email)+".";
            db.update("INSERT INTO security_events(occurred_at,event_type,user_id,subject_key,source_key,source_label,route,details) VALUES(?,?,?,?,?,?,?,?)",Timestamp.from(Instant.now()),"ALERT_AUTO_ARCHIVED",userId,alert.get("subject_key"),subject("investsmart-system"),"System","/admin/alerts",bounded(details,300));
            archived++;
        }
        return archived;
    }
    @Scheduled(cron="0 0 * * * *") @Transactional public void archiveStaleAlertsHourly(){archiveStaleAlerts();}
    public static String bounded(String value,int max){if(value==null)return "";return value.replaceAll("[\\p{Cntrl}]"," ").substring(0,Math.min(max,value.length()));}
    @Scheduled(cron="0 30 3 * * *") public void expire(){independent.executeWithoutResult(status->{db.update("DELETE FROM security_events WHERE occurred_at<?",Timestamp.from(Instant.now().minus(Duration.ofDays(eventDays))));db.update("DELETE FROM admin_audit WHERE occurred_at<?",Timestamp.from(Instant.now().minus(Duration.ofDays(auditDays))));db.update("DELETE FROM auth_actions WHERE expires_at<?",Timestamp.from(Instant.now().minus(Duration.ofDays(1))));db.update("DELETE FROM passkey_challenges WHERE expires_at<?",Timestamp.from(Instant.now().minus(Duration.ofDays(1))));db.update("DELETE FROM trusted_device_locks WHERE NOT EXISTS(SELECT 1 FROM auth_sessions s WHERE s.id=trusted_device_locks.session_id AND s.revoked=FALSE AND s.expires_at>?)",Timestamp.from(Instant.now()));db.update("DELETE FROM auth_sessions WHERE expires_at<?",Timestamp.from(Instant.now().minus(Duration.ofDays(1))));});}
}

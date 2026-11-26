package com.example.investsmart.security;

import java.sql.Timestamp;
import java.time.Instant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountMail {
    private final JdbcTemplate db;private final SecurityCrypto crypto;private final ObjectProvider<JavaMailSender> sender;private final String base,from,smtpHost,smtpUsername,smtpPassword;private final int smtpPort;
    public AccountMail(JdbcTemplate db,SecurityCrypto crypto,ObjectProvider<JavaMailSender> sender,SecuritySchema schema,@Value("${investsmart.public-url:http://localhost:5173}") String base,@Value("${investsmart.mail.from:no-reply@example.invalid}") String from,@Value("${spring.mail.host:}") String smtpHost,@Value("${spring.mail.port:587}") int smtpPort,@Value("${spring.mail.username:}") String smtpUsername,@Value("${spring.mail.password:}") String smtpPassword){this.db=db;this.crypto=crypto;this.sender=sender;this.base=DevelopmentOrigin.canonical(base).replaceAll("/+$","");this.from=from;this.smtpHost=smtpHost;this.smtpPort=smtpPort;this.smtpUsername=smtpUsername;this.smtpPassword=smtpPassword;}
    @Transactional public long queue(String email,String purpose,String token){
        if(!java.util.List.of("VERIFY","RESET","PHONE_PAIR").contains(purpose))throw new IllegalArgumentException("Unsupported account mail purpose.");
        String path="PHONE_PAIR".equals(purpose)?"phone#pair":"VERIFY".equals(purpose)?"verify-email":"reset-password";
        String subject="PHONE_PAIR".equals(purpose)?"Pair your InvestSmart phone":"VERIFY".equals(purpose)?"Verify your InvestSmart email":"Reset your InvestSmart password";
        String body=subject+"\n\n"+base+"/"+path+("PHONE_PAIR".equals(purpose)?"=":"?token=")+URLEncoder.encode(token,StandardCharsets.UTF_8)+"\n\nIf you did not request this, ignore this message.";
        long id=queueMessage(email,subject,body);
        db.update("INSERT INTO mail_action_links(mail_id,token_hash,purpose) VALUES(?,?,?)",id,crypto.fingerprint(token),purpose);
        return id;
    }
    public long queueAccountDeletion(String email,String name,String reason){
        String recipientName=SecurityEvents.bounded(name,80).trim();
        String providedReason=SecurityEvents.bounded(reason,1000).trim();
        String explanation=providedReason.isEmpty()
                ?"This action was completed by an authorized administrator as part of account administration."
                :"Reason provided: "+providedReason;
        String body="Hello "+(recipientName.isEmpty()?"InvestSmart user":recipientName)+",\n\n"
                +"This message confirms that your InvestSmart account has been permanently deleted by an administrator. "
                +"You can no longer sign in to this account, and its account data has been removed.\n\n"
                +explanation+"\n\n"
                +"If you believe this action was taken in error, please contact InvestSmart support.\n\n"
                +"Regards,\nInvestSmart Support";
        return queueMessage(email,"Your InvestSmart account has been deleted",body);
    }
    private long queueMessage(String email,String subject,String body){
        db.update("DELETE FROM mail_delivery_failures WHERE mail_id IN (SELECT id FROM mail_outbox WHERE recipient=? AND subject_line=? AND sent=FALSE)",email,subject);
        db.update("DELETE FROM mail_outbox WHERE recipient=? AND subject_line=? AND sent=FALSE",email,subject);
        var keys=new GeneratedKeyHolder();
        db.update(connection->{var statement=connection.prepareStatement("INSERT INTO mail_outbox(recipient,subject_line,body_cipher,created_at) VALUES(?,?,?,?)",PreparedStatement.RETURN_GENERATED_KEYS);statement.setString(1,email);statement.setString(2,subject);statement.setString(3,crypto.encrypt(body));statement.setTimestamp(4,Timestamp.from(Instant.now()));return statement;},keys);
        if(keys.getKey()==null)throw new IllegalStateException("Could not queue account email.");
        return keys.getKey().longValue();
    }
    @Scheduled(fixedDelay=30000) public void deliver(){
        for(var row:db.queryForList("SELECT id FROM mail_outbox WHERE sent=FALSE AND attempts<5 ORDER BY id LIMIT 10"))deliver(((Number)row.get("id")).longValue());
        db.update("DELETE FROM mail_outbox WHERE created_at<?",Timestamp.from(Instant.now().minusSeconds(86400)));
        db.update("DELETE FROM mail_delivery_failures WHERE mail_id NOT IN (SELECT id FROM mail_outbox)");
    }
    public void deliver(long id){
        var rows=db.queryForList("SELECT * FROM mail_outbox WHERE id=? AND sent=FALSE AND attempts<5",id);
        if(rows.isEmpty())return;
        var row=rows.getFirst();
        var links=db.queryForList("SELECT token_hash,purpose FROM mail_action_links WHERE mail_id=?",id);
        boolean actionSubject=java.util.List.of("Verify your InvestSmart email","Reset your InvestSmart password","Pair your InvestSmart phone").contains(row.get("subject_line"));
        if(!links.isEmpty() || actionSubject){
            // Legacy action messages without a link cannot establish validity; request a fresh message.
            boolean valid=false;
            if(!links.isEmpty()){
                var link=links.getFirst();
                valid=db.queryForObject("SELECT COUNT(*) FROM auth_actions a JOIN users u ON u.id=a.user_id WHERE a.token_hash=? AND a.purpose=? AND a.used=FALSE AND a.expires_at>? AND u.email=? AND u.account_status='ACTIVE' AND (a.purpose IN ('RESET','PHONE_PAIR') OR (a.purpose='VERIFY' AND u.email_verified=FALSE))",Integer.class,link.get("token_hash"),link.get("purpose"),Timestamp.from(Instant.now()),row.get("recipient"))>0;
            }
            if(!valid){
                db.update("DELETE FROM mail_delivery_failures WHERE mail_id=?",id);
                db.update("DELETE FROM mail_outbox WHERE id=?",id);
                return;
            }
        }
        JavaMailSender mailer=sender.getIfAvailable();
        if(!configured()||mailer==null){recordFailure(id,"SMTP_NOT_CONFIGURED");return;}
        try{
            var message=new SimpleMailMessage();message.setFrom(from);message.setTo((String)row.get("recipient"));message.setSubject((String)row.get("subject_line"));message.setText(crypto.decrypt((String)row.get("body_cipher")));mailer.send(message);
            db.update("UPDATE mail_outbox SET sent=TRUE,body_cipher='' WHERE id=?",id);
            db.update("DELETE FROM mail_delivery_failures WHERE mail_id=?",id);
        }catch(MailException|IllegalStateException failure){recordFailure(id,"SMTP_DELIVERY_FAILED");}
    }
    private void recordFailure(long id,String code){
        db.update("UPDATE mail_outbox SET attempts=attempts+1 WHERE id=?",id);
        db.update("DELETE FROM mail_delivery_failures WHERE mail_id=?",id);
        db.update("INSERT INTO mail_delivery_failures(mail_id,error_code,recorded_at) VALUES(?,?,?)",id,code,Timestamp.from(Instant.now()));
    }
    public Map<String,Object> status(){
        boolean configured=configured();
        Integer failures=db.queryForObject("SELECT COUNT(*) FROM mail_outbox m JOIN mail_delivery_failures f ON f.mail_id=m.id WHERE m.sent=FALSE",Integer.class);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("configured",configured);
        result.put("deliveryIssue",!configured||failures>0);
        result.put("message",!configured
            ?"Verification email delivery is not configured. Ask the operator to set SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD and MAIL_FROM, and confirm PUBLIC_URL is the HTTPS frontend origin. Your account remains unverified; resend after the mail service is configured."
            :failures>0
                ?"The email service has not accepted some account messages. Ask the operator to check SMTP host, port, authentication, STARTTLS and sender settings. Do not share credentials; request a fresh verification link after correction."
                :"No pending delivery failures are recorded. Inbox receipt has not been independently confirmed; check spam and request a fresh link if needed.");
        return result;
    }
    public Map<String,Object> adminStatus(){
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("configured",configured());
        result.put("messages",db.query("SELECT m.id,m.recipient,m.subject_line,m.created_at,m.attempts,m.sent,f.error_code,f.recorded_at AS failed_at FROM mail_outbox m LEFT JOIN mail_delivery_failures f ON f.mail_id=m.id ORDER BY m.id DESC LIMIT 50",(rs,n)->{
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("id",rs.getLong("id"));row.put("recipient",rs.getString("recipient"));row.put("subject_line",rs.getString("subject_line"));row.put("created_at",rs.getTimestamp("created_at").toInstant());row.put("attempts",rs.getInt("attempts"));row.put("sent",rs.getBoolean("sent"));row.put("error_code",rs.getString("error_code"));var failed=rs.getTimestamp("failed_at");row.put("failed_at",failed==null?null:failed.toInstant());return row;
        }));
        result.put("pending",db.queryForObject("SELECT COUNT(*) FROM mail_outbox WHERE sent=FALSE",Integer.class));
        result.put("failed",db.queryForObject("SELECT COUNT(*) FROM mail_delivery_failures",Integer.class));
        return result;
    }
    private boolean configured(){return sender.getIfAvailable()!=null&&!smtpHost.isBlank()&&smtpPort>0&&smtpPort<=65535&&!smtpUsername.isBlank()&&!smtpPassword.isBlank()&&!from.isBlank()&&from.contains("@");}
}

package com.example.investsmart.settlement;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementCalendar {
    public static final ZoneId INDIA=ZoneId.of("Asia/Kolkata");
    public record Result(String status,LocalDate expectedDate,Instant simulationCutoff,String versionId,String detail) {}
    public record Coverage(String exchange,String segment,String versionId,LocalDate firstDate,LocalDate lastDate,String source,String checksum) {}
    private final JdbcTemplate db;private final Clock clock;
    public SettlementCalendar(JdbcTemplate db,Clock clock){this.db=db;this.clock=clock;}
    @Transactional public String activate(ReviewedCalendar calendar,String operator,String reason){
        if(operator==null||operator.isBlank()||operator.length()>100||reason==null||reason.trim().length()<5||reason.length()>300||operator.contains("\n")||reason.contains("\n"))throw new IllegalArgumentException("A bounded operator label and review reason of at least five characters are required.");
        db.queryForObject("SELECT id FROM settlement_calendar_mutex WHERE id=1 FOR UPDATE",Integer.class);
        var existing=db.queryForList("SELECT id FROM settlement_calendar_versions WHERE exchange_code=? AND segment_code=? AND content_sha256=?",String.class,calendar.exchange(),calendar.segment(),calendar.checksum());
        String id;
        if(existing.isEmpty()){
            id=UUID.randomUUID().toString();db.update("INSERT INTO settlement_calendar_versions(id,exchange_code,segment_code,first_date,last_date,source_url,content_sha256,imported_at) VALUES(?,?,?,?,?,?,?,?)",id,calendar.exchange(),calendar.segment(),calendar.days().getFirst().date(),calendar.days().getLast().date(),calendar.source(),calendar.checksum(),Timestamp.from(clock.instant()));
            db.batchUpdate("INSERT INTO settlement_calendar_days(version_id,calendar_date,trading_open,settlement_open,note) VALUES(?,?,?,?,?)",calendar.days(),250,(statement,day)->{statement.setString(1,id);statement.setDate(2,java.sql.Date.valueOf(day.date()));statement.setBoolean(3,day.tradingOpen());statement.setBoolean(4,day.settlementOpen());statement.setString(5,day.note());});
        }else id=existing.getFirst();
        var active=db.queryForList("SELECT version_id FROM settlement_calendar_active WHERE exchange_code=? AND segment_code=?",String.class,calendar.exchange(),calendar.segment());
        if(!active.isEmpty()&&active.getFirst().equals(id))return id;
        db.update("INSERT INTO settlement_calendar_active(exchange_code,segment_code,version_id) VALUES(?,?,?) ON DUPLICATE KEY UPDATE version_id=VALUES(version_id)",calendar.exchange(),calendar.segment(),id);
        db.update("INSERT INTO settlement_calendar_audit(id,version_id,activated_at,operator_label,reason) VALUES(?,?,?,?,?)",UUID.randomUUID().toString(),id,Timestamp.from(clock.instant()),operator,reason.trim());return id;
    }
    public Optional<Coverage> coverage(String exchange,String segment){
        var rows=db.query("SELECT v.id,v.first_date,v.last_date,v.source_url,v.content_sha256 FROM settlement_calendar_active a JOIN settlement_calendar_versions v ON v.id=a.version_id WHERE a.exchange_code=? AND a.segment_code=?",(rs,n)->new Coverage(exchange,segment,rs.getString(1),rs.getDate(2).toLocalDate(),rs.getDate(3).toLocalDate(),rs.getString(4),rs.getString(5)),exchange,segment);return rows.stream().findFirst();
    }
    @Transactional(readOnly=true) public Result next(String exchange,String segment,LocalDate tradeDate){
        Objects.requireNonNull(tradeDate,"Trade date required");var coverage=coverage(exchange,segment);
        if(coverage.isEmpty()||tradeDate.isBefore(coverage.get().firstDate())||tradeDate.isAfter(coverage.get().lastDate()))return new Result("COVERAGE_MISSING",null,null,null,"Reviewed settlement-calendar coverage is unavailable; keep the obligation pending.");
        String id=coverage.get().versionId();
        Boolean open=db.queryForObject("SELECT trading_open FROM settlement_calendar_days WHERE version_id=? AND calendar_date=?",Boolean.class,id,tradeDate);
        if(!Boolean.TRUE.equals(open))return new Result("TRADING_CLOSED",null,null,id,"The reviewed calendar marks this trade date closed.");
        var dates=db.query("SELECT calendar_date FROM settlement_calendar_days WHERE version_id=? AND calendar_date>? AND settlement_open=TRUE ORDER BY calendar_date",(rs,n)->rs.getDate(1).toLocalDate(),id,tradeDate);
        if(dates.isEmpty())return new Result("COVERAGE_MISSING",null,null,id,"No next settlement day is covered; do not guess a date.");
        LocalDate due=dates.getFirst();return new Result("READY",due,due.atTime(18,0).atZone(INDIA).toInstant(),id,"18:00 IST is a disclosed simulation cutoff, not a guaranteed broker payout time.");
    }
}

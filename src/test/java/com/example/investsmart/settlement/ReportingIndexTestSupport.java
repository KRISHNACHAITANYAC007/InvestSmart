package com.example.investsmart.settlement;

import com.example.investsmart.deployment.MigrationLedger;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.*;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic sources supplied only by the opt-in container or in-memory test fixtures. */
final class ReportingIndexTestSupport {
    static final String MIGRATION="20261127_reporting_period_indexes.sql";
    static final Map<String,List<String>> EXPECTED=Map.of(
        "ix_finance_event_user_time",List.of("user_id","occurred_at","event_key"),
        "ix_paper_fill_user_time",List.of("user_id","created_at","id"));
    static Map<String,List<String>> indexes(DataSource source)throws Exception{
        var result=new TreeMap<String,List<String>>();
        try(var connection=source.getConnection()){
            boolean h2=connection.getMetaData().getDatabaseProductName().equals("H2");
            for(String table:List.of("paper_finance_events","paper_fills")){
                var ordered=new TreeMap<String,TreeMap<Integer,String>>();
                try(var rows=connection.getMetaData().getIndexInfo(connection.getCatalog(),null,h2?table.toUpperCase(Locale.ROOT):table,false,false)){
                    while(rows.next()){
                        String name=rows.getString("INDEX_NAME");if(name==null)continue;name=name.toLowerCase(Locale.ROOT);
                        if(!EXPECTED.containsKey(name))continue;
                        assertTrue(rows.getBoolean("NON_UNIQUE"),"Reporting indexes must remain nonunique");
                        ordered.computeIfAbsent(name,key->new TreeMap<>()).put(rows.getInt("ORDINAL_POSITION"),rows.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                    }
                }
                ordered.forEach((key,columns)->result.put(key,List.copyOf(columns.values())));
            }
        }
        return result;
    }
    static void apply(DataSource source)throws Exception{
        try(var connection=source.getConnection()){
            MigrationLedger.run(connection,new String[]{"--apply",MIGRATION,"--backup-confirmed","--writes-stopped"});
        }
    }
    static void assertApplied(DataSource source)throws Exception{
        assertEquals(EXPECTED,indexes(source));var db=new JdbcTemplate(source);
        assertEquals("APPLIED",db.queryForObject("SELECT state FROM investsmart_migrations WHERE version_name=?",String.class,MIGRATION));
        String sql=Files.readString(Path.of("src/main/resources/db/migrations",MIGRATION)).replace("\r\n","\n");
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(sql.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals(hash,db.queryForObject("SELECT checksum_sha256 FROM investsmart_migrations WHERE version_name=?",String.class,MIGRATION));
    }
    static Map<String,Object> rows(JdbcTemplate db){
        var result=new TreeMap<String,Object>();
        for(String table:List.of("users","holdings","paper_tickets","paper_fills","paper_financing","paper_finance_events","paper_intraday_positions","paper_intraday_events","settlement_accounts","settlement_obligations","settlement_allocations","settlement_allocation_events","settlement_journal"))result.put(table,db.queryForList("SELECT * FROM "+table+" ORDER BY 1,2"));
        return result;
    }
    static List<Object> reports(JournalReportFixture f){
        var from=JournalReportFixture.AT;var to=from.plusSeconds(1);String email=f.owner.getEmail();
        return List.of(f.context.getBean(JournalPeriodService.class).read(email,from,to),f.context.getBean(FinancePeriodService.class).read(email,from,to),f.context.getBean(ExecutionFeePeriodService.class).read(email,from,to),f.context.getBean(PendingPolicyPeriodService.class).read(email,from,to));
    }
    static void seed(JournalReportFixture f){
        f.account(1);var at=Timestamp.from(JournalReportFixture.AT);
        f.db.update("INSERT INTO paper_financing(id,user_id,stock_id,product,quantity,cost_amount,principal,interest,interest_rate,created_at,last_accrued_date) VALUES('kept-position',1,1,'MTF',0,100,75,9,0.1,?,'2026-11-23')",at);
        f.db.update("INSERT INTO paper_finance_events VALUES('kept-interest','kept-position',1,'INTEREST',0,2.50,?)",at);
        f.db.update("INSERT INTO paper_tickets(id,user_id,request_id,side,product,variety,order_kind,validity,quantity,filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,policy_version,expires_at,created_at) VALUES('kept-ticket',1,'kept-ticket','BUY','CASH','REGULAR','MARKET','DAY',1,1,1,1,1,10,0,'FILLED',1,'test',?,?)",at,at);
        f.db.update("INSERT INTO paper_fills VALUES('kept-fill','kept-ticket',1,0,1,10,1,NULL,?)",at);
        f.db.update("INSERT INTO settlement_allocations VALUES('kept-reservation',1,'ORDER','kept-order','SETTLED',8,8,'OPEN',?)",at);
        f.entry(1,"kept-reserve","RESERVE","8",JournalReportFixture.AT);
        f.db.update("UPDATE settlement_journal SET reference_kind='ORDER',reference_id='kept-order' WHERE event_key='kept-reserve'");
        f.db.update("INSERT INTO settlement_allocation_events VALUES(1,'kept-reserve','kept-reservation','RESERVE',8)");
    }
    static void verifyUpgrade(DataSource source)throws Exception{
        try(var fixture=new JournalReportFixture(source,true)){
            seed(fixture);assertTrue(indexes(source).isEmpty());var before=rows(fixture.db);var report=reports(fixture);
            apply(source);assertApplied(source);assertEquals(before,rows(fixture.db));assertEquals(report,reports(fixture));
            // A new operator connection consults the persisted ledger and skips the exact migration.
            apply(source);assertApplied(source);assertEquals(before,rows(fixture.db));assertEquals(report,reports(fixture));
            assertEquals(1,fixture.db.queryForObject("SELECT COUNT(*) FROM investsmart_migrations",Integer.class));
        }
    }
    static void fresh(DataSource source,boolean runtime)throws Exception{
        new ResourceDatabasePopulator(new FileSystemResource("deploy/mysql-init/001-app-schema.sql"),runtime?new ClassPathResource("db/security-schema.sql"):new FileSystemResource("deploy/mysql-init/002-security-schema.sql")).execute(source);
        assertEquals(EXPECTED,indexes(source));
    }
}

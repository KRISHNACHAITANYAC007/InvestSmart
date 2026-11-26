package com.example.investsmart.settlement;

import com.example.investsmart.dto.SettlementJournalPage;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PeriodChargeRepository {
    private final NamedParameterJdbcTemplate db;
    public PeriodChargeRepository(JdbcTemplate db){this.db=new NamedParameterJdbcTemplate(db);}
    List<SettlementJournalPage.Entry> children(long user,List<String> references,int limit){
        if(references.size()>100||limit<1||limit>1001)throw new IllegalArgumentException("Invalid charge batch limit.");
        if(references.isEmpty())return List.of();
        var keys=new ArrayList<String>();for(var reference:references){keys.add("CHARGE_RESERVE:"+reference);keys.add("CHARGE_SPEND:"+reference);}
        return db.query("""
            SELECT event_key,action,amount,reference_kind,reference_id,occurred_at FROM settlement_journal
            WHERE user_id=:user AND ((reference_kind='CHARGE' AND reference_id IN (:references)) OR event_key IN (:keys))
            ORDER BY event_key LIMIT :limit
            """,Map.of("user",user,"references",references,"keys",keys,"limit",limit),
            (r,n)->new SettlementJournalPage.Entry(r.getString("event_key"),r.getString("action"),r.getBigDecimal("amount"),r.getString("reference_kind"),r.getString("reference_id"),r.getTimestamp("occurred_at").toInstant()));
    }
}

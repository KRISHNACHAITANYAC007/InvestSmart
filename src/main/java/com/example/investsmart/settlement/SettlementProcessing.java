package com.example.investsmart.settlement;

import java.time.Clock;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import com.example.investsmart.repository.UserRepository;

@Service
public class SettlementProcessing {
    private final JdbcTemplate db;
    private final UserRepository users;
    private final SettlementFunds funds;
    private final SettlementAccounting accounting;
    private final Clock clock;
    public SettlementProcessing(JdbcTemplate db,UserRepository users,SettlementFunds funds,SettlementAccounting accounting,Clock clock){
        this.db=db;this.users=users;this.funds=funds;this.accounting=accounting;this.clock=clock;
    }
    public List<String> candidates(){
        return db.queryForList("SELECT o.id FROM settlement_obligations o LEFT JOIN settlement_checks c ON c.obligation_id=o.id WHERE o.status='PENDING' ORDER BY CASE WHEN o.cutoff_at IS NULL OR o.cutoff_at<=? THEN 0 ELSE 1 END,CASE WHEN c.obligation_id IS NULL THEN 0 ELSE 1 END,COALESCE(c.last_checked_at,o.created_at),o.id LIMIT 20",String.class,Timestamp.from(clock.instant()));
    }
    /** Independent observation survives an unsuccessful financial transaction and prevents starvation. */
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void touch(String id){
        var rows=db.queryForList("SELECT user_id FROM settlement_obligations WHERE id=? AND status='PENDING'",Long.class,id);
        if(!rows.isEmpty())db.update("INSERT INTO settlement_checks(obligation_id,user_id,last_checked_at) VALUES(?,?,?) ON DUPLICATE KEY UPDATE last_checked_at=VALUES(last_checked_at)",id,rows.getFirst(),Timestamp.from(clock.instant()));
    }
    @Transactional public boolean process(String id){
        var ids=db.queryForList("SELECT user_id FROM settlement_obligations WHERE id=?",Long.class,id);
        if(ids.isEmpty())return false;
        var user=users.findByIdForUpdate(ids.getFirst()).orElseThrow();
        // Do not initialize an account from a background obligation if the opening marker is missing.
        if(db.queryForObject("SELECT COUNT(*) FROM settlement_accounts WHERE user_id=?",Integer.class,user.getId())!=1)throw new IllegalStateException("Settlement opening account is missing.");
        boolean settled=funds.settle(user.getId(),id);
        accounting.project(user);
        return settled;
    }
}

package com.example.investsmart.settlement;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.investsmart.repository.UserRepository;

@Service
@Transactional(readOnly=true)
public class SettlementStatus {
    public record Summary(BigDecimal settled,BigDecimal reusable,BigDecimal restricted,BigDecimal reserved,BigDecimal purchasable,BigDecimal withdrawable,boolean initialized,String simulationCutoff){}
    public record Entry(String id,String kind,String policy,BigDecimal amount,BigDecimal remainingCredit,String status,LocalDate tradeDate,LocalDate expectedDate,Instant cutoffAt,Instant settledAt,String calendarVersion){}
    public record HoldingUnits(long stockId,long settledUnits,long t1Units){}
    private final JdbcTemplate db;private final UserRepository users;
    public SettlementStatus(JdbcTemplate db,UserRepository users){this.db=db;this.users=users;}
    public long user(String email){return users.findByEmailIgnoreCase(email).orElseThrow().getId();}
    public Summary summary(long user){
        var accounts=db.queryForList("SELECT settled_cash FROM settlement_accounts WHERE user_id=?",BigDecimal.class,user);
        boolean initialized=!accounts.isEmpty();
        BigDecimal settled=initialized?accounts.getFirst():users.findById(user).orElseThrow().getVirtualBalance();
        BigDecimal reusable=sum("SELECT COALESCE(SUM(remaining_credit),0) FROM settlement_obligations WHERE user_id=? AND status='PENDING' AND credit_policy='REUSABLE'",user);
        BigDecimal restricted=sum("SELECT COALESCE(SUM(remaining_credit),0) FROM settlement_obligations WHERE user_id=? AND status='PENDING' AND credit_policy='RESTRICTED'",user);
        BigDecimal reserved;
        if(initialized)reserved=sum("SELECT COALESCE(SUM(remaining_amount),0) FROM settlement_allocations WHERE user_id=?",user);
        else reserved=sum("SELECT COALESCE(SUM(reserved_cash),0) FROM equity_pending_orders WHERE user_id=? AND status='PENDING'",user)
                .add(sum("SELECT COALESCE(SUM(reserved_cash),0) FROM option_orders WHERE user_id=? AND status='PENDING'",user))
                .add(sum("SELECT COALESCE(SUM(reserved_cash),0) FROM paper_tickets WHERE user_id=? AND status IN ('PENDING','TRIGGERED','PART_FILLED')",user))
                .add(sum("SELECT COALESCE(SUM(reserved_amount),0) FROM ipo_applications WHERE user_id=? AND status IN ('PENDING','AWAITING_DATA')",user));
        BigDecimal zero=new BigDecimal("0.00");
        return new Summary(settled,reusable,restricted,reserved,settled==null?null:settled.signum()<0?zero:settled.add(reusable),settled==null?null:settled.max(zero),initialized,"18:00 Asia/Kolkata on reviewed T+1 settlement day; simulation only");
    }
    public List<Entry> history(long user,int page){
        if(page<1||page>100000)throw new com.example.investsmart.exception.BadRequestException("Invalid settlement history page.");
        return db.query("SELECT id,obligation_kind,credit_policy,amount,remaining_credit,status,trade_date,expected_date,cutoff_at,settled_at,calendar_version FROM settlement_obligations WHERE user_id=? ORDER BY created_at DESC,id LIMIT 50 OFFSET ?",(rs,n)->new Entry(rs.getString(1),rs.getString(2),rs.getString(3),rs.getBigDecimal(4),rs.getBigDecimal(5),rs.getString(6),rs.getDate(7).toLocalDate(),rs.getDate(8)==null?null:rs.getDate(8).toLocalDate(),rs.getTimestamp(9)==null?null:rs.getTimestamp(9).toInstant(),rs.getTimestamp(10)==null?null:rs.getTimestamp(10).toInstant(),rs.getString(11)),user,(page-1)*50);
    }
    public List<HoldingUnits> holdings(long user){
        boolean initialized=db.queryForObject("SELECT COUNT(*) FROM settlement_accounts WHERE user_id=?",Integer.class,user)>0;
        if(!initialized)return db.query("SELECT stock_id,SUM(quantity) FROM holdings WHERE user_id=? GROUP BY stock_id",(rs,n)->new HoldingUnits(rs.getLong(1),rs.getLong(2),0),user);
        return db.query("SELECT stock_id,SUM(CASE WHEN settled=TRUE THEN remaining_units ELSE 0 END),SUM(CASE WHEN settled=FALSE THEN remaining_units ELSE 0 END) FROM settlement_lots WHERE user_id=? GROUP BY stock_id",(rs,n)->new HoldingUnits(rs.getLong(1),rs.getLong(2),rs.getLong(3)),user);
    }
    private BigDecimal sum(String sql,long user){return db.queryForObject(sql,BigDecimal.class,user).setScale(2);}
}

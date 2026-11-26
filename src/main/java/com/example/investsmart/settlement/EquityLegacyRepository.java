package com.example.investsmart.settlement;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EquityLegacyRepository {
    record Fill(int units,BigDecimal price,String legacyId,Long stock,String product,String side) {}
    record Legacy(long stock,String side,int units,BigDecimal price,BigDecimal total,String status) {}
    private final JdbcTemplate db;
    public EquityLegacyRepository(JdbcTemplate db){this.db=db;}
    Fill ownedFill(long user,String id){
        var rows=db.query("""
                SELECT f.quantity,f.price,f.legacy_order_id,t.stock_id,t.product,t.side
                FROM paper_fills f JOIN paper_tickets t ON t.id=f.ticket_id AND t.user_id=f.user_id
                WHERE f.id=? AND f.user_id=?
                """,(rs,row)->new Fill(rs.getInt("quantity"),rs.getBigDecimal("price"),rs.getString("legacy_order_id"),
                rs.getObject("stock_id")==null?null:((Number)rs.getObject("stock_id")).longValue(),rs.getString("product"),rs.getString("side")),id,user);
        return rows.isEmpty()?null:rows.getFirst();
    }
    Legacy ownedOrder(long user,long id){
        var rows=db.query("SELECT stock_id,order_type,quantity,price,total_amount,status FROM orders WHERE id=? AND user_id=?",
                (rs,row)->new Legacy(rs.getLong("stock_id"),rs.getString("order_type"),rs.getInt("quantity"),
                rs.getBigDecimal("price"),rs.getBigDecimal("total_amount"),rs.getString("status")),id,user);
        return rows.isEmpty()?null:rows.getFirst();
    }
    List<String> equityReferences(String legacyId){
        return db.queryForList("""
                SELECT f.id FROM paper_fills f JOIN paper_tickets t ON t.id=f.ticket_id
                WHERE f.legacy_order_id=? AND t.stock_id IS NOT NULL AND t.product='CASH' LIMIT 2
                """,String.class,legacyId);
    }
}

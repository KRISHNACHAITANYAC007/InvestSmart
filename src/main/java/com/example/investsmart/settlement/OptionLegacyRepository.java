package com.example.investsmart.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OptionLegacyRepository {
    record Fill(int units,BigDecimal price,String legacyId,boolean equity,String product,String side,String key,LocalDate expiry,int lot) {}
    record Legacy(String id,String key,LocalDate expiry,String side,int units,int lot,BigDecimal price,String status,String kind) {}
    private final JdbcTemplate db;
    public OptionLegacyRepository(JdbcTemplate db){this.db=db;}
    Fill ownedFill(long user,String id){
        var rows=db.query("""
                SELECT f.quantity,f.price,f.legacy_order_id,t.stock_id,t.product,t.side,t.instrument_key,t.expiry,t.lot_size
                FROM paper_fills f JOIN paper_tickets t ON t.id=f.ticket_id AND t.user_id=f.user_id
                WHERE f.id=? AND f.user_id=?
                """,(rs,row)->new Fill(rs.getInt("quantity"),rs.getBigDecimal("price"),rs.getString("legacy_order_id"),
                rs.getObject("stock_id")!=null,rs.getString("product"),rs.getString("side"),rs.getString("instrument_key"),
                rs.getDate("expiry")==null?null:rs.getDate("expiry").toLocalDate(),rs.getInt("lot_size")),id,user);
        return rows.isEmpty()?null:rows.getFirst();
    }
    Legacy ownedOrder(long user,String id){
        var rows=db.query("SELECT id,instrument_key,expiry,side,units,lot_size,fill_price,status,order_kind FROM option_orders WHERE id=? AND user_id=?",
                (rs,row)->new Legacy(rs.getString("id"),rs.getString("instrument_key"),rs.getDate("expiry")==null?null:rs.getDate("expiry").toLocalDate(),
                rs.getString("side"),rs.getInt("units"),rs.getInt("lot_size"),rs.getBigDecimal("fill_price"),rs.getString("status"),rs.getString("order_kind")),id,user);
        return rows.isEmpty()?null:rows.getFirst();
    }
    List<String> optionReferences(String id){
        return db.queryForList("""
                SELECT f.id FROM paper_fills f JOIN paper_tickets t ON t.id=f.ticket_id
                WHERE f.legacy_order_id=? AND t.stock_id IS NULL AND t.product='CASH' LIMIT 2
                """,String.class,id);
    }
}

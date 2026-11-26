package com.example.investsmart.settlement;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Bounded returned rows; existing schema does not guarantee bounded scan cost. */
@Repository
public class CombinedTradeRepository {
    record Fill(String id, boolean ticketOwned, EquityLegacyRepository.Fill equity, OptionLegacyRepository.Fill option) {}
    record Equity(long id, EquityLegacyRepository.Legacy value) {}
    record Option(String id, OptionLegacyRepository.Legacy value) {}
    record Reference(String link, long count, String onlyId) {}
    private final JdbcTemplate db;
    public CombinedTradeRepository(JdbcTemplate db) { this.db=db; }
    List<Fill> fills(long user) {
        return db.query("""
            SELECT f.id,f.quantity,f.price,f.legacy_order_id,t.id AS owned_ticket,t.stock_id,t.product,t.side,
                   t.instrument_key,t.expiry,t.lot_size
            FROM paper_fills f LEFT JOIN paper_tickets t ON t.id=f.ticket_id AND t.user_id=f.user_id
            WHERE f.user_id=? ORDER BY f.id LIMIT 1001
            """,(r,n)->{
                Long stock=r.getObject("stock_id")==null?null:((Number)r.getObject("stock_id")).longValue();
                return new Fill(r.getString("id"),r.getString("owned_ticket")!=null,
                    new EquityLegacyRepository.Fill(r.getInt("quantity"),r.getBigDecimal("price"),r.getString("legacy_order_id"),stock,r.getString("product"),r.getString("side")),
                    new OptionLegacyRepository.Fill(r.getInt("quantity"),r.getBigDecimal("price"),r.getString("legacy_order_id"),stock!=null,r.getString("product"),r.getString("side"),r.getString("instrument_key"),r.getDate("expiry")==null?null:r.getDate("expiry").toLocalDate(),r.getInt("lot_size")));
            },user);
    }
    List<Equity> equities(long user) {
        return db.query("SELECT id,stock_id,order_type,quantity,price,total_amount,status FROM orders WHERE user_id=? ORDER BY id LIMIT 1001",
            (r,n)->new Equity(r.getLong("id"),new EquityLegacyRepository.Legacy(r.getLong("stock_id"),r.getString("order_type"),r.getInt("quantity"),r.getBigDecimal("price"),r.getBigDecimal("total_amount"),r.getString("status"))),user);
    }
    List<Option> options(long user) {
        return db.query("SELECT id,instrument_key,expiry,side,units,lot_size,fill_price,status,order_kind FROM option_orders WHERE user_id=? ORDER BY id LIMIT 1001",
            (r,n)->new Option(r.getString("id"),new OptionLegacyRepository.Legacy(r.getString("id"),r.getString("instrument_key"),r.getDate("expiry")==null?null:r.getDate("expiry").toLocalDate(),r.getString("side"),r.getInt("units"),r.getInt("lot_size"),r.getBigDecimal("fill_price"),r.getString("status"),r.getString("order_kind"))),user);
    }
    Map<String,List<String>> references(Set<String> links, boolean equity) {
        if(links.isEmpty())return Map.of();
        // Missing/cross-owner tickets have unknown namespace and block either interpretation.
        String namespace=equity?"t.stock_id IS NOT NULL":"t.stock_id IS NULL";
        var rows=new NamedParameterJdbcTemplate(db).query("""
            SELECT f.legacy_order_id AS link,COUNT(*) AS refs,MIN(f.id) AS only_id
            FROM paper_fills f LEFT JOIN paper_tickets t ON t.id=f.ticket_id AND t.user_id=f.user_id
            WHERE f.legacy_order_id IN (:links) AND (t.id IS NULL OR (t.product='CASH' AND (
            """+namespace+"))) GROUP BY f.legacy_order_id LIMIT 1001",Map.of("links",links),
            (r,n)->new Reference(r.getString("link"),r.getLong("refs"),r.getString("only_id")));
        var result=new HashMap<String,List<String>>();
        for(var row:rows) result.put(row.link(),row.count()==1?List.of(row.onlyId()):List.of(row.onlyId(),row.onlyId()));
        return result;
    }
}

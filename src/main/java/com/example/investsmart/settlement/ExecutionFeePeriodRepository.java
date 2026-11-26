package com.example.investsmart.settlement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class ExecutionFeePeriodRepository {
    record Selected(TicketFeeSliceReconciler.Fill fill,TicketFeeSliceReconciler.Ticket ticket,
                    IntradayTicketSliceReconciler.Ticket metadata) {}
    record MisContext(String ticketId, IntradayTicketSliceReconciler.Position position,
                      IntradayTicketSliceReconciler.Event event, boolean foreignEvent) {}
    record Context(long user,TicketFeeSliceReconciler.Fill fill) {}
    private final JdbcTemplate db;
    public ExecutionFeePeriodRepository(JdbcTemplate db){this.db=db;}
    List<Selected> selected(long user,Instant from,Instant to){
        return db.query("""
            SELECT f.id,t.id AS owned_ticket,f.leg_number,f.quantity AS units,f.fee,f.created_at,
                   t.quantity,t.filled_units,t.filled_legs,t.fees,t.position_id,t.stock_id,t.product,t.side,t.status
            FROM paper_fills f LEFT JOIN paper_tickets t ON t.id=f.ticket_id AND t.user_id=f.user_id
            WHERE f.user_id=? AND f.created_at>=? AND f.created_at<? ORDER BY f.created_at,f.id LIMIT 1001
            """,(r,n)->new Selected(new TicketFeeSliceReconciler.Fill(r.getString("id"),r.getString("owned_ticket"),r.getInt("leg_number"),r.getInt("units"),r.getBigDecimal("fee"),r.getTimestamp("created_at").toInstant()),
                r.getString("owned_ticket")==null?null:new TicketFeeSliceReconciler.Ticket(r.getInt("quantity"),r.getInt("filled_units"),r.getInt("filled_legs"),r.getBigDecimal("fees")),
                r.getString("owned_ticket")==null?null:new IntradayTicketSliceReconciler.Ticket(r.getString("owned_ticket"),r.getString("position_id"),r.getLong("stock_id"),r.getString("product"),r.getString("side"),r.getString("status"),new TicketFeeSliceReconciler.Ticket(r.getInt("quantity"),r.getInt("filled_units"),r.getInt("filled_legs"),r.getBigDecimal("fees")))),user,Timestamp.from(from),Timestamp.from(to));
    }
    List<MisContext> misContext(long user,List<String> tickets,int limit){
        if(tickets.size()>100||limit<1||limit>1001)throw new IllegalArgumentException("Invalid MIS batch.");
        if(tickets.isEmpty())return List.of();
        return new NamedParameterJdbcTemplate(db).query("""
            SELECT t.id,p.id AS owned_position,p.stock_id,p.direction,
                   e.event_key,e.user_id AS event_user,e.position_id,e.event_type,e.units,e.fee
            FROM paper_tickets t
            LEFT JOIN paper_intraday_positions p ON p.id=t.position_id AND p.user_id=t.user_id
            LEFT JOIN paper_intraday_events e ON e.event_key=t.id
            WHERE t.user_id=:user AND t.id IN (:tickets) ORDER BY t.id,e.event_key LIMIT :limit
            """,Map.of("user",user,"tickets",tickets,"limit",limit),(r,n)->new MisContext(r.getString("id"),
                r.getString("owned_position")==null?null:new IntradayTicketSliceReconciler.Position(r.getString("owned_position"),r.getLong("stock_id"),r.getString("direction")),
                r.getString("event_key")==null?null:new IntradayTicketSliceReconciler.Event(r.getString("event_key"),r.getString("position_id"),r.getString("event_type"),r.getInt("units"),r.getBigDecimal("fee")),
                r.getString("event_key")!=null&&r.getLong("event_user")!=user));
    }
    List<Context> context(List<String> ownedTickets,int limit){
        if(ownedTickets.size()>100||limit<1||limit>1001)throw new IllegalArgumentException("Invalid context batch.");
        if(ownedTickets.isEmpty())return List.of();
        // Ownership of each parent has already been established; detect foreign child corruption internally.
        return new NamedParameterJdbcTemplate(db).query("SELECT id,ticket_id,user_id,leg_number,quantity,fee,created_at FROM paper_fills WHERE ticket_id IN (:tickets) ORDER BY ticket_id,leg_number LIMIT :limit",Map.of("tickets",ownedTickets,"limit",limit),
            (r,n)->new Context(r.getLong("user_id"),new TicketFeeSliceReconciler.Fill(r.getString("id"),r.getString("ticket_id"),r.getInt("leg_number"),r.getInt("quantity"),r.getBigDecimal("fee"),r.getTimestamp("created_at").toInstant())));
    }
}

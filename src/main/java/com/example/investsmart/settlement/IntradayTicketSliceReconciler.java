package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.math.*;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import static com.example.investsmart.dto.IntradayTicketSliceEvidence.Status.*;

/** Caller authorizes and supplies a complete ticket, its fills and all events keyed by that ticket. */
@Component
public class IntradayTicketSliceReconciler {
    public record Position(String id, long stockId, String direction) {}
    public record Ticket(String id, String positionId, long stockId, String product, String side,
                         String status, TicketFeeSliceReconciler.Ticket amounts) {}
    public record Event(String key, String positionId, String kind, int units, BigDecimal fee) {}
    private final TicketFeeSliceReconciler slices;
    public IntradayTicketSliceReconciler(TicketFeeSliceReconciler slices) { this.slices=slices; }

    public IntradayTicketSliceEvidence reconcile(Ticket ticket, Position position, List<Event> events,
            List<TicketFeeSliceReconciler.Fill> fills, Instant from, Instant to, boolean limited) {
        if(ticket==null||position==null||events==null||fills==null||limited||events.size()>1000||fills.size()>1000)
            return unavailable(INCOMPLETE,"LINK_EVIDENCE_UNAVAILABLE_OR_LIMITED");
        if(!key(ticket.id())||!key(ticket.positionId())||!key(position.id())||ticket.stockId()<1
                ||ticket.stockId()!=position.stockId()||!ticket.positionId().equals(position.id())
                ||!"MIS".equals(ticket.product())||!"FILLED".equals(ticket.status())
                ||!("LONG".equals(position.direction())||"SHORT".equals(position.direction())))
            return unavailable(MISMATCH,"TICKET_POSITION_CONFLICT");
        if(events.isEmpty())return unavailable(INCOMPLETE,"EVENT_MISSING");
        if(events.size()!=1)return unavailable(MISMATCH,"DUPLICATE_OR_EXTRA_EVENT");
        var event=events.getFirst();
        if(event==null||!ticket.id().equals(event.key())||!position.id().equals(event.positionId())
                ||event.units()<1||!money(event.fee()))return unavailable(MISMATCH,"EVENT_CONFLICT");
        if(!("OPEN".equals(event.kind())||"CLOSE".equals(event.kind())))return unavailable(INCOMPLETE,"UNSUPPORTED_EVENT");
        boolean buy="LONG".equals(position.direction())=="OPEN".equals(event.kind());
        var amounts=ticket.amounts();
        if(! (buy?"BUY":"SELL").equals(ticket.side())||amounts==null||amounts.quantity()!=event.units()
                ||amounts.filledUnits()!=event.units()||amounts.filledLegs()!=1||!money(amounts.fee())
                ||amounts.fee().compareTo(event.fee())!=0)return unavailable(MISMATCH,"TICKET_EVENT_CONFLICT");
        var slice=slices.reconcile(ticket.id(),amounts,fills,from,to,false);
        if(slice.status()!=TicketFeeSliceEvidence.Status.MATCHED_TICKET_SLICE)
            return unavailable(slice.status()==TicketFeeSliceEvidence.Status.INCOMPLETE?INCOMPLETE:MISMATCH,slice.limitation());
        if(fills.size()!=1||fills.getFirst().leg()!=0)return unavailable(MISMATCH,"MIS_SINGLE_FILL_CONFLICT");
        // Event and ticket fees corroborate the fill; only the fill timestamp selects the canonical fee.
        return new IntradayTicketSliceEvidence(MATCHED_EVENT_TICKET_SLICE,"EVENT_TICKET_FILL_AGREE_NO_ADDITIONAL_FEE",slice);
    }
    private static boolean key(String value){return value!=null&&!value.isBlank()&&value.length()<=36&&value.chars().noneMatch(Character::isISOControl);}
    private static boolean money(BigDecimal value){if(value==null||value.signum()<0)return false;try{return value.setScale(2,RoundingMode.UNNECESSARY).precision()<=19;}catch(ArithmeticException invalid){return false;}}
    private static IntradayTicketSliceEvidence unavailable(IntradayTicketSliceEvidence.Status status,String reason){return new IntradayTicketSliceEvidence(status,reason,null);}
}

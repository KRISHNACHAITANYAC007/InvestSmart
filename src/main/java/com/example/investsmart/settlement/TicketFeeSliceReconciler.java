package com.example.investsmart.settlement;

import com.example.investsmart.dto.TicketFeeSliceEvidence;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.example.investsmart.dto.TicketFeeSliceEvidence.Status.*;

/** Caller retrieves/authorizes full ticket and fill evidence in one snapshot; no DB/authentication here. */
@Component
public class TicketFeeSliceReconciler {
    public record Ticket(int quantity,int filledUnits,int filledLegs,BigDecimal fee) {}
    public record Fill(String id,String ticketId,int leg,int units,BigDecimal fee,Instant occurredAt) {}
    public TicketFeeSliceEvidence reconcile(String ticketId,Ticket ticket,List<Fill> fills,Instant from,Instant to,boolean limited){
        if(ticket==null||fills==null||limited||fills.size()>1000)return unavailable(ticketId,INCOMPLETE,"FULL_TICKET_EVIDENCE_UNAVAILABLE_OR_LIMITED");
        if(!key(ticketId)||from==null||to==null||!from.isBefore(to)||from.getNano()!=0||to.getNano()!=0
            ||from.isBefore(Instant.parse("1970-01-01T00:00:01Z"))||to.isAfter(Instant.parse("2038-01-19T03:14:07Z"))
            ||Duration.between(from,to).compareTo(Duration.ofDays(366))>0)return unavailable(ticketId,MISMATCH,"INVALID_ID_OR_PERIOD");
        if(ticket.quantity()<1||ticket.filledUnits()<0||ticket.filledUnits()>ticket.quantity()||ticket.filledLegs()<0||!money(ticket.fee()))return unavailable(ticketId,MISMATCH,"INVALID_TICKET");
        var identities=new HashSet<String>();var legs=new HashSet<Integer>();long units=0;BigDecimal fullFee=new BigDecimal("0.00");
        for(var fill:fills){
            if(fill==null||!key(fill.id())||!ticketId.equals(fill.ticketId())||!identities.add(fill.id())||fill.leg()<0||!legs.add(fill.leg())
                ||fill.units()<1||!money(fill.fee())||fill.occurredAt()==null||fill.occurredAt().getNano()!=0)return unavailable(ticketId,MISMATCH,"INVALID_OR_CONFLICTING_FILL");
            units+=fill.units();fullFee=fullFee.add(fill.fee().setScale(2));
        }
        if(fills.size()<ticket.filledLegs()||units<ticket.filledUnits())return unavailable(ticketId,INCOMPLETE,"FULL_TICKET_DETAIL_MISSING");
        if(fills.size()!=ticket.filledLegs()||units!=ticket.filledUnits()||fullFee.compareTo(ticket.fee())!=0)return unavailable(ticketId,MISMATCH,"TICKET_FILL_COVERAGE_OR_FEE_CONFLICT");
        var selected=new ArrayList<TicketFeeSliceEvidence.SelectedFill>();BigDecimal selectedFee=new BigDecimal("0.00");
        for(var fill:fills)if(!fill.occurredAt().isBefore(from)&&fill.occurredAt().isBefore(to)){
            selected.add(new TicketFeeSliceEvidence.SelectedFill(fill.id(),fill.fee().setScale(2),fill.occurredAt()));selectedFee=selectedFee.add(fill.fee().setScale(2));
        }
        // A stable presentation order does not establish causal order for equal timestamps.
        selected.sort(Comparator.comparing(TicketFeeSliceEvidence.SelectedFill::occurredAt).thenComparing(TicketFeeSliceEvidence.SelectedFill::fillId));
        return new TicketFeeSliceEvidence(ticketId,MATCHED_TICKET_SLICE,selected.size(),fills.size()-selected.size(),selectedFee,selected,
            "Verified supplied ticket only; selected fill fees once, ticket total/context fills are not added. No intraday/legacy/account expense completeness or ownership proof.");
    }
    private static boolean key(String value){return value!=null&&!value.isBlank()&&value.length()<=36&&value.chars().noneMatch(Character::isISOControl);}
    private static boolean money(BigDecimal value){if(value==null||value.signum()<0)return false;try{return value.setScale(2,RoundingMode.UNNECESSARY).precision()<=19;}catch(ArithmeticException invalid){return false;}}
    private static TicketFeeSliceEvidence unavailable(String ticket,TicketFeeSliceEvidence.Status status,String reason){return new TicketFeeSliceEvidence(ticket,status,null,null,null,List.of(),reason);}
}

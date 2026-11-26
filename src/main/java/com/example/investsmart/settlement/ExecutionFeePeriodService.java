package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import com.example.investsmart.exception.*;
import com.example.investsmart.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class ExecutionFeePeriodService {
    private final UserRepository users;private final SettlementJournalRepository journal;
    private final ExecutionFeePeriodRepository records;private final TicketFeeSliceReconciler slices;
    private final IntradayTicketSliceReconciler mis;
    public ExecutionFeePeriodService(UserRepository users,SettlementJournalRepository journal,ExecutionFeePeriodRepository records,TicketFeeSliceReconciler slices,IntradayTicketSliceReconciler mis){this.users=users;this.journal=journal;this.records=records;this.slices=slices;this.mis=mis;}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,propagation=Propagation.REQUIRES_NEW)
    public ExecutionFeePeriodEvidence read(String email,Instant from,Instant to){
        if(from==null||to==null||!from.isBefore(to)||from.getNano()!=0||to.getNano()!=0||from.isBefore(Instant.parse("1970-01-01T00:00:01Z"))||to.isAfter(Instant.parse("2038-01-19T03:14:07Z"))||Duration.between(from,to).compareTo(Duration.ofDays(366))>0)throw new BadRequestException("Invalid execution-fee period.");
        if(email==null||email.isBlank())throw missing();var user=users.findByEmailIgnoreCase(email).orElseThrow(ExecutionFeePeriodService::missing);user.requireActive();
        var coverage=journal.coverage(user.getId());var rows=records.selected(user.getId(),from,to);boolean selectedLimited=rows.size()>1000;
        var selected=rows.stream().limit(1000).toList();var tickets=new LinkedHashMap<String,TicketFeeSliceReconciler.Ticket>();
        for(var row:selected)if(row.ticket()!=null)tickets.put(row.fill().ticketId(),row.ticket());
        var ids=new ArrayList<>(tickets.keySet());var context=new ArrayList<ExecutionFeePeriodRepository.Context>();
        if(!selectedLimited)for(int offset=0;offset<ids.size();offset+=100){context.addAll(records.context(ids.subList(offset,Math.min(offset+100,ids.size())),1001-context.size()));if(context.size()>1000)break;}
        boolean contextLimited=context.size()>1000;var grouped=context.stream().collect(java.util.stream.Collectors.groupingBy(r->r.fill().ticketId()));
        var metadata=new LinkedHashMap<String,IntradayTicketSliceReconciler.Ticket>();
        for(var row:selected)if(row.metadata()!=null&&"MIS".equals(row.metadata().product()))metadata.put(row.fill().ticketId(),row.metadata());
        var misIds=new ArrayList<>(metadata.keySet());var links=new ArrayList<ExecutionFeePeriodRepository.MisContext>();
        if(!selectedLimited&&!contextLimited)for(int offset=0;offset<misIds.size();offset+=100){links.addAll(records.misContext(user.getId(),misIds.subList(offset,Math.min(offset+100,misIds.size())),1001-links.size()));if(links.size()>1000)break;}
        boolean misLimited=links.size()>1000;var linked=links.stream().collect(java.util.stream.Collectors.groupingBy(ExecutionFeePeriodRepository.MisContext::ticketId));
        var results=new ArrayList<TicketFeeSliceEvidence>();
        for(var entry:tickets.entrySet()){
            var full=grouped.getOrDefault(entry.getKey(),List.of());
            if(full.stream().anyMatch(row->row.user()!=user.getId()))results.add(new TicketFeeSliceEvidence(entry.getKey(),TicketFeeSliceEvidence.Status.MISMATCH,null,null,null,List.of(),"CHILD_OWNERSHIP_CONFLICT"));
            else if(metadata.containsKey(entry.getKey())){
                var detail=linked.getOrDefault(entry.getKey(),List.of());
                if(detail.stream().anyMatch(ExecutionFeePeriodRepository.MisContext::foreignEvent))results.add(new TicketFeeSliceEvidence(entry.getKey(),TicketFeeSliceEvidence.Status.MISMATCH,null,null,null,List.of(),"MIS_EVENT_OWNERSHIP_CONFLICT"));
                else {
                    var check=mis.reconcile(metadata.get(entry.getKey()),detail.isEmpty()?null:detail.getFirst().position(),detail.stream().map(ExecutionFeePeriodRepository.MisContext::event).filter(Objects::nonNull).toList(),full.stream().map(ExecutionFeePeriodRepository.Context::fill).toList(),from,to,selectedLimited||contextLimited||misLimited);
                    results.add(check.slice()!=null?check.slice():new TicketFeeSliceEvidence(entry.getKey(),check.status()==IntradayTicketSliceEvidence.Status.MISMATCH?TicketFeeSliceEvidence.Status.MISMATCH:TicketFeeSliceEvidence.Status.INCOMPLETE,null,null,null,List.of(),check.reason()));
                }
            }else results.add(slices.reconcile(entry.getKey(),entry.getValue(),full.stream().map(ExecutionFeePeriodRepository.Context::fill).toList(),from,to,selectedLimited||contextLimited));
        }
        var limitations=new ArrayList<String>(List.of("Selected paper-fill fee evidence only; no complete expense total/history or balances.","OTHER_PRODUCT_COVERAGE_NOT_CERTIFIED","MIS_EVENT_ONLY_HISTORY_NOT_SELECTED"));
        if(misLimited)limitations.add("MIS_CONTEXT_LIMIT");
        if(!coverage.initialized())limitations.add("ACCOUNT_NOT_INITIALIZED");else if(from.isBefore(coverage.initializedAt()))limitations.add("RANGE_STARTS_BEFORE_INITIALIZATION");
        if(selectedLimited)limitations.add("SELECTED_FILL_LIMIT");if(contextLimited)limitations.add("FULL_CONTEXT_LIMIT");if(selected.stream().anyMatch(row->row.ticket()==null))limitations.add("OWNED_TICKET_UNAVAILABLE");
        var raw=selected.stream().map(row->new ExecutionFeePeriodEvidence.SelectedFill(row.fill().id(),row.fill().ticketId(),row.fill().fee(),row.fill().occurredAt())).toList();
        return new ExecutionFeePeriodEvidence(from,to,coverage,false,selectedLimited,contextLimited,raw,results,limitations);
    }
    private static ResourceNotFoundException missing(){return new ResourceNotFoundException("Execution-fee evidence not found");}
}

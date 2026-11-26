package com.example.investsmart.dto;

import java.util.List;

/** Groups supplied selected rows only; no ownership, period completeness or financial totals. */
public record GroupedChargeEvidence(Status status,List<Group> groups,List<String> limitations,List<ChargeGroupSourceEvidence> sourceEvidence){
    public GroupedChargeEvidence(Status status,List<Group> groups,List<String> limitations){this(status,groups,limitations,List.of());}
    public enum Status { GROUPED_SUPPLIED_ROWS, INCOMPLETE }
    public enum Kind { VERIFIED_CHARGE, UNGROUPED }
    public record Group(Kind kind,String representativeKey,List<SettlementJournalPage.Entry> selectedRows,
                        List<String> outsideSelectionChildKeys){
        public Group{selectedRows=List.copyOf(selectedRows);outsideSelectionChildKeys=List.copyOf(outsideSelectionChildKeys);}
    }
    public GroupedChargeEvidence{groups=List.copyOf(groups);limitations=List.copyOf(limitations);sourceEvidence=List.copyOf(sourceEvidence);}
}

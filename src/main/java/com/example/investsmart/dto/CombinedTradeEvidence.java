package com.example.investsmart.dto;

import java.util.List;

/** Supplied record identities only; not a complete account/period statement. */
public record CombinedTradeEvidence(Status status,List<Group> groups,List<String> limitations) {
    public enum Status { COMBINED_SUPPLIED_RECORDS, INCOMPLETE }
    public enum Source { PAPER_FILL, EQUITY_ORDER, OPTION_ORDER }
    public record Key(Source source,String id) {}
    public record Group(Key representative,List<Key> records) {
        public Group { records=List.copyOf(records); }
    }
    public CombinedTradeEvidence { groups=List.copyOf(groups);limitations=List.copyOf(limitations); }
}

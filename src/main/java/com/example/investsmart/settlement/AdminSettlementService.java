package com.example.investsmart.settlement;

import com.example.investsmart.security.SecurityAccounts;
import com.example.investsmart.security.SecurityEvents;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminSettlementService {
    private final JdbcTemplate db;
    private final SettlementCalendar calendar;
    private final SecurityAccounts accounts;
    private final SecurityEvents events;

    public AdminSettlementService(JdbcTemplate db, SettlementCalendar calendar, SecurityAccounts accounts, SecurityEvents events) {
        this.db = db; this.calendar = calendar; this.accounts = accounts; this.events = events;
    }

    @Transactional(readOnly = true)
    public List<SettlementCalendar.Coverage> coverage() {
        return db.query("SELECT v.exchange_code,v.segment_code,v.id,v.first_date,v.last_date,v.source_url,v.content_sha256 FROM settlement_calendar_active a JOIN settlement_calendar_versions v ON v.id=a.version_id ORDER BY v.exchange_code,v.segment_code",
                (rs, n) -> new SettlementCalendar.Coverage(rs.getString(1),rs.getString(2),rs.getString(3),rs.getDate(4).toLocalDate(),rs.getDate(5).toLocalDate(),rs.getString(6),rs.getString(7)));
    }

    @Transactional
    public SettlementCalendar.Coverage activate(String email, String exchange, String segment, String source, String csv, String reason, boolean reviewed, HttpServletRequest request) {
        accounts.requireRecent(request);
        var actor = accounts.user(email);
        if (!"ADMIN".equals(actor.getRole()) || !accounts.isOwner(actor.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Owner administrator required.");
        if (!reviewed) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirm independent review of the source and every calendar date.");
        ReviewedCalendar parsed;
        try {
            if (csv == null || reason == null || reason.trim().length() < 5 || reason.length() > 300 || reason.chars().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("A review reason of 5-300 characters is required.");
            parsed = ReviewedCalendar.parse(csv.getBytes(StandardCharsets.UTF_8),exchange,segment,source);
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,invalid.getMessage());
        }
        String id = calendar.activate(parsed,"OWNER_ADMIN",reason);
        events.audit(actor.getId(),null,"SETTLEMENT_CALENDAR_ACTIVATE",parsed.exchange()+"/"+parsed.segment()+" version "+id+"; "+reason.trim());
        events.record("ADMIN_ACTION",actor.getId(),email,request,"SETTLEMENT_CALENDAR_ACTIVATE");
        return calendar.coverage(parsed.exchange(),parsed.segment()).orElseThrow();
    }
}

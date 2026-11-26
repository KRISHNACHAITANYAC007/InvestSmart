package com.example.investsmart.settlement;

import com.example.investsmart.dto.SettlementFundingPage;
import com.example.investsmart.dto.SettlementJournalPage;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.exception.ResourceNotFoundException;
import com.example.investsmart.repository.UserRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.example.investsmart.dto.SettlementFundingPage.Verification.*;

/** Internal source-evidence view; never initializes accounts or changes financial state. */
@Service
public class SettlementFundingService {
    private static final Set<String> FUNDING = Set.of("RESERVE", "SPEND", "RELEASE");
    private final UserRepository users;
    private final SettlementJournalRepository journal;
    private final SettlementFundingRepository funding;

    public SettlementFundingService(UserRepository users, SettlementJournalRepository journal,
                                    SettlementFundingRepository funding) {
        this.users = users; this.journal = journal; this.funding = funding;
    }

    /** Identity must come from the authenticated server context, not a request user identifier. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    public SettlementFundingPage read(String authenticatedEmail, Integer page, Integer size) {
        int p = page == null ? 0 : page, s = size == null ? 50 : size;
        if (p < 0 || p > 100000 || s < 1 || s > 100) throw new BadRequestException("Invalid journal page or size.");
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) throw new ResourceNotFoundException("User not found");
        var user = users.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.requireActive();
        var coverage = journal.coverage(user.getId());
        var entries = journal.entries(user.getId(), s, Math.multiplyExact((long)p, s));
        var keys = entries.stream().filter(e -> FUNDING.contains(e.action())).map(e -> e.eventKey()).toList();
        var evidence = funding.evidence(user.getId(), keys);
        boolean truncated = evidence.size() > SettlementFundingRepository.MAX_EVIDENCE;
        var grouped = evidence.stream().collect(Collectors.groupingBy(SettlementFundingRepository.Evidence::eventKey));
        return new SettlementFundingPage(coverage, p, s, entries.stream()
                .map(e -> FundingEvidenceChecker.check(e, grouped.getOrDefault(e.eventKey(), List.of()), truncated)).toList());
    }

}

package com.example.investsmart.settlement;

import com.example.investsmart.dto.SettlementJournalPage;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.exception.ResourceNotFoundException;
import com.example.investsmart.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** No endpoint: callers must supply server-authenticated identity, never a request user ID. */
@Service
public class SettlementJournalService {
    private final UserRepository users;
    private final SettlementJournalRepository journal;

    public SettlementJournalService(UserRepository users, SettlementJournalRepository journal) {
        this.users = users;
        this.journal = journal;
    }

    // Own the snapshot even when a caller already has a weaker-isolation transaction.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ,
            propagation = Propagation.REQUIRES_NEW)
    public SettlementJournalPage read(String authenticatedEmail, Integer page, Integer size) {
        int selectedPage = page == null ? 0 : page;
        int selectedSize = size == null ? 50 : size;
        if (selectedPage < 0 || selectedPage > 100000 || selectedSize < 1 || selectedSize > 100) {
            throw new BadRequestException("Invalid journal page or size.");
        }
        long offset = Math.multiplyExact((long) selectedPage, selectedSize);
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new ResourceNotFoundException("User not found");
        }
        var user = users.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.requireActive();
        return new SettlementJournalPage(journal.coverage(user.getId()), selectedPage, selectedSize,
                journal.entries(user.getId(), selectedSize, offset));
    }
}

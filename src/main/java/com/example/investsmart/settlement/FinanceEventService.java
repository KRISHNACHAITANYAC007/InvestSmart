package com.example.investsmart.settlement;

import com.example.investsmart.dto.FinanceEventPage;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.exception.ResourceNotFoundException;
import com.example.investsmart.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Internal read only. Identity must come from server authentication, never request/model identity. */
@Service
public class FinanceEventService {
    private final UserRepository users;
    private final FinanceEventRepository events;
    public FinanceEventService(UserRepository users, FinanceEventRepository events) {
        this.users = users; this.events = events;
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    public FinanceEventPage read(String authenticatedEmail, String positionId, Integer page, Integer size) {
        int selectedPage = page == null ? 0 : page, selectedSize = size == null ? 50 : size;
        if (selectedPage < 0 || selectedPage > 100000 || selectedSize < 1 || selectedSize > 100
                || positionId == null || positionId.isBlank() || positionId.length() > 36) {
            throw new BadRequestException("Invalid financing position or page.");
        }
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) throw missing();
        var user = users.findByEmailIgnoreCase(authenticatedEmail).orElseThrow(FinanceEventService::missing);
        user.requireActive();
        if (!events.ownedPosition(user.getId(), positionId)) throw missing();
        return new FinanceEventPage(positionId, selectedPage, selectedSize,
                events.entries(user.getId(), positionId, selectedSize, (long) selectedPage * selectedSize));
    }
    private static ResourceNotFoundException missing() { return new ResourceNotFoundException("Financing evidence not found"); }
}

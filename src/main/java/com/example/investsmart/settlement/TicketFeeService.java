package com.example.investsmart.settlement;

import com.example.investsmart.dto.TicketFeeEvidence;
import com.example.investsmart.exception.*;
import com.example.investsmart.repository.UserRepository;
import java.math.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.example.investsmart.dto.TicketFeeEvidence.Verification.*;

/** Internal, bounded evidence comparison; no money writes or account-wide fee total. */
@Service
public class TicketFeeService {
    private final UserRepository users;
    private final TicketFeeRepository fees;
    public TicketFeeService(UserRepository users, TicketFeeRepository fees) { this.users = users; this.fees = fees; }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    public TicketFeeEvidence read(String authenticatedEmail, String ticketId) {
        if (ticketId == null || ticketId.isBlank() || ticketId.length() > 36) throw new BadRequestException("Invalid ticket.");
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) throw missing();
        var user = users.findByEmailIgnoreCase(authenticatedEmail).orElseThrow(TicketFeeService::missing);
        user.requireActive();
        var ticket = fees.owned(user.getId(), ticketId);
        if (ticket == null) throw missing();
        var rows = fees.fills(ticketId);
        if (rows.size() > 1000) return result(ticketId, INCOMPLETE, "DETAIL_LIMIT", ticket.fee(), null);
        if (!money(ticket.fee()) || ticket.quantity() < 1 || ticket.filledUnits() < 0
                || ticket.filledUnits() > ticket.quantity() || ticket.filledLegs() < 0)
            return result(ticketId, MISMATCH, "INVALID_TICKET", ticket.fee(), null);
        BigDecimal sum = new BigDecimal("0.00"); long units = 0;
        var legs = new java.util.HashSet<Integer>();
        for (var row : rows) {
            if (row.userId() != user.getId()) return result(ticketId, MISMATCH, "OWNERSHIP_CONFLICT", ticket.fee(), null);
            if (!money(row.fee()) || row.quantity() < 1 || row.leg() < 0 || !legs.add(row.leg()))
                return result(ticketId, MISMATCH, "INVALID_DETAIL", ticket.fee(), null);
            sum = sum.add(row.fee()); units += row.quantity();
        }
        if (rows.size() < ticket.filledLegs() || units < ticket.filledUnits())
            return result(ticketId, INCOMPLETE, "MISSING_DETAIL", ticket.fee(), null);
        if (rows.size() != ticket.filledLegs() || units != ticket.filledUnits())
            return result(ticketId, MISMATCH, "COVERAGE_CONFLICT", ticket.fee(), sum);
        if (sum.compareTo(ticket.fee()) != 0) return result(ticketId, MISMATCH, "FEE_CONFLICT", ticket.fee(), sum);
        return result(ticketId, MATCHED, "TICKET_AND_FILL_FEES_AGREE", ticket.fee(), sum);
    }
    private static TicketFeeEvidence result(String id, TicketFeeEvidence.Verification status, String reason,
                                             BigDecimal recorded, BigDecimal detail) {
        return new TicketFeeEvidence(id, status, reason, recorded, detail, status == MATCHED ? recorded : null);
    }
    private static boolean money(BigDecimal value) {
        if (value == null || value.signum() < 0) return false;
        try { return value.setScale(2, RoundingMode.UNNECESSARY).precision() <= 19; }
        catch (ArithmeticException invalid) { return false; }
    }
    private static ResourceNotFoundException missing() { return new ResourceNotFoundException("Ticket fee evidence not found"); }
}

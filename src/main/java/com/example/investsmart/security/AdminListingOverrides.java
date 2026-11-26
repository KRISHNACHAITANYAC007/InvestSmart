package com.example.investsmart.security;

import com.example.investsmart.investment.BondMarketService;
import com.example.investsmart.investment.IpoMarketService;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// This JDBC table is defined in db/security-schema.sql and initialized by SecuritySchema.
// IntelliJ's connected database model may not include the runtime-managed schema.
@SuppressWarnings("SqlResolve")
public class AdminListingOverrides {
    public record Override(String assetType, String referenceId, boolean available,
                           LocalDate openDate, LocalDate closeDate, LocalDate allotmentDate,
                           LocalDate listingDate, String reason, Long actorId, Instant updatedAt) { }

    private final JdbcTemplate db;
    private final Clock clock;

    public AdminListingOverrides(JdbcTemplate db, Clock clock) {
        this.db = db;
        this.clock = clock;
    }

    public Optional<Override> find(String assetType, String referenceId) {
        List<Override> rows = db.query(
                "SELECT asset_type,reference_id,available,open_date,close_date,allotment_date,listing_date,reason,actor_id,updated_at FROM admin_listing_overrides WHERE asset_type=? AND reference_id=?",
                (rs, row) -> new Override(rs.getString("asset_type"), rs.getString("reference_id"),
                        rs.getBoolean("available"), rs.getObject("open_date", LocalDate.class),
                        rs.getObject("close_date", LocalDate.class), rs.getObject("allotment_date", LocalDate.class),
                        rs.getObject("listing_date", LocalDate.class), rs.getString("reason"),
                        rs.getObject("actor_id", Long.class), rs.getTimestamp("updated_at").toInstant()),
                assetType, referenceId);
        return rows.stream().findFirst();
    }

    @Transactional
    public Override save(String assetType, String referenceId, boolean available, LocalDate openDate,
                         LocalDate closeDate, LocalDate allotmentDate, LocalDate listingDate,
                         String reason, Long actorId) {
        Instant now = clock.instant();
        var existing = find(assetType, referenceId);
        if (existing.isPresent()) {
            db.update("UPDATE admin_listing_overrides SET available=?,open_date=?,close_date=?,allotment_date=?,listing_date=?,reason=?,actor_id=?,updated_at=? WHERE asset_type=? AND reference_id=?",
                    available, openDate, closeDate, allotmentDate, listingDate, reason, actorId,
                    Timestamp.from(now), assetType, referenceId);
        } else {
            db.update("INSERT INTO admin_listing_overrides(asset_type,reference_id,available,open_date,close_date,allotment_date,listing_date,reason,actor_id,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
                    assetType, referenceId, available, openDate, closeDate, allotmentDate, listingDate,
                    reason, actorId, Timestamp.from(now));
        }
        return find(assetType, referenceId).orElseThrow();
    }

    public IpoMarketService.Ipo apply(IpoMarketService.Ipo ipo) {
        return find("IPO", ipo.id()).map(override -> new IpoMarketService.Ipo(
                ipo.id(), ipo.symbol(), ipo.name(), ipo.isin(), ipo.status(), ipo.issueType(),
                ipo.minimumPrice(), ipo.maximumPrice(), ipo.lotSize(), ipo.minimumQuantity(),
                override.openDate() == null ? ipo.openDate() : override.openDate(),
                override.closeDate() == null ? ipo.closeDate() : override.closeDate(),
                ipo.dailyStart(), ipo.dailyEnd(),
                override.allotmentDate() == null ? ipo.allotmentDate() : override.allotmentDate(),
                override.listingDate() == null ? ipo.listingDate() : override.listingDate(),
                ipo.cutoffPrice(), ipo.subscription(), ipo.exchange(), ipo.rhpUrl(), ipo.drhpUrl(),
                ipo.updatedAt(), ipo.stale(), override.available(), override.openDate() != null)).orElseGet(() ->
                        new IpoMarketService.Ipo(ipo.id(), ipo.symbol(), ipo.name(), ipo.isin(), ipo.status(),
                                ipo.issueType(), ipo.minimumPrice(), ipo.maximumPrice(), ipo.lotSize(),
                                ipo.minimumQuantity(), ipo.openDate(), ipo.closeDate(), ipo.dailyStart(),
                                ipo.dailyEnd(), ipo.allotmentDate(), ipo.listingDate(), ipo.cutoffPrice(),
                                ipo.subscription(), ipo.exchange(), ipo.rhpUrl(), ipo.drhpUrl(), ipo.updatedAt(),
                                ipo.stale(), true, false));
    }

    public BondMarketService.Bond apply(BondMarketService.Bond bond) {
        boolean available = find("BOND", bond.instrumentKey()).map(Override::available).orElse(true);
        return new BondMarketService.Bond(bond.instrumentKey(), bond.symbol(), bond.name(), bond.isin(),
                bond.exchange(), bond.category(), bond.instrumentType(), bond.terms(), available);
    }
}

# Expanded paper-ticket simulation

All orders, financing, interest and charges are virtual. No real broker order, loan, pledge or payment API is called. Supported NSE/BSE cash equities are simulation-eligible for MTF/Cover; this is not a claim of actual broker eligibility. Options remain premium-funded long positions, with no futures, naked sales or option writing.

## Entry and exit rules

Quick simplifies Regular market entry. Amount entry rounds down to whole shares/lots; switching units/lots preserves exposure. The server previews and revalidates each submission. Request IDs deduplicate retries. Only unfilled, unlinked pending entries can be replaced atomically; failed replacement preserves the original reservation.

Regular supports Market, Limit, SL-limit and SL-market. Buy SL-limit requires limit >= trigger; sell SL-limit requires limit <= trigger. Triggered stop-limits can remain unfilled after a gap. Buy Market/SL-market reserves a five-percent protection ceiling above the greater of reference and trigger. Orders cannot spend beyond that ceiling. Listing identity, fresh quotes, ownership, market sessions and available funds are checked again on execution.

DAY expires at 15:30 IST. IOC attempts a whole child immediately and cancels any unfilled remainder; no partial child fills. GTT expires after 365 days or contract expiry. Attached exits use percentages of actual average entry fill or absolute prices. One OCO pair shares a single unit reservation. Triggering one cancels its sibling, even if its later fill is rejected. Trigger observations commit separately from financial execution. Triggered GTT exits become DAY limit orders; a trigger does not guarantee execution.

Iceberg splits a parent into 2–10 sequential whole-share/whole-lot children, respecting each child's freeze limit. The parent reserves all cash/units up front. Each child uses fresh executable data and has a separate fee. Cancelling remaining children preserves completed trades. Attached exits activate only after the whole parent fills; a partly completed/cancelled iceberg needs manual management of acquired units until separate exits are placed.

## Financing and fees

Default equity buying power is **2×**: reserve at least half the protected notional plus fees. MTF/Cover positions are separate from delivery holdings. Cover requires a protective stop-market exit and attempts closure at **15:20 IST**; no new Cover entry fills after that cutoff. Missing data or closed sessions leave closure retryable, without inventing fills.

MTF interest defaults to **12% yearly** on each IST calendar day's closing outstanding principal, including weekends. Daily interest uses a 365-day denominator and HALF_UP rounding to paise; it does not compound. Dated principal ledger events reconstruct past balances. Accrual is idempotent, with 31-date catch-up batches. Incomplete accrual or stale marks makes financed aggregate valuation unavailable. Unpaid interest is a liability until repaid. Cover interest is zero.

Closing proceeds repay principal and unpaid interest proportionally; the last close repays exact remainders. Maintenance equity is **25% of current market value**. Breaches request liquidation using fresh executable data. Gaps may leave negative virtual cash; new buys are blocked until resolved, while deposits and closing positions remain available.

Simulated brokerage defaults to **0.03% of turnover, capped at ₹20 per filled order/iceberg child**. Statutory taxes and other charges are excluded, not approximated. Paid fees are expensed in realized P&L; unpaid interest reduces net equity. Existing legacy order-service/API behavior and historical fee-free records remain compatible. New ticket fees are explicitly recorded in the paper fill/ticket ledger.

Properties: `investsmart.paper.leverage` (1–2), `investsmart.paper.annual-interest` (0–1 fraction), `investsmart.paper.fee-rate` (0–0.1 fraction), and `investsmart.paper.fee-cap` (nonnegative INR). Policy changes cancel/refund pending tickets rather than silently repricing them. Historical fees and financed positions' stored interest rates remain intact; future closing trades use the then-current fee schedule.

Portfolio distinguishes exposure, principal, interest, reserved funds, paid fees and net equity. Borrowed principal is never profit or deposited cash. Ordinary allocation percentages are unavailable while financed exposure makes the unleveraged calculation misleading.

## Jobs, history and deployment

Polling uses bounded 30-second batches (20 tickets and 50 financed positions), not exchange-native orders. Delays, downtime and movements between polls can miss triggers; restart catch-up does not reconstruct past intraday ticks. Operations shows ticket/financing rows, the financing ledger and persistent job health. Rejected submissions stay security events rather than accepted orders.

Managed databases require reviewed `20261111_paper_ticket.sql` before application startup. It creates four tables without modifying existing trades/balances. Fresh baselines and local/test initialization include the tables. Back up and rehearse migration/restore through the existing ledger; never copy a fresh baseline over retained data.

All deployment steps and user dependencies live in [DEPLOYMENT_CHECKLIST.md](../DEPLOYMENT_CHECKLIST.md). New code needs a new validated archive and rendered/device acceptance. Reference concepts: [Kite order types/icebergs](https://kite.trade/docs/kite/orders/), [GTT](https://support.zerodha.com/category/trading-and-markets/charts-and-orders/gtt/articles/what-is-the-good-till-triggered-gtt-feature), and [Cover](https://support.zerodha.com/category/trading-and-markets/charts-and-orders/order/articles/what-are-cover-orders-and-how-to-use-them). The financing and fee settings above are educational simulations, not actual broker rules.
# Intraday/MIS simulation (2026-11-12)

Product and execution style are now separate controls. Products are Delivery (CASH), Intraday (MIS), MTF and Cover. MIS supports NSE/BSE equity long and short positions through Quick Market or Regular Market/Limit/SL/SL-M with DAY/IOC. MIS GTT, attached GTT/OCO exits, options and Iceberg are explicitly unavailable; Delivery Iceberg and existing MTF/Cover flows remain supported under their original policies.

MIS uses a separate position/event model, never delivery holdings. Initial collateral is 50% of protected entry notional (2x simulated buying power) plus explicit fees. Collateral must be settled cash; reusable/restricted unsettled credits cannot fund it. Short entry turnover is not credited to available cash. There is no overnight interest. These are disclosed application settings, not a claim of broker eligibility or margin rates.

Entries are permitted from 09:15 until 15:10 IST with an open supported exchange session required for fills. New entries stop at 15:10; pending entries expire and closure is attempted for open positions. Fresh pricing and an open session remain required. Missing data, outages or gaps may leave a visible closure request pending across restart/next session. Such exposure is never silently converted to delivery; overdue/requested closures block new MIS entries. Closing orders reference an owned position, use its opposite side and reserve units to prevent duplicate reductions. Whole-child fills only; partial position exits are supported.

For exits, remaining collateral is released proportionally, rounding partial releases down with the remainder assigned to the final exit. A loss consumes released collateral first; excess loss becomes disclosed virtual cash debt. Positive realized P&L becomes restricted credit until reviewed equity T+1 settlement. Exit fees can create debt when no eligible cash remains, so inability to pay a fee does not trap a position open. Order/position mutations share the same user lock as wallet, delivery, options and IPO flows, with idempotent fill/accounting events. Stale position marks make aggregate valuation unavailable. Restricted credits are included once in portfolio valuation, but remain excluded from buying power/withdrawable cash.

Existing retained databases require reviewed `20261112_intraday.sql` after the prior settlement migrations. The runtime/fresh baselines and managed startup validation include both new tables. Back up and rehearse an isolated restore before migration; do not apply fresh initialization to existing data. No user database migration is performed by source editing or automated synthetic tests.


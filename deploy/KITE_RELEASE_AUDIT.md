# V2 deployment audit using Kite as reference

Release target: current V2 educational paper trading after completing T+1. The broader approved catalogue remains in WORK_QUEUE.md for subsequent releases. This audit does not claim Kite parity, broker authorization or live execution. Verdict: **not ready for deployment** until the open gates below have evidence.

## Reference and evidence matrix

| Capability | Current evidence / difference | Release disposition |
| --- | --- | --- |
| Settled funds versus withdrawable funds | 177 selected integration tests passed; wallet rejects withdrawals funded only by pending credits | Retest in final MySQL/release candidate |
| T1 holdings / BTST restrictions | Delivery lots, restricted proceeds and original-delivery dependencies are tested | Finish displayed T1 quantities and fund breakdown |
| Automatic settlement | New bounded worker, independent retry observations and persistent health are under verification | Blocking until verified with catch-up/restart and UI |
| Settlement calendar | Reviewed-file import, separate trading/settlement flags, immutable versions and audited activation | Reviewed production coverage required; unknown coverage stays pending |
| Settlement timing | Explicit 18:00 IST simulation cutoff | Intentionally differs from actual broker payout timing; disclose consistently |
| Equity market/limit/SL/SL-M | Legacy and shared paper routes use the ledger and provider/session checks | Verify all supported controls in final build and rendered acceptance |
| GTT, linked OCO, iceberg | Persisted paper-ticket behavior and independent OCO trigger observation tested | Simulation controls; no claim of exact Kite matching/exchange queue priority |
| Fees / financing | Disclosed simulated fees, collateral, interest, maintenance and debt handling | Intentionally different; statutory taxes excluded and must be clearly stated |
| NIFTY CE/PE | Provider contract, lot/freeze/depth checks; sales and expiry payouts remain restricted until FNO settlement | Provider permissions and final settlement regression required |
| Holdings / positions / P&L | Historical weighted-average analytics preserved | Add restricted cash to valuation exactly once; verify ledger/portfolio reconciliation |
| Funds/history presentation | Read-only settlement APIs being added | Blocking until UI clearly separates available and withdrawable amounts |
| Charts / watchlists / search | Existing V2 implementation | Check freshness, listing identity, empty/stale/error states and narrow screens |
| IPO applications / listing transfer | Existing guarded reservation/draw/transfer flows integrated with ledger | Paper simulation, not broker IPO application; retest final candidate |
| Owner Admin / authentication | Existing automated security acceptance | Real owner/device/rendered acceptance remains required |
| AMO / baskets / alerts / expanded reports | Remaining approved roadmap tasks | Deferred from this V2 launch; do not advertise as available |
| Futures / option writing / MF-SIP / expanded bond execution | Remaining approved roadmap tasks | Deferred; do not expose unsupported execution or fabricated inputs |
| Real deposits, bank payouts, demat/KYC, broker orders | Outside educational simulation | Excluded; virtual actions must not imply real money transfers |
| Equity Intraday/MIS long and short | Separate settled collateral, positions, partial exits, restricted profits and 15:10 closure attempts implemented | Disclosed 2x simulation; full release/container and rendered acceptance still required |

Official references reviewed on 2026-11-11:

- [Zerodha withdrawal eligibility](https://support.zerodha.com/category/funds/fund-withdrawal/withdrawal-timeline/articles/withdrawal-timeframe): sale/position proceeds become withdrawable after settlement.
- [Zerodha T1 sale-credit restrictions](https://zerodha.com/marketintel/bulletin/352863/changes-in-early-pay-in-epi-process-for-sale-of-t1-holdings): unsettled-share proceeds cannot fund further trades on the sale day.
- [Kite order types](https://support.zerodha.com/category/trading-and-markets/general-kite/others-kite/articles/order-types-and-execution): use as a terminology/behavior reference; order execution is not guaranteed.
- [Kite portfolio manual](https://kite.trade/docs/kite/portfolio/): T1 holdings presentation reference.

These references do not establish Upstox account permissions or certify simulation prices, fees, financing or payout times.

## Final evidence required

1. Complete T1-04 settlement processing and presentation, then required financial/ownership/idempotency/concurrency tests.
2. Run full backend/frontend/tooling verification and isolated MySQL 8.4 migration/restore/restart checks against the same candidate.
3. Run `node scripts/build-v2-release.mjs --require-containers`, verify archive/payload checksums and migration catalogue, and retain rollback artifacts.
4. Complete final desktop/mobile rendered acceptance and user-operated phone/passkey/owner MFA acceptance. Automated checks are separate evidence.
5. Verify final hosting/HTTPS/mail/provider/calendar/owner/backup gates. The sole open user-action list is DEPLOYMENT_CHECKLIST.md; no requests are duplicated here.

Earlier archive `InvestSmart-v2-57A7Tw.tar.gz` certifies its prior snapshot only. No public deployment is authorized by this audit and no completed launch is asserted.

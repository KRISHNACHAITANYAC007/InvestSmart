package com.example.investsmart.settlement;

import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in synthetic MySQL observations, never an operator URL, index migration or SLA certificate. */
final class ReportingQueryCostProbe {
    static void run(DataSource source)throws Exception{
        var capture=new ReportingQueryCapture(source);var observations=new ArrayList<Map<String,Object>>();
        var at=JournalReportFixture.AT.plusSeconds(172800);var to=at.plusSeconds(1);
        try(var f=new JournalReportFixture(capture,false)){
            f.entry(1,"cost-credit","SETTLED_CREDIT","10",at);
            f.db.update("INSERT INTO paper_financing(id,user_id,stock_id,product,quantity,cost_amount,principal,interest,interest_rate,created_at,last_accrued_date) VALUES('cost-position',1,1,'MTF',0,100,75,0,0.1,?,'2026-11-23')",Timestamp.from(at.minusSeconds(86400)));
            f.db.update("INSERT INTO paper_finance_events VALUES('cost-interest','cost-position',1,'INTEREST',0,2.50,?)",Timestamp.from(at));
            f.db.update("INSERT INTO paper_tickets(id,user_id,request_id,side,product,variety,order_kind,validity,quantity,filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,policy_version,expires_at,created_at) VALUES('cost-ticket',1,'cost-ticket','BUY','CASH','REGULAR','MARKET','DAY',1,1,1,1,1,10,0,'FILLED',1,'test',?,?)",Timestamp.from(to),Timestamp.from(at));
            f.db.update("INSERT INTO paper_fills VALUES('cost-fill','cost-ticket',1,0,1,10,1,NULL,?)",Timestamp.from(at));
            new SettlementFunds(f.db,null,null,Clock.fixed(at,ZoneOffset.UTC)).journal(1,"cost-policy","PENDING_CREDIT",new java.math.BigDecimal("30.00"),null,null,"cost-source","NSE","EQUITY","2026-11-23","REUSABLE");
            f.db.update("INSERT INTO settlement_obligations(id,user_id,source_key,exchange_code,segment_code,trade_date,obligation_kind,credit_policy,amount,remaining_credit,status,created_at) VALUES('cost-obligation',1,'cost-source','NSE','EQUITY','2026-11-23','CASH_CREDIT','REUSABLE',30,0,'SETTLED',?)",Timestamp.from(at.minusSeconds(86400)));
            int seeded=0;
            for(int foreignRows:List.of(100,2000)){
                var args=new ArrayList<Object[]>();for(int i=seeded;i<foreignRows;i++)args.add(new Object[]{"cost-foreign-"+i,Timestamp.from(at)});
                f.write.executeWithoutResult(ignored->{f.db.batchUpdate("INSERT INTO settlement_journal VALUES(2,?,'WITHDRAW','synthetic-cost',1,NULL,NULL,?)",args);
                f.db.batchUpdate("INSERT INTO paper_finance_events VALUES(?,'cost-position',2,'INTEREST',0,1,?)",args);
                f.db.batchUpdate("INSERT INTO paper_fills VALUES(?,?,2,0,1,10,1,NULL,?)",args.stream().map(a->new Object[]{a[0],"ticket-"+a[0],a[1]}).toList());});seeded=foreignRows;
                for(String table:List.of("settlement_journal","paper_finance_events","paper_fills","settlement_obligations"))f.db.execute("ANALYZE TABLE "+table);
                measure(source,capture,observations,"journal",foreignRows,()->f.context.getBean(JournalPeriodService.class).read(f.owner.getEmail(),at,to));
                measure(source,capture,observations,"finance",foreignRows,()->f.context.getBean(FinancePeriodService.class).read(f.owner.getEmail(),at,to));
                measure(source,capture,observations,"execution",foreignRows,()->f.context.getBean(ExecutionFeePeriodService.class).read(f.owner.getEmail(),at,to));
                measure(source,capture,observations,"pending-policy",foreignRows,()->f.context.getBean(PendingPolicyPeriodService.class).read(f.owner.getEmail(),at,to));
                var journal=f.context.getBean(JournalPeriodService.class).read(f.owner.getEmail(),at,to);assertEquals(2,journal.entries().size());assertFalse(journal.toString().contains("cost-foreign"));
                assertEquals(1,f.context.getBean(FinancePeriodService.class).read(f.owner.getEmail(),at,to).entries().size());
                assertEquals(1,f.context.getBean(ExecutionFeePeriodService.class).read(f.owner.getEmail(),at,to).selectedFills().size());
                assertEquals("REUSABLE",f.context.getBean(PendingPolicyPeriodService.class).read(f.owner.getEmail(),at,to).entries().getFirst().policy().policy());
            }
            var candidateScenarios=new ArrayList<Map<String,Object>>();
            for(int target:List.of(100,1000,1001)){
                int current=f.db.queryForObject("SELECT COUNT(*) FROM settlement_obligations WHERE user_id=1 AND obligation_kind='CASH_CREDIT'",Integer.class);
                var args=new ArrayList<Object[]>();for(int i=current;i<target;i++)args.add(new Object[]{"cost-owned-"+i,"cost-owned-source-"+i,Timestamp.from(at.minusSeconds(86400))});
                f.write.executeWithoutResult(ignored->f.db.batchUpdate("INSERT INTO settlement_obligations(id,user_id,source_key,exchange_code,segment_code,trade_date,obligation_kind,credit_policy,amount,remaining_credit,status,created_at) VALUES(?,1,?,'NSE','EQUITY','2026-11-23','CASH_CREDIT','REUSABLE',30,0,'SETTLED',?)",args));
                f.db.execute("ANALYZE TABLE settlement_obligations");
                var policies=f.context.getBean(PendingPolicyPeriodService.class);capture.clear();
                var result=policies.read(f.owner.getEmail(),at,to);assertEquals(1,result.entries().size());assertFalse(result.selectedTruncated());assertEquals(target>1000,result.candidatesTruncated());
                if(target<=1000){assertEquals("REUSABLE",result.entries().getFirst().policy().policy());assertEquals("cost-obligation",result.entries().getFirst().policy().obligationId());}
                else{assertNull(result.entries().getFirst().policy().policy());assertNull(result.entries().getFirst().policy().obligationId());assertTrue(result.limitations().contains("CANDIDATE_LIMIT"));}
                candidateScenarios.add(Map.of("ownedCandidates",target,"selectedInstructions",1,"candidatesTruncated",result.candidatesTruncated(),"reportingQueries",capture.snapshot().size(),"meaning","Settled zero-remaining/outside-period candidates; no current cash availability inference"));
                assertEquals(3,capture.snapshot().size());
                measure(source,capture,observations,"pending-owned-"+target,2000,()->policies.read(f.owner.getEmail(),at,to));
            }
            f.db.update("DELETE FROM settlement_obligations WHERE user_id=1 AND id='cost-owned-1000'");
            assertEquals(1000,f.db.queryForObject("SELECT COUNT(*) FROM settlement_obligations WHERE user_id=1 AND obligation_kind='CASH_CREDIT'",Integer.class));
            // Repeated original instruction shapes with unique journal keys stress policy proof, not cash totals.
            var instructionArgs=new ArrayList<Object[]>();for(int i=0;i<999;i++)instructionArgs.add(new Object[]{"cost-instruction-"+i});
            f.write.executeWithoutResult(ignored->f.db.batchUpdate("INSERT INTO settlement_journal SELECT user_id,?,action,fingerprint,amount,reference_kind,reference_id,occurred_at FROM settlement_journal WHERE user_id=1 AND event_key='cost-policy'",instructionArgs));
            var policies=f.context.getBean(PendingPolicyPeriodService.class);capture.clear();var thousand=policies.read(f.owner.getEmail(),at,to);
            assertEquals(1000,thousand.entries().size());assertFalse(thousand.selectedTruncated());assertFalse(thousand.candidatesTruncated());assertTrue(thousand.entries().stream().allMatch(e->"REUSABLE".equals(e.policy().policy())));assertEquals(3,capture.snapshot().size());
            measure(source,capture,observations,"pending-selected-1000",2000,()->policies.read(f.owner.getEmail(),at,to));
            var instructions=f.pendingPolicyRepository.instructions(1,at,to);var candidates=f.pendingPolicyRepository.candidates(1);var rules=f.context.getBean(PendingCreditPolicyReconciler.class);
            // Compare complete proofs outside timing; timing assertions alone only checked policy labels.
            var expectedProofs=instructions.stream().map(i->rules.reconcile(i.row(),i.fingerprint(),candidates,false)).toList();
            var preparedProofs=rules.prepare(candidates,false);
            assertEquals(expectedProofs,instructions.stream().map(i->preparedProofs.reconcile(i.row(),i.fingerprint())).toList());
            assertEquals(1000,expectedProofs.size());
            var cpu=java.lang.management.ManagementFactory.getThreadMXBean();var pureSamples=new ArrayList<Map<String,Object>>();
            for(int repeat=0;repeat<3;repeat++){
                long cpuStart=cpu.isCurrentThreadCpuTimeSupported()&&cpu.isThreadCpuTimeEnabled()?cpu.getCurrentThreadCpuTime():-1;long start=System.nanoTime();
                for(var instruction:instructions)assertEquals("REUSABLE",rules.reconcile(instruction.row(),instruction.fingerprint(),candidates,false).policy());
                double wallMs=(System.nanoTime()-start)/1_000_000.0;long cpuEnd=cpuStart>=0?cpu.getCurrentThreadCpuTime():-1;
                var sample=new LinkedHashMap<String,Object>();sample.put("wallMs",wallMs);sample.put("currentThreadCpuMs",cpuStart>=0&&cpuEnd>=cpuStart?(cpuEnd-cpuStart)/1_000_000.0:null);pureSamples.add(sample);
            }
            var preparedSamples=new ArrayList<Map<String,Object>>();
            for(int repeat=0;repeat<3;repeat++){
                long cpuStart=cpu.isCurrentThreadCpuTimeSupported()&&cpu.isThreadCpuTimeEnabled()?cpu.getCurrentThreadCpuTime():-1;long start=System.nanoTime();var prepared=rules.prepare(candidates,false);
                for(var instruction:instructions)assertEquals("REUSABLE",prepared.reconcile(instruction.row(),instruction.fingerprint()).policy());
                double wallMs=(System.nanoTime()-start)/1_000_000.0;long cpuEnd=cpuStart>=0?cpu.getCurrentThreadCpuTime():-1;var sample=new LinkedHashMap<String,Object>();sample.put("wallMs",wallMs);sample.put("currentThreadCpuMs",cpuStart>=0&&cpuEnd>=cpuStart?(cpuEnd-cpuStart)/1_000_000.0:null);preparedSamples.add(sample);
            }
            f.db.update("INSERT INTO settlement_journal SELECT user_id,'cost-instruction-sentinel',action,fingerprint,amount,reference_kind,reference_id,occurred_at FROM settlement_journal WHERE user_id=1 AND event_key='cost-policy'");
            capture.clear();var limited=policies.read(f.owner.getEmail(),at,to);assertTrue(limited.selectedTruncated());assertEquals(1000,limited.entries().size());assertTrue(limited.entries().stream().allMatch(e->e.policy().policy()==null));assertEquals(2,capture.snapshot().size());
            measure(source,capture,observations,"pending-selected-1001",2000,()->policies.read(f.owner.getEmail(),at,to));
            var emptyFrom=to.plusSeconds(60);var emptyTo=emptyFrom.plusSeconds(1);var wideTo=at.plus(Duration.ofDays(366));
            for(boolean empty:List.of(true,false)){
                var start=empty?emptyFrom:at;var end=empty?emptyTo:wideTo;var label=empty?"empty":"366day";
                var journal=f.context.getBean(JournalPeriodService.class);var finance=f.context.getBean(FinancePeriodService.class);var execution=f.context.getBean(ExecutionFeePeriodService.class);
                assertEquals(empty?0:1000,journal.read(f.owner.getEmail(),start,end).entries().size());assertEquals(!empty,journal.read(f.owner.getEmail(),start,end).truncated());
                assertEquals(empty?0:1,finance.read(f.owner.getEmail(),start,end).entries().size());assertEquals(empty?0:1,execution.read(f.owner.getEmail(),start,end).selectedFills().size());
                assertEquals(empty?0:1000,policies.read(f.owner.getEmail(),start,end).entries().size());
                measure(source,capture,observations,"journal-"+label,2000,()->journal.read(f.owner.getEmail(),start,end));measure(source,capture,observations,"finance-"+label,2000,()->finance.read(f.owner.getEmail(),start,end));measure(source,capture,observations,"execution-"+label,2000,()->execution.read(f.owner.getEmail(),start,end));measure(source,capture,observations,"pending-"+label,2000,()->policies.read(f.owner.getEmail(),start,end));
            }
            var productScenarios=products(source,capture,observations,f,at.plusSeconds(3600));
            var batchScenarios=batches(source,capture,observations,f,at.plusSeconds(7200));
            var mixedScenario=mixed(source,capture,observations,f,at.plusSeconds(7200));
            var distinctScenario=distinctInstructions(source,capture,observations,f,at.plusSeconds(1800));
            var parallelScenarios=List.of(parallelReads(f,at,to),parallelReads(f,at.plusSeconds(7200),at.plusSeconds(7202)));
            var overflowScenarios=overflows(source,capture,observations,f,at.plusSeconds(7200));
            var indexComparison=compareIndexes(source,capture,observations,f,at,to);
            var report=new LinkedHashMap<String,Object>();report.put("engine",f.db.queryForObject("SELECT VERSION()",String.class));report.put("scope","Fresh synthetic source graphs, foreign/owned/selection bounds, CPU, ranges and100/101batch transitions; no SLA.");report.put("productScenarios",productScenarios);report.put("batchScenarios",batchScenarios);report.put("pureMatchingSamples",pureSamples);report.put("pureMatchingShape","1000 unique event keys with repeated normalized original instruction,1000 candidates; policy evidence only, not1000economic transactions. Thread CPU excludes other threads/GC and is null if unsupported.");report.put("ownedCandidateScenarios",candidateScenarios);report.put("rowsExamined","Not available as exact statement counter; Handler_read deltas and EXPLAIN ANALYZE actual iterator rows are separate observations, not equivalent.");
            var counts=new LinkedHashMap<String,Long>();var indexes=new LinkedHashMap<String,Object>();for(String table:List.of("settlement_journal","paper_finance_events","paper_fills","settlement_obligations")){counts.put(table,f.db.queryForObject("SELECT COUNT(*) FROM "+table,Long.class));indexes.put(table,f.db.queryForList("SHOW INDEX FROM "+table));}
            report.put("preparedMatchingSamples",preparedSamples);report.put("preparedScope","Includes request-local candidate preparation; identical single-row proof and error precedence, no global/persistent/user-shared cache.");
            report.put("matchingComparison",Map.of("instructions",expectedProofs.size(),"fullProofsEqual",true,"scope","Complete evidence DTO equality against original single-row oracle, outside timed samples."));
            report.put("fixtureSeeding","Test-only explicit seed transactions commit before each measured read; no measured request is inside a fixture write transaction.");
            report.put("mixedScenario",mixedScenario);
            report.put("distinctInstructionScenario",distinctScenario);
            report.put("parallelReadScenarios",parallelScenarios);
            report.put("overflowScenarios",overflowScenarios);report.put("indexComparison",indexComparison);report.put("finalTableRows",counts);report.put("indexes",indexes);report.put("observations",observations);report.put("remaining","Broader scale/concurrency, threshold acceptance and approved source-index remediation remain open; synthetic repeatability does not establish a production SLA.");
            Files.createDirectories(Path.of("target"));Files.writeString(Path.of("target/core05bg06-prepared-match-cost.json"),new tools.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report));
        }
    }
    private static Map<String,Object> parallelReads(JournalReportFixture f,Instant from,Instant to)throws Exception{
        var journal=f.context.getBean(JournalPeriodService.class);var finance=f.context.getBean(FinancePeriodService.class);
        var execution=f.context.getBean(ExecutionFeePeriodService.class);var pending=f.context.getBean(PendingPolicyPeriodService.class);
        var requests=new ArrayList<Supplier<Object>>();for(var user:List.of(f.owner,f.other)){
            requests.add(()->journal.read(user.getEmail(),from,to));requests.add(()->finance.read(user.getEmail(),from,to));
            requests.add(()->execution.read(user.getEmail(),from,to));requests.add(()->pending.read(user.getEmail(),from,to));
        }
        var balances=f.db.queryForList("SELECT * FROM settlement_accounts ORDER BY user_id");var users=f.db.queryForList("SELECT * FROM users ORDER BY id");var holdings=f.db.queryForList("SELECT * FROM holdings ORDER BY id");
        var expected=requests.stream().map(Supplier::get).toList();var pool=java.util.concurrent.Executors.newFixedThreadPool(4);
        try{
            for(int repeat=0;repeat<3;repeat++){
                var tasks=requests.stream().map(request->(java.util.concurrent.Callable<Object>)request::get).toList();
                var futures=pool.invokeAll(tasks,30,java.util.concurrent.TimeUnit.SECONDS);
                for(int i=0;i<futures.size();i++){assertFalse(futures.get(i).isCancelled(),"Parallel read exceeded synthetic check bound");assertEquals(expected.get(i),futures.get(i).get());}
            }
        }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS));}
        assertEquals(balances,f.db.queryForList("SELECT * FROM settlement_accounts ORDER BY user_id"));assertEquals(users,f.db.queryForList("SELECT * FROM users ORDER BY id"));assertEquals(holdings,f.db.queryForList("SELECT * FROM holdings ORDER BY id"));
        return Map.of("from",from.toString(),"to",to.toString(),"identities",2,"workers",4,"rounds",3,"completedReads",24,"fullDtosEqualToSequential",true,"scope","Small simultaneous-read correctness check, not throughput/latency or production concurrent-capacity acceptance. No writes; no cross-request cache.");
    }
    private static Map<String,Object> distinctInstructions(DataSource source,ReportingQueryCapture capture,List<Map<String,Object>> output,JournalReportFixture f,Instant at)throws Exception{
        var candidates=f.pendingPolicyRepository.candidates(1);assertEquals(1000,candidates.size());
        f.write.executeWithoutResult(ignored->{var writer=new SettlementFunds(f.db,null,null,Clock.fixed(at,ZoneOffset.UTC));
            for(var candidate:candidates)writer.journal(1,"distinct-"+candidate.id(),"PENDING_CREDIT",candidate.originalAmount(),null,null,candidate.source(),candidate.exchange(),candidate.segment(),candidate.tradeDate().toString(),candidate.policy());
        });
        var end=at.plusSeconds(1);var instructions=f.pendingPolicyRepository.instructions(1,at,end);assertEquals(1000,instructions.size());assertEquals(1000,instructions.stream().map(i->i.fingerprint()).distinct().count());
        var rules=f.context.getBean(PendingCreditPolicyReconciler.class);var prepared=rules.prepare(candidates,false);
        var expected=instructions.stream().map(i->rules.reconcile(i.row(),i.fingerprint(),candidates,false)).toList();
        assertEquals(expected,instructions.stream().map(i->prepared.reconcile(i.row(),i.fingerprint())).toList());assertEquals(1000,expected.stream().map(e->e.obligationId()).distinct().count());assertTrue(expected.stream().allMatch(e->e.obligationId()!=null));
        var service=f.context.getBean(PendingPolicyPeriodService.class);capture.clear();var selected=service.read(f.owner.getEmail(),at,end);assertFalse(selected.selectedTruncated());assertFalse(selected.candidatesTruncated());assertEquals(expected,selected.entries().stream().map(e->e.policy()).toList());assertEquals(3,capture.snapshot().size());
        var cpu=java.lang.management.ManagementFactory.getThreadMXBean();var controlSamples=new ArrayList<Map<String,Object>>();var preparedSamples=new ArrayList<Map<String,Object>>();
        for(boolean indexed:List.of(false,true))for(int repeat=0;repeat<3;repeat++){
            long cpuStart=cpu.isCurrentThreadCpuTimeSupported()&&cpu.isThreadCpuTimeEnabled()?cpu.getCurrentThreadCpuTime():-1;long start=System.nanoTime();var lookup=indexed?rules.prepare(candidates,false):null;
            for(int i=0;i<instructions.size();i++){var instruction=instructions.get(i);assertEquals(expected.get(i),indexed?lookup.reconcile(instruction.row(),instruction.fingerprint()):rules.reconcile(instruction.row(),instruction.fingerprint(),candidates,false));}
            double wallMs=(System.nanoTime()-start)/1_000_000.0;long cpuEnd=cpuStart>=0?cpu.getCurrentThreadCpuTime():-1;var sample=new LinkedHashMap<String,Object>();sample.put("wallMs",wallMs);sample.put("currentThreadCpuMs",cpuStart>=0&&cpuEnd>=cpuStart?(cpuEnd-cpuStart)/1_000_000.0:null);(indexed?preparedSamples:controlSamples).add(sample);
        }
        measure(source,capture,output,"pending-distinct-1000",2000,()->service.read(f.owner.getEmail(),at,end));
        var candidate=candidates.getFirst();new SettlementFunds(f.db,null,null,Clock.fixed(at,ZoneOffset.UTC)).journal(1,"distinct-sentinel","PENDING_CREDIT",candidate.originalAmount(),null,null,candidate.source(),candidate.exchange(),candidate.segment(),candidate.tradeDate().toString(),candidate.policy());
        capture.clear();var limited=service.read(f.owner.getEmail(),at,end);assertTrue(limited.selectedTruncated());assertEquals(1000,limited.entries().size());assertTrue(limited.entries().stream().allMatch(e->e.policy().obligationId()==null&&e.policy().policy()==null));assertEquals(2,capture.snapshot().size());
        measure(source,capture,output,"pending-distinct-1001",2000,()->service.read(f.owner.getEmail(),at,end));
        return Map.of("instructions",1000,"distinctFingerprints",1000,"matchedObligationIdentities",1000,"fullProofsEqual",true,"controlSamples",controlSamples,"preparedSamples",preparedSamples,"sentinelQueries",2,"scope","Actual writer encoding on distinct synthetic instructions; same owned candidate snapshot, no availability/account-history proof. Preparation included in optimized samples; CPU granularity preserved.");
    }
    private static Map<String,Object> mixed(DataSource source,ReportingQueryCapture capture,List<Map<String,Object>> output,JournalReportFixture f,Instant at)throws Exception{
        var addedAt=at.plusSeconds(1);var end=at.plusSeconds(2);
        f.write.executeWithoutResult(ignored->{
            f.entry(1,"mixed-credit","SETTLED_CREDIT","10",addedAt);
            new SettlementFunds(f.db,null,null,Clock.fixed(addedAt,ZoneOffset.UTC)).journal(1,"mixed-policy","PENDING_CREDIT",new java.math.BigDecimal("30.00"),null,null,"cost-source","NSE","EQUITY","2026-11-23","REUSABLE");
            var rows=new ArrayList<Object[]>();for(int i=0;i<100;i++)rows.add(new Object[]{"mixed-finance-"+i,Timestamp.from(addedAt)});
            f.db.batchUpdate("INSERT INTO paper_finance_events VALUES(?,'cost-position',1,'INTEREST',0,1,?)",rows);
        });
        // These are independent synthetic source graphs, never an economically complete account.
        var balances=f.db.queryForList("SELECT * FROM settlement_accounts ORDER BY user_id");
        var users=f.db.queryForList("SELECT * FROM users ORDER BY id");var holdings=f.db.queryForList("SELECT * FROM holdings ORDER BY id");
        var journal=f.context.getBean(JournalPeriodService.class);var finance=f.context.getBean(FinancePeriodService.class);
        var execution=f.context.getBean(ExecutionFeePeriodService.class);var policies=f.context.getBean(PendingPolicyPeriodService.class);
        capture.clear();var charges=journal.read(f.owner.getEmail(),at,end);assertEquals(305,charges.entries().size());assertFalse(charges.truncated());assertFalse(charges.fundingEvidenceTruncated());assertFalse(charges.completeFinancialHistory());
        assertEquals(101,charges.chargeGrouping().sourceEvidence().size());assertTrue(charges.chargeGrouping().groups().stream().allMatch(g->g.outsideSelectionChildKeys().isEmpty()));
        // 101 complete charges at -70 each plus one direct +10; pending contributes zero.
        assertEquals(new java.math.BigDecimal("-7060.00"),charges.selectedGroupedChanges().buckets().get(0).change());assertEquals(new java.math.BigDecimal("0.00"),charges.selectedGroupedChanges().buckets().get(1).change());
        assertTrue(charges.selectedDirectChanges().buckets().stream().allMatch(b->b.change()==null));assertFalse(charges.selectedGroupedChanges().fullAccountSummaryAvailable());assertEquals(10,capture.snapshot().size());
        capture.clear();var financed=finance.read(f.owner.getEmail(),at,end);assertEquals(100,financed.entries().size());assertFalse(financed.truncated());assertEquals(2,capture.snapshot().size());
        capture.clear();var fees=execution.read(f.owner.getEmail(),at,end);assertEquals(101,fees.selectedFills().size());assertFalse(fees.selectedTruncated());assertFalse(fees.contextTruncated());assertTrue(fees.tickets().stream().allMatch(t->new java.math.BigDecimal("1.00").equals(t.selectedFee())));assertEquals(6,capture.snapshot().size());
        capture.clear();var pending=policies.read(f.owner.getEmail(),at,end);assertEquals(1,pending.entries().size());assertEquals("cost-obligation",pending.entries().getFirst().policy().obligationId());assertEquals(3,capture.snapshot().size());
        measure(source,capture,output,"mixed-journal",2000,()->journal.read(f.owner.getEmail(),at,end));measure(source,capture,output,"mixed-finance",2000,()->finance.read(f.owner.getEmail(),at,end));
        measure(source,capture,output,"mixed-execution",2000,()->execution.read(f.owner.getEmail(),at,end));measure(source,capture,output,"mixed-pending",2000,()->policies.read(f.owner.getEmail(),at,end));
        var wideEnd=at.plus(Duration.ofDays(366));
        capture.clear();var wideJournal=journal.read(f.owner.getEmail(),at,wideEnd);assertEquals(charges.entries(),wideJournal.entries());assertEquals(charges.chargeGrouping(),wideJournal.chargeGrouping());assertEquals(charges.selectedGroupedChanges().buckets(),wideJournal.selectedGroupedChanges().buckets());assertFalse(wideJournal.truncated());assertFalse(wideJournal.fundingEvidenceTruncated());assertEquals(10,capture.snapshot().size());
        capture.clear();var wideFinance=finance.read(f.owner.getEmail(),at,wideEnd);assertEquals(financed.entries(),wideFinance.entries());assertFalse(wideFinance.truncated());assertEquals(2,capture.snapshot().size());
        capture.clear();var wideFees=execution.read(f.owner.getEmail(),at,wideEnd);assertEquals(fees.selectedFills(),wideFees.selectedFills());assertEquals(fees.tickets(),wideFees.tickets());assertFalse(wideFees.selectedTruncated());assertFalse(wideFees.contextTruncated());assertEquals(6,capture.snapshot().size());
        capture.clear();var widePending=policies.read(f.owner.getEmail(),at,wideEnd);assertEquals(pending.entries(),widePending.entries());assertFalse(widePending.selectedTruncated());assertFalse(widePending.candidatesTruncated());assertEquals(3,capture.snapshot().size());
        measure(source,capture,output,"mixed-366day-journal",2000,()->journal.read(f.owner.getEmail(),at,wideEnd));measure(source,capture,output,"mixed-366day-finance",2000,()->finance.read(f.owner.getEmail(),at,wideEnd));
        measure(source,capture,output,"mixed-366day-execution",2000,()->execution.read(f.owner.getEmail(),at,wideEnd));measure(source,capture,output,"mixed-366day-pending",2000,()->policies.read(f.owner.getEmail(),at,wideEnd));
        assertEquals(balances,f.db.queryForList("SELECT * FROM settlement_accounts ORDER BY user_id"));assertEquals(users,f.db.queryForList("SELECT * FROM users ORDER BY id"));assertEquals(holdings,f.db.queryForList("SELECT * FROM holdings ORDER BY id"));
        return Map.of("journalRows",305,"completeCharges",101,"financeRows",100,"fills",101,"pendingInstructions",1,"queries",Map.of("journal",10,"finance",2,"execution",6,"pending",3),"selectedGroupedSettledChange","-7060.00","selectedGroupedReservedChange","0.00","wideRangeDays",366,"scope","Mixed two-second and 366-day windows have equal selected evidence/proofs and query counts, not full account expenses/P&L/history; balances/users/holdings unchanged by all repeated reads.");
    }
    private static List<Map<String,Object>> overflows(DataSource source,ReportingQueryCapture capture,List<Map<String,Object>> output,JournalReportFixture f,Instant at)throws Exception{
        var results=new ArrayList<Map<String,Object>>();
        f.write.executeWithoutResult(ignored->{for(int i=101;i<501;i++){
            String key="matrix-charge-"+i,ref=ChargeEvidenceReconciler.reference(key),allocation="matrix-allocation-"+i;f.entry(1,key,"REALIZED_CHARGE","70",at);
            f.db.update("INSERT INTO settlement_allocations VALUES(?,1,'CHARGE',?,'SETTLED',50,0,'CLOSED',?)",allocation,ref,Timestamp.from(at));
            for(String action:List.of("RESERVE","SPEND")){String child="CHARGE_"+action+":"+ref;f.entry(1,child,action,"50",at.plusSeconds(1));f.db.update("UPDATE settlement_journal SET reference_kind='CHARGE',reference_id=? WHERE user_id=1 AND event_key=?",ref,child);f.db.update("INSERT INTO settlement_allocation_events VALUES(1,?,?,?,50)",child,allocation,action);}
        }});
        var journal=f.context.getBean(JournalPeriodService.class);var execution=f.context.getBean(ExecutionFeePeriodService.class);var end=at.plusSeconds(1);
        capture.clear();var children=journal.read(f.owner.getEmail(),at,end);assertEquals(501,children.entries().size());assertTrue(children.chargeGrouping().groups().isEmpty());assertTrue(children.chargeGrouping().sourceEvidence().isEmpty());assertEquals(8,capture.snapshot().size());
        results.add(Map.of("case","charge-child-budget","selectedParents",501,"queryCount",8,"proofAvailable",false));measure(source,capture,output,"charge-child-overflow",2000,()->journal.read(f.owner.getEmail(),at,end));
        var extra=new ArrayList<Object[]>();for(int i=1;i<=1000;i++)extra.add(new Object[]{"matrix-extra-fill-"+i,i,Timestamp.from(at.minusSeconds(1))});
        f.write.executeWithoutResult(ignored->f.db.batchUpdate("INSERT INTO paper_fills VALUES(?,'matrix-mis-0',1,?,1,10,0,NULL,?)",extra));
        capture.clear();var fills=execution.read(f.owner.getEmail(),at,end);assertEquals(101,fills.selectedFills().size());assertTrue(fills.contextTruncated());assertTrue(fills.tickets().stream().allMatch(t->t.selectedFee()==null));assertEquals(3,capture.snapshot().size());
        results.add(Map.of("case","adversarial-MIS-fill-context","selectedFills",101,"extraOutsideRows",1000,"queryCount",3,"proofAvailable",false));measure(source,capture,output,"mis-fill-context-overflow",2000,()->execution.read(f.owner.getEmail(),at,end));
        var sourceAt=at.plusSeconds(3600);var sourceEnd=sourceAt.plusSeconds(1);
        f.write.executeWithoutResult(ignored->{
        for(int i=0;i<4;i++)f.db.update("INSERT INTO settlement_obligations(id,user_id,source_key,exchange_code,segment_code,trade_date,obligation_kind,credit_policy,amount,remaining_credit,status,created_at) VALUES(?,1,?,'NSE','EQUITY','2026-11-25','CASH_CREDIT','REUSABLE',1000,0,'SETTLED',?)","matrix-credit-"+i,"matrix-credit-source-"+i,Timestamp.from(sourceAt));
        for(int i=0;i<101;i++){
            String key="matrix-source-charge-"+i,ref=ChargeEvidenceReconciler.reference(key);f.entry(1,key,"REALIZED_CHARGE","70",sourceAt);
            for(String action:List.of("RESERVE","SPEND")){String child="CHARGE_"+action+":"+ref;f.entry(1,child,action,"50",sourceAt.plusSeconds(1));f.db.update("UPDATE settlement_journal SET reference_kind='CHARGE',reference_id=? WHERE user_id=1 AND event_key=?",ref,child);}
            for(int s=0;s<5;s++){String allocation="matrix-source-"+i+"-"+s;f.db.update("INSERT INTO settlement_allocations VALUES(?,1,'CHARGE',?,?,10,0,'CLOSED',?)",allocation,ref,s==0?"SETTLED":"matrix-credit-"+(s-1),Timestamp.from(sourceAt));for(String action:List.of("RESERVE","SPEND"))f.db.update("INSERT INTO settlement_allocation_events VALUES(1,?,?,?,10)","CHARGE_"+action+":"+ref,allocation,action);}
        }});
        capture.clear();var sources=journal.read(f.owner.getEmail(),sourceAt,sourceEnd);assertEquals(101,sources.entries().size());assertTrue(sources.chargeGrouping().groups().isEmpty());assertTrue(sources.chargeGrouping().sourceEvidence().isEmpty());assertEquals(7,capture.snapshot().size());
        results.add(Map.of("case","charge-source-budget","selectedParents",101,"sourceRows",1010,"queryCount",7,"proofAvailable",false));measure(source,capture,output,"charge-source-overflow",2000,()->journal.read(f.owner.getEmail(),sourceAt,sourceEnd));return results;
    }
    private static Map<String,Object> compareIndexes(DataSource source,ReportingQueryCapture capture,List<Map<String,Object>> output,JournalReportFixture f,Instant at,Instant to)throws Exception{
        var finance=f.context.getBean(FinancePeriodService.class);var execution=f.context.getBean(ExecutionFeePeriodService.class);var beforeFinance=finance.read(f.owner.getEmail(),at,to);var beforeExecution=execution.read(f.owner.getEmail(),at,to);
        measure(source,capture,output,"finance-index-before",2000,()->finance.read(f.owner.getEmail(),at,to));measure(source,capture,output,"fills-index-before",2000,()->execution.read(f.owner.getEmail(),at,to));
        try{
            f.db.execute("CREATE INDEX ix_cost_probe_finance ON paper_finance_events(user_id,occurred_at,event_key)");f.db.execute("CREATE INDEX ix_cost_probe_fills ON paper_fills(user_id,created_at,id)");f.db.execute("ANALYZE TABLE paper_finance_events,paper_fills");
            assertEquals(beforeFinance,finance.read(f.owner.getEmail(),at,to));assertEquals(beforeExecution,execution.read(f.owner.getEmail(),at,to));measure(source,capture,output,"finance-index-after",2000,()->finance.read(f.owner.getEmail(),at,to));measure(source,capture,output,"fills-index-after",2000,()->execution.read(f.owner.getEmail(),at,to));
        }finally{f.db.execute("DROP INDEX ix_cost_probe_finance ON paper_finance_events");f.db.execute("DROP INDEX ix_cost_probe_fills ON paper_fills");}
        return Map.of("scope","Disposable test schema only; exact DTOs unchanged; indexes removed before final catalog. No production DDL or migration acceptance.","financeColumns",List.of("user_id","occurred_at","event_key"),"fillColumns",List.of("user_id","created_at","id"));
    }
    private static List<Map<String,Object>> products(DataSource source,ReportingQueryCapture capture,List<Map<String,Object>> output,JournalReportFixture f,Instant at)throws Exception{
        var result=new ArrayList<Map<String,Object>>();
        f.db.update("INSERT INTO paper_tickets(id,user_id,request_id,side,product,variety,order_kind,validity,quantity,legs,lot_size,ceiling_price,reserved_cash,status,policy_version,expires_at,created_at) VALUES('matrix-ticket',1,'matrix-ticket','BUY','CASH','ICEBERG','MARKET','DAY',1001,1001,1,10,0,'PART_FILLED','test',?,?)",Timestamp.from(at.plusSeconds(1)),Timestamp.from(at));
        int seeded=0;for(int size:List.of(1000,1001)){
            var rows=new ArrayList<Object[]>();var fills=new ArrayList<Object[]>();for(int i=seeded;i<size;i++){rows.add(new Object[]{"matrix-finance-"+i,Timestamp.from(at)});fills.add(new Object[]{"matrix-fill-"+i,i,Timestamp.from(at)});}seeded=size;
            f.write.executeWithoutResult(ignored->{f.db.batchUpdate("INSERT INTO paper_finance_events VALUES(?,'cost-position',1,'INTEREST',0,1,?)",rows);f.db.batchUpdate("INSERT INTO paper_fills VALUES(?,'matrix-ticket',1,?,1,10,1,NULL,?)",fills);
            f.db.update("UPDATE paper_tickets SET filled_units=?,filled_legs=?,fees=?,status=? WHERE id='matrix-ticket'",size,size,size,size==1000?"PART_FILLED":"FILLED");});
            var finance=f.context.getBean(FinancePeriodService.class);var execution=f.context.getBean(ExecutionFeePeriodService.class);var end=at.plusSeconds(1);
            var financed=finance.read(f.owner.getEmail(),at,end);assertEquals(1000,financed.entries().size());assertEquals(size>1000,financed.truncated());
            capture.clear();var executed=execution.read(f.owner.getEmail(),at,end);assertEquals(1000,executed.selectedFills().size());assertEquals(size>1000,executed.selectedTruncated());assertEquals(size==1000?3:2,capture.snapshot().size());
            if(size==1000)assertEquals(new java.math.BigDecimal("1000.00"),executed.tickets().getFirst().selectedFee());else assertTrue(executed.tickets().stream().allMatch(t->t.selectedFee()==null));
            result.add(Map.of("ownedRows",size,"rawRowsReturned",1000,"truncated",size>1000,"executionQueries",capture.snapshot().size()));
            measure(source,capture,output,"finance-owned-"+size,2000,()->finance.read(f.owner.getEmail(),at,end));measure(source,capture,output,"fills-owned-"+size,2000,()->execution.read(f.owner.getEmail(),at,end));
        }return result;
    }
    private static List<Map<String,Object>> batches(DataSource source,ReportingQueryCapture capture,List<Map<String,Object>> output,JournalReportFixture f,Instant at)throws Exception{
        var results=new ArrayList<Map<String,Object>>();int seeded=0;
        for(int size:List.of(100,101)){
            final int seedStart=seeded;
            f.write.executeWithoutResult(ignored->{for(int i=seedStart;i<size;i++){
                String key="matrix-charge-"+i,ref=ChargeEvidenceReconciler.reference(key),allocation="matrix-allocation-"+i;
                f.entry(1,key,"REALIZED_CHARGE","70",at);
                for(String action:List.of("RESERVE","SPEND")){
                    String child="CHARGE_"+action+":"+ref;f.entry(1,child,action,"50",at.plusSeconds(1));f.db.update("UPDATE settlement_journal SET reference_kind='CHARGE',reference_id=? WHERE user_id=1 AND event_key=?",ref,child);
                    f.db.update("INSERT INTO settlement_allocation_events VALUES(1,?,?,?,50)",child,allocation,action);
                }
                f.db.update("INSERT INTO settlement_allocations VALUES(?,1,'CHARGE',?,'SETTLED',50,0,'CLOSED',?)",allocation,ref,Timestamp.from(at));
                String ticket="matrix-mis-"+i,position="matrix-pos-"+i;var time=Timestamp.from(at);
                f.db.update("INSERT INTO paper_intraday_positions(id,user_id,stock_id,direction,quantity,entry_price,collateral,realized_pnl,trade_date,created_at) VALUES(?,1,1,'LONG',1,10,5,0,'2026-11-25',?)",position,time);
                f.db.update("INSERT INTO paper_tickets(id,user_id,request_id,stock_id,position_id,side,product,variety,order_kind,validity,quantity,filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,policy_version,expires_at,created_at) VALUES(?,1,?,1,?,'BUY','MIS','REGULAR','MARKET','DAY',1,1,1,1,1,10,0,'FILLED',1,'test',?,?)",ticket,ticket,position,time,time);
                f.db.update("INSERT INTO paper_fills VALUES(?,?,1,0,1,10,1,NULL,?)","fill-"+ticket,ticket,time);f.db.update("INSERT INTO paper_intraday_events VALUES(?,1,?,'OPEN',1,5,0,1,?)",ticket,position,time);
            }});seeded=size;var end=at.plusSeconds(1);var journal=f.context.getBean(JournalPeriodService.class);var execution=f.context.getBean(ExecutionFeePeriodService.class);
            capture.clear();var charges=journal.read(f.owner.getEmail(),at,end);assertEquals(size,charges.entries().size());assertEquals(size,charges.chargeGrouping().sourceEvidence().size());assertTrue(charges.selectedGroupedChanges().buckets().stream().allMatch(b->b.change()==null));int chargeQueries=capture.snapshot().size();assertEquals(size==100?5:7,chargeQueries);
            capture.clear();var fees=execution.read(f.owner.getEmail(),at,end);assertEquals(size,fees.selectedFills().size());assertEquals(size,fees.tickets().size());assertTrue(fees.tickets().stream().allMatch(t->new java.math.BigDecimal("1.00").equals(t.selectedFee())));int feeQueries=capture.snapshot().size();assertEquals(size==100?4:6,feeQueries);
            results.add(Map.of("parentsOrTickets",size,"chargeQueries",chargeQueries,"misQueries",feeQueries,"scope","Independent synthetic source graphs, not historical account balances. Charge children outside window remain context only."));
            measure(source,capture,output,"charge-batch-"+size,2000,()->journal.read(f.owner.getEmail(),at,end));measure(source,capture,output,"mis-batch-"+size,2000,()->execution.read(f.owner.getEmail(),at,end));
        }return results;
    }
    private static void measure(DataSource source,ReportingQueryCapture capture,List<Map<String,Object>> output,String reader,int foreignRows,Supplier<Object> request)throws Exception{
        assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),"Measured reads must follow committed fixture seeding");
        capture.clear();var expected=request.get();var queries=capture.snapshot();assertFalse(queries.isEmpty());var readerMs=new ArrayList<Double>();
        for(int repeat=0;repeat<3;repeat++){long start=System.nanoTime();assertEquals(expected,request.get());readerMs.add((System.nanoTime()-start)/1_000_000.0);}
        for(var query:queries){
            var record=new LinkedHashMap<String,Object>();record.put("reader",reader);record.put("addedForeignRowsPerTable",foreignRows);record.put("sql",query.sql());record.put("parameterTypes",query.parameters().stream().map(p->p==null?"null":p.getClass().getSimpleName()).toList());record.put("readerRepeatedMs",readerMs);
            try(var connection=source.getConnection()){
                var db=new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection,true));
                var params=query.parameters().stream().map(p->p instanceof Instant time?Timestamp.from(time):p instanceof LocalDate date?java.sql.Date.valueOf(date):p).toArray();
                record.put("estimatedPlan",db.queryForObject("EXPLAIN FORMAT=JSON "+query.sql(),String.class,params));
                record.put("actualIteratorPlan",db.queryForList("EXPLAIN ANALYZE "+query.sql(),String.class,params));
                var timings=new ArrayList<Map<String,Object>>();for(int repeat=0;repeat<3;repeat++){
                    var before=handlers(db);long start=System.nanoTime();var rows=db.queryForList(query.sql(),params);double ms=(System.nanoTime()-start)/1_000_000.0;var after=handlers(db);var delta=new LinkedHashMap<String,Long>();after.forEach((key,value)->delta.put(key,value-before.getOrDefault(key,0L)));
                    timings.add(Map.of("returnedRows",rows.size(),"elapsedMs",ms,"handlerReadDelta",delta));assertTrue(rows.size()<=1001);
                }record.put("queryRepeats",timings);
            }output.add(record);
        }
    }
    private static Map<String,Long> handlers(JdbcTemplate db){var result=new LinkedHashMap<String,Long>();db.query("SHOW SESSION STATUS LIKE 'Handler_read%'",(org.springframework.jdbc.core.RowCallbackHandler)r->result.put(r.getString(1),Long.valueOf(r.getString(2))));return result;}
}

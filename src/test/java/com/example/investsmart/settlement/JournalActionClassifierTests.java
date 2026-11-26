package com.example.investsmart.settlement;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.investsmart.settlement.JournalActionClassifier.Kind.*;

class JournalActionClassifierTests {
    final JournalActionClassifier classifier=new JournalActionClassifier();
    BigDecimal n(String value){return new BigDecimal(value);}
    JournalActionClassifier.Interpretation classify(String action,String amount){return classifier.classify(action,n(amount),null,null);}
    @Test void directCashActionsHaveSignedEffectsWithoutCallingThemProfit(){
        var credit=classify("SETTLED_CREDIT","12.50");assertEquals(SETTLED_CREDIT,credit.kind());assertEquals(n("12.50"),credit.settledDelta());assertEquals(n("0.00"),credit.reservedDelta());
        var withdraw=classify("WITHDRAW","12.50");assertEquals(WITHDRAWAL,withdraw.kind());assertEquals(n("-12.50"),withdraw.settledDelta());
        var pending=classify("PENDING_CREDIT","12.50");assertEquals(PENDING_CREDIT,pending.kind());assertEquals(n("0.00"),pending.settledDelta());
        assertTrue(pending.limitation().contains("no reusable/restricted split"));
    }
    @Test void fundingTransferNeverInventsSourceSplitOrDoubleDebitsSettledCash(){
        var reserve=classifier.classify("RESERVE",n("10"),"ORDER","order");assertEquals(RESERVATION,reserve.kind());assertNull(reserve.settledDelta());assertEquals(n("10.00"),reserve.reservedDelta());
        var spend=classifier.classify("SPEND",n("10"),"ORDER","order");assertEquals(RESERVED_SPEND,spend.kind());assertEquals(n("0.00"),spend.settledDelta());assertEquals(n("-10.00"),spend.reservedDelta());
        var refund=classifier.classify("RELEASE",n("10"),"ORDER","order");assertEquals(REFUND,refund.kind());assertNull(refund.settledDelta());assertEquals(n("-10.00"),refund.reservedDelta());
        var charge=classify("REALIZED_CHARGE","10");assertEquals(COMPOSITE_CHARGE,charge.kind());assertNull(charge.settledDelta());assertNull(charge.reservedDelta());
    }
    @Test void zeroSettlementAndMetadataAreValidButZeroChargesAreNot(){
        var settle=classifier.classify("SETTLE",n("0"),"OBLIGATION","obligation");assertEquals(SETTLEMENT_TRANSFER,settle.kind());assertEquals(n("0.00"),settle.settledDelta());
        assertEquals(n("10.00"),classifier.classify("SETTLE",n("10"),"OBLIGATION","obligation").settledDelta());
        assertEquals(DELIVERY_METADATA,classifier.classify("LINK_DELIVERIES",n("0"),"OBLIGATION","obligation").kind());
        assertEquals(INVALID,classifier.classify("LINK_DELIVERIES",n("1"),"OBLIGATION","obligation").kind());
        for(var action:new String[]{"SETTLED_CREDIT","WITHDRAW","PENDING_CREDIT","REALIZED_CHARGE"})assertEquals(INVALID,classify(action,"0").kind());
    }
    @Test void malformedMagnitudesAndReferencesWithholdEveryDelta(){
        for(var amount:new BigDecimal[]{null,n("-1"),n("0.001"),n("100000000000000000.00")}){
            var result=classifier.classify("SETTLED_CREDIT",amount,null,null);assertEquals(INVALID,result.kind());assertNull(result.settledDelta());assertNull(result.reservedDelta());
        }
        assertEquals(INVALID,classifier.classify("RESERVE",n("1"),"ORDER",null).kind());
        assertEquals(INVALID,classifier.classify("RESERVE",n("1"),"order","x").kind());
        assertEquals(INVALID,classifier.classify("RELEASE",n("1"),"ORDER","x".repeat(101)).kind());
        assertEquals(INVALID,classifier.classify("SETTLE",n("1"),"ORDER","x").kind());
        assertEquals(INVALID,classifier.classify("SETTLED_CREDIT",n("1"),"ORDER","x").kind());
        assertEquals(n("99999999999999999.99"),classify("SETTLED_CREDIT","99999999999999999.99").settledDelta());
    }
    @Test void unknownAndLotQuantitiesAreNeverTreatedAsCash(){
        for(var action:new String[]{null,"UNKNOWN","DISPOSE","PURCHASE_LOT","DELIVER_SETTLED","withdraw"}){
            var result=classifier.classify(action,n("100"),"STOCK","1");assertEquals(UNSUPPORTED,result.kind());assertNull(result.settledDelta());assertNull(result.reservedDelta());
        }
    }
    @Test void actualMixedSourceRefundRemainsUnknownEvenAfterSourceSettles(){
        var f=new SettlementFundsTests();f.setup();f.cash("deposit","20");
        var obligation=f.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);f.reserve("reserve","30");
        f.time(SettlementFundsTests.CUTOFF);f.get(()->f.funds.settle(1,obligation));f.release("refund","30");
        var before=f.balances();var result=read(f,"refund");assertEquals(REFUND,result.kind());assertNull(result.settledDelta());
        assertEquals(n("-30.00"),result.reservedDelta());assertEquals(n("50.00"),before.settled());assertEquals(before,f.balances());
        assertEquals(n("20.00"),read(f,"SETTLE:"+obligation).settledDelta());
    }
    @Test void actualChargeHasLinkedFundingAndDebtWhichCannotBeSummedAsOneExpense(){
        var f=new SettlementFundsTests();f.setup();f.cash("deposit","20");f.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);
        f.run(()->f.funds.chargeDebt(1,"charge",n("70")));
        var before=f.balances();assertEquals(n("-20.00"),before.settled());
        var result=read(f,"charge");assertEquals(COMPOSITE_CHARGE,result.kind());assertNull(result.settledDelta());assertNull(result.reservedDelta());
        assertEquals(2,f.db.queryForObject("SELECT COUNT(*) FROM settlement_journal WHERE reference_kind='CHARGE'",Integer.class));
        assertEquals(before,f.balances());
    }
    JournalActionClassifier.Interpretation read(SettlementFundsTests f,String key){
        return f.db.queryForObject("SELECT action,amount,reference_kind,reference_id FROM settlement_journal WHERE user_id=1 AND event_key=?",
            (r,row)->classifier.classify(r.getString(1),r.getBigDecimal(2),r.getString(3),r.getString(4)),key);
    }
}

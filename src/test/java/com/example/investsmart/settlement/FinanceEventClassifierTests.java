package com.example.investsmart.settlement;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.investsmart.settlement.FinanceEventClassifier.Kind.*;

class FinanceEventClassifierTests {
    final FinanceEventClassifier classifier=new FinanceEventClassifier();
    BigDecimal n(String value){return new BigDecimal(value);}

    @Test void borrowingContributionIsNeitherInterestNorAnExpense() {
        var r=classifier.classify("BORROW",n("500.00"),n("500.00"));
        assertEquals(BORROWING,r.kind());assertEquals(n("500.00"),r.principalChange());
        assertEquals(n("500.00"),r.contribution());assertNull(r.interestAccrued());assertNull(r.interestPaid());
    }
    @Test void accrualAndRepaymentAreSeparateViewsOfInterestNotAdditiveExpenses() {
        var accrued=classifier.classify("INTEREST",n("0"),n("1.25"));
        var first=classifier.classify("REPAY",n("-200.00"),n("0.50"));
        var last=classifier.classify("REPAY",n("-300.00"),n("0.75"));
        assertEquals(INTEREST_ACCRUAL,accrued.kind());assertNull(accrued.interestPaid());
        assertEquals(INTEREST_PAYMENT,first.kind());assertNull(first.interestAccrued());
        assertEquals(accrued.interestAccrued(),first.interestPaid().add(last.interestPaid()));
        assertEquals(n("-500.00"),first.principalChange().add(last.principalChange()));
    }
    @Test void zeroInterestAndFullyRepaidPrincipalAreValidWithoutInventedAmounts() {
        assertEquals(INTEREST_ACCRUAL,classifier.classify("INTEREST",n("0"),n("0.00")).kind());
        assertEquals(INTEREST_PAYMENT,classifier.classify("REPAY",n("0"),n("0.00")).kind());
    }
    @Test void malformedDirectionPrecisionOrNegativeInterestFailsInvalid() {
        assertEquals(INVALID,classifier.classify("BORROW",n("-1"),n("1")).kind());
        assertEquals(INVALID,classifier.classify("INTEREST",n("1"),n("1")).kind());
        assertEquals(INVALID,classifier.classify("REPAY",n("1"),n("1")).kind());
        assertEquals(INVALID,classifier.classify("INTEREST",n("0"),n("-1")).kind());
        assertEquals(INVALID,classifier.classify("INTEREST",n("0"),n("0.001")).kind());
        assertEquals(INVALID,classifier.classify("INTEREST",n("0"),n("100000000000000000.00")).kind());
        assertEquals(INVALID,classifier.classify("REPAY",null,n("0")).kind());
        assertNull(classifier.classify("REPAY",null,n("0")).interestPaid());
    }
    @Test void unknownEventNeverBecomesZeroInterest() {
        var r=classifier.classify("FUTURE_EVENT",n("1"),n("2"));
        assertEquals(UNSUPPORTED,r.kind());assertNull(r.interestAccrued());assertNull(r.interestPaid());
        assertEquals(UNSUPPORTED,classifier.classify(null,null,null).kind());
    }
}

package com.example.investsmart.trading;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PaperPolicyTests {
    @Test void contributionAlwaysRoundsUpSoFractionalLeverageCannotUnderfundAPosition(){var p=new PaperPolicy(new BigDecimal("1.5"),new BigDecimal("0.12"),new BigDecimal("0.0003"),new BigDecimal("20"));assertEquals(new BigDecimal("0.02"),p.contribution(new BigDecimal("0.02")));assertEquals(new BigDecimal("66.67"),p.contribution(new BigDecimal("100")));}
    @Test void feesHaveAPaiseFloorAndPerChildCapAndPolicyFingerprintChangesWithRates(){var p=new PaperPolicy(new BigDecimal("2"),new BigDecimal("0.12"),new BigDecimal("0.0003"),new BigDecimal("20"));assertEquals(new BigDecimal("20.00"),p.fee(new BigDecimal("100000")));assertEquals(new BigDecimal("0.30"),p.fee(new BigDecimal("1000")));var changed=new PaperPolicy(new BigDecimal("2"),new BigDecimal("0.15"),new BigDecimal("0.0003"),new BigDecimal("20"));assertNotEquals(p.version(),changed.version());}
}

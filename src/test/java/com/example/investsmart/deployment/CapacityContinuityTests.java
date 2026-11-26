package com.example.investsmart.deployment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CapacityContinuityTests {
    @Test void standbyAndClockReversalInvalidateTheBenchmark(){
        assertThrows(CapacityContinuity.InterruptedBenchmark.class,()->CapacityContinuity.check(1000,24_000,0));
        assertThrows(CapacityContinuity.InterruptedBenchmark.class,()->CapacityContinuity.check(1000,999,0));
    }
    @Test void schedulerCannotReplayAnInterruptedBacklog(){
        assertThrows(CapacityContinuity.InterruptedBenchmark.class,()->CapacityContinuity.check(1000,1001,2_000_000_001L));
        assertDoesNotThrow(()->CapacityContinuity.check(1000,1010,100_000_000L));
    }
}

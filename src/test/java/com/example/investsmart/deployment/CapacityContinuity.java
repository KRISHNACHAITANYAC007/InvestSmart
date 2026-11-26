package com.example.investsmart.deployment;

/** Test-only guard: interrupted benchmarks must not replay a backlog as a burst. */
final class CapacityContinuity {
    private CapacityContinuity() {}
    static void check(long previousWallMillis,long wallMillis,long scheduleLagNanos) {
        if(wallMillis<previousWallMillis||wallMillis-previousWallMillis>20_000||scheduleLagNanos>2_000_000_000L)
            throw new InterruptedBenchmark();
    }
    static final class InterruptedBenchmark extends IllegalStateException {
        InterruptedBenchmark(){super("Benchmark interrupted or load generator unable to maintain schedule; no capacity acceptance.");}
    }
}

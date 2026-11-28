package com.example.investsmart.settlement;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

class ReportingIndexSchemaTests {
    DriverManagerDataSource source(){return new DriverManagerDataSource("jdbc:h2:mem:report-index-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");}
    @Test void populatedUpgradePreservesRecordsReportsAndSkipsPersistedReplay()throws Exception{ReportingIndexTestSupport.verifyUpgrade(source());}
    @Test void freshAndRuntimeBaselinesMatchManagedIndexDefinitions()throws Exception{
        var fresh=source();var runtime=source();ReportingIndexTestSupport.fresh(fresh,false);ReportingIndexTestSupport.fresh(runtime,true);
        assertEquals(ReportingIndexTestSupport.indexes(fresh),ReportingIndexTestSupport.indexes(runtime));
    }
    @Test void partialIndexFailureStaysStartedAndCannotAutomaticallyReplay()throws Exception{
        var source=source();try(var f=new JournalReportFixture(source,true)){
            ReportingIndexTestSupport.seed(f);var before=ReportingIndexTestSupport.rows(f.db);
            f.db.execute("CREATE INDEX ix_paper_fill_user_time ON paper_fills(user_id)");
            assertThrows(java.sql.SQLException.class,()->ReportingIndexTestSupport.apply(source));
            assertEquals("STARTED",f.db.queryForObject("SELECT state FROM investsmart_migrations",String.class));
            assertThrows(IllegalArgumentException.class,()->ReportingIndexTestSupport.apply(source));
            assertEquals(before,ReportingIndexTestSupport.rows(f.db));
            assertNotEquals(ReportingIndexTestSupport.EXPECTED,ReportingIndexTestSupport.indexes(source));
        }
    }
}

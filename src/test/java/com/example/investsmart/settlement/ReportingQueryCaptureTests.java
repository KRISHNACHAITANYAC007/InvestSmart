package com.example.investsmart.settlement;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
class ReportingQueryCaptureTests {
    ReportingQueryCapture source(){return new ReportingQueryCapture(new DriverManagerDataSource("jdbc:h2:mem:capture-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));}
    @Test void capturesActualRepositoryQueryWithImmutableParametersAndIdenticalResults(){
        var capture=source();try(var f=new JournalReportFixture(capture,true)){
            f.entry(1,"owned","WITHDRAW","1",JournalReportFixture.AT);f.entry(2,"foreign","WITHDRAW","9",JournalReportFixture.AT);capture.clear();
            var result=f.periodRepository.entries(1,JournalReportFixture.AT,JournalReportFixture.AT.plusSeconds(1));
            assertEquals(1,result.size());var queries=capture.snapshot();assertEquals(1,queries.size());assertTrue(queries.getFirst().sql().contains("LIMIT 1001"));assertEquals(List.of(1L,JournalReportFixture.AT,JournalReportFixture.AT.plusSeconds(1)),queries.getFirst().parameters());
            assertThrows(UnsupportedOperationException.class,()->queries.clear());assertThrows(UnsupportedOperationException.class,()->queries.getFirst().parameters().clear());
        }
    }
    @Test void reusedStatementsCaptureNullAndClearedBindingsWithoutCapturingWrites()throws Exception{
        var capture=source();var db=new JdbcTemplate(capture);db.execute("CREATE TABLE sample(id INT)");db.update("INSERT INTO sample VALUES(?)",1);assertTrue(capture.snapshot().isEmpty());
        try(var connection=capture.getConnection();var query=connection.prepareStatement("SELECT ?")){
            query.setInt(1,7);query.executeQuery().close();query.clearParameters();query.setNull(1,java.sql.Types.INTEGER);query.executeQuery().close();
        }
        assertEquals(List.of(7),capture.snapshot().getFirst().parameters());assertNull(capture.snapshot().getLast().parameters().getFirst());capture.clear();assertTrue(capture.snapshot().isEmpty());
    }
}

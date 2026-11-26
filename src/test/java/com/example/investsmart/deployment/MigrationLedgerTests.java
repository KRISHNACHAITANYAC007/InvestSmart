package com.example.investsmart.deployment;

import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MigrationLedgerTests {
    Connection database()throws Exception{return DriverManager.getConnection("jdbc:h2:mem:ledger-"+UUID.randomUUID()+";MODE=MySQL","sa","");}
    @Test void statusDoesNotCreateTablesAndWritesRequireExplicitOperatorFlags()throws Exception{
        try(var db=database()){
            MigrationLedger.run(db,new String[]{"--status"});assertFalse(MigrationLedger.exists(db));
            assertThrows(IllegalArgumentException.class,()->MigrationLedger.run(db,new String[]{"--apply","20261110_phone_verification.sql"}));assertFalse(MigrationLedger.exists(db));
        }
    }
    @Test void phoneUpgradePreservesOldSessionsAndLedgerRefusesReplayAndChangedChecksums()throws Exception{
        try(var db=database()){
            db.createStatement().execute("CREATE TABLE auth_sessions(id INT PRIMARY KEY)");db.createStatement().execute("INSERT INTO auth_sessions VALUES(1)");
            String[] args={"--apply","20261110_phone_verification.sql","--backup-confirmed","--writes-stopped"};
            MigrationLedger.run(db,args);MigrationLedger.run(db,args);
            try(var rows=db.createStatement().executeQuery("SELECT factor_completed FROM auth_sessions WHERE id=1")){assertTrue(rows.next());assertFalse(rows.getBoolean(1));}
            db.createStatement().execute("UPDATE investsmart_migrations SET checksum_sha256='changed'");
            assertThrows(IllegalArgumentException.class,()->MigrationLedger.run(db,args));
        }
    }
    @Test void failedDdlIsRecordedAndCannotBeRetriedAutomatically()throws Exception{
        try(var db=database()){
            String[] args={"--apply","20261110_phone_verification.sql","--backup-confirmed","--writes-stopped"};
            assertThrows(SQLException.class,()->MigrationLedger.run(db,args));
            db.createStatement().execute("CREATE TABLE auth_sessions(id INT)");
            assertThrows(IllegalArgumentException.class,()->MigrationLedger.run(db,args));
        }
    }
}

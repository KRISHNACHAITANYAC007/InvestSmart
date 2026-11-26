package com.example.investsmart.security;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/** Local/test convenience only. Managed deployments apply the versioned migration explicitly. */
@Component
public class SecuritySchema {
    public SecuritySchema(DataSource dataSource,@Value("${investsmart.security.initialize-schema:true}") boolean initialize) {
        if(initialize){new ResourceDatabasePopulator(new ClassPathResource("db/security-schema.sql")).execute(dataSource);ensureColumn(dataSource,"auth_sessions","factor_completed","BOOLEAN NOT NULL DEFAULT FALSE");ensureColumn(dataSource,"auth_sessions","last_activity_at","TIMESTAMP NULL");var db=new org.springframework.jdbc.core.JdbcTemplate(dataSource);db.update("UPDATE auth_sessions SET last_activity_at=created_at WHERE last_activity_at IS NULL");}
        else { var db=new org.springframework.jdbc.core.JdbcTemplate(dataSource);for(String table:java.util.List.of("auth_sessions","phone_credentials","phone_actions","phone_sessions","auth_actions","passkey_credentials","passkey_challenges","trusted_device_locks","admin_credentials","owner_admin","security_events","security_alerts","admin_audit","security_mutex","equity_pending_orders","option_positions","option_orders","mail_outbox","mail_action_links","mail_delivery_failures","admin_balance_adjustments","admin_listing_overrides","admin_runtime_health","paper_tickets","paper_fills","paper_financing","paper_finance_events","settlement_calendar_versions","settlement_calendar_days","settlement_calendar_active","settlement_calendar_audit","settlement_calendar_mutex","settlement_accounts","settlement_obligations","settlement_lots","settlement_allocations","settlement_journal","settlement_allocation_events","settlement_lot_events","settlement_dependencies","settlement_checks","paper_intraday_positions","paper_intraday_events"))db.queryForList("SELECT * FROM "+table+" WHERE 1=0");db.queryForList("SELECT factor_completed FROM auth_sessions WHERE 1=0"); }
    }
    private void ensureColumn(DataSource dataSource,String table,String column,String definition){try(var connection=dataSource.getConnection()){var metadata=connection.getMetaData();for(String tableName:java.util.List.of(table,table.toUpperCase(java.util.Locale.ROOT)))for(String columnName:java.util.List.of(column,column.toUpperCase(java.util.Locale.ROOT)))try(var columns=metadata.getColumns(connection.getCatalog(),null,tableName,columnName)){if(columns.next())return;}try(var statement=connection.createStatement()){statement.execute("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition);}}catch(java.sql.SQLException failure){throw new IllegalStateException("Could not ensure Admin V2 schema column "+table+"."+column,failure);}}
}

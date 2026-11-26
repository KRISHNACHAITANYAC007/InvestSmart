package com.example.investsmart.deployment;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

/** Explicit one-file-at-a-time operator tool, independent of application/schema startup. */
public final class MigrationLedger {
    static final Path DIRECTORY=Path.of("src/main/resources/db/migrations");
    private MigrationLedger(){}
    public static void main(String[] args){
        try{
            if(args.length==1&&"--catalog".equals(args[0])){
                try(var files=Files.list(DIRECTORY)){for(Path path:files.filter(p->p.toString().endsWith(".sql")).sorted().toList())System.out.println(path.getFileName()+" "+checksum(path));}
                return;
            }
            if(args.length==0)throw new IllegalArgumentException("Use --catalog, --status, --apply FILE or --adopt FILE. Writes also require --backup-confirmed --writes-stopped.");
            String url=required("DB_URL");if(!url.startsWith("jdbc:mysql:"))throw new IllegalArgumentException("DB_URL must use MySQL.");
            try(var connection=DriverManager.getConnection(url,required("DB_USERNAME"),required("DB_PASSWORD"))){run(connection,args);}
        }catch(Exception failure){
            // JDBC diagnostics can include connection strings, usernames or supplied SQL values.
            System.err.println(failure instanceof IllegalArgumentException?failure.getMessage():"Migration stopped. Inspect the ledger and database privately before retrying; no automatic repair was attempted.");
            System.exit(1);
        }
    }
    public static void run(Connection connection,String[] args)throws Exception{
        String operation=args[0];
        if("--status".equals(operation)){
            if(!exists(connection)){System.out.println("No migration ledger. Inspect the schema and adopt only independently verified migrations.");return;}
            try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT version_name,checksum_sha256,state FROM investsmart_migrations ORDER BY recorded_at,version_name")){
                while(rows.next())System.out.println(rows.getString(1)+" "+rows.getString(2)+" "+rows.getString(3));
            }return;
        }
        if(!List.of("--apply","--adopt").contains(operation)||args.length!=4
                ||!Set.of(args[2],args[3]).equals(Set.of("--backup-confirmed","--writes-stopped")))
            throw new IllegalArgumentException("Use --apply FILE or --adopt FILE with --backup-confirmed --writes-stopped.");
        String version=args[1];
        if(!version.matches("[0-9]{8}_[a-z0-9_]+\\.sql"))throw new IllegalArgumentException("Choose an exact migration filename from --catalog.");
        Path path=DIRECTORY.resolve(version);if(!Files.isRegularFile(path))throw new IllegalArgumentException("Migration file is missing.");
        String checksum=checksum(path);
        // Serialize operators on MySQL. H2 is used only by isolated automated tests.
        boolean mysql=connection.getMetaData().getDatabaseProductName().equals("MySQL");
        if(mysql)try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT GET_LOCK('investsmart-migration',0)")){if(!rows.next()||rows.getInt(1)!=1)throw new IllegalArgumentException("Another migration operator holds the lock.");}
        try{
            try(var statement=connection.createStatement()){statement.execute("CREATE TABLE IF NOT EXISTS investsmart_migrations(version_name VARCHAR(150) PRIMARY KEY,checksum_sha256 VARCHAR(64) NOT NULL,state VARCHAR(20) NOT NULL,recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");}
            try(var query=connection.prepareStatement("SELECT checksum_sha256,state FROM investsmart_migrations WHERE version_name=?")){
                query.setString(1,version);try(var rows=query.executeQuery()){if(rows.next()){
                    if(!checksum.equals(rows.getString(1)))throw new IllegalArgumentException("Migration checksum changed. Stop and reconcile the history.");
                    if(!List.of("APPLIED","ADOPTED").contains(rows.getString(2)))throw new IllegalArgumentException("Migration is incomplete. Restore/reconcile privately before retrying; automatic replay is refused.");
                    System.out.println(version+" already recorded; skipped.");return;
                }}
            }
            try(var query=connection.prepareStatement("INSERT INTO investsmart_migrations(version_name,checksum_sha256,state) VALUES(?,?,?)")){
                query.setString(1,version);query.setString(2,checksum);query.setString(3,"--adopt".equals(operation)?"ADOPTED":"STARTED");query.executeUpdate();
            }
            if("--apply".equals(operation)){
                // Shipping scripts contain no stored procedures or semicolons inside SQL literals.
                String sql=Files.readString(path,StandardCharsets.UTF_8).replaceAll("(?m)^\\s*--[^\\r\\n]*","");
                for(String statement:sql.split(";"))if(!statement.isBlank())try(var query=connection.createStatement()){query.execute(statement);}
                try(var query=connection.prepareStatement("UPDATE investsmart_migrations SET state='APPLIED' WHERE version_name=?")){query.setString(1,version);query.executeUpdate();}
            }
            System.out.println(version+("--apply".equals(operation)?" applied.":" adopted after operator schema inspection."));
        }finally{if(mysql)try(var statement=connection.createStatement()){statement.execute("SELECT RELEASE_LOCK('investsmart-migration')");}}
    }
    static boolean exists(Connection connection)throws SQLException{
        for(String name:List.of("investsmart_migrations","INVESTSMART_MIGRATIONS"))try(var tables=connection.getMetaData().getTables(connection.getCatalog(),null,name,new String[]{"TABLE"})){if(tables.next())return true;}return false;
    }
    static String checksum(Path path)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readString(path,StandardCharsets.UTF_8).replace("\r\n","\n").getBytes(StandardCharsets.UTF_8)));}
    private static String required(String key){String value=System.getenv(key);if(value==null||value.isBlank())throw new IllegalArgumentException("Missing "+key);return value;}
}

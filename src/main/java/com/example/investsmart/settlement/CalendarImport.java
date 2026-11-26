package com.example.investsmart.settlement;

import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Deliberate operator command. Does not start the application or load local Spring configuration. */
public final class CalendarImport {
    private CalendarImport(){}
    public static void main(String[] args){
        try{
            Map<String,String> options=options(args);
            ReviewedCalendar calendar=ReviewedCalendar.read(Path.of(options.get("--file")),options.get("--exchange"),options.get("--segment"),options.get("--source"));
            if(options.containsKey("--validate")){System.out.println("PASS: reviewed calendar CSV shape, contiguous dates and content checksum. No database changed.");return;}
            if(!options.containsKey("--reviewed"))throw new IllegalArgumentException("Import requires --reviewed after independently checking source and every date.");
            String url=required("DB_URL");if(!url.startsWith("jdbc:mysql:"))throw new IllegalArgumentException("Calendar import requires a MySQL database.");
            var source=new DriverManagerDataSource(url,required("DB_USERNAME"),required("DB_PASSWORD"));
            var service=new SettlementCalendar(new JdbcTemplate(source),Clock.systemUTC());
            String id=new TransactionTemplate(new DataSourceTransactionManager(source)).execute(status->service.activate(calendar,"OPERATOR_CLI",options.get("--reason")));
            System.out.println("Calendar activated: "+id+"; "+calendar.exchange()+"/"+calendar.segment()+"; "+calendar.days().getFirst().date()+" through "+calendar.days().getLast().date()+". Source review is operator-attested, not inferred from CSV shape.");
        }catch(IllegalArgumentException failure){System.err.println(failure.getMessage());System.exit(1);}
        catch(Exception failure){System.err.println("Calendar operation stopped. Inspect private configuration/schema and retry only after resolving the failure; no raw file/JDBC diagnostics are printed.");System.exit(1);}
    }
    static Map<String,String> options(String[] args){
        Set<String> valued=Set.of("--file","--exchange","--segment","--source","--reason");Map<String,String> out=new LinkedHashMap<>();
        for(int i=0;i<args.length;i++){
            String key=args[i];if(out.containsKey(key))throw new IllegalArgumentException("Duplicate calendar option.");
            if(Set.of("--validate","--reviewed").contains(key))out.put(key,"true");
            else if(valued.contains(key)&&i+1<args.length&&!args[i+1].startsWith("--"))out.put(key,args[++i]);
            else throw new IllegalArgumentException("Use --file PATH --exchange NSE|BSE|MCX --segment SEGMENT --source HTTPS_URL and either --validate or --reviewed --reason TEXT.");
        }
        for(String key:Set.of("--file","--exchange","--segment","--source"))if(!out.containsKey(key))throw new IllegalArgumentException("Calendar option required: "+key);
        if(out.containsKey("--validate")==out.containsKey("--reviewed"))throw new IllegalArgumentException("Choose exactly one of --validate or --reviewed.");
        if(out.containsKey("--reviewed")&&!out.containsKey("--reason"))throw new IllegalArgumentException("Calendar review reason is required.");return out;
    }
    private static String required(String name){String value=System.getenv(name);if(value==null||value.isBlank())throw new IllegalArgumentException("Required private setting: "+name);return value;}
}

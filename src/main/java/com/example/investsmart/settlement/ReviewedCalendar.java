package com.example.investsmart.settlement;

import java.net.URI;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;

/** Complete, operator-reviewed date coverage. No weekday/holiday assumptions are generated. */
public record ReviewedCalendar(String exchange,String segment,String source,String checksum,List<Day> days) {
    public record Day(LocalDate date,boolean tradingOpen,boolean settlementOpen,String note) {}
    public ReviewedCalendar {
        exchange=code(exchange,Set.of("NSE","BSE","MCX"));
        segment=code(segment,Set.of("EQUITY","FNO","CURRENCY","COMMODITY","MUTUAL_FUND","BOND"));
        try {URI uri=URI.create(source);if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||source.length()>1000)throw new IllegalArgumentException();}
        catch(Exception invalid){throw new IllegalArgumentException("Calendar source must be a public HTTPS URL without credentials, query or fragment.");}
        if(checksum==null||!checksum.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Calendar content checksum is required.");
        if(days==null||days.isEmpty()||days.size()>1830)throw new IllegalArgumentException("Calendar must contain 1–1830 explicitly reviewed dates.");
        if(days.stream().anyMatch(day->day==null||day.date()==null))throw new IllegalArgumentException("Every calendar row needs a valid date.");
        var sorted=new ArrayList<>(days);sorted.sort(Comparator.comparing(Day::date));
        for(int i=0;i<sorted.size();i++){var day=sorted.get(i);if(day.date()==null||day.note()==null||day.note().length()>300||day.note().contains("\r")||day.note().contains("\n"))throw new IllegalArgumentException("Calendar date/note is invalid.");if(i>0&&!day.date().equals(sorted.get(i-1).date().plusDays(1)))throw new IllegalArgumentException("Calendar contains duplicate dates or missing date coverage.");}
        days=List.copyOf(sorted);
    }
    private static String code(String value,Set<String> allowed){String normalized=value==null?"":value.toUpperCase(Locale.ROOT);if(!allowed.contains(normalized))throw new IllegalArgumentException("Unsupported calendar exchange or segment.");return normalized;}
    public static ReviewedCalendar read(Path path,String exchange,String segment,String source)throws java.io.IOException {
        if(Files.size(path)>4*1024*1024)throw new IllegalArgumentException("Calendar file is too large.");
        return parse(Files.readAllBytes(path),exchange,segment,source);
    }
    public static ReviewedCalendar parse(byte[] bytes,String exchange,String segment,String source) {
        if(bytes==null||bytes.length>4*1024*1024)throw new IllegalArgumentException("Calendar file is too large.");
        String text;
        try{text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();}
        catch(CharacterCodingException invalid){throw new IllegalArgumentException("Calendar must be UTF-8 text.");}
        if(text.startsWith("\ufeff"))text=text.substring(1);
        String[] lines=text.split("\\R",-1);
        if(lines.length==0||!lines[0].equals("date,trading_open,settlement_open,note"))throw new IllegalArgumentException("Expected calendar CSV header: date,trading_open,settlement_open,note");
        var days=new ArrayList<Day>();
        for(int i=1;i<lines.length;i++){
            if(lines[i].isBlank())continue;
            try{var fields=fields(lines[i]);if(fields.size()!=4||days.size()>=1830)throw new IllegalArgumentException();days.add(new Day(LocalDate.parse(fields.get(0)),flag(fields.get(1)),flag(fields.get(2)),fields.get(3)));}
            catch(RuntimeException invalid){throw new IllegalArgumentException("Invalid calendar CSV row "+(i+1)+". Values are not echoed.");}
        }
        try{return new ReviewedCalendar(exchange,segment,source,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),days);}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException("SHA-256 unavailable.");}
    }
    private static boolean flag(String value){if(value.equals("true"))return true;if(value.equals("false"))return false;throw new IllegalArgumentException();}
    private static List<String> fields(String line){
        var out=new ArrayList<String>();var field=new StringBuilder();boolean quoted=false,closed=false;
        for(int i=0;i<line.length();i++){char c=line.charAt(i);if(quoted){if(c=='"'){if(i+1<line.length()&&line.charAt(i+1)=='"'){field.append('"');i++;}else{quoted=false;closed=true;}}else field.append(c);}
            else if(c==','){out.add(field.toString());field.setLength(0);closed=false;}
            else if(c=='"'){if(field.length()!=0||closed)throw new IllegalArgumentException();quoted=true;}
            else {if(closed)throw new IllegalArgumentException();field.append(c);}}
        if(quoted)throw new IllegalArgumentException();out.add(field.toString());return out;
    }
}

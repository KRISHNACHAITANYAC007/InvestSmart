package com.example.investsmart.reports;

import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.security.SecurityEvents;
import com.example.investsmart.settlement.JournalPeriodService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/reports")
public class ReportController {
    private final UserRepository users;private final SecurityEvents events;private final JournalPeriodService reports;private final JournalPeriodReportMapper mapper;
    public ReportController(UserRepository users,SecurityEvents events,JournalPeriodService reports,JournalPeriodReportMapper mapper){this.users=users;this.events=events;this.reports=reports;this.mapper=mapper;}
    @GetMapping("/journal-period")
    public JournalPeriodReport journal(Principal principal,HttpServletRequest request){
        if(principal==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var user=users.findByEmailIgnoreCase(principal.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));user.requireActive();
        var params=request.getParameterMap();if(!params.keySet().equals(Set.of("from","to"))||params.values().stream().anyMatch(v->v.length!=1))throw invalid();
        var from=time(request.getParameter("from"));var to=time(request.getParameter("to"));
        if(!from.isBefore(to)||from.isBefore(Instant.parse("1970-01-01T00:00:01Z"))||to.isAfter(Instant.parse("2038-01-19T03:14:07Z"))||Duration.between(from,to).compareTo(Duration.ofDays(366))>0)throw invalid();
        events.limit("REPORT_JOURNAL",user.getEmail(),request,10,30,user.getId());
        return mapper.map(reports.read(principal.getName(),from,to));
    }
    private static Instant time(String value){if(value==null||!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z"))throw invalid();try{var parsed=Instant.parse(value);if(!parsed.toString().equals(value))throw invalid();return parsed;}catch(DateTimeParseException bad){throw invalid();}}
    private static ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Supply one from and to UTC whole-second timestamp in an increasing range of at most 366 days.");}
}

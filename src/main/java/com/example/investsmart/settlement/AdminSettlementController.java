package com.example.investsmart.settlement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/operations/settlement-calendars")
public class AdminSettlementController {
    public record Update(@NotBlank String exchange,@NotBlank String segment,@NotBlank @Size(max=1000) String source,
                         @NotBlank @Size(max=1000000) String csv,@NotBlank @Size(min=5,max=300) String reason,boolean reviewed) {}
    private final AdminSettlementService service;
    public AdminSettlementController(AdminSettlementService service) { this.service=service; }
    @GetMapping public List<SettlementCalendar.Coverage> coverage() { return service.coverage(); }
    @PostMapping public SettlementCalendar.Coverage activate(Principal principal,@Valid @RequestBody Update body,HttpServletRequest request) {
        return service.activate(principal.getName(),body.exchange(),body.segment(),body.source(),body.csv(),body.reason(),body.reviewed(),request);
    }
}

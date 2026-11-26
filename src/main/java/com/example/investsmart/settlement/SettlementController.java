package com.example.investsmart.settlement;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/settlement")
public class SettlementController {
    private final SettlementStatus status;
    public SettlementController(SettlementStatus status){this.status=status;}
    @GetMapping("/summary") public SettlementStatus.Summary summary(Authentication auth){return status.summary(status.user(auth.getName()));}
    @GetMapping("/history") public List<SettlementStatus.Entry> history(Authentication auth,@RequestParam(defaultValue="1") int page){return status.history(status.user(auth.getName()),page);}
    @GetMapping("/holdings") public List<SettlementStatus.HoldingUnits> holdings(Authentication auth){return status.holdings(status.user(auth.getName()));}
}

package com.example.investsmart.investment;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
@RestController
public class InvestmentController {
    private final IpoMarketService ipos;private final BondMarketService bonds;private final InvestmentAccountService accounts;private final IpoApplicationTransactions transactions;
    public InvestmentController(IpoMarketService ipos,BondMarketService bonds,InvestmentAccountService accounts,IpoApplicationTransactions transactions){this.ipos=ipos;this.bonds=bonds;this.accounts=accounts;this.transactions=transactions;}
    @GetMapping("/market/ipos") public IpoMarketService.Page<IpoMarketService.Ipo> ipos(@RequestParam(defaultValue="open") String status,@RequestParam(defaultValue="all") String issueType,@RequestParam(defaultValue="1") int page){return ipos.list(status,issueType,page);}
    @GetMapping("/market/ipos/{id}") public IpoMarketService.Ipo ipo(@PathVariable String id){return ipos.detail(id,false);}
    @GetMapping("/market/bonds") public BondMarketService.BondPage bonds(@RequestParam(defaultValue="all") String category,@RequestParam(defaultValue="") String query,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="false") boolean verifiedOnly){return bonds.list(category,query,page,verifiedOnly);}
    @GetMapping("/market/bonds/detail") public BondMarketService.Detail bond(@RequestParam String instrumentKey){return bonds.detail(instrumentKey);}
    @GetMapping("/investments/saved") public List<InvestmentAccountService.Interest> saved(Principal principal){return accounts.saved(principal.getName());}
    public record SaveRequest(@NotBlank String kind,@NotBlank @Size(max=180) String referenceId) { }
    @PostMapping("/investments/saved") public InvestmentAccountService.Interest save(Principal p,@Valid @RequestBody SaveRequest request){
        String name=switch(request.kind()){
            case "IPO" -> ipos.detail(request.referenceId(),false).name();
            case "BOND" -> bonds.resolve(request.referenceId()).name();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select IPO or BOND");
        };return accounts.save(p.getName(),request.kind(),request.referenceId(),name);
    }
    @DeleteMapping("/investments/saved/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(Principal p,@PathVariable Long id){accounts.remove(p.getName(),id);}
    @GetMapping("/investments/ipo-applications") public List<IpoApplicationView> applications(Principal p){return accounts.applications(p.getName());}
    public record ApplyRequest(@NotNull @Min(1) @Max(1000000) Integer lots) { }
    @PostMapping("/investments/ipo-applications/{ipoId}") public IpoApplicationView apply(Principal p,@PathVariable String ipoId,@Valid @RequestBody ApplyRequest request){return transactions.apply(p.getName(),ipos.detail(ipoId,true),request.lots());}
    @PostMapping("/investments/ipo-applications/{id}/cancel") public IpoApplicationView cancel(Principal p,@PathVariable Long id){return transactions.cancel(p.getName(),id);}
}

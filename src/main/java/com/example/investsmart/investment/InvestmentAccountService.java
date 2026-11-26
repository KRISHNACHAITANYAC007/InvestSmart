package com.example.investsmart.investment;
import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;
@Service
public class InvestmentAccountService {
    public record Interest(Long id,String kind,String referenceId,String name,java.time.Instant savedAt) {
        static Interest from(SavedInterest s){return new Interest(s.getId(),s.getKind(),s.getReferenceId(),s.getName(),s.getSavedAt());}
    }
    private final UserRepository users; private final SavedInterestRepository saved; private final IpoApplicationRepository applications; private final Clock clock;
    public InvestmentAccountService(UserRepository users,SavedInterestRepository saved,IpoApplicationRepository applications,Clock clock){this.users=users;this.saved=saved;this.applications=applications;this.clock=clock;}
    private Long userId(String email){return users.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("User not found")).getId();}
    @Transactional(readOnly=true) public List<Interest> saved(String email){return saved.findByUserIdOrderByIdDesc(userId(email)).stream().map(Interest::from).toList();}
    @Transactional(readOnly=true) public List<IpoApplicationView> applications(String email){return applications.findByUserIdOrderByIdDesc(userId(email)).stream().map(IpoApplicationView::from).toList();}
    @Transactional public Interest save(String email,String kind,String referenceId,String name){
        var user=users.findByEmailForUpdate(email).orElseThrow(()->new ResourceNotFoundException("User not found"));user.requireActive();
        var row=saved.findByUserIdAndKindAndReferenceId(user.getId(),kind,referenceId).orElseGet(SavedInterest::new);
        row.setUserId(user.getId());row.setKind(kind);row.setReferenceId(referenceId);row.setName(name);row.setSavedAt(clock.instant());return Interest.from(saved.save(row));
    }
    @Transactional public void remove(String email,Long id){
        var user=users.findByEmailForUpdate(email).orElseThrow(()->new ResourceNotFoundException("User not found"));user.requireActive();
        var row=saved.findById(id).filter(s->s.getUserId().equals(user.getId())).orElseThrow(()->new ResourceNotFoundException("Saved interest not found"));saved.delete(row);
    }
}

package com.example.investsmart.trading;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** A trigger observation survives a later rejected fill without committing financial writes. */
@Component
public class PaperTriggerTransaction {
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void commitObservation(Runnable observation){observation.run();}
}

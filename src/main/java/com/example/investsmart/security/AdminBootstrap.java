package com.example.investsmart.security;
import com.example.investsmart.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;

/** Deliberate one-time CLI operation, never triggered by a signup or an HTTP request. */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;private final JdbcTemplate db;private final SecurityAccounts accounts;private final SecurityEvents events;
    private final String configuredOwnerEmail;
    public AdminBootstrap(UserRepository users,JdbcTemplate db,SecurityAccounts accounts,SecurityEvents events,@Value("${investsmart.admin.email:}") String configuredOwnerEmail){this.users=users;this.db=db;this.accounts=accounts;this.events=events;this.configuredOwnerEmail=configuredOwnerEmail;}
    @Override @Transactional public void run(ApplicationArguments args){
        boolean emailOption=args.containsOption("bootstrap-admin-email"), bootstrapOption=args.containsOption("bootstrap-admin");
        if(!emailOption&&!bootstrapOption)return;
        var values=args.getOptionValues("bootstrap-admin-email");
        String email;
        if(emailOption){if(values==null||values.size()!=1)throw new IllegalStateException("Supply one verified account email.");email=values.getFirst();}
        else {
            var bootstrapValues=args.getOptionValues("bootstrap-admin");
            if(bootstrapValues==null||bootstrapValues.size()!=1||!"true".equalsIgnoreCase(bootstrapValues.getFirst()))throw new IllegalStateException("Use --bootstrap-admin=true for the explicit environment-based bootstrap.");
            if(configuredOwnerEmail.isBlank())throw new IllegalStateException("Set INVESTSMART_ADMIN_EMAIL to the verified owner account email before bootstrapping.");
            email=configuredOwnerEmail;
        }
        if(!args.containsOption("spring.main.web-application-type")||!args.getOptionValues("spring.main.web-application-type").contains("none"))throw new IllegalStateException("Bootstrap must run with --spring.main.web-application-type=none");
        db.queryForObject("SELECT id FROM security_mutex WHERE id=1 FOR UPDATE",Integer.class);
        var user=users.findByEmailIgnoreCase(email).orElseThrow(()->new IllegalStateException("Register and verify the configured owner account before bootstrapping."));
        user=users.findByIdForUpdate(user.getId()).orElseThrow();user.requireActive();if(!user.isEmailVerified())throw new IllegalStateException("Verify the owner's email before bootstrapping.");
        if(accounts.isOwner(user.getId()))return;
        if(db.queryForObject("SELECT COUNT(*) FROM owner_admin",Integer.class)>0)throw new IllegalStateException("An owner administrator already exists.");
        db.update("INSERT INTO owner_admin(singleton_id,user_id) VALUES(1,?)",user.getId());
        user.setRole("ADMIN");users.saveAndFlush(user);accounts.revokeAll(user.getId());events.audit(user.getId(),user.getId(),"OWNER_BOOTSTRAP","Explicit server CLI bootstrap; authenticator enrollment required at next login.");
    }
}

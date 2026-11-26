package com.example.investsmart.security;

import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE,properties={"spring.datasource.url=jdbc:h2:mem:owner-bootstrap-cli;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE","investsmart.admin.email=owner-cli@example.invalid"})
@ActiveProfiles("test")
class NonWebBootstrapTests {
    @Autowired ApplicationContext context;@Autowired AdminBootstrap bootstrap;@Autowired UserRepository users;@Autowired SecurityAccounts accounts;
    @Test void configuredEmailRequiresAnExplicitNonWebBootstrapAndVerifiedAccount(){
        assertTrue(context.getBeansOfType(SecurityFilterChain.class).isEmpty());
        assertDoesNotThrow(()->bootstrap.run(new DefaultApplicationArguments()));
        var owner=new User();owner.setName("Configured owner");owner.setEmail("owner-cli@example.invalid");owner.setRole("USER");owner.setEmailVerified(true);owner.setVirtualBalance(BigDecimal.ZERO);owner=users.saveAndFlush(owner);
        bootstrap.run(new DefaultApplicationArguments("--bootstrap-admin=true","--spring.main.web-application-type=none"));
        assertTrue(accounts.isOwner(owner.getId()));assertEquals("ADMIN",users.findById(owner.getId()).orElseThrow().getRole());assertFalse(accounts.hasMfa(owner.getId()));
    }

    @Test void environmentBootstrapCannotRunInWebMode(){
        assertThrows(IllegalStateException.class,()->bootstrap.run(new DefaultApplicationArguments("--bootstrap-admin=true")));
    }
}

package com.example.investsmart.deployment;

import com.example.investsmart.security.ProductionRequirements;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class ProductionRequirementsTests {
    @Test void shippedMailPolicyRequiresTlsAndCertificateIdentity()throws Exception{
        var properties=new java.util.Properties();
        try(var stream=new org.springframework.core.io.ClassPathResource("application-prod.properties").getInputStream()){properties.load(stream);}
        assertEquals("true",properties.getProperty("spring.mail.properties.mail.smtp.starttls.required"));
        assertEquals("true",properties.getProperty("spring.mail.properties.mail.smtp.ssl.checkserveridentity"));
    }
    MockEnvironment valid(){
        var env=new MockEnvironment();env.setActiveProfiles("prod");
        return env.withProperty("spring.datasource.url","jdbc:mysql://database.invalid/investsmart")
                .withProperty("spring.datasource.username","application").withProperty("spring.datasource.password","fixture-db")
                .withProperty("upstox.api.token","fixture-provider").withProperty("jwt.secret","j".repeat(40))
                .withProperty("investsmart.security.encryption-key","e".repeat(40)).withProperty("spring.mail.host","mail.invalid")
                .withProperty("spring.mail.username","fixture").withProperty("spring.mail.password","fixture-mail")
                .withProperty("investsmart.mail.from","sender@example.invalid").withProperty("investsmart.public-url","https://invest.example")
                .withProperty("investsmart.cors.origins","https://invest.example").withProperty("investsmart.security.secure-cookie","true")
                .withProperty("investsmart.security.initialize-schema","false").withProperty("spring.jpa.hibernate.ddl-auto","validate");
    }
    @Test void validProductionConfigurationPasses(){assertDoesNotThrow(()->new ProductionRequirements(valid()));}
    @Test void mixedProfilesCannotOverrideProductionSafety(){
        for(String[] profiles:new String[][]{{},{"prod","test"},{"local","prod"}}){
            var env=valid();env.setActiveProfiles(profiles);
            assertThrows(IllegalStateException.class,()->new ProductionRequirements(env));
        }
    }
    @Test void missingProviderAndUnsafeDatabaseOrSessionSettingsFail(){
        for(var pair:new String[][]{{"upstox.api.token",""},{"spring.datasource.username","root"},{"spring.datasource.url","jdbc:h2:mem:test"},
                {"jwt.secret","e".repeat(40)},{"investsmart.security.secure-cookie","false"},{"investsmart.security.initialize-schema","true"},
                {"spring.jpa.hibernate.ddl-auto","update"},{"spring.mail.port","70000"}})
            assertThrows(IllegalStateException.class,()->new ProductionRequirements(valid().withProperty(pair[0],pair[1])),pair[0]);
    }
    @Test void invalidOriginsAndSenderFailWithoutEchoingSecrets(){
        for(String value:new String[]{"https://user:private-value@example.invalid","https://invest.example/","https://invest.example/path","https://bad private-value", "http://invest.example"}){
            var failure=assertThrows(IllegalStateException.class,()->new ProductionRequirements(valid().withProperty("investsmart.public-url",value)));
            assertFalse(failure.getMessage().contains(value));assertFalse(failure.getMessage().contains("private-value"));
        }
        assertThrows(IllegalStateException.class,()->new ProductionRequirements(valid().withProperty("investsmart.cors.origins","https://other.example")));
        assertThrows(IllegalStateException.class,()->new ProductionRequirements(valid().withProperty("investsmart.mail.from","invalid")));
    }
}

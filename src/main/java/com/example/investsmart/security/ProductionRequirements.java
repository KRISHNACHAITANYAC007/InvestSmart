package com.example.investsmart.security;
import java.net.URI;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
@Component @Profile("prod")
public class ProductionRequirements {
    public ProductionRequirements(Environment env){
        if(!java.util.Arrays.equals(env.getActiveProfiles(),new String[]{"prod"}))throw new IllegalStateException("Production must run with only the prod profile.");
        for(String key:new String[]{"spring.datasource.url","spring.datasource.username","spring.datasource.password","upstox.api.token","jwt.secret","investsmart.security.encryption-key","spring.mail.host","spring.mail.username","spring.mail.password","investsmart.mail.from","investsmart.public-url","investsmart.cors.origins"})if(env.getProperty(key,"").isBlank())throw new IllegalStateException("Production configuration required: "+key);
        if(!env.getProperty("spring.datasource.url").startsWith("jdbc:mysql:"))throw new IllegalStateException("Production database must use MySQL.");
        if("root".equalsIgnoreCase(env.getProperty("spring.datasource.username").trim()))throw new IllegalStateException("Production requires a non-root database account.");
        if(env.getProperty("investsmart.security.encryption-key").length()<32)throw new IllegalStateException("Use at least 32 random characters for the security encryption key.");
        if(env.getProperty("jwt.secret").getBytes(java.nio.charset.StandardCharsets.UTF_8).length<32)throw new IllegalStateException("Use at least 32 random bytes for the JWT signing key.");
        if(env.getProperty("jwt.secret").equals(env.getProperty("investsmart.security.encryption-key")))throw new IllegalStateException("Use separate JWT signing and security encryption keys.");
        if(!env.getProperty("investsmart.security.secure-cookie",Boolean.class,false)||env.getProperty("investsmart.security.initialize-schema",Boolean.class,true)||!"validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto")))throw new IllegalStateException("Production requires secure cookies, managed schema and schema validation.");
        String publicOrigin=origin(env.getProperty("investsmart.public-url"));
        var cors=java.util.Arrays.stream(env.getProperty("investsmart.cors.origins").split(",",-1)).map(String::trim).map(ProductionRequirements::origin).toList();
        if(!cors.contains(publicOrigin))throw new IllegalStateException("Production CORS origins must include the public origin.");
        if(!env.getProperty("investsmart.mail.from").matches("[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+"))throw new IllegalStateException("Production mail sender must be an email address.");
        for(String key:java.util.List.of("server.port","spring.mail.port")){
            try{int port=Integer.parseInt(env.getProperty(key,key.equals("server.port")?"8080":"587"));if(port<1||port>65535)throw new NumberFormatException();}
            catch(NumberFormatException failure){throw new IllegalStateException("Production port is invalid: "+key);}
        }
    }
    private static String origin(String value){
        try{
            URI uri=URI.create(value);
            if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null
                    ||!uri.getPath().isEmpty()||value.contains("*")||uri.getPort()==0||uri.getPort()>65535)
                throw new IllegalArgumentException();
            return uri.getScheme()+"://"+uri.getRawAuthority();
        }catch(IllegalArgumentException failure){throw new IllegalStateException("Production URLs must be exact HTTPS origins without trailing slashes.");}
    }
}

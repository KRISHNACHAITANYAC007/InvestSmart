package com.example.investsmart.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DevelopmentOriginTests {
    @Test void canonicalizesOnlyHttpLoopbackOrigins(){assertEquals("http://localhost:5173",DevelopmentOrigin.canonical("http://127.0.0.1:5173"));assertEquals("http://localhost:5173",DevelopmentOrigin.canonical("http://[::1]:5173/"));assertEquals("http://localhost:5173",DevelopmentOrigin.canonical("http://localhost:5173"));}
    @Test void preservesProductionAndInvalidOriginsForExistingValidation(){for(String value:new String[]{"https://investsmart.example","https://127.0.0.1:5173","http://127.0.0.1:5173/path","http://user@127.0.0.1:5173","http://127.0.0.1:5173?bad=1","http:///bad"})assertEquals(value,DevelopmentOrigin.canonical(value));}
}

package com.example.investsmart.security;

import java.net.URI;
import java.util.Set;

/** Use a domain RP ID for local WebAuthn; production HTTPS origins are unchanged. */
public final class DevelopmentOrigin {
    private DevelopmentOrigin() {}
    public static String canonical(String value){
        try{
            URI uri=URI.create(value);
            if("http".equals(uri.getScheme())&&uri.getHost()!=null&&Set.of("127.0.0.1","[::1]","::1").contains(uri.getHost())&&uri.getUserInfo()==null&&uri.getQuery()==null&&uri.getFragment()==null&&(uri.getPath().isEmpty()||"/".equals(uri.getPath())))
                return "http://localhost"+(uri.getPort()<0?"":":"+uri.getPort());
        }catch(IllegalArgumentException invalid){/* Existing configuration validation reports malformed origins. */}
        return value;
    }
}

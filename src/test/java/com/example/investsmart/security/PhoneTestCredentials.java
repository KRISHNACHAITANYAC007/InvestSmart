package com.example.investsmart.security;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;

/** Fixture for tests concerned with code verification rather than WebAuthn cryptography. */
final class PhoneTestCredentials {
    static final String SECRET="GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    static final String RECOVERY="test-only-phone-recovery";
    static void enroll(JdbcTemplate db,SecurityCrypto crypto,long user){
        db.update("INSERT INTO phone_credentials(user_id,credential_hash,credential_id,user_handle,public_key_cose,signature_count,secret_cipher,last_step,recovery_hashes,ready,created_at) VALUES(?,?,?,?,?,0,?,-1,?,TRUE,?)",user,crypto.fingerprint("phone-test-"+user),"cGhvbmU","aGFuZGxl","a2V5",crypto.encrypt(SECRET),crypto.fingerprint(RECOVERY),Timestamp.from(Instant.now()));
    }
    static String code(){return SecurityCrypto.totp(SECRET,Instant.now().getEpochSecond()/30);}
}

package com.example.investsmart.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurityCrypto {
    private final byte[] key;
    private final SecureRandom random=new SecureRandom();
    public SecurityCrypto(@Value("${investsmart.security.encryption-key:${jwt.secret}}") String secret){
        try{key=MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}
    }
    public String randomToken(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    public String fingerprint(String text){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    public String encrypt(String text){try{byte[] iv=new byte[12];random.nextBytes(iv);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));byte[] encrypted=c.doFinal(text.getBytes(StandardCharsets.UTF_8));return Base64.getEncoder().encodeToString(ByteBuffer.allocate(12+encrypted.length).put(iv).put(encrypted).array());}catch(Exception e){throw new IllegalStateException("Security encryption failed");}}
    public String decrypt(String text){try{byte[] bytes=Base64.getDecoder().decode(text);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,Arrays.copyOf(bytes,12)));return new String(c.doFinal(Arrays.copyOfRange(bytes,12,bytes.length)),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("Security decryption failed");}}
    public String newTotpSecret(){byte[] b=new byte[20];random.nextBytes(b);return base32(b);}
    private static String base32(byte[] bytes){String alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";StringBuilder s=new StringBuilder();int value=0,bits=0;for(byte b:bytes){value=(value<<8)|(b&255);bits+=8;while(bits>=5){bits-=5;s.append(alphabet.charAt((value>>bits)&31));}}if(bits>0)s.append(alphabet.charAt((value<<(5-bits))&31));return s.toString();}
    public static String totp(String secret,long step){try{int value=0,bits=0;var out=new java.io.ByteArrayOutputStream();for(char ch:secret.toUpperCase(Locale.ROOT).toCharArray()){int n="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(ch);if(n<0)throw new IllegalArgumentException();value=(value<<5)|n;bits+=5;if(bits>=8){bits-=8;out.write((value>>bits)&255);}}Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(out.toByteArray(),"HmacSHA1"));byte[] h=mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());int offset=h[h.length-1]&15;int code=((h[offset]&127)<<24)|((h[offset+1]&255)<<16)|((h[offset+2]&255)<<8)|(h[offset+3]&255);return String.format(Locale.ROOT,"%06d",code%1000000);}catch(Exception e){throw new IllegalArgumentException("Invalid authenticator secret");}}
}

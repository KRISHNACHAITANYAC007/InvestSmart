package com.example.investsmart.security;

import com.upokecenter.cbor.CBORObject;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.*;
import tools.jackson.databind.ObjectMapper;

/** Software authenticator for protocol tests only; real phone acceptance remains separate. */
final class PhoneWebAuthnFixture {
    private final KeyPair pair;
    private final byte[] id=new byte[32];
    private final ObjectMapper json;
    private String userHandle;
    PhoneWebAuthnFixture(ObjectMapper json)throws Exception{
        this.json=json;new SecureRandom().nextBytes(id);
        var generator=KeyPairGenerator.getInstance("EC");generator.initialize(new ECGenParameterSpec("secp256r1"));pair=generator.generateKeyPair();
    }
    String registration(String options,String origin,boolean verified)throws Exception{
        var root=json.readTree(options).path("publicKey");
        userHandle=root.path("user").path("id").asText();
        byte[] client=client("webauthn.create",root.path("challenge").asText(),origin);
        var key=(ECPublicKey)pair.getPublic();
        var cose=CBORObject.NewMap().Add(1,2).Add(3,-7).Add(-1,1).Add(-2,coordinate(key.getW().getAffineX().toByteArray())).Add(-3,coordinate(key.getW().getAffineY().toByteArray())).EncodeToBytes();
        byte[] auth=ByteBuffer.allocate(37+16+2+id.length+cose.length).put(hash("localhost".getBytes(StandardCharsets.UTF_8))).put((byte)(verified?0x45:0x41)).putInt(0).put(new byte[16]).putShort((short)id.length).put(id).put(cose).array();
        byte[] attestation=CBORObject.NewMap().Add("fmt","none").Add("attStmt",CBORObject.NewMap()).Add("authData",auth).EncodeToBytes();
        return json.writeValueAsString(Map.of("id",b64(id),"rawId",b64(id),"type","public-key","clientExtensionResults",Map.of(),"response",Map.of("clientDataJSON",b64(client),"attestationObject",b64(attestation),"transports",List.of("internal"))));
    }
    String assertion(String options,String origin,boolean verified,int counter)throws Exception{
        var root=json.readTree(options).path("publicKey");
        byte[] client=client("webauthn.get",root.path("challenge").asText(),origin);
        byte[] auth=ByteBuffer.allocate(37).put(hash("localhost".getBytes(StandardCharsets.UTF_8))).put((byte)(verified?5:1)).putInt(counter).array();
        var signature=Signature.getInstance("SHA256withECDSA");signature.initSign(pair.getPrivate());signature.update(auth);signature.update(hash(client));
        return json.writeValueAsString(Map.of("id",b64(id),"rawId",b64(id),"type","public-key","clientExtensionResults",Map.of(),"response",Map.of("clientDataJSON",b64(client),"authenticatorData",b64(auth),"signature",b64(signature.sign()),"userHandle",userHandle)));
    }
    private byte[] client(String type,String challenge,String origin)throws Exception{return json.writeValueAsString(Map.of("type",type,"challenge",challenge,"origin",origin,"crossOrigin",false)).getBytes(StandardCharsets.UTF_8);}
    private byte[] hash(byte[] value)throws Exception{return MessageDigest.getInstance("SHA-256").digest(value);}
    private String b64(byte[] value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value);}
    private byte[] coordinate(byte[] value){byte[] result=new byte[32];System.arraycopy(value,Math.max(0,value.length-32),result,Math.max(0,32-value.length),Math.min(32,value.length));return result;}
}

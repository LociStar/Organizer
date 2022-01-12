package com.locibot.locibot.utils.JWT;

import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TokenVerification {

    private final String header;
    private final String payload;
    private final String signature;

    public TokenVerification(String token) {
        String[] tokenArray = token.split("\\.");
        this.header = tokenArray[0];
        this.payload = tokenArray[1];
        this.signature = tokenArray[2];
    }

    public Boolean verify(String key) throws Exception {

        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        String body = header + "." + payload;
        byte[] hmacDataBytes = sha256_HMAC.doFinal(body.getBytes(StandardCharsets.UTF_8.name()));
        String hmacData = Base64.getUrlEncoder().encodeToString(hmacDataBytes);

        return hmacData.equals(signature + "="); // Compare signatures here...
    }

    public JSONObject getPayload() {
        return new JSONObject(new String(Base64.getUrlDecoder().decode(payload.getBytes())));
    }
}

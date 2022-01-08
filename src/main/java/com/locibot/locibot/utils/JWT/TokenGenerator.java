package com.locibot.locibot.utils.JWT;

import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Generates a JWT Token as accepted by a service like Zoom. Adjust the header and payload to fit the
 * service you are interacting with. Be sure to account for all spaces in header/payload! Also token string
 * is without padding. If you need padding, be sure to remove "withoutPadding()" calls.
 *
 * @return returns JWT token string based on header, payload and secretKey
 */
public class TokenGenerator {

    private JSONObject body;
    private JSONObject head;

    public TokenGenerator() {
        body = new JSONObject();
        head = new JSONObject();
    }

    public TokenGenerator(long guildId, Long userId, String sub) throws IOException {
        body = JsonHandler.generateBodyJson(guildId, userId, sub);
        head = new JSONObject("{\"alg\": \"HS256\",\"typ\": \"JWT\"}");
    }

    public TokenGenerator(long guildId, Long userId) throws IOException {
        this(guildId, userId, "login");
    }

    public static String readFileAsString(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get(file)));
    }

    public String generateJWTToken(String secretKey) throws Exception {
        String json = head.toString();

        String base64UrlHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());

        // JWT token expires 60 seconds from now
        String payload = body.toString();
        String base64UrlPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());

        try {
            String base64UrlSignature = hmacEncode(base64UrlHeader + "." + base64UrlPayload, secretKey);

            return base64UrlHeader + "." + base64UrlPayload + "." + base64UrlSignature;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to generate a JWT token.");
        }
    }

    /**
     * Helper method that encodes data using HmacSHA256 and key.
     *
     * @param data data to encode
     * @param key  Secret key used during encoding.
     * @return Base64UrlEncoded string without padding
     */
    private String hmacEncode(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(sha256_HMAC.doFinal(data.getBytes()));
    }

    public void setHead(String json) {
        this.head = JsonHandler.gernerateHead(json);
    }

    public JSONObject getBody() {
        return this.body;
    }

    public void setBody(String json) {
        this.body = JsonHandler.generateBodyJson(json);
    }
}


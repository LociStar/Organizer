package com.locibot.locibot.utils.JWT;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;

public abstract class JsonHandler {

    public static JSONObject generateBodyJson(long guildId, long userId, String sub) throws IOException {
        //read json from file
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        JSONObject root = new JSONObject();

        //generate timestamps
        long iat = Instant.now().getEpochSecond();
        long exp = iat + 600;
        long nbf = iat - 120;

        //generate jti
        RandomString session = new RandomString();
        String jti = session.nextString();

        //set json attributes
        root.put("jti", jti);
        root.put("sub", sub);

        root.put("exp", exp);
        root.put("nbf", nbf);
        root.put("iat", iat);

        root.put("uid", userId);
        root.put("gid", guildId);

        return root;
    }

    public static JSONObject generateBodyJson(String value) {
        return new JSONObject(value);
    }


    public static JSONObject gernerateHead(String value) {
        return new JSONObject(value);
    }
}
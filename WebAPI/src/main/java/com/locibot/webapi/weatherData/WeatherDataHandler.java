package com.locibot.webapi.weatherData;

import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.locations.entity.DBLocation;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.JWT.TokenVerification;
import com.locibot.webapi.weatherData.ptv.PTVResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class WeatherDataHandler {
    public Mono<ServerResponse> weatherData(ServerRequest request) {

        String token = request.queryParam("token").orElse("invalid login token");
        String city = request.queryParam("city").orElse("Frankfurt");

        TokenVerification tokenVerification = new TokenVerification(token);

        try {
            if (tokenVerification.verify(Objects.requireNonNull(CredentialManager.get(Credential.JWT_SECRET)))) {
                return DatabaseManager.getLocations().getLocation(city).flatMap(dbLocation -> {
                    System.out.println("Test");
                    //no Location found
                    if (dbLocation.getBean().getLatitude() == 0) {
                        System.out.println("Nicht gefunden");
                        return RequestHelper.fromUrl("https://api.myptv.com/geocoding/v1/locations/by-text?searchText=" + city + "&apiKey=" + CredentialManager.get(Credential.PTV))
                                .addHeaders("searchText", city)
                                .addHeaders("apiKey", CredentialManager.get(Credential.PTV))
                                .to(PTVResponse.class).flatMap(ptvResponse -> { //TODO: needs to be saved in a Database
                                    Double latitude = ptvResponse.result().get(0).referencePosition().get("latitude");
                                    Double longitude = ptvResponse.result().get(0).referencePosition().get("longitude");
                                    DBLocation dbLocationNew = new DBLocation(city, longitude, latitude);
                                    return dbLocationNew.insert().then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                            .body(BodyInserters.fromValue(new WeatherData(latitude, longitude))));
                                });
                    }
                    //location found
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(new WeatherData(dbLocation.getBean().getLongitude(), dbLocation.getBean().getLatitude())));
                });


            }
        } catch (Exception e) {
            return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new WeatherError("bad token")));
        }

        return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new WeatherError("fatal error")));
    }

}


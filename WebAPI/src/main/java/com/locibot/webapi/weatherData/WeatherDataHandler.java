package com.locibot.webapi.weatherData;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.prominence.openweathermap.api.OpenWeatherMapClient;
import com.github.prominence.openweathermap.api.enums.Language;
import com.github.prominence.openweathermap.api.enums.OneCallResultOptions;
import com.github.prominence.openweathermap.api.enums.UnitSystem;
import com.github.prominence.openweathermap.api.model.Coordinate;
import com.github.prominence.openweathermap.api.model.onecall.current.CurrentWeatherData;
import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.locations.entity.DBLocation;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.JWT.TokenVerification;
import com.locibot.webapi.weatherData.ptv.PTVResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
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
                    //no Location found
                    if (dbLocation.getBean().getLatitude() == 0) {
                        return RequestHelper.fromUrl("https://api.myptv.com/geocoding/v1/locations/by-text?searchText=" + city + "&apiKey=" + CredentialManager.get(Credential.PTV))
                                .addHeaders("searchText", city)
                                .addHeaders("apiKey", CredentialManager.get(Credential.PTV))
                                .to(PTVResponse.class).flatMap(ptvResponse -> { //TODO: needs to be saved in a Database
                                    Double latitude = ptvResponse.result().get(0).referencePosition().get("latitude");
                                    Double longitude = ptvResponse.result().get(0).referencePosition().get("longitude");
                                    DBLocation dbLocationNew = new DBLocation(city, longitude, latitude);
                                    OpenWeatherMapClient openWeatherClient = new OpenWeatherMapClient(CredentialManager.get(Credential.OPENWEATHERMAP_API_KEY));
                                    CurrentWeatherData currentWeatherData = openWeatherClient
                                            .oneCall()
                                            .current()
                                            .byCoordinate(Coordinate.of(latitude, longitude))
                                            .language(Language.GERMAN)
                                            .unitSystem(UnitSystem.METRIC)
                                            .exclude(OneCallResultOptions.HOURLY, OneCallResultOptions.MINUTELY)
                                            .retrieve()
                                            .asJava();
                                    return dbLocationNew.insert().then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                            .body(BodyInserters.fromValue(new WeatherData(currentWeatherData))));
                                });
                    }
                    //location found
                    if (dbLocation.getWeatherData() != null) { //WeatherData in DB found
                        if (Instant.ofEpochMilli(dbLocation.getBean().getCreationTime()).isBefore(Instant.now().minusMillis(600000)))//Data too old
                            return saveDataToDB(dbLocation);
                        else
                            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromValue(dbLocation.getWeatherData()));
                    } else {
                        return saveDataToDB(dbLocation);
                    }
                });


            }
        } catch (Exception e) {
            return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new WeatherError("bad token")));
        }

        return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new WeatherError("fatal error")));
    }

    @NotNull
    private Mono<? extends ServerResponse> saveDataToDB(DBLocation dbLocation) {
        OpenWeatherMapClient openWeatherClient = new OpenWeatherMapClient(CredentialManager.get(Credential.OPENWEATHERMAP_API_KEY));
        CurrentWeatherData currentWeatherData = openWeatherClient
                .oneCall()
                .current()
                .byCoordinate(Coordinate.of(dbLocation.getBean().getLatitude(), dbLocation.getBean().getLongitude()))
                .language(Language.GERMAN)
                .unitSystem(UnitSystem.METRIC)
                .exclude(OneCallResultOptions.HOURLY, OneCallResultOptions.MINUTELY)
                .retrieve()
                .asJava();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String data = "";
        try {
            data = mapper.writeValueAsString(currentWeatherData);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return dbLocation.setWeather("{\"data\":" + data + "}").then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(new WeatherData(currentWeatherData))));
    }

    @Bean
    public ObjectMapper defaultMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

}


package com.locibot.locibot.utils.weather;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.prominence.openweathermap.api.OpenWeatherMapClient;
import com.github.prominence.openweathermap.api.enums.Language;
import com.github.prominence.openweathermap.api.enums.OneCallResultOptions;
import com.github.prominence.openweathermap.api.enums.UnitSystem;
import com.github.prominence.openweathermap.api.model.Coordinate;
import com.github.prominence.openweathermap.api.model.forecast.Forecast;
import com.github.prominence.openweathermap.api.model.onecall.current.CurrentWeatherData;
import com.github.prominence.openweathermap.api.model.weather.Weather;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.locations.entity.DBLocation;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.weather.ptv.PTVResponse;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class WeatherManager {
    private final OpenWeatherMapClient openWeatherClient;

    public WeatherManager() {
        this.openWeatherClient = new OpenWeatherMapClient(CredentialManager.get(Credential.OPENWEATHERMAP_API_KEY));
    }

    private CurrentWeatherData getCurrentWeatherData(double latitude, double longitude) {
        return openWeatherClient
                .oneCall()
                .current()
                .byCoordinate(Coordinate.of(latitude, longitude))
                .language(Language.GERMAN)
                .unitSystem(UnitSystem.METRIC)
                .exclude(OneCallResultOptions.HOURLY, OneCallResultOptions.MINUTELY)
                .retrieve()
                .asJava();
    }

    private Forecast get5DayWeatherData(double latitude, double longitude) {
        return openWeatherClient
                .forecast5Day3HourStep()
                .byCoordinate(Coordinate.of(latitude, longitude))
                .language(Language.GERMAN)
                .unitSystem(UnitSystem.METRIC)
                .retrieve()
                .asJava();
    }

    private Forecast get5DayWeatherData(String city) {
        return openWeatherClient
                .forecast5Day3HourStep()
                .byCityName(city)
                .language(Language.GERMAN)
                .unitSystem(UnitSystem.METRIC)
                .retrieve()
                .asJava();
    }

    public Mono<String> getSavedCurrentWeatherData(String city, Double user_longitude, Double user_latitude) {
        String api_url = "https://api.myptv.com/geocoding/v1/locations/by-text?searchText=" + city.replaceAll("\\s+", "") + "&apiKey=" + CredentialManager.get(Credential.PTV);
        if (city.equals("XXX_No_City")) {
            api_url = "https://api.myptv.com/geocoding/v1/locations/by-position/" + user_latitude + "/" + user_longitude + "?language=de" + "&apiKey=" + CredentialManager.get(Credential.PTV);
        }
        String finalApi_url = api_url;
        return DatabaseManager.getLocations().getLocation(city).flatMap(dbLocation -> {
            if (dbLocation.getBean().getLatitude() == 0) {
                return RequestHelper.fromUrl(finalApi_url)
                        //.addHeaders("searchText", city)
                        //.addHeaders("apiKey", CredentialManager.get(Credential.PTV))
                        .to(PTVResponse.class).flatMap(ptvResponse -> {
                            Double latitude = ptvResponse.result().get(0).referencePosition().get("latitude");
                            Double longitude = ptvResponse.result().get(0).referencePosition().get("longitude");
                            if (city.equals("XXX_No_City")) {
                                String final_city = city;
                                final_city = ptvResponse.result().get(0).address().city();
                                String final_city1 = final_city;
                                return DatabaseManager.getLocations().getLocation(final_city).flatMap(dbLocation2 -> {
                                    DBLocation dbLocationNew = new DBLocation(final_city1, longitude, latitude);
                                    if (dbLocation2.getBean().getLatitude() == 0) {
                                        return dbLocationNew.insert().then(saveDataToDB(dbLocationNew));
                                    } else {
                                        return saveDataToDB(dbLocationNew);
                                    }
                                });
                            }
                            DBLocation dbLocationNew = new DBLocation(city, longitude, latitude);
                            return dbLocationNew.insert().then(saveDataToDB(dbLocationNew));
                        });
            }
            //location found
            if (dbLocation.getWeatherData() != null && !Instant.ofEpochMilli(dbLocation.getBean().getCreationTime()).isBefore(Instant.now().minusMillis(Config.WEATHER_DATA))) {
                //WeatherData in DB found
                return Mono.just(dbLocation.getWeatherData());
            } else {
                //Data too old
                return saveDataToDB(dbLocation);
            }
        });
    }

    @NotNull
    private Mono<String> saveDataToDB(DBLocation dbLocation) {
        CurrentWeatherData currentWeatherData = getCurrentWeatherData(dbLocation.getBean().getLatitude(), dbLocation.getBean().getLongitude());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String data = "";
        try {
            data = mapper.writeValueAsString(currentWeatherData);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return dbLocation.setWeather("{\"data\":" + data + "}").then(Mono.just("{\"data\":" + data + "}"));
    }

    @NotNull
    private Mono<String> save5DayDataToDB(DBLocation dbLocation) {
        Forecast forecast = get5DayWeatherData(dbLocation.getBean().getLatitude(), dbLocation.getBean().getLongitude());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String data = "";
        try {
            data = mapper.writeValueAsString(forecast);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return dbLocation.set5DayWeather("{\"data\":" + data + "}").then(Mono.just("{\"data\":" + data + "}"));
    }

    public Mono<String> getSaved5DayWeatherData(String city, Double user_longitude, Double user_latitude) {
        String api_url = "https://api.myptv.com/geocoding/v1/locations/by-text?searchText=" + city.replaceAll("\\s+", "") + "&apiKey=" + CredentialManager.get(Credential.PTV);
        if (city.equals("XXX_No_City")) {
            api_url = "https://api.myptv.com/geocoding/v1/locations/by-position/" + user_latitude + "/" + user_longitude + "?language=de" + "&apiKey=" + CredentialManager.get(Credential.PTV);
        }
        String finalApi_url = api_url;
        return DatabaseManager.getLocations().getLocation(city).flatMap(dbLocation -> {
            if (dbLocation.getBean().getLatitude() == 0) {
                return RequestHelper.fromUrl(finalApi_url)
                        //.addHeaders("searchText", city)
                        //.addHeaders("apiKey", CredentialManager.get(Credential.PTV))
                        .to(PTVResponse.class).flatMap(ptvResponse -> {
                            Double latitude = ptvResponse.result().get(0).referencePosition().get("latitude");
                            Double longitude = ptvResponse.result().get(0).referencePosition().get("longitude");
                            if (city.equals("XXX_No_City")) {
                                String final_city = city;
                                final_city = ptvResponse.result().get(0).address().city();
                                String final_city1 = final_city;
                                return DatabaseManager.getLocations().getLocation(final_city).flatMap(dbLocation2 -> {
                                    DBLocation dbLocationNew = new DBLocation(final_city1, longitude, latitude);
                                    if (dbLocation2.getBean().getLatitude() == 0) {
                                        return dbLocationNew.insert().then(save5DayDataToDB(dbLocationNew));
                                    } else {
                                        return save5DayDataToDB(dbLocationNew);
                                    }
                                });
                            }
                            DBLocation dbLocationNew = new DBLocation(city, longitude, latitude);
                            return dbLocationNew.insert().then(save5DayDataToDB(dbLocationNew));
                        });
            }
            //location found
            if (dbLocation.get5DayWeatherData() != null && !Instant.ofEpochMilli(dbLocation.getBean().getFiveDayCreationTime()).isBefore(Instant.now().minusMillis(Config.FIVE_DAY_WEATHER_DATA))) {
                //WeatherData in DB found
                return Mono.just(dbLocation.get5DayWeatherData());
            } else {
                //Data too old
                return save5DayDataToDB(dbLocation);
            }
        });
    }

    public Mono<String> getSaved5DayWeatherData(String city) {
        return DatabaseManager.getLocations().getLocation(city).flatMap(dbLocation -> {
            if (dbLocation.getBean().getLatitude() == 0) {
                Forecast forecast = get5DayWeatherData(city);
                DBLocation dbLocationNew = new DBLocation(city, forecast.getLocation().getCoordinate().getLongitude(), forecast.getLocation().getCoordinate().getLatitude());
                return dbLocationNew.insert().then(save5DayDataToDB(dbLocation));
            }
            //location found
            if (dbLocation.get5DayWeatherData() != null && !Instant.ofEpochMilli(dbLocation.getBean().getFiveDayCreationTime()).isBefore(Instant.now().minusMillis(Config.FIVE_DAY_WEATHER_DATA))) {
                //WeatherData in DB found
                return Mono.just(dbLocation.get5DayWeatherData());
            } else {
                //Data too old
                return save5DayDataToDB(dbLocation);
            }
        });
    }

    private Mono<String> saveCurrentWatherDataToDB(DBLocation dbLocation, Weather weather) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String data = "";
        try {
            data = mapper.writeValueAsString(weather);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return dbLocation.setCurrentWeather("{\"data\":" + data + "}").then(Mono.just("{\"data\":" + data + "}"));
    }
}

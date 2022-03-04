package com.locibot.locibot.database.locations.entity;

import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseEntity;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.SerializableEntity;
import com.locibot.locibot.database.locations.LocationsCollection;
import com.locibot.locibot.database.locations.bean.DBLocationBean;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public class DBLocation extends SerializableEntity<DBLocationBean> implements DatabaseEntity {

    public DBLocation(DBLocationBean dbLocationBean) {
        super(dbLocationBean);
    }

    public DBLocation(String name, double longitude, double latitude) {
        super(new DBLocationBean(name, longitude, latitude));
    }

    public Mono<UpdateResult> setWeather(String data) {
        return Mono.from(DatabaseManager.getLocations()
                        .getCollection()
                        .updateOne(
                                Filters.eq("_id", this.getBean().getName()),
                                List.of(Updates.set("weatherData", data),
                                        Updates.set("creationTime", Instant.now().toEpochMilli())),
                                new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    LocationsCollection.LOGGER.debug("[DBLocation {}] Location update: {}", this.getBean().getName(), data + " ");
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getLocations().getName()).inc();
                })
                .doOnNext(result -> LocationsCollection.LOGGER.trace("[DBLocation {}] Location update result: {}",
                        this.getBean().getName(), result))
                .doOnTerminate(() -> DatabaseManager.getLocations().invalidateCache(this.getBean().getName()));
    }

    public Mono<UpdateResult> set5DayWeather(String data) {
        return Mono.from(DatabaseManager.getLocations()
                        .getCollection()
                        .updateOne(
                                Filters.eq("_id", this.getBean().getName()),
                                List.of(Updates.set("fiveDayWeatherData", data),
                                        Updates.set("fiveDayCreationTime", Instant.now().toEpochMilli())),
                                new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    LocationsCollection.LOGGER.debug("[DBLocation {}] Location update: {}", this.getBean().getName(), data + " ");
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getLocations().getName()).inc();
                })
                .doOnNext(result -> LocationsCollection.LOGGER.trace("[DBLocation {}] Location update result: {}",
                        this.getBean().getName(), result))
                .doOnTerminate(() -> DatabaseManager.getLocations().invalidateCache(this.getBean().getName()));
    }

    public Mono<UpdateResult> setCurrentWeather(String data) {
        return Mono.from(DatabaseManager.getLocations()
                        .getCollection()
                        .updateOne(
                                Filters.eq("_id", this.getBean().getName()),
                                List.of(Updates.set("currentWeatherData", data),
                                        Updates.set("currentWeatherCreationTime", Instant.now().toEpochMilli())),
                                new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    LocationsCollection.LOGGER.debug("[DBLocation {}] Location update: {}", this.getBean().getName(), data + " ");
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getLocations().getName()).inc();
                })
                .doOnNext(result -> LocationsCollection.LOGGER.trace("[DBLocation {}] Location update result: {}",
                        this.getBean().getName(), result))
                .doOnTerminate(() -> DatabaseManager.getLocations().invalidateCache(this.getBean().getName()));
    }

    public String getWeatherData() {
        return this.getBean().getWeatherData();
    }

    public String get5DayWeatherData() {
        return this.getBean().getFiveDayWeatherData();
    }

    public String getCurrentWeatherData() {
        return this.getBean().getCurrentWeatherData();
    }

    @Override
    public Mono<Void> insert() {
        return Mono.from(DatabaseManager.getLocations()
                        .getCollection()
                        .insertOne(this.toDocument()))
                .doOnSubscribe(__ -> {
                    LocationsCollection.LOGGER.debug("[DBLocation {}] Insertion", this.getBean().getName());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getLocations().getName()).inc();
                })
                .doOnNext(result -> LocationsCollection.LOGGER.trace("[DBLocation {}] Insertion result: {}",
                        this.getBean().getName(), result))
                .doOnTerminate(() -> DatabaseManager.getLocations().invalidateCache(this.getBean().getName()))
                .then();
    }

    @Override
    public Mono<Void> delete() {
        return null;
    }
}

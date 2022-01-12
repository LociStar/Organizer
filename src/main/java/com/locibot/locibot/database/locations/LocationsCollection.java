package com.locibot.locibot.database.locations;

import com.locibot.locibot.core.cache.MultiValueCache;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseCollection;
import com.locibot.locibot.database.events_db.bean.DBEventBean;
import com.locibot.locibot.database.events_db.entity.DBEvent;
import com.locibot.locibot.database.locations.bean.DBLocationBean;
import com.locibot.locibot.database.locations.entity.DBLocation;
import com.locibot.locibot.utils.LogUtil;
import com.locibot.locibot.utils.NetUtil;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

public class LocationsCollection extends DatabaseCollection {

    public static final Logger LOGGER = LogUtil.getLogger(LocationsCollection.class, LogUtil.Category.DATABASE);
    private final MultiValueCache<String, DBLocation> eventCache;

    public LocationsCollection(MongoDatabase database) {
        super(database, "locations");
        this.eventCache = MultiValueCache.Builder.<String, DBLocation>builder().withInfiniteTtl().build();
    }

    public Mono<DBLocation> getLocation(String name) {
        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", name))
                .first();

        final Mono<DBLocation> getDBLocation = Mono.from(request)
                .map(document -> document.toJson(JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, DBLocationBean.class)))
                .map(DBLocation::new)
                .doOnSuccess(consumer -> {
                    if (consumer == null) {
                        LOGGER.debug("[DBLocation {}] Not found", name);
                    }
                })
                .defaultIfEmpty(new DBLocation(name, 0, 0))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[DBLocation {}] Request", name);
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                });

        return this.eventCache.getOrCache(name, getDBLocation);
    }

    public void invalidateCache(String locationName) {
        LOGGER.trace("{Location ID: {}} Cache invalidated", locationName);
        this.eventCache.remove(locationName);
    }
}

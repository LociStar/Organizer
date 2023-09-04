package com.locibot.organizer2.database.repositories;

import com.locibot.organizer2.database.tables.User;
import discord4j.gateway.GatewayObserver;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.time.ZoneId;

public interface UserRepository extends R2dbcRepository<User, Integer> {

    Mono<User> findById(long id);

    @Query("""
            INSERT INTO user_ (id, zone_id)
            VALUES ($1, $2)
            ON CONFLICT (id) DO UPDATE SET zone_id = $2
            RETURNING id;""")
    Mono<Long> setZoneId(long userId, ZoneId zoneId);

    Mono<?> deleteById(long userId);

}

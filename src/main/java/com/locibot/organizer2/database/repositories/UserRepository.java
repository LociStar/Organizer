package com.locibot.organizer2.database.repositories;

import com.locibot.organizer2.database.tables.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.time.ZoneId;

public interface UserRepository extends R2dbcRepository<User, Integer> {

    Mono<User> findById(long id);

    @Query("SELECT zone_id FROM user_ WHERE id = $1")
    Mono<String> getZoneId(long userId);

    @Query("""
            DELETE FROM user_
            WHERE id = $1;""")
    Mono<?> deleteById(long userId);

    @Query("""
            UPDATE user_
            SET last_used_timestamp = EXTRACT(EPOCH FROM NOW())
            WHERE id = $1;""")
    Mono<?> updateLastUsedTimestamp(long userId);

    @Query("""
            DELETE FROM user_
            WHERE last_used_timestamp < EXTRACT(EPOCH FROM NOW() - INTERVAL '2' month);""")
    Mono<?> deleteAllOldUserData();

    @Query("""
            INSERT INTO user_ (id, zone_id)
            VALUES ($1, $2)
            ON CONFLICT (id) DO UPDATE SET zone_id = $2
            RETURNING id;""")
    Mono<Long> setZoneId(long userId, ZoneId zoneId);
}

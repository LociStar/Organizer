package com.locibot.organizer2.database.repositories;

import com.locibot.organizer2.database.tables.Event;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventRepository extends R2dbcRepository<Event, Long> {
    @Query("""
            INSERT INTO event (name, description, timestamp, is_public, icon, owner_id)
            VALUES ($1, $2, $3, $4, $5, $6)
            ON CONFLICT (id) DO UPDATE SET name = $1, description = $2, timestamp = $3, is_public = $4, icon = $5, owner_id = $6
            RETURNING *;""")
    Mono<Event> save(String name, String description, long timestamp, boolean isPublic, String icon, long owner_id);

    @Query("SELECT * FROM event WHERE event.name = $1 AND event.owner_id = $2")
    Flux<Event> findByNameAndOwnerId(String name, Long ownerId);
}

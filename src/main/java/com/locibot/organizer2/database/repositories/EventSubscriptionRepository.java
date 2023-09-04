package com.locibot.organizer2.database.repositories;

import com.locibot.organizer2.database.tables.EventSubscription;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface EventSubscriptionRepository extends R2dbcRepository<EventSubscription, Long> {
    @Query("""
            INSERT INTO event_subscription (event_id, user_id)
            VALUES ($1, $2)
            RETURNING *;""")
    Mono<EventSubscription> save(Long eventId, Long userId);

    @Query("DELETE FROM event_subscription WHERE event_id = $1 AND user_id = $2;")
    Mono<?> delete(Long eventId, Long userId);

    @Query("SELECT * FROM event_subscription WHERE event_id = $1 AND user_id = $2;")
    Mono<EventSubscription> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("DELETE FROM event_subscription WHERE user_id = $1;")
    Mono<?> deleteAllByUserId(long aLong);

    @Query("DELETE FROM event_subscription WHERE event_id = $1;")
    Mono<?> deleteByEventId(long aLong);
}

package com.locibot.organizer2.database.repositories;

import com.locibot.organizer2.database.tables.Analytics;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AnalyticsRepository extends R2dbcRepository<Analytics, Long> {

    @Query("""
            INSERT INTO analytics (guild_id, month, year)
            VALUES ($1, $2, $3)
            ON CONFLICT (guild_id, month, year)
            DO UPDATE SET message_count = analytics.message_count + 1
            """)
    Mono<?> updateMessageCount(Long guild_id, int month, int year);

    @Query("""
            INSERT INTO analytics (guild_id, month, year)
            VALUES ($1, $2, $3)
            ON CONFLICT (guild_id, month, year)
            DO UPDATE SET slash_command_count = analytics.slash_command_count + 1
            """)
    Mono<?> updateSlashCommandCount(Long guild_id, int month, int year);

    @Query("""
            INSERT INTO analytics (guild_id, month, year)
            VALUES ($1, $2, $3)
            ON CONFLICT (guild_id, month, year)
            DO UPDATE SET member_join_event_count = analytics.member_join_event_count + 1
            """)
    Mono<?> updateMemberJoinEventCount(Long guild_id, int month, int year);

    @Query("SELECT * FROM analytics WHERE guild_id = $1 LIMIT 12")
    Flux<Analytics> getAllById(Long id);
}

package com.locibot.organizer2.database.repositories;

import com.locibot.organizer2.database.tables.Iam;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface IamRepository extends R2dbcRepository<Iam, Integer> {
    @Query("""
            INSERT INTO iam (message_id, guild_id, role_ids)
            VALUES ($1, $2, $3)
            ON CONFLICT (message_id, guild_id) DO UPDATE SET message_id = $1, guild_id = $2, role_ids = $3
            RETURNING *;""")
    Mono<Iam> save(Long message_id, Long guild_id, String role_ids);

    Mono<Iam> findByMessageIdAndGuildId(Long messageId, Long guildId);
}

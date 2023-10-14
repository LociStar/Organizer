package com.locibot.organizer2.database.repositories;

import com.locibot.organizer2.database.tables.Guild;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public interface GuildRepository extends R2dbcRepository<Guild, Long> {

    //save and do nothing if exists
    @Query("""
            INSERT INTO guild (id, owner_id, name)
            VALUES ($1, $2, $3)
            ON CONFLICT (id) DO UPDATE SET id = EXCLUDED.id, name=$3;
            """)
    Mono<?> save(Long id, Long owner_id, String name);

    @Query("""
            SELECT
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM guild WHERE id = $1 AND owner_id = $2
                ) THEN 'true'
                ELSE 'false'
            END;
            """)
    Mono<String> existByIdAndOwnerId(Long id, Long owner_id);

    default Mono<Boolean> existByIdAndOwnerIdAsBoolean(Long id, Long owner_id) {
        return existByIdAndOwnerId(id, owner_id)
                .map(Boolean::parseBoolean);
    }

    @Query("""
            SELECT * FROM guild WHERE owner_id = $1
            """)
    Flux<Guild> getGuildsByOwnerId(Long id);

    @Query("""
            SELECT * FROM guild
            """)
    Flux<Guild> getAllGuilds();

}

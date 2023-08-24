package com.locibot.organizer2.database.repositories;

import com.locibot.organizer2.database.tables.Guild;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public interface GuildRepository extends R2dbcRepository<Guild, Long> {

    //save and do nothing if exists
    @Query("INSERT INTO guild (id)\n" +
            "VALUES ($1)\n" +
            "ON CONFLICT (id) DO UPDATE SET id = EXCLUDED.id\n" +
            "RETURNING id;")
    Mono<Guild> save(Long id);

    @Query("INSERT INTO guild (id)\n" +
            "VALUES ($1)\n" +
            "ON CONFLICT (id) DO UPDATE SET id = EXCLUDED.id\n" +
            "RETURNING id;")
    Mono<Guild> findByIdAndInsert(Long id);

}

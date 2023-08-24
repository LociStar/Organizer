package com.locibot.organizer2.database.repositories;

import com.locibot.organizer2.database.tables.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface UserRepository extends R2dbcRepository<User, Integer> {
}

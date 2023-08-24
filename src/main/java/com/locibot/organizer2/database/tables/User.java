package com.locibot.organizer2.database.tables;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_")
public class User {
    @Id
    private long id;

    @Column("keycloak_id")
    private String keycloak_id;

    @Column("guild_id")
    private long guild_id;

    @Column("zone_id")
    private String time_zone;

    @Column("coins")
    private Integer coins;

    @Column("achievements")
    private Integer achievements;
}

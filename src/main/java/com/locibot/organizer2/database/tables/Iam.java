package com.locibot.organizer2.database.tables;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("iam")
public class Iam {
    @Id()
    @Column("message_id")
    private long messageId;

    @Column("guild_id")
    private long guildId;

    @Column("role_ids")
    private String roleIds;

    @Column("created_timestamp")
    private LocalDateTime createdTimestamp;
}

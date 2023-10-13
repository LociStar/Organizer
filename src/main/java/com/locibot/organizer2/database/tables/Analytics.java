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
@Table("analytics")
public class Analytics {
    @Id
    @Column("guild_id")
    private long guildId;
    private int month;
    private int year;
    private long message_count;
    private long slash_command_count;
    private long member_join_event_count;
}

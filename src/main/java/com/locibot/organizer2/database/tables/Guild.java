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
@Table("guild")
public class Guild {
    @Id
    private long id;

    @Column("join_message")
    private String join_message;

    @Column("leave_message")
    private String leave_message;

    @Column("locale")
    private String locale;

    @Column("message_channel_id")
    private long message_channel_id;
}

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
@Table("event")
public class Event {
    @Id
    private long id;
    @Column("name")
    private String name;
    @Column("description")
    private String description;
    @Column("timestamp")
    private long timestamp;
    @Column("is_public")
    private boolean isPublic;
    @Column("icon")
    private String icon;
    @Column("owner_id")
    private long owner_id;
}

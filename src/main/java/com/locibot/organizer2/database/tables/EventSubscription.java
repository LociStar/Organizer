package com.locibot.organizer2.database.tables;

import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("event_subscription")
public class EventSubscription {
    private long event_id;
    private long user_id;
}

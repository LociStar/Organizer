package com.locibot.locibot.listener;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.Setting;
import com.locibot.locibot.database.DatabaseManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

public class RoleDeleteListener implements EventListener<RoleDeleteEvent> {

    @Override
    public Class<RoleDeleteEvent> getEventType() {
        return RoleDeleteEvent.class;
    }

    @Override
    public Mono<?> execute(RoleDeleteEvent event) {
        return DatabaseManager.getGuilds()
                .getDBGuild(event.getGuildId())
                .flatMap(dbGuild -> {
                    final Snowflake roleId = event.getRoleId();
                    Mono<Void> request = Mono.empty();

                    final var restrictedRoles = dbGuild.getSettings().getRestrictedRoles();
                    // If the role was a restricted role...
                    if (restrictedRoles.containsKey(roleId)) {
                        // ...update settings to remove the deleted one
                        restrictedRoles.get(roleId).clear();

                        final var restrictedRolesSeralized = restrictedRoles.entrySet().stream()
                                .collect(Collectors.toUnmodifiableMap(
                                        entry -> entry.getKey().asString(),
                                        entry -> entry.getValue().stream()
                                                .map(BaseCmd::getName)
                                                .collect(Collectors.toUnmodifiableSet())));

                        request = request
                                .and(dbGuild.updateSetting(Setting.RESTRICTED_ROLES, restrictedRolesSeralized));
                    }

                    final Set<Snowflake> allowedRoleIds = dbGuild.getSettings().getAllowedRoleIds();
                    // If the role was an allowed role...
                    if (allowedRoleIds.remove(roleId)) {
                        // ...update settings to remove the deleted one
                        request = request
                                .and(dbGuild.updateSetting(Setting.ALLOWED_ROLES, allowedRoleIds));
                    }

                    return request;
                });
    }

}

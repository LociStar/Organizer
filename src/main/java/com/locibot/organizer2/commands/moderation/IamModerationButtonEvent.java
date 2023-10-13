package com.locibot.organizer2.commands.moderation;

import com.locibot.organizer2.commands.ButtonEvent;
import com.locibot.organizer2.core.ButtonEventContext;
import com.locibot.organizer2.database.repositories.IamRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.GuildMemberEditSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class IamModerationButtonEvent implements ButtonEvent {

    private final IamRepository iamRepository;

    public IamModerationButtonEvent(IamRepository iamRepository) {
        this.iamRepository = iamRepository;
    }

    @Override
    public String getName() {
        return "iamButtonEvent";
    }

    @Override
    public Mono<?> handle(ButtonEventContext<ButtonInteractionEvent> context) {
        String eventIdString = context.getEvent().getCustomId().split("_")[1];
        return context.getGuild().flatMap(guild -> iamRepository.findByMessageIdAndGuildId(Long.parseLong(eventIdString), guild.getId().asLong()).flatMap(iam -> {

            String roleIdsString = iam.getRoleIds().substring(1, iam.getRoleIds().length() - 1).replace(" ", "");
            List<Snowflake> roleIds = Arrays.stream(roleIdsString.split(","))
                    .map(Snowflake::of)
                    .collect(Collectors.toList());

            // checks if bot has higher role than the role it is trying to give
            return Flux.fromIterable(roleIds)
                    .flatMap(roleId -> guild.getMemberById(context.getEvent().getClient().getSelfId())
                            .flatMap(bot -> bot.getHighestRole().flatMap(Role::getPosition))
                            .flatMap(highestRolePosition -> guild.getRoleById(roleId)
                                    .flatMap(Role::getPosition)
                                    .filter(rolePosition -> rolePosition < highestRolePosition).map(rolePosition -> roleId)))
                    .collectList()
                    .flatMap(roleIdsFiltered -> context.getEvent().getInteraction().getMember().get()
                            .edit(GuildMemberEditSpec.builder().addAllRoles(roleIdsFiltered).build()))
                    .then(context.getEvent().reply(context.localize("event.iam.success")).withEphemeral(true));
        }));
    }
}

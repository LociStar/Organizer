package com.locibot.organizer2.commands.moderation;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.command.CommandContext;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.GuildMemberEditSpec;
import discord4j.discordjson.possible.Possible;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class NicknameResetModerationCommand implements SlashCommand {
    @Override
    public String getName() {
        return "moderation nickname_reset";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        Mono<Member> memberMono = getMemberMono(context);
        Mono<Role> roleMono = context.getOptionAsRole("role");
        Flux<Member> memberFlux = context.getGuild().flatMapMany(Guild::requestMembers);

        return memberMono
                .filterWhen(member -> filterMember(context, member))
                .flatMap(this::editMember)
                .then(handleRoleContext(roleMono, memberFlux, context))
                .then(buildReply(context));
    }

    private Mono<Member> getMemberMono(CommandContext<?> context) {
        return context.getOptionAsUser("user")
                .flatMap(user -> user.asMember(context.getEvent().getInteraction().getGuildId().get()));
    }

    private Mono<Boolean> filterMember(CommandContext<?> context, Member member) {
        return Mono.zip(
                        member.isHigher(context.getEvent().getClient().getSelfId()).map(higher -> !higher),
                        Mono.just(!member.getId().equals(context.getEvent().getClient().getSelfId())))
                .map(tuple -> tuple.getT1() && tuple.getT2());
    }

    private Mono<Member> editMember(Member member) {
        return member.edit(GuildMemberEditSpec.builder().nickname(Possible.of(Optional.empty())).build());
    }

    private Mono<List<Member>> handleRoleContext(Mono<Role> roleMono, Flux<Member> memberFlux, CommandContext<?> context) {
        return roleMono.flatMap(role -> {
            Predicate<Member> roleFilter = setRoleFilter(context, role);
            return memberFlux
                    .filter(roleFilter)
                    .filterWhen(member -> filterMember(context, member))
                    .flatMap(this::editMember)
                    .collectList();
        });
    }

    private Predicate<Member> setRoleFilter(CommandContext<?> context, Role role) {
        if (role.getId().equals(context.getEvent().getInteraction().getGuildId().orElseGet(() -> Snowflake.of(0L)))) {
            return member -> true;
        }
        return member -> member.getRoleIds().contains(role.getId());
    }

    private Mono<?> buildReply(CommandContext<?> context) {
        return context.getEvent().reply()
                .withEphemeral(true)
                .withContent(context.localize("moderation.nickname_prefix.reset_success"));
    }
}

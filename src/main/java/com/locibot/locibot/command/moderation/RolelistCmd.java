package com.locibot.locibot.command.moderation;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.LociBotUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RolelistCmd extends BaseCmd {

    public RolelistCmd() {
        super(CommandCategory.MODERATION, "rolelist", "Show a list of members with specific role(s)");

        this.addOption("role1", "The first role to have", true, ApplicationCommandOptionType.ROLE);
        this.addOption("role2", "The second role to have", false, ApplicationCommandOptionType.ROLE);
        this.addOption("role3", "The third role to have", false, ApplicationCommandOptionType.ROLE);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Flux<Role> getRoles = Flux.fromStream(IntStream.range(1, 4).boxed())
                .flatMap(index -> context.getOptionAsRole("role%d".formatted(index)));

        return getRoles
                .collectList()
                .flatMap(mentionedRoles -> {
                    final List<Snowflake> mentionedRoleIds = mentionedRoles
                            .stream()
                            .map(Role::getId)
                            .collect(Collectors.toList());

                    final Mono<List<String>> getUsernames = context.getGuild()
                            .flatMapMany(Guild::requestMembers)
                            .filter(member -> !Collections.disjoint(member.getRoleIds(), mentionedRoleIds))
                            .map(Member::getUsername)
                            .distinct()
                            .collectList();

                    return Mono.zip(Mono.just(mentionedRoles), getUsernames);
                })
                .map(TupleUtils.function((mentionedRoles, usernames) -> {
                    EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
                    embed.author(EmbedCreateFields.Author.of(context.localize("rolelist.title"), null, context.getAuthorAvatar()));

                    if (usernames.isEmpty()) {
                        if (mentionedRoles.size() == 1) {
                            embed.description(context.localize("rolelist.nobody.singular"));
                        } else {
                            embed.description(context.localize("rolelist.nobody.plural"));
                        }
                    }

                    final String rolesFormatted = FormatUtil.format(mentionedRoles, Role::getMention, ", ");
                    embed.description(context.localize("rolelist.description")
                            .formatted(rolesFormatted));

                    FormatUtil.createColumns(usernames, 25)
                            .forEach(field -> embed.addFields(EmbedCreateFields.Field.of(field.name(), field.value(), true)));
                    return LociBotUtil.getDefaultEmbed(embed.build());
                }))
                .flatMap(context::createFollowupMessage);
    }

}

package com.locibot.locibot.command.moderation;

import com.locibot.locibot.core.command.*;
import com.locibot.locibot.database.guilds.bean.setting.IamBean;
import com.locibot.locibot.database.guilds.entity.setting.Iam;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.ShadbotUtil;
import discord4j.core.object.entity.Role;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.reaction.ReactionEmoji.Unicode;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IamCmd extends BaseCmd {

    public static final Unicode REACTION = ReactionEmoji.unicode("✅");

    public IamCmd() {
        super(CommandCategory.MODERATION, CommandPermission.ADMIN,
                "iam", "Create a message with a reaction that gives role(s)");

        this.addOption(option -> option.name("role1")
                .description("The first role to grant")
                .type(ApplicationCommandOptionType.ROLE.getValue())
                .required(true));
        this.addOption(option -> option.name("role2")
                .description("The second role to grant")
                .type(ApplicationCommandOptionType.ROLE.getValue())
                .required(false));
        this.addOption(option -> option.name("role3")
                .description("The third role to grant")
                .type(ApplicationCommandOptionType.ROLE.getValue())
                .required(false));
        this.addOption(option -> option.name("role4")
                .description("The fourth role to grant")
                .type(ApplicationCommandOptionType.ROLE.getValue())
                .required(false));
        this.addOption(option -> option.name("role5")
                .description("The fifth role to grant")
                .type(ApplicationCommandOptionType.ROLE.getValue())
                .required(false));
        this.addOption(option -> option.name("text")
                .description("Replace the default text")
                .type(ApplicationCommandOptionType.STRING.getValue())
                .required(false));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Flux<Role> getRoles = Flux.fromStream(IntStream.range(1, 6).boxed())
                .flatMap(index -> context.getOptionAsRole("role%d".formatted(index)));
        final Optional<String> text = context.getOptionAsString("text");

        return context.getChannel()
                .flatMap(channel -> DiscordUtil.requirePermissions(channel, Permission.MANAGE_ROLES, Permission.ADD_REACTIONS)
                        .then(getRoles.collectList())
                        .flatMap(roles -> {
                            final String description = text.orElse("Click on %s to get role(s): %s"
                                    .formatted(REACTION.getRaw(), FormatUtil.format(roles,
                                            role -> "`@%s`".formatted(role.getName()), "\n")));

                            final EmbedCreateSpec embedConsumer = ShadbotUtil.getDefaultEmbed(
                                    EmbedCreateSpec.builder()
                                            .author(EmbedCreateFields.Author.of("Iam: %s"
                                                            .formatted(FormatUtil.format(roles,
                                                                    role -> "@%s".formatted(role.getName()), ", ")),
                                                    null, context.getAuthorAvatar()))
                                            .description(description).build());

                            return context.createFollowupMessage(embedConsumer)
                                    .flatMap(message -> message.addReaction(REACTION)
                                            .thenReturn(message))
                                    .flatMap(message -> {
                                        // Converts the new message to an IamBean
                                        final List<IamBean> iamList = roles.stream()
                                                .map(Role::getId)
                                                .map(roleId -> new Iam(message.getId(), roleId))
                                                .map(Iam::getBean)
                                                .collect(Collectors.toList());

                                        // Add previous Iam to the new one
                                        iamList.addAll(context.getDbGuild()
                                                .getSettings()
                                                .getIam()
                                                .stream()
                                                .map(Iam::getBean)
                                                .collect(Collectors.toList()));

                                        return context.getDbGuild().updateSetting(Setting.IAM_MESSAGES, iamList);
                                    });
                        }));
    }

}


package com.locibot.locibot.core.command;

import com.locibot.locibot.LociBot;
import com.locibot.locibot.core.i18n.I18nContext;
import com.locibot.locibot.core.i18n.I18nManager;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.guilds.entity.DBGuild;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.EnumUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.ImmutableWebhookMessageEditRequest;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.util.MultipartRequest;
import discord4j.rest.util.Permission;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class Context implements InteractionContext, I18nContext {

    private final InteractionCreateEvent event;
    private final DBGuild dbGuild;
    private final AtomicLong replyId;

    public Context(InteractionCreateEvent event, DBGuild dbGuild) {
        this.event = event;
        this.dbGuild = dbGuild;
        this.replyId = new AtomicLong();
    }

    public InteractionCreateEvent getEvent() {
        return this.event;
    }

    public DBGuild getDbGuild() {
        return this.dbGuild;
    }

    public GatewayDiscordClient getClient() {
        return this.event.getClient();
    }

    public String getFullCommandName() {
        final List<String> cmds = new ArrayList<>();
        cmds.add(this.getCommandName());
        this.getSubCommandGroupName().ifPresent(cmds::add);
        this.getSubCommandName().ifPresent(cmds::add);
        return String.join(" ", cmds);
    }

    public Optional<String> getSubCommandGroupName() {
        return DiscordUtil.flattenOptions(this.event.getInteraction().getCommandInteraction().orElseThrow())
                .stream()
                .filter(option -> option.getType() == ApplicationCommandOption.Type.SUB_COMMAND_GROUP)
                .map(ApplicationCommandInteractionOption::getName)
                .findFirst();
    }

    public Optional<String> getSubCommandName() {
        return DiscordUtil.flattenOptions(this.event.getInteraction().getCommandInteraction().orElseThrow())
                .stream()
                .filter(option -> option.getType() == ApplicationCommandOption.Type.SUB_COMMAND)
                .map(ApplicationCommandInteractionOption::getName)
                .findFirst();
    }

    public String getCommandName() {
        return this.event.getInteraction().getCommandInteraction().flatMap(ApplicationCommandInteraction::getName).orElseThrow();
    }

    public String getLastCommandName() {
        return this.getSubCommandGroupName()
                .orElse(this.getSubCommandName()
                        .orElse(this.getCommandName()));
    }

    public Mono<Guild> getGuild() {
        return this.getEvent().getInteraction().getGuild();
    }

    public Snowflake getGuildId() {
        return this.getEvent().getInteraction().getGuildId().orElseThrow();
    }

    public Mono<TextChannel> getChannel() {
        return this.getEvent().getInteraction().getChannel().cast(TextChannel.class);
    }

    public Snowflake getChannelId() {
        return this.getEvent().getInteraction().getChannelId();
    }

    public Mono<Boolean> isChannelNsfw() {
        return this.getChannel().map(TextChannel::isNsfw);
    }

    public Member getAuthor() {
        return this.getEvent().getInteraction().getMember().orElseThrow();
    }

    public Snowflake getAuthorId() {
        return this.getEvent().getInteraction().getUser().getId();
    }

    public String getAuthorName() {
        return this.getAuthor().getUsername();
    }

    public String getAuthorAvatar() {
        return this.getAuthor().getAvatarUrl();
    }

    public Optional<ApplicationCommandInteractionOptionValue> getOption(String name) {
        final List<ApplicationCommandInteractionOption> options =
                DiscordUtil.flattenOptions(this.getEvent().getInteraction().getCommandInteraction().get());
        return options.stream()
                .filter(option -> option.getName().equals(name))
                .filter(option -> option.getValue().isPresent())
                .findFirst()
                .flatMap(ApplicationCommandInteractionOption::getValue);
    }

    public <T extends Enum<T>> Optional<T> getOptionAsEnum(Class<T> enumClass, String name) {
        return this.getOptionAsString(name).map(it -> EnumUtil.parseEnum(enumClass, it));
    }

    public Optional<String> getOptionAsString(String name) {
        return this.getOption(name).map(ApplicationCommandInteractionOptionValue::asString);
    }

    public Optional<Snowflake> getOptionAsSnowflake(String name) {
        return this.getOption(name).map(ApplicationCommandInteractionOptionValue::asSnowflake);
    }

    public Optional<Long> getOptionAsLong(String name) {
        return this.getOption(name).map(ApplicationCommandInteractionOptionValue::asLong);
    }

    public Optional<Boolean> getOptionAsBool(String name) {
        return this.getOption(name).map(ApplicationCommandInteractionOptionValue::asBoolean);
    }

    public Mono<User> getOptionAsUser(String name) {
        return Mono.justOrEmpty(this.getOption(name))
                .flatMap(ApplicationCommandInteractionOptionValue::asUser);
    }

    public Mono<Member> getOptionAsMember(String name) {
        return Mono.justOrEmpty(this.getOption(name))
                .flatMap(ApplicationCommandInteractionOptionValue::asUser)
                .flatMap(user -> user.asMember(this.getGuildId()));
    }

    public Mono<Role> getOptionAsRole(String name) {
        return Mono.justOrEmpty(this.getOption(name)).flatMap(ApplicationCommandInteractionOptionValue::asRole);
    }

    public Mono<Channel> getOptionAsChannel(String name) {
        return Mono.justOrEmpty(this.getOption(name)).flatMap(ApplicationCommandInteractionOptionValue::asChannel);
    }

    public Flux<CommandPermission> getPermissions() {
        // The author is a bot's owner
        final Mono<CommandPermission> ownerPerm = Mono.just(this.getAuthorId())
                .filter(LociBot.getOwnerId()::equals)
                .map(__ -> CommandPermission.OWNER);

        // The member is an administrator or it's a private message
        final Mono<CommandPermission> adminPerm = this.getChannel()
                .filterWhen(channel -> BooleanUtils.or(
                        DiscordUtil.hasPermission(channel, this.getAuthorId(), Permission.ADMINISTRATOR),
                        Mono.just(channel.getType() == Channel.Type.DM)))
                .map(__ -> CommandPermission.ADMIN);
        return Flux.merge(ownerPerm, adminPerm, Mono.just(CommandPermission.USER_GUILD), Mono.just(CommandPermission.USER_GLOBAL));
    }

    /////////////////////////////////////////////
    ///////////// InteractionContext
    /////////////////////////////////////////////

    @Override
    public Locale getLocale() {
        //In case of private channel, look for a local. If not found use default local.
        if (this.getDbGuild() == null && this.event.getInteraction().getUser().getUserData().locale().isAbsent())
            if (this.event.getInteraction().getUser().getUserData().locale().isAbsent())
                return Config.DEFAULT_LOCALE;
            else return Locale.forLanguageTag(this.event.getInteraction().getUser().getUserData().locale().toString());
        return this.getDbGuild().getLocale();
    }

    @Override
    public String localize(String key) {
        return I18nManager.localize(this.getLocale(), key);
    }

    @Override
    public String localize(double number) {
        return I18nManager.localize(this.getLocale(), number);
    }

    /////////////////////////////////////////////
    ///////////// InteractionContext
    /////////////////////////////////////////////

    @Override
    public Mono<Void> reply(Emoji emoji, String message) {
        return this.event.reply("%s %s".formatted(emoji, message))
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc());
    }

    @Override
    public Mono<Void> replyEphemeral(Emoji emoji, String message) {
        return this.event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .ephemeral(true)
                        .content("%s %s".formatted(emoji, message)).build())
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc());
    }

    @Override
    public Mono<Message> createFollowupMessage(String str) {
        return this.event.getInteractionResponse()
                .createFollowupMessage(str)
                .map(data -> new Message(this.getClient(), data))
                .doOnNext(message -> this.replyId.set(message.getId().asLong()))
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc());
    }

    public Mono<Message> createFollowupMessageEphemeral(String str) {
        return this.event.createFollowup(str).withEphemeral(true)
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc());
    }

    @Override
    public Mono<Message> createFollowupMessage(Emoji emoji, String str) {
        return this.createFollowupMessage("%s %s".formatted(emoji, str));
    }

//    public Mono<Message> createFollowupMessage(Consumer<EmbedCreateSpec> embed) {
//        final EmbedCreateSpec mutatedSpec = EmbedCreateSpec.builder();
//        embed.accept(mutatedSpec);
//        return this.event.getInteractionResponse().createFollowupMessage(MultipartRequest.ofRequest(
//                WebhookExecuteRequest.builder()
//                        .addEmbed(mutatedSpec.asRequest())
//                        .build()))
//                .map(data -> new Message(this.getClient(), data))
//                .doOnNext(message -> this.replyId.set(message.getId().asLong()))
//                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc());
//    }

    @Override
    public Mono<Message> createFollowupMessage(EmbedCreateSpec embed) {
        return this.event.getInteractionResponse().createFollowupMessage(MultipartRequest.ofRequest(
                        WebhookExecuteRequest.builder()
                                .addEmbed(embed.asRequest())
                                .build()))
                .map(data -> new Message(this.getClient(), data))
                .doOnNext(message -> this.replyId.set(message.getId().asLong()))
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc());
    }

    @Override
    public Mono<Message> createFollowupMessage(InteractionFollowupCreateSpec spec) {
        return this.event.createFollowup(spec)
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc());
    }

    public Mono<Message> createFollowupButton(EmbedCreateSpec embed, Button... buttons) {
        return this.event.createFollowup().withEmbeds(embed).withComponents(ActionRow.of(buttons));
    }

    @Override
    public Mono<Message> editFollowupMessage(String message) {
        return Mono.fromCallable(this.replyId::get)
                .filter(messageId -> messageId > 0)
                .flatMap(messageId -> this.event.getInteractionResponse()
                        .editFollowupMessage(messageId, ImmutableWebhookMessageEditRequest.builder()
                                .contentOrNull(message)
                                .build(), true))
                .map(data -> new Message(this.getClient(), data))
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc())
                .switchIfEmpty(this.createFollowupMessage(message));
    }

    @Override
    public Mono<Message> editFollowupMessage(Emoji emoji, String message) {
        return this.editFollowupMessage("%s %s".formatted(emoji, message));
    }

    @Override
    public Mono<Message> editFollowupMessage(EmbedCreateSpec embed) {
        return Mono.fromCallable(this.replyId::get)
                .filter(messageId -> messageId > 0)
                .flatMap(messageId -> this.event.getInteractionResponse()
                        .editFollowupMessage(messageId, ImmutableWebhookMessageEditRequest.builder()
                                .contentOrNull("")
                                .embeds(List.of(embed.asRequest()))
                                .build(), true))
                .map(data -> new Message(this.getClient(), data))
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc())
                .switchIfEmpty(this.createFollowupMessage(embed));
    }

    @Override
    public Mono<Message> editInitialFollowupMessage(EmbedCreateSpec embed) {
        return Mono.defer(() -> this.event.getInteractionResponse()
                .editInitialResponse(ImmutableWebhookMessageEditRequest.builder()
                        .contentOrNull("")
                        .embeds(List.of(embed.asRequest()))
                        .build())
                .map(data -> new Message(this.getClient(), data))
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc())
                .switchIfEmpty(this.createFollowupMessage(embed)));
    }

    public boolean isPrivate() {
        return false;
    }

}

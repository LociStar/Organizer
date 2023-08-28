package com.locibot.organizer2.core;

import com.locibot.organizer2.data.Config;
import com.locibot.organizer2.database.repositories.GuildRepository;
import com.locibot.organizer2.database.repositories.UserRepository;
import com.locibot.organizer2.utils.DiscordUtil;
import com.locibot.organizer2.utils.EnumUtil;
import com.locibot.organizer2.utils.I18nManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.*;

public class CommandContext<T extends ApplicationCommandInteractionEvent> {
    private final T event;
    private final GuildRepository guildRepository;
    private final UserRepository userRepository;
    private Locale locale;

    public CommandContext(T event, GuildRepository guildRepository, UserRepository userRepository) {
        this.event = event;
        this.guildRepository = guildRepository;
        this.userRepository = userRepository;
    }

    public T getEvent() {
        return event;
    }

//    public Locale getLocale() {
//        //In case of private channel, look for a local. If not found use default local.
//        if (guildRepository == null && event.getInteraction().getUser().getUserData().locale().isAbsent())
//            if (event.getInteraction().getUser().getUserData().locale().isAbsent())
//                return Config.DEFAULT_LOCALE;
//            else return Locale.forLanguageTag(event.getInteraction().getUser().getUserData().locale().toString());
//        return guildRepository.findById(event.getInteraction().getGuildId().get().asLong()).map(guild -> Locale.forLanguageTag(guild.getLocale())).onErrorReturn(Config.DEFAULT_LOCALE).block();
//    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @Deprecated
    public Mono<Locale> getLocale_old() {
        //return cachedLocale.getOrCache(this.getUncachedLocale());
        return Mono.just(this.locale);
    }

    public Mono<Locale> getUncachedLocale() {
        if (event.getInteraction().getGuildId().isPresent()) {
            System.out.println("DB ACCESS");
            return guildRepository.findById(event.getInteraction().getGuildId().get().asLong())
                    .map(guild -> Locale.forLanguageTag(guild.getLocale()))
                    .onErrorReturn(Config.DEFAULT_LOCALE);//TODO: remove blocking and/or add caching
        }
        return Mono.just(Config.DEFAULT_LOCALE);
    }

    @Deprecated
    public Mono<String> localize_old(String key) {
        return this.getLocale_old().flatMap(locale -> Mono.just(I18nManager.localize(locale, key)));
    }

    public String localize(String key) {
        return I18nManager.localize(this.getLocale(), key);
    }

    @Deprecated
    public Mono<String[]> localize_old(String... key) {
        return this.getLocale_old().flatMap(locale -> {
            for (int i = 0; i < key.length; i++) {
                key[i] = I18nManager.localize(locale, key[i]);
            }
            return Mono.just(key);
        });
    }

    public String[] localize(String... key) {
        for (int i = 0; i < key.length; i++) {
            key[i] = I18nManager.localize(this.getLocale(), key[i]);
        }
        return key;
    }

    @Deprecated
    public Mono<String> localize_old(double number) {
        return this.getLocale_old().flatMap(locale -> Mono.just(I18nManager.localize(locale, number)));
    }

    public String localize(double number) {
        return I18nManager.localize(this.getLocale(), number);
    }

    public Member getAuthor() {
        return event.getInteraction().getMember().orElseThrow();
    }

    public Snowflake getAuthorId() {
        return this.getEvent().getInteraction().getUser().getId();
    }

    public String getAuthorAvatar() {
        return getAuthor().getAvatarUrl();
    }

    /**
     * @return A default {@link EmbedCreateSpec} with the default color set.
     */
    public EmbedCreateSpec getDefaultEmbed(EmbedCreateSpec embed) {
        return embed.withColor(Config.BOT_COLOR);
    }

    public Mono<Guild> getGuild() {
        return this.getEvent().getInteraction().getGuild();
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

    public Optional<ApplicationCommandInteractionOptionValue> getOption(String name) {
        final List<ApplicationCommandInteractionOption> options =
                DiscordUtil.flattenOptions(this.getEvent().getInteraction().getCommandInteraction()
                        .orElseThrow(() -> new IllegalStateException("No command interaction present")));
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
                .flatMap(user -> user.asMember(this.getEvent().getInteraction().getGuildId().orElseThrow()));
    }

    public Mono<Role> getOptionAsRole(String name) {
        return Mono.justOrEmpty(this.getOption(name)).flatMap(ApplicationCommandInteractionOptionValue::asRole);
    }

    public Mono<Channel> getOptionAsChannel(String name) {
        return Mono.justOrEmpty(this.getOption(name)).flatMap(ApplicationCommandInteractionOptionValue::asChannel);
    }


    public GuildRepository getGuildRepository() {
        return guildRepository;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }
}

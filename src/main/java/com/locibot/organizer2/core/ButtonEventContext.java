package com.locibot.organizer2.core;

import com.locibot.organizer2.data.Config;
import com.locibot.organizer2.database.repositories.EventRepository;
import com.locibot.organizer2.database.repositories.EventSubscriptionRepository;
import com.locibot.organizer2.database.repositories.GuildRepository;
import com.locibot.organizer2.database.repositories.UserRepository;
import com.locibot.organizer2.utils.DiscordUtil;
import com.locibot.organizer2.utils.EnumUtil;
import com.locibot.organizer2.utils.I18nManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ButtonEventContext <T extends ComponentInteractionEvent>{

    private final T event;
    private final GuildRepository guildRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventSubscriptionRepository eventSubscriptionRepository;
    private Locale locale;

    public ButtonEventContext(T event, GuildRepository guildRepository, UserRepository userRepository, EventRepository eventRepository, EventSubscriptionRepository eventSubscriptionRepository) {
        this.event = event;
        this.guildRepository = guildRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventSubscriptionRepository = eventSubscriptionRepository;
    }

    public T getEvent() {
        return event;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Mono<Locale> getUncachedLocale() {
        if (event.getInteraction().getGuildId().isPresent()) {
            return guildRepository.findById(event.getInteraction().getGuildId().get().asLong())
                    .map(guild -> Locale.forLanguageTag(guild.getLocale()))
                    .onErrorReturn(Config.DEFAULT_LOCALE);//TODO: remove blocking and/or add caching
        }
        return Mono.just(Config.DEFAULT_LOCALE);
    }

    public String localize(String key) {
        return I18nManager.localize(this.getLocale(), key);
    }

    public String[] localize(String... key) {
        for (int i = 0; i < key.length; i++) {
            key[i] = I18nManager.localize(this.getLocale(), key[i]);
        }
        return key;
    }

    public String localize(double number) {
        return I18nManager.localize(this.getLocale(), number);
    }

    public User getAuthor() {
        return event.getInteraction().getUser();
    }

    public Snowflake getAuthorId() {
        return this.getAuthor().getId();
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
        return this.getEvent().getCustomId();
    }

    public String getCommandName() {
        return this.getEvent().getCustomId().split("_")[0];
    }

    public Long getCommandId() {
        return Long.valueOf(this.getEvent().getCustomId().split("_")[1]);
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

    public EventRepository getEventRepository() {
        return eventRepository;
    }

    public EventSubscriptionRepository getEventSubscriptionRepository() {
        return eventSubscriptionRepository;
    }
}

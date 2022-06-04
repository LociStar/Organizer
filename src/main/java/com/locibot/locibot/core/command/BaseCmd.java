package com.locibot.locibot.core.command;

import com.locibot.locibot.core.ratelimiter.RateLimiter;
import com.locibot.locibot.object.help.CommandHelpBuilder;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BaseCmd {

    protected static final Supplier<RateLimiter> DEFAULT_RATELIMITER = () ->
            new RateLimiter(3, Duration.ofSeconds(5));
    protected static final Supplier<RateLimiter> DEFAULT_GAME_RATELIMITER = () ->
            new RateLimiter(1, Duration.ofSeconds(3));

    private final CommandCategory category;
    private final CommandPermission permission;
    @Nullable
    private final List<Requirement> requirements;
    private final String name;
    private final String description;
    private final List<ApplicationCommandOptionData> options;
    @Nullable
    private final ApplicationCommandOption.Type type;

    @Nullable
    private RateLimiter rateLimiter;
    private boolean isEnabled;

    protected BaseCmd(CommandCategory category, CommandPermission permission, @Nullable List<Requirement> requirements, String name, String description,
                      @Nullable ApplicationCommandOption.Type type) {
        this.category = category;
        this.permission = permission;
        this.requirements = requirements;
        this.name = name;
        this.description = description;
        this.type = type;
        this.options = new ArrayList<>();
        this.rateLimiter = DEFAULT_RATELIMITER.get();
        this.isEnabled = true;
    }

    protected BaseCmd(CommandCategory category, CommandPermission permission, String name, String description,
                      @Nullable ApplicationCommandOption.Type type) {
        this(category, permission, null, name, description, type);
    }

    protected BaseCmd(CommandCategory category, CommandPermission permission, String name, String description) {
        this(category, permission, null, name, description, null);
    }

    protected BaseCmd(CommandCategory category, CommandPermission permission, String name, String description, Requirement... requirements) {
        this(category, permission, List.of(requirements), name, description, null);
    }

    protected BaseCmd(CommandCategory category, String name, String description) {
        this(category, CommandPermission.USER_GUILD, name, description);
    }

    public ApplicationCommandRequest asRequest() {
        return ApplicationCommandRequest.builder()
                .name(this.getName())
                .description(this.getDescription())
                .addAllOptions(this.getOptions())
                .build();
    }

    public abstract Mono<?> execute(Context context);

    public EmbedCreateSpec getHelp(Context context) {
        return CommandHelpBuilder.create(context, this).build();
    }

    public CommandCategory getCategory() {
        return this.category;
    }

    public CommandPermission getPermission() {
        return this.permission;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public List<ApplicationCommandOptionData> getOptions() {
        return Collections.unmodifiableList(this.options);
    }

    public Optional<ApplicationCommandOption.Type> getType() {
        return Optional.ofNullable(this.type);
    }

    public Optional<RateLimiter> getRateLimiter() {
        return Optional.ofNullable(this.rateLimiter);
    }

    public void setRateLimiter(@Nullable RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setGameRateLimiter() {
        this.setRateLimiter(DEFAULT_GAME_RATELIMITER.get());
    }

    public List<Requirement> getRequirements() {
        return requirements == null ? List.of(Requirement.NONE) : this.requirements;
    }

    // TODO: Remove
    public void addOption(String name, String description, boolean required, ApplicationCommandOption.Type type) {
        this.addOption(option -> option.name(name).description(description).required(required).type(type.getValue()));
    }

    // TODO: Remove
    public void addOption(String name, String description, boolean required, ApplicationCommandOption.Type type,
                          List<ApplicationCommandOptionChoiceData> choices) {
        this.addOption(option -> option.name(name).description(description)
                .required(required).type(type.getValue()).choices(choices));
    }

    public void addOption(Consumer<ImmutableApplicationCommandOptionData.Builder> option) {
        final ImmutableApplicationCommandOptionData.Builder mutatedOption = ApplicationCommandOptionData.builder();
        option.accept(mutatedOption);
        this.options.add(mutatedOption.build());
    }

}

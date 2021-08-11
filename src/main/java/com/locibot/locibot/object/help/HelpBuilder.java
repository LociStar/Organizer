package com.locibot.locibot.object.help;

import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.discordjson.possible.Possible;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class HelpBuilder {

    protected final Context context;

    protected final List<ApplicationCommandOptionData> options;
    protected final List<ImmutableEmbedFieldData> fields;

    @Nullable
    private String authorName;
    @Nullable
    private String authorUrl;
    @Nullable
    private String thumbnailUrl;
    @Nullable
    private String description;
    @Nullable
    private String source;
    @Nullable
    private String footer;

    protected HelpBuilder(Context context) {
        this.context = context;
        this.options = new ArrayList<>();
        this.fields = new ArrayList<>();
    }

    public HelpBuilder author(String authorName, String authorUrl) {
        this.authorName = authorName;
        this.authorUrl = authorUrl;
        return this;
    }

    public HelpBuilder thumbnail(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        return this;
    }

    public HelpBuilder description(String description) {
        this.description = "**%s**".formatted(description);
        return this;
    }

    public HelpBuilder setSource(String source) {
        this.source = source;
        return this;
    }

    public HelpBuilder footer(String footer) {
        this.footer = footer;
        return this;
    }

    public HelpBuilder fields(String name, String value, boolean inline) {
        this.fields.add(ImmutableEmbedFieldData.of(name, value, Possible.of(inline)));
        return this;
    }

    public EmbedCreateSpec build() {
        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
        if (this.authorName != null && !this.authorName.isBlank()) {
            embed.author(EmbedCreateFields.Author.of(this.authorName, this.authorUrl, this.context.getAuthorAvatar()));
        }
        embed.fields(List.of(EmbedCreateFields.Field.of(this.context.localize("help.usage"), this.getUsage(), false)));

        if (this.description != null && !this.description.isBlank()) {
            embed.description(this.description);
        }

        if (this.thumbnailUrl != null && !this.thumbnailUrl.isBlank()) {
            embed.thumbnail(this.thumbnailUrl);
        }

        if (!this.getArguments().isEmpty()) {
            embed.fields(List.of(EmbedCreateFields.Field.of(this.context.localize("help.arguments"), this.getArguments(), false)));
        }

        if (this.source != null && !this.source.isBlank()) {
            embed.fields(List.of(EmbedCreateFields.Field.of(this.context.localize("help.source"), this.source, false)));
        }

        for (final ImmutableEmbedFieldData field : this.fields) {
            embed.fields(List.of(EmbedCreateFields.Field.of(field.name(), field.value(), field.inline().get())));
        }

        if (this.footer != null && !this.footer.isBlank()) {
            embed.footer(EmbedCreateFields.Footer.of(this.footer, null));
        }
        return ShadbotUtil.getDefaultEmbed(embed.build());
    }

    protected abstract String getCommandName();

    private String getUsage() {
        if (this.options.isEmpty()) {
            return "`/%s`".formatted(this.getCommandName());
        }

        return "`/%s %s`".formatted(this.getCommandName(),
                FormatUtil.format(this.options,
                        option -> String.format(option.required().toOptional().orElse(false) ? "<%s>" : "[<%s>]",
                                option.name()), " "));
    }

    private String getArguments() {
        return this.options.stream()
                .map(option -> "%n**%s** %s - %s"
                        .formatted(option.name(), option.required().toOptional().orElse(false) ?
                                        "" : this.context.localize("help.optional"),
                                option.description()))
                .collect(Collectors.joining());
    }
}

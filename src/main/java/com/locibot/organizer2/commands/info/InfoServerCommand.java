package com.locibot.organizer2.commands.info;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.CommandContext;
import com.locibot.organizer2.object.Emoji;
import com.locibot.organizer2.utils.FormatUtil;
import com.locibot.organizer2.utils.TimeUtil;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.rest.util.Image;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

@Component
public class InfoServerCommand implements SlashCommand {

    private final DateTimeFormatter dateFormatter;

    public InfoServerCommand() {
        this.dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.MEDIUM);
    }

    @Override
    public String getName() {
        return "info server";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        final Mono<Guild> getGuild = context.getGuild().cache();
        return Mono.zip(Mono.just(context),
                        getGuild,
                        getGuild.flatMapMany(Guild::getChannels).collectList(),
                        getGuild.flatMap(Guild::getOwner))
                .map(TupleUtils.function(this::formatEmbed))
                .flatMap(embedCreateSpecMono -> embedCreateSpecMono.flatMap(spec -> context.getEvent().reply(
                        InteractionApplicationCommandCallbackSpec.builder()
                                .addEmbed(spec)
                                .build())));
    }

    private Mono<EmbedCreateSpec> formatEmbed(CommandContext<?> context, Guild guild, List<GuildChannel> channels,
                                              Member owner) {//, Region regions) {
        final LocalDateTime creationTime = TimeUtil.toLocalDateTime(guild.getId().getTimestamp());
        final long voiceChannels = channels.stream().filter(VoiceChannel.class::isInstance).count();
        final long textChannels = channels.stream().filter(TextChannel.class::isInstance).count();

        final Mono<DateTimeFormatter> dateFormatterMono = context.getLocale_old().map(this.dateFormatter::withLocale);

        final Mono<String> idTitleMono = context.localize_old("serverinfo.id").map(text -> Emoji.ID + " " + text);
        final Mono<String> ownerTitleMono = context.localize_old("serverinfo.owner").map(text -> Emoji.CROWN + " " + text);
        //final String regionTitle = Emoji.MAP + " " + context.localize("serverinfo.region");
        final Mono<String> creationTitleMono = context.localize_old("serverinfo.creation").map(text -> Emoji.BIRTHDAY + " " + text);
        final Mono<String> creationFieldMono = dateFormatterMono.flatMap(dateFormatter -> context.getLocale_old().map(locale -> "%s\n(%s)"
                .formatted(creationTime.format(dateFormatter),
                        FormatUtil.formatLongDuration(locale, creationTime))));
        final Mono<String> channelsTitleMono = context.localize_old("serverinfo.channels").map(text -> Emoji.SPEECH_BALLOON + " " + text);
        final Mono<String> channelsFieldMono = context.localize_old("serverinfo.channels.field").map(text -> text
                .formatted(Emoji.MICROPHONE, voiceChannels, Emoji.KEYBOARD, textChannels));
        final Mono<String> membersTitleMono = context.localize_old("serverinfo.members").map(text -> Emoji.BUSTS_IN_SILHOUETTE + " " + text);


        return Mono.zip(idTitleMono,
                        ownerTitleMono,
                        creationTitleMono,
                        creationFieldMono,
                        channelsTitleMono,
                        channelsFieldMono,
                        context.localize_old("serverinfo.title"),
                        context.localize_old(guild.getMemberCount()))
                .flatMap(TupleUtils.function((idTitle, ownerTitle, creationTitle, creationField, channelsTitle, channelsField, title, memberCount) ->
                        membersTitleMono.map(membersTitle ->
                                context.getDefaultEmbed(EmbedCreateSpec.builder()
                                        .author(EmbedCreateFields.Author.of(title.formatted(guild.getName()), null,
                                                context.getAuthorAvatar()))
                                        .thumbnail(guild.getIconUrl(Image.Format.JPEG).orElse(""))
                                        .fields(List.of(
                                                EmbedCreateFields.Field.of(idTitle, guild.getId().asString(), true),
                                                EmbedCreateFields.Field.of(ownerTitle, owner.getTag(), true),
                                                //EmbedCreateFields.Field.of(regionTitle, regions.get(0).getName(), true),
                                                EmbedCreateFields.Field.of(creationTitle, creationField, true),
                                                EmbedCreateFields.Field.of(channelsTitle, channelsField, true),
                                                EmbedCreateFields.Field.of(membersTitle, memberCount, true))).build()))));
    }
}

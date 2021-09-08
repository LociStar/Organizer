package com.locibot.locibot.command.info;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.LociBotUtil;
import com.locibot.locibot.utils.TimeUtil;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Image.Format;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

public class ServerInfoCmd extends BaseCmd {

    private final DateTimeFormatter dateFormatter;

    public ServerInfoCmd() {
        super(CommandCategory.INFO, "server", "Show server info");
        this.dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.MEDIUM);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Mono<Guild> getGuild = context.getGuild().cache();
        return Mono.zip(Mono.just(context),
                getGuild,
                getGuild.flatMapMany(Guild::getChannels).collectList(),
                getGuild.flatMap(Guild::getOwner))
                //getGuild.flatMap(Guild::getRegion))
                .map(TupleUtils.function(this::formatEmbed))
                .flatMap(context::createFollowupMessage);
    }

    private EmbedCreateSpec formatEmbed(Context context, Guild guild, List<GuildChannel> channels,
                                        Member owner){//, Region regions) {
        final LocalDateTime creationTime = TimeUtil.toLocalDateTime(guild.getId().getTimestamp());
        final long voiceChannels = channels.stream().filter(VoiceChannel.class::isInstance).count();
        final long textChannels = channels.stream().filter(TextChannel.class::isInstance).count();

        final DateTimeFormatter dateFormatter = this.dateFormatter.withLocale(context.getLocale());

        final String idTitle = Emoji.ID + " " + context.localize("serverinfo.id");
        final String ownerTitle = Emoji.CROWN + " " + context.localize("serverinfo.owner");
        //final String regionTitle = Emoji.MAP + " " + context.localize("serverinfo.region");
        final String creationTitle = Emoji.BIRTHDAY + " " + context.localize("serverinfo.creation");
        final String creationField = "%s\n(%s)"
                .formatted(creationTime.format(dateFormatter),
                        FormatUtil.formatLongDuration(context.getLocale(), creationTime));
        final String channelsTitle = Emoji.SPEECH_BALLOON + " " + context.localize("serverinfo.channels");
        final String channelsField = context.localize("serverinfo.channels.field")
                .formatted(Emoji.MICROPHONE, voiceChannels, Emoji.KEYBOARD, textChannels);
        final String membersTitle = Emoji.BUSTS_IN_SILHOUETTE + " " + context.localize("serverinfo.members");

        return LociBotUtil.getDefaultEmbed(EmbedCreateSpec.builder()
                .author(EmbedCreateFields.Author.of(context.localize("serverinfo.title").formatted(guild.getName()), null,
                        context.getAuthorAvatar()))
                .thumbnail(guild.getIconUrl(Format.JPEG).orElse(""))
                .fields(List.of(
                        EmbedCreateFields.Field.of(idTitle, guild.getId().asString(), true),
                        EmbedCreateFields.Field.of(ownerTitle, owner.getTag(), true),
                        //EmbedCreateFields.Field.of(regionTitle, regions.get(0).getName(), true),
                        EmbedCreateFields.Field.of(creationTitle, creationField, true),
                        EmbedCreateFields.Field.of(channelsTitle, channelsField, true),
                        EmbedCreateFields.Field.of(membersTitle, context.localize(guild.getMemberCount()), true))).build());
    }

}

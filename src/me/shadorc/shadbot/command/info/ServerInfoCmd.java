package me.shadorc.shadbot.command.info;

import discord4j.core.object.Region;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Image.Format;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ServerInfoCmd extends BaseCmd {

    private final DateTimeFormatter dateFormatter;

    public ServerInfoCmd() {
        super(CommandCategory.INFO, List.of("server_info", "server-info", "serverinfo"));
        this.setDefaultRateLimiter();

        this.dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH'h'mm", Locale.ENGLISH);
    }

    @Override
    public Mono<Void> execute(Context context) {
        return Mono.zip(context.getGuild(),
                context.getGuild().flatMap(Guild::getOwner),
                context.getGuild().flatMapMany(Guild::getChannels).collectList(),
                context.getGuild().flatMap(Guild::getRegion))
                .map(tuple -> this.getEmbed(tuple.getT1(), tuple.getT3(), tuple.getT2(), tuple.getT4(), context.getAvatarUrl()))
                .flatMap(embed -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
                .then();
    }

    private Consumer<EmbedCreateSpec> getEmbed(Guild guild, List<GuildChannel> channels, Member owner, Region region, String avatarUrl) {
        final String creationDate = String.format("%s%n(%s)",
                TimeUtils.toLocalDate(guild.getId().getTimestamp()).format(this.dateFormatter),
                FormatUtils.longDuration(guild.getId().getTimestamp()));
        final long voiceChannels = channels.stream().filter(VoiceChannel.class::isInstance).count();
        final long textChannels = channels.stream().filter(TextChannel.class::isInstance).count();

        return EmbedUtils.getDefaultEmbed()
                .andThen(embed -> embed.setAuthor(String.format("Server Info: %s", guild.getName()), null, avatarUrl)
                        .setThumbnail(guild.getIconUrl(Format.JPEG).orElse(""))
                        .addField("Owner", owner.getUsername(), true)
                        .addField("Server ID", guild.getId().asString(), true)
                        .addField("Creation date", creationDate, true)
                        .addField("Region", region.getName(), true)
                        .addField("Channels", String.format("**Voice:** %d%n**Text:** %d", voiceChannels, textChannels), true)
                        .addField("Members", Integer.toString(guild.getMemberCount().getAsInt()), true));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show info about this server.")
                .build();
    }

}

package me.shadorc.shadbot.command.admin;

import java.util.List;
import java.util.Set;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.BanQuerySpec;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "softban" })
public class SoftBanCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		Set<Snowflake> mentionedUserIds = context.getMessage().getUserMentionIds();
		if(mentionedUserIds.isEmpty()) {
			throw new MissingArgumentException();
		}

		if(mentionedUserIds.contains(context.getAuthorId())) {
			throw new CommandException("You cannot softban yourself.");
		}

		final Snowflake guildId = context.getGuildId();

		return DiscordUtils.hasPermissions(context.getMember(), Permission.BAN_MEMBERS)
				.zipWith(DiscordUtils.hasPermissions(context.getSelf(), guildId, Permission.BAN_MEMBERS))
				.filterWhen(authorAndSelfPerm -> {
					if(!authorAndSelfPerm.getT1()) {
						throw new CommandException("You don't have permission to softban.");
					}

					if(authorAndSelfPerm.getT2()) {
						return BotUtils.sendMessage(TextUtils.missingPerm(Permission.BAN_MEMBERS), context.getChannel())
								.thenReturn(false);
					}

					return Mono.just(true);
				})
				.flatMap(ignored -> context.getMessage().getUserMentions().collectList())
				.zipWith(context.getGuild())
				.flatMap(mentionsAndGuild -> {

					final List<User> mentions = mentionsAndGuild.getT1();
					final Guild guild = mentionsAndGuild.getT2();

					StringBuilder reason = new StringBuilder();
					reason.append(StringUtils.remove(arg, FormatUtils.format(mentions, User::getMention, " ")).trim());
					if(reason.length() > DiscordUtils.MAX_REASON_LENGTH) {
						throw new CommandException(String.format("Reason cannot exceed **%d characters**.", DiscordUtils.MAX_REASON_LENGTH));
					}

					if(reason.length() == 0) {
						reason.append("Reason not specified.");
					}

					Flux<Void> banFlux = Flux.empty();
					for(User user : mentions) {
						if(!user.isBot()) {
							banFlux.concatWith(BotUtils.sendMessage(
									String.format(Emoji.INFO + " You were softbanned from the server **%s** by **%s**. Reason: `%s`",
											guild.getName(), context.getUsername(), reason), user.getPrivateChannel().cast(MessageChannel.class))
									.then());
						}

						BanQuerySpec banQuery = new BanQuerySpec()
								.setReason(reason.toString())
								.setDeleteMessageDays(7);
						banFlux.concatWith(user.asMember(guildId)
								.flatMap(member -> member.ban(banQuery)));
						banFlux.concatWith(user.asMember(guildId)
								.flatMap(Member::unban));
					}

					return banFlux
							.then(BotUtils.sendMessage(String.format(Emoji.INFO + " (Requested by **%s**) **%s** got softbanned. Reason: `%s`",
									context.getUsername(), FormatUtils.format(mentions, User::getUsername, ", "), reason),
									context.getChannel()))
							.then();
				});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Ban and instantly unban user(s).\nIt's like kicking him/them but it also deletes his/their messages "
						+ "from the last 7 days.")
				.addArg("@user(s)", false)
				.addArg("reason", true)
				.build();
	}

}

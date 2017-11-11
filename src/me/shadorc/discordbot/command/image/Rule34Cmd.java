package me.shadorc.discordbot.command.image;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class Rule34Cmd extends AbstractCommand {

	private static final int MAX_TAGS_LENGTH = 400;

	public Rule34Cmd() {
		super(CommandCategory.IMAGE, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "rule34");
		this.setAlias("r34");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.getChannel().isNSFW()) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " This must be a NSFW-channel. If you're an admin, you can use "
					+ "`" + context.getPrefix() + "settings " + Setting.NSFW + " toggle`", context.getChannel());
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			JSONObject mainObj = XML.toJSONObject(NetUtils.getBody("https://rule34.xxx/index.php?"
					+ "page=dapi"
					+ "&s=post"
					+ "&q=index"
					+ "&tags=" + URLEncoder.encode(context.getArg().replace(" ", "_"), "UTF-8")));

			JSONObject postsObj = mainObj.getJSONObject("posts");

			if(postsObj.getInt("count") == 0) {
				BotUtils.sendMessage(TextUtils.noResult(context.getArg()), context.getChannel());
				return;
			}

			JSONObject postObj;
			if(postsObj.get("post") instanceof JSONArray) {
				JSONArray postsArray = postsObj.getJSONArray("post");
				postObj = postsArray.getJSONObject(MathUtils.rand(postsArray.length() - 1));
			} else {
				postObj = postsObj.getJSONObject("post");
			}

			String[] tags = postObj.getString("tags").trim().split(" ");

			if(postObj.getBoolean("has_children") || this.isLegal(tags)) {
				BotUtils.sendMessage(Emoji.WARNING + " Sorry, I don't display images that contain children or that are tagged with `loli` or `shota`.", context.getChannel());
				return;
			}

			String formattedtags = StringUtils.formatArray(tags, tag -> "`" + tag.toString().trim() + "`", " ");
			if(formattedtags.length() > MAX_TAGS_LENGTH) {
				formattedtags = formattedtags.substring(0, MAX_TAGS_LENGTH - 3) + "...";
			}

			StringBuilder fileUrl = new StringBuilder(postObj.getString("file_url"));
			if(!fileUrl.toString().isEmpty() && !fileUrl.toString().startsWith("http")) {
				fileUrl.insert(0, "http:");
			}

			// This can be a number for some obscure reasons
			StringBuilder sourceUrl = new StringBuilder();
			Object source = postObj.get("source");
			if(source instanceof String && !source.toString().isEmpty()) {
				sourceUrl.append(source.toString());
				if(!sourceUrl.toString().startsWith("http") && sourceUrl.toString().startsWith("//")) {
					sourceUrl.insert(0, "http:");
				}
			}

			EmbedBuilder embed = Utils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Rule34 (Search: " + context.getArg() + ")")
					.withUrl(fileUrl.toString())
					.withThumbnail("http://rule34.paheal.net/themes/rule34v2/rule34_logo_top.png")
					.appendField("Resolution", postObj.getInt("width") + "x" + postObj.getInt("height"), false)
					.appendField("Source", sourceUrl.toString(), false)
					.appendField("Tags", formattedtags, false)
					.withImage(fileUrl.toString())
					.withFooterText("If there is no preview, click on the title to see the media (probably a video)");
			BotUtils.sendMessage(embed.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			ExceptionUtils.manageException("getting an image from Rule34", context, err);
		}
	}

	private boolean isLegal(String... tags) {
		return Arrays.asList(tags).stream().anyMatch(tag -> tag.contains("loli") || tag.contains("shota"));
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show a random image corresponding to a tag from Rule34 website.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <tag>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}

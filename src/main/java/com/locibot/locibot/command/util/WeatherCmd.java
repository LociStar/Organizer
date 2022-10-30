package com.locibot.locibot.command.util;

import com.locibot.locibot.api.wrapper.WeatherWrapper;
import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CmdAnnotation;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.EnumUtil;
import com.locibot.locibot.utils.LociBotUtil;
import com.locibot.locibot.utils.StringUtil;
import com.locibot.locibot.utils.weather.WeatherManager;
import com.locibot.locibot.utils.weather.WeatherMapManager;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import net.aksingh.owmjapis.api.APIException;
import net.aksingh.owmjapis.core.OWM;
import net.aksingh.owmjapis.core.OWM.Unit;
import org.apache.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@CmdAnnotation
public class WeatherCmd extends BaseCmd {

    private final DateTimeFormatter dateFormatter;
    private final OWM owm;

    public WeatherCmd() {
        super(CommandCategory.UTILS, "weather", "Search weather report for a city");
        this.addOption("city", "The city", true, ApplicationCommandOption.Type.STRING);
        this.addOption("country", "The country", false, ApplicationCommandOption.Type.STRING);

        this.dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.MEDIUM);
        final String apiKey = CredentialManager.get(Credential.OPENWEATHERMAP_API_KEY);
        if (apiKey != null) {
            this.owm = new OWM(apiKey);
            this.owm.setUnit(Unit.METRIC);
        } else {
            this.owm = null;
        }
    }

    private static Predicate<Throwable> isNotFound() {
        return thr -> thr instanceof APIException err && err.getCode() == HttpStatus.SC_NOT_FOUND;
    }

    @Override
    public Mono<?> execute(Context context) {
        final String city = context.getOptionAsString("city").orElseThrow();
        final Optional<String> countryOpt = context.getOptionAsString("country");

        WeatherManager weatherManager = new WeatherManager();

        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("weather.loading"))
                .then(Mono.fromCallable(() -> {
                    if (countryOpt.isPresent()) {
                        final String countryStr = countryOpt.get();
                        final OWM.Country country = EnumUtil.parseEnum(OWM.Country.class,
                                countryStr.replace(" ", "_"));
                        if (country == null) {
                            throw new CommandException(
                                    context.localize("weather.country.not.found").formatted(countryStr));
                        }
                        return this.owm.currentWeatherByCityName(city, country);
                    } else {
                        return this.owm.currentWeatherByCityName(city);
                    }
                }))
                .map(WeatherWrapper::new)
                .map(weather -> this.formatEmbed(context, weather))
                .flatMap(context::editFollowupMessage)
                .onErrorResume(WeatherCmd.isNotFound(), err -> {
                    if (countryOpt.isPresent()) {
                        return context.editFollowupMessage(Emoji.MAGNIFYING_GLASS, context.localize("weather.exception.country")
                                .formatted(city, countryOpt.orElseThrow()));
                    }
                    return context.editFollowupMessage(Emoji.MAGNIFYING_GLASS, context.localize("weather.exception.city")
                            .formatted(city));
                })
                .onErrorMap(APIException.class, IOException::new)
                .then(weatherManager.getSaved5DayWeatherData(city, 0.0, 0.0).flatMap(data -> {
                    WeatherMapManager weatherMapManager = new WeatherMapManager(data);
                    return context.getChannel().flatMap(textChannel -> textChannel.createMessage(messageCreateSpec -> {
                        try {
                            byte[] bytes = weatherMapManager.createHeatMap();
                            if (bytes.length > 0)
                                messageCreateSpec.addFile("temperature.png",
                                        new ByteArrayInputStream(bytes));
                            else
                                messageCreateSpec.setContent("Please provide a real city ;)");
                        } catch (IOException ignored) {
                        }
                        try {
                            byte[] bytes = weatherMapManager.createRainMap();
                            if (bytes.length > 0)
                                messageCreateSpec.addFile("rain.png",
                                        new ByteArrayInputStream(bytes));
                            else messageCreateSpec.setContent("No City No Rain");
                        } catch (IOException ignored) {
                        }
                    }));
                }));
    }

    private EmbedCreateSpec formatEmbed(Context context, WeatherWrapper weather) {
        final DateTimeFormatter formatter = this.dateFormatter.withLocale(context.getLocale());

        final String title = context.localize("weather.title")
                .formatted(weather.getCityName(), weather.getCountryCode());
        final String url = "https://openweathermap.org/city/%d".formatted(weather.getCityId());
        final String lastUpdated = formatter.format(weather.getDateTime());

        final String clouds = StringUtil.capitalize(weather.getCloudsDescription());
        final String wind = context.localize("weather.wind.speed")
                .formatted(weather.getWindDescription(context), context.localize(weather.getWindSpeed()));
        final String rain = weather.getPrecipVol3h() //TODO: need to change, because rain is somehow always none
                .map(data -> context.localize("weather.precip.volume").formatted(context.localize(data)))
                .orElse(context.localize("weather.none"));
        final String humidity = "%s%%".formatted(context.localize(weather.getHumidity()));
        final String temperature = "%s°C".formatted(context.localize(weather.getTemp()));

        return LociBotUtil.getDefaultEmbed(EmbedCreateSpec.builder()
                .author(EmbedCreateFields.Author.of(title, url, context.getAuthorAvatar()))
                .thumbnail(weather.getIconLink())
                .description(context.localize("weather.last.updated").formatted(lastUpdated))
                .fields(List.of(
                        EmbedCreateFields.Field.of(Emoji.CLOUD + " " + context.localize("weather.clouds"), clouds, true),
                        EmbedCreateFields.Field.of(Emoji.WIND + " " + context.localize("weather.wind"), wind, true),
                        EmbedCreateFields.Field.of(Emoji.RAIN + " " + context.localize("weather.rain"), rain, true),
                        EmbedCreateFields.Field.of(Emoji.DROPLET + " " + context.localize("weather.humidity"), humidity, true),
                        EmbedCreateFields.Field.of(Emoji.THERMOMETER + " " + context.localize("weather.temperature"), temperature, true))).build());
    }

}

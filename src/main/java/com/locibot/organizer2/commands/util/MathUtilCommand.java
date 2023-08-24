package com.locibot.organizer2.commands.util;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.locibot.organizer2.commands.CommandException;
import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.CommandContext;
import com.locibot.organizer2.object.Emoji;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

@Component
public class MathUtilCommand implements SlashCommand {

    private final DoubleEvaluator evaluator;

    public MathUtilCommand() {
        this.evaluator = new DoubleEvaluator();
    }

    @Override
    public String getName() {
        return "util math";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        final String arg = context.getOptionAsString("expression").orElseThrow();
        final double result;
        try {
            result = this.evaluator.evaluate(arg);
        } catch (final IllegalArgumentException err) {
            return Mono.error(new CommandException(err.getMessage()));
        }

        final Mono<DecimalFormat> formatterMono = context.getLocale_old().map(locale -> new DecimalFormat("#.##",
                new DecimalFormatSymbols(locale)));
        return formatterMono.flatMap(formatter -> context.getEvent().reply(Emoji.TRIANGULAR_RULER + "%s = %s"
                .formatted(arg.replace("*", "\\*"), formatter.format(result))));
    }
}

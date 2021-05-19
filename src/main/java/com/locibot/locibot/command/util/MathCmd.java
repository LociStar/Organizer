package com.locibot.locibot.command.util;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class MathCmd extends BaseCmd {

    private final DoubleEvaluator evaluator;

    public MathCmd() {
        super(CommandCategory.UTILS, "math", "Evaluate an expression");
        this.addOption("expression", "Expression to evaluate (example: 2*cos(pi))", true,
                ApplicationCommandOptionType.STRING);

        this.evaluator = new DoubleEvaluator();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String arg = context.getOptionAsString("expression").orElseThrow();
        final double result;
        try {
            result = this.evaluator.evaluate(arg);
        } catch (final IllegalArgumentException err) {
            return Mono.error(new CommandException(err.getMessage()));
        }

        final DecimalFormat formatter = new DecimalFormat("#.##",
                new DecimalFormatSymbols(context.getLocale()));
        return context.createFollowupMessage(Emoji.TRIANGULAR_RULER, "%s = %s"
                .formatted(arg.replace("*", "\\*"), formatter.format(result)));
    }

}

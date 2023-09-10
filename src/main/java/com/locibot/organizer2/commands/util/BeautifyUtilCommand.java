package com.locibot.organizer2.commands.util;

import com.loci.textbeautifier.Font;
import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.command.CommandContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class BeautifyUtilCommand implements SlashCommand {
    @Override
    public String getName() {
        return "util beautify";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        String text = context.getOptionAsString("text").orElseThrow();
        String font = context.getOptionAsString("font").orElseThrow();

        switch (font) {
            case "Gothic Style" -> text = Font.toGothicStyle(text);
            case "Emoji" -> text = Font.toEmojiStyle(text);
            case "Black Square" -> text = Font.toBlackSquareStyle(text);
            case "Bubble" -> text = Font.toBubbleStyle(text);
            case "Math Sans Bold" -> text = Font.toMathSansBoldStyle(text);
            case "Math Script Bold" -> text = Font.toMathScriptBoldStyle(text);
            case "Word With Decor" -> text = Font.toWordWithDecoration(text);
            case "Letter with Decor 1" -> text = Font.toLetterWithDecoration(text);
            case "Letter with Decor 2" -> text = Font.toLetterWithDecoration2(text);
            case "Letter with Decor 3" -> text = Font.toLetterWithDecoration3(text);
            case "Text with Decor" -> text = Font.toTextWithDecoration(text);

        }
        return context.getEvent().reply(text);
    }
}

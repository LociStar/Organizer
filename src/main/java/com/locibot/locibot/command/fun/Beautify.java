package com.locibot.locibot.command.fun;

import com.loci.textbeautifier.Font;
import com.loci.textbeautifier.fonts.*;
import com.locibot.locibot.core.command.*;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@CmdAnnotation
public class Beautify extends BaseCmd {
    public Beautify() {
        super(CommandCategory.FUN, CommandPermission.USER_GUILD, "beautify", "You might get greeted");
        List<ApplicationCommandOptionChoiceData> choices = new ArrayList<>();
        choices.add(ApplicationCommandOptionChoiceData.builder().name(GothicStyle.getName()).value(GothicStyle.getName()).build());
        choices.add(ApplicationCommandOptionChoiceData.builder().name(Emoji.getName()).value(Emoji.getName()).build());
        choices.add(ApplicationCommandOptionChoiceData.builder().name(BlackSquare.getName()).value(BlackSquare.getName()).build());
        choices.add(ApplicationCommandOptionChoiceData.builder().name(Bubble.getName()).value(Bubble.getName()).build());
        choices.add(ApplicationCommandOptionChoiceData.builder().name(MathSansBold.getName()).value(MathSansBold.getName()).build());
        choices.add(ApplicationCommandOptionChoiceData.builder().name("Word With Decor").value("Word With Decor").build());
        choices.add(ApplicationCommandOptionChoiceData.builder().name("Letter with Decor 1").value("Letter with Decor 1").build());
        choices.add(ApplicationCommandOptionChoiceData.builder().name("Letter with Decor 2").value("Letter with Decor 2").build());
        choices.add(ApplicationCommandOptionChoiceData.builder().name("Letter with Decor 3").value("Letter with Decor 3").build());
        choices.add(ApplicationCommandOptionChoiceData.builder().name("Text with Decor").value("Text with Decor").build());

        this.addOption(option -> option.name("font")
                .description("Select a Font")
                .addAllChoices(choices)
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue()));

        this.addOption(option -> option.name("text")
                .description("the text you want to beautify")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
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
        return context.createFollowupMessage(text);
    }
}

package com.locibot.locibot.command.image;

import com.locibot.locibot.command.util.petpetUtil.ThreadedImageBrightener;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.squareup.gifencoder.*;
import com.twelvemonkeys.image.ResampleOp;
import discord4j.rest.util.Image;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;

public class PetPet extends BaseCmd {

    private static final int FPS_CAP = 60;
    private static final int HAND_SIZE = 112;
    private static final int DEFAULT_X = 12;
    private static final int DEFAULT_Y = 18;
    private static final float PET_SIZE = 100.0F;

    protected PetPet() {
        super(CommandCategory.IMAGE, CommandPermission.USER_GUILD, "petpet", "create a petpet gif");
    }

    @Override
    public Mono<?> execute(Context context) {
        Image image = context.getAuthor().getAvatar().block();

        int fps = this.getClampedFPS(20);
        float scale = 1;

        try {
            return context.createFollowupMessage("Here is your pet: ").then(this.processPet(context, fps, scale, new ByteArrayInputStream(image.getData())));
        } catch (OutOfMemoryError | Exception error) {
            error.printStackTrace();
        }

        return context.createFollowupMessage("Ups.. something broke^^");
    }

    private int getClampedFPS(int fps) {
        return fps > FPS_CAP ? FPS_CAP : fps < 0 ? 1 : fps;
    }

    private Mono<?> processPet(Context context, int fps, float scale, InputStream stream) throws Exception {
        File outputGif = new File("./petpet_output.gif");
        FileOutputStream outputStream = new FileOutputStream(outputGif);
        GifEncoder gifEncoder = new GifEncoder(outputStream, 112, 112, 0);
        ImageOptions options = new ImageOptions()
                .setTransparencyColor(Color.BLACK.getRGB())
                .setColorQuantizer(MedianCutQuantizer.INSTANCE)
                .setDitherer(FloydSteinbergDitherer.INSTANCE)
                .setDisposalMethod(DisposalMethod.DO_NOT_DISPOSE);
        System.out.println(Arrays.toString(options.getClass().getDeclaredFields()));
        Field delayCentiSeconds = options.getClass().getDeclaredField("delayCentiSeconds");
        delayCentiSeconds.setAccessible(true);
        delayCentiSeconds.set(options, Math.round(100.0F / fps));
        try {
            BufferedImage bufferedImage = ImageIO.read(this.getClass().getResource("/Pictures/petpet.png"));
            BufferedImage image = ImageIO.read(stream);
            ThreadedImageBrightener imageDarkener = new ThreadedImageBrightener(image, 0.05F);
            imageDarkener.run();

            int inputWidth = image.getWidth(null);
            int inputHeight = image.getHeight(null);

            scale = Math.min(PET_SIZE / (float) inputWidth, PET_SIZE / (float) inputHeight) * scale;
            int width = (int) ((float) inputWidth * scale);
            int height = (int) ((float) inputHeight * scale);

            FrameDrawerType[] drawerTypes = FrameDrawerType.values();
            for (int i = 0; i < drawerTypes.length; i++) {
                BufferedImage combined = new BufferedImage(HAND_SIZE, HAND_SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics combinedGraphics = combined.getGraphics();
                drawerTypes[i].drawer.draw(combinedGraphics, image, width, height);
                combinedGraphics.drawImage(bufferedImage.getSubimage(i * 112, 0, 112, 112), 0, 0, null);
                combinedGraphics.dispose();
                int[] rgb = combined.getRGB(0, 0, HAND_SIZE, HAND_SIZE, new int[HAND_SIZE * HAND_SIZE], 0, HAND_SIZE);
                gifEncoder.addImage(rgb, HAND_SIZE, options);
            }

            gifEncoder.finishEncoding();
            stream.close();

            File outputGif2 = new File("./petpet_output.gif");

            InputStream inputStreamReader = new FileInputStream(outputGif2);

            return context.getChannel().flatMap(textChannel -> textChannel.createMessage(messageCreateSpec -> {
                try {
                    messageCreateSpec.addFile("petpet.gif", new ByteArrayInputStream(inputStreamReader.readAllBytes()));
                    outputGif.deleteOnExit();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Mono.empty();
    }

    //TODO: Change to work mutatively, as in it scales off the previous scaled image to improve performance.
    private enum FrameDrawerType {
        FIRST(((graphics, bufferedImage, width, height) -> {
            graphics.drawImage(new ResampleOp(width, height, ResampleOp.FILTER_LANCZOS).filter(bufferedImage, null), DEFAULT_X + 4, DEFAULT_Y, null);
        })),
        SECOND(((graphics, bufferedImage, width, height) -> {
            graphics.drawImage(new ResampleOp(width, (int) ((float) height * 0.82F), ResampleOp.FILTER_LANCZOS).filter(bufferedImage, null), DEFAULT_X + 4, DEFAULT_Y + 14, null);
        })),
        THIRD(((graphics, bufferedImage, width, height) -> {
            graphics.drawImage(new ResampleOp((int) ((float) width * 1.1F), (int) ((float) height * 0.81F), ResampleOp.FILTER_LANCZOS).filter(bufferedImage, null), DEFAULT_X - 4, DEFAULT_Y + 17, null);
        })),
        FOURTH(((graphics, bufferedImage, width, height) -> {
            graphics.drawImage(new ResampleOp((int) ((float) width * 1.025F), (int) ((float) height * 0.86F), ResampleOp.FILTER_LANCZOS).filter(bufferedImage, null), DEFAULT_X - 4, DEFAULT_Y + 12, null);
        })),
        LAST(((graphics, bufferedImage, width, height) -> {
            graphics.drawImage(new ResampleOp(width, height, ResampleOp.FILTER_LANCZOS).filter(bufferedImage, null), DEFAULT_X, DEFAULT_Y, null);
        }));

        private final Drawer drawer;

        FrameDrawerType(Drawer drawer) {
            this.drawer = drawer;
        }

        @FunctionalInterface
        interface Drawer {
            void draw(Graphics graphics, BufferedImage image, int width, int height);
        }
    }
}


package com.locibot.locibot.command.util.petpetUtil;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * @Author: EDZ
 * @Description: ${description}
 * @Date: 2019/5/9 13:05
 * @Version: 1.0
 */
public class CopeImageUtil {
    public static BufferedImage cutHeadImages(ByteArrayInputStream byteArrayInputStream) {
        BufferedImage avatarImage = null;
        try {
            avatarImage = ImageIO.read(byteArrayInputStream);
            avatarImage = scaleByPercentage(avatarImage, avatarImage.getWidth(), avatarImage.getWidth());
            int width = avatarImage.getWidth();
            // Picture with transparent background
            BufferedImage formatAvatarImage = new BufferedImage(width, width, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D graphics = formatAvatarImage.createGraphics();
            //Cut the picture into a garden
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //Leave a blank area of ​​one pixel, this is very important, cover this when drawing a circle
            int border = 1;
            //The picture is a circle
            Ellipse2D.Double shape = new Ellipse2D.Double(border, border, width - border * 2, width - border * 2);
            //The area to be reserved
            graphics.setClip(shape);
            graphics.drawImage(avatarImage, border, border, width - border * 2, width - border * 2, null);
            graphics.dispose();
            //Draw another circle outside the circle chart
            //Create a new graphic so that the circle drawn will not be jagged
            graphics = formatAvatarImage.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int border1 = 3;
            //The brush is 4.5 pixels, the use of BasicStroke can check the following reference document
            //When making the brush, it will basically extend a certain pixel like the outside, and you can test it when you can use it
//            Stroke s = new BasicStroke(5F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
//            graphics.setStroke(s);
//            graphics.setColor(Color.WHITE);
//            graphics.drawOval(border1, border1, width - border1 * 2, width - border1 * 2);
//            graphics.dispose();
//            OutputStream os = new FileOutputStream("./13000.png");//When publishing a project, such as: Tomcat he will create this under the local tomcat webapps file on the server file name
//            ImageIO.write(formatAvatarImage, "PNG", os);
            return formatAvatarImage;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Reduce Image, this method returns the image after the source image is scaled under the given width and height restrictions
     *
     * @param inputImage : Width after compression
     *                   : Height after compression
     * @throws java.io.IOException return
     */
    public static BufferedImage scaleByPercentage(BufferedImage inputImage, int newWidth, int newHeight) {
        // Get the original image transparency type
        try {
            int type = inputImage.getColorModel().getTransparency();
            int width = inputImage.getWidth();
            int height = inputImage.getHeight();
            // Turn on anti-aliasing
            RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Use high quality compression
            renderingHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            BufferedImage img = new BufferedImage(newWidth, newHeight, type);
            Graphics2D graphics2d = img.createGraphics();
            graphics2d.setRenderingHints(renderingHints);
            graphics2d.drawImage(inputImage, 0, 0, newWidth, newHeight, 0, 0, width, height, null);
            graphics2d.dispose();
            return img;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        //cutHeadImages("https://mjmall.oss-cn-shanghai.aliyuncs.com/18/1/merchantIcon.png");
    }
}

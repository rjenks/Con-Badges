package com.universalbits.conorganizer.badger.control;

import java.util.Base64;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Base64Utils {

    public static String imageToPNGBase64(RenderedImage image) throws IOException {
    	final Base64.Encoder base64Encoder = Base64.getEncoder();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        out.close();
        return base64Encoder.encodeToString(out.toByteArray());
    }
}

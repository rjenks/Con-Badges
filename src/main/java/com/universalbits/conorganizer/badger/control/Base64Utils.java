package com.universalbits.conorganizer.badger.control;

import org.apache.xerces.impl.dv.util.Base64;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Base64Utils {

    public static String imageToPNGBase64(RenderedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        out.close();
        return Base64.encode(out.toByteArray());
    }
}

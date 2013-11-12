package com.universalbits.conorganizer.badger.control;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.xerces.impl.dv.util.Base64;

public class Base64Utils {

	public static String imageToPNGBase64(RenderedImage image) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		out.close();
		return Base64.encode(out.toByteArray());
	}
}

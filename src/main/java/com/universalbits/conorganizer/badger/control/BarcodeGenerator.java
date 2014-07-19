package com.universalbits.conorganizer.badger.control;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class BarcodeGenerator {

    public static void main(String[] args) throws Exception {
        System.out.println(qrCodePNGBase64("http://animefest.org/", 400));
        System.out.println(code128PNGBase64("00110000-00012345", 400, 200));
    }

    @SuppressWarnings("unused")
    private static void imageToPNGFile(RenderedImage image, File file) throws IOException {
        ImageIO.write(image, "png", new File("SampleOut-Code128.png"));
    }

    public static String qrCodePNGBase64(String text, int size) throws Exception {
        java.util.Map<EncodeHintType, ErrorCorrectionLevel> encodeHints = new HashMap<>();
        encodeHints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        BitMatrix bitMatrix = multiFormatWriter.encode(text, BarcodeFormat.QR_CODE, size, size, encodeHints);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        return Base64Utils.imageToPNGBase64(bufferedImage);
    }

    public static String code128PNGBase64(String code, int width, int height) throws Exception {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        BitMatrix bitMatrix = multiFormatWriter.encode(code, BarcodeFormat.CODE_128, width, height);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        return Base64Utils.imageToPNGBase64(bufferedImage);
    }

}

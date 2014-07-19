package com.universalbits.conorganizer.badger.control;

import com.universalbits.conorganizer.badger.model.BadgeInfo;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.xerces.impl.dv.util.Base64;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;

import javax.print.PrintService;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BadgePrinter {
    private static final Logger LOGGER = Logger.getLogger(BadgePrinter.class.getName());

    private long t = 0;
    private int widthInInches = 4;
    private int heightInInches = 6;
    private int dpi = 300;
    private boolean stopped = false;

    public BadgePrinter() {

    }

    private void t(String event) {
        long n = System.currentTimeMillis();
        long d = t == 0 ? 0 : n - t;
        t = n;
        LOGGER.info(event + " " + d);
    }

    private File badgeDataDir;

    private File getBadgeFile(String name) {
        if (badgeDataDir == null) {
            badgeDataDir = new File("badgedata");
            if (!badgeDataDir.exists() || !badgeDataDir.isDirectory()) {
                throw new RuntimeException("badgedata directory not found");
            }
        }
        return new File(badgeDataDir, name);
    }

    private File getBadgeImage(String name) {
        File image = getBadgeFile(name + ".png");
        if (!image.exists()) {
            image = getBadgeFile(name + ".jpg");
        }
        return image;
    }

    private String loadImage(File picFile) throws IOException {
        String picBase64 = null;
        t("begin loading " + picFile);
        byte[] picData = toByteArray(picFile);
        if (picFile.getName().endsWith(".png")) {
            picBase64 = "data:image/png;base64," + Base64.encode(picData);
        } else if (picFile.getName().endsWith(".jpg")) {
            picBase64 = "data:image/jpeg;base64," + Base64.encode(picData);
        }
        t("end loading " + picFile);
        return picBase64;
    }

    private String getImage(final String picture, final String userId) throws IOException {
        String picBase64 = null;
        if (userId != null) {
            File uniquePicFile = getBadgeImage(userId);
            if (uniquePicFile.exists()) {
                picBase64 = loadImage(uniquePicFile);
            } else {
                LOGGER.log(Level.INFO, "No unique picture for member " + userId);
            }
        }
        // if we didn't find a unique picture for this member
        if (picBase64 == null) {
            final File picFile = getBadgeImage(picture);
            picBase64 = loadImage(picFile);
        }
        return picBase64;
    }

    private SVGDocument getSVGTemplate(String template) throws IOException, URISyntaxException {
        template = template + ".svg";
        final String uri = getBadgeFile(template).toURI().toString();
        t("begin loading " + uri);
        final String parser = XMLResourceDescriptor.getXMLParserClassName();
        final SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
        final SVGDocument doc = f.createSVGDocument(uri);
        t("end loading " + uri);
        return doc;
    }

    private Properties getProperties(String type) throws IOException, InterruptedException {
        final File propsFileUTF8 = getBadgeFile(type + ".utf8.properties");
        final Properties props = new Properties();
        if (propsFileUTF8.exists() && propsFileUTF8.canRead()) {
            final Reader propsReader = new InputStreamReader(new FileInputStream(propsFileUTF8), "UTF-8");
            props.load(propsReader);
        }
        return props;
    }

    private void printBadge(final BufferedImage image, PrintService ps) throws IOException, InterruptedException {
        try {
            final PrinterJob printJob = PrinterJob.getPrinterJob();
            printJob.setPrintService(ps);
            final PageFormat pf = printJob.defaultPage();
            System.out.println("x=" + pf.getImageableX() + " y=" + pf.getImageableY());
            System.out.println("width=" + pf.getImageableWidth() + " height=" + pf.getImageableHeight());
            final Paper paper = new Paper();
            // TODO - Read paper size from config
            paper.setSize(4.1 * 72, 6.15 * 72);
            paper.setImageableArea(0.0, 0.0, paper.getWidth(), paper.getHeight());
            pf.setPaper(paper);
            printJob.setPrintable(new Printable() {
                public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                    Graphics2D g2 = (Graphics2D) graphics;
                    // TODO - Read from config
                    final double xScale = 1.025;
                    final double xTranslate = -16;
                    final double yScale = 1.025;
                    final double yTranslate = 8;
                    final double widthScale = (pageFormat.getWidth() / image.getWidth()) * xScale;
                    final double heightScale = (pageFormat.getHeight() / image.getHeight()) * yScale;
                    final AffineTransform at = AffineTransform.getScaleInstance(widthScale, heightScale);
                    at.translate(xTranslate, yTranslate);
                    if (pageIndex != 0) {
                        return NO_SUCH_PAGE;
                    }
                    g2.drawRenderedImage(image, at);
                    return PAGE_EXISTS;
                }
            }, pf);
            printJob.print();
        } catch (Exception e) {
            throw new RuntimeException("Error printing", e);
        }
    }

    public void stop() {
        this.stopped = true;
    }

    public void printBadges(BadgeSource badgeSource, PrintService printService) {
        while (!stopped) {
            BadgeInfo badgeInfo = badgeSource.getBadgeToPrint();
            String status = "ERROR - UNKNOWN";
            if (badgeInfo == null) {
                return;
            }
            try {
                final SVGDocument doc = generateBadge(badgeInfo);
                final BufferedImage image = generateImage(doc);
                printBadge(image, printService);
                badgeSource.reportDone(badgeInfo);
                status = "OK";
            } catch (Exception e) {
                badgeSource.reportProblem(badgeInfo);
                status = "ERROR - " + e.getMessage();
                LOGGER.log(Level.SEVERE, "Error while printing badge " + badgeInfo);
            } finally {
                final Object context = badgeInfo.getContext();
                if (context != null && context instanceof BadgeStatusListener) {
                    ((BadgeStatusListener) context).notifyBadgeStatus(badgeInfo, status);
                }
            }
        }
    }

    public void generateBadgePNGs(BadgeSource badgeSource, File outDir) {
        while (!stopped) {
            BadgeInfo badgeInfo = badgeSource.getBadgeToPrint();
            if (badgeInfo == null) {
                return;
            }
            try {
                SVGDocument doc = generateBadge(badgeInfo);
                generatePNG(badgeInfo, doc, outDir);
            } catch (Exception e) {
                badgeSource.reportProblem(badgeInfo);
                e.printStackTrace();
            }
        }
    }

    private SVGDocument generateBadge(BadgeInfo badgeInfo) throws Exception {
        Element e;
        final String type = badgeInfo.get(BadgeInfo.TYPE);
        // add the type specific defaults as needed 
        final Properties props = getProperties(type);
        for (Object key : props.keySet()) {
            final String stringKey = key.toString();
            //if the badge info doesn't contain this property, set it
            if (!badgeInfo.containsKey(stringKey)) {
                badgeInfo.put(stringKey, props.getProperty(stringKey));
            }
        }
        final SVGDocument doc = getSVGTemplate(badgeInfo.get(BadgeInfo.TEMPLATE));
        t("parse");
        for (String key : badgeInfo.keySet()) {
            switch (key) {
                case BadgeInfo.QRCODE:
                    // TODO if image else if text
                    e = doc.getElementById(BadgeInfo.QRCODE);
                    final String qrCodeURL = badgeInfo.get(BadgeInfo.QRCODE);
                    if (e != null && qrCodeURL != null) {
                        final int qrCodeWidth = Math.round(Float.parseFloat(e.getAttribute("width"))) * 7;
                        final String qrCodeData = "data:image/png;base64," + BarcodeGenerator.qrCodePNGBase64(qrCodeURL, qrCodeWidth);
                        t("gen qrcode");
                        e.setAttributeNS("http://www.w3.org/1999/xlink", "href", qrCodeData);
                        t("set qrcode");
                    }
                    break;
                case BadgeInfo.BARCODE:
                    // TODO if image else if text
                    e = doc.getElementById(BadgeInfo.BARCODE);
                    final String barcodeValue = badgeInfo.get(BadgeInfo.BARCODE);
                    if (e != null && barcodeValue != null) {
                        final int barcodeWidth = Math.round(Float.parseFloat(e.getAttribute("width"))) * 7;
                        final int barcodeHeight = Math.round(Float.parseFloat(e.getAttribute("height"))) * 7;
                        final String barcodeData = "data:image/png;base64," + BarcodeGenerator.code128PNGBase64(barcodeValue, barcodeWidth, barcodeHeight);
                        t("gen barcode");
                        e.setAttributeNS("http://www.w3.org/1999/xlink", "href", barcodeData);
                        t("set barcode");
                    }
                    break;
                case BadgeInfo.PICTURE:
                    e = doc.getElementById(BadgeInfo.PICTURE);
                    if (e != null) {
                        String picture = badgeInfo.get(BadgeInfo.PICTURE);
                        String userId = badgeInfo.get(BadgeInfo.ID_USER);
                        String picBase64 = getImage(picture, userId);
                        e.setAttributeNS("http://www.w3.org/1999/xlink", "href", picBase64);
                        t("set pic");
                    }
                    break;
                default:
                    e = doc.getElementById(key);
                    if (e != null) {
                        // Inkscape often puts a tspan element inside the text element for formatting. We need to preserve it.
                        final NodeList children = e.getChildNodes();
                        if (children.getLength() > 0) {
                            Node eChild = children.item(0);
                            String nodeName = eChild.getNodeName();
                            if (eChild instanceof Element && nodeName.equals("tspan")) {
                                e = (Element) eChild;
                            }
                        }
                        e.setTextContent(badgeInfo.get(key));
                    }
                    break;
            }
        }
        t("get elements");
        // Save modified XML
        /*
         * TransformerFactory tFactory = TransformerFactory.newInstance();
         * Transformer transformer = tFactory.newTransformer(); DOMSource source
         * = new DOMSource(doc); FileOutputStream xmlOut = new
         * FileOutputStream(memberRoleID + ".svg"); StreamResult result = new
         * StreamResult(xmlOut); transformer.transform(source, result);
         * xmlOut.close(); t("save xml");
         */
        return doc;
    }


    private File generatePNG(BadgeInfo badgeInfo, SVGDocument doc, File outDir) throws TranscoderException, IOException {
        final String userId = badgeInfo.get(BadgeInfo.ID_USER);
        final String badgeId = badgeInfo.get(BadgeInfo.ID_BADGE);
        final String type = badgeInfo.get(BadgeInfo.TYPE);
        String fileName;
        if (userId != null && badgeId != null) {
            fileName = userId + "-" + badgeId + "-" + type + ".png";
        } else if (badgeId != null) {
            fileName = badgeId + "-" + type + ".png";
        } else if (userId != null) {
            fileName = userId + "-" + type + ".png";
        } else {
            fileName = System.currentTimeMillis() + "-" + type + ".png";
        }
        PNGTranscoder t = new PNGTranscoder();
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) widthInInches * dpi);
        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) heightInInches * dpi);
        t.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 25.4f / 300.0f);
        // Set the transcoder input and output.
        TranscoderInput input = new TranscoderInput(doc);
        File outFile = new File(outDir, fileName);
        OutputStream outStream = new FileOutputStream(outFile);
        TranscoderOutput output = new TranscoderOutput(outStream);
        // Perform the transcoding.
        t.transcode(input, output);
        outStream.flush();
        outStream.close();
        t("save png");
        return outFile;
    }

    private BufferedImage generateImage(SVGDocument doc) throws IOException, TranscoderException {
        BufferedImageTranscoder t = new BufferedImageTranscoder();
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) widthInInches * dpi);
        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) heightInInches * dpi);
        t.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 25.4f / 300.0f);

        TranscoderInput input = new TranscoderInput(doc);
        t.transcode(input, null);

        BufferedImage image = t.getBufferedImage();
        t("generate image");
        return image;
    }

    private static byte[] toByteArray(File file) throws IOException {
        int length = (int) file.length();
        byte[] array = new byte[length];
        InputStream in = new FileInputStream(file);
        int offset = 0;
        while (offset < length) {
            int count = in.read(array, offset, (length - offset));
            offset += count;
        }
        in.close();
        return array;
    }

    private static class BufferedImageTranscoder extends ImageTranscoder {
        @Override
        public BufferedImage createImage(int w, int h) {
            return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void writeImage(BufferedImage img, TranscoderOutput output) {
            this.img = img;
        }

        public BufferedImage getBufferedImage() {
            return img;
        }

        private BufferedImage img = null;
    }

}

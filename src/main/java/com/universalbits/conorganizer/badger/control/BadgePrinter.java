package com.universalbits.conorganizer.badger.control;

import com.universalbits.conorganizer.badger.model.BadgeInfo;
import com.universalbits.conorganizer.common.ISettings;
import org.apache.batik.bridge.*;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.xerces.impl.dv.util.Base64;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGLength;
import org.w3c.dom.svg.SVGSVGElement;

import javax.print.PrintService;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 * Problems:
 * - SVG Version problems.  Workaround: remove version attribute from svg tag.
 * - Empty space in SVG is cut off.  Workaround: add viewbox attribute to svg tag.
 */
public class BadgePrinter {
    private static final Logger LOGGER = Logger.getLogger(BadgePrinter.class.getName());
    public static final String XLINK_NAMESPACE_URI = "http://www.w3.org/1999/xlink";
    public static final String ATTRIBUTE_HREF = "href";
    public static final String BADGE_DATA_DIR = "badgedata";

    public static final String PROPERTY_FIELDS = "fields";
    public static final String PROPERTY_PAGE_WIDTH = "pageWidth";
    public static final String PROPERTY_PAGE_HEIGHT = "pageHeight";
    public static final String PROPERTY_X_SCALE = "xScale";
    public static final String PROPERTY_X_TRANSLATE = "xTranslate";
    public static final String PROPERTY_Y_SCALE = "yScale";
    public static final String PROPERTY_Y_TRANSLATE = "yTranslate";
    public static final double DEFAULT_X_SCALE = 1;//0.975;
    public static final double DEFAULT_X_TRANSLATE = 0;//12.0;
    public static final double DEFAULT_Y_SCALE = 1;//0.975;
    public static final double DEFAULT_Y_TRANSLATE = 0;//18.0;
    public static final double DEFAULT_PAGE_WIDTH = 4.133;//4.1
    public static final double DEFAULT_PAGE_HEIGHT = 6.147;//6.15

    private long t = 0;
    private int widthInInches = 4;
    private int heightInInches = 6;
    private int dpi = 300;
    private boolean stopped = false;
    private ISettings settings;

    public BadgePrinter(ISettings settings) {
        this.settings = settings;
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
            badgeDataDir = new File(BADGE_DATA_DIR);
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
        final File propsFileUTF8 = getBadgeFile(type + ".properties");
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

            final Paper paper = new Paper();
            double pageWidth = settings.getPropertyDouble(PROPERTY_PAGE_WIDTH, DEFAULT_PAGE_WIDTH) * 72;
            double pageHeight = settings.getPropertyDouble(PROPERTY_PAGE_HEIGHT, DEFAULT_PAGE_HEIGHT) * 72;
            System.out.println("Setting paper size to " + pageWidth + "x" + pageHeight);
            paper.setSize(pageWidth, pageHeight);
            paper.setImageableArea(0.0, 0.0, paper.getWidth(), paper.getHeight());
            pf.setPaper(paper);

            System.out.println("Buffered Image is " + image.getWidth() + "x" + image.getHeight());
            System.out.println("Printer Page width=" + pf.getWidth() + " height=" + pf.getHeight());
            System.out.println("Printer Page Imageable x=" + pf.getImageableX() + " y=" + pf.getImageableY()
                    + " width=" + pf.getImageableWidth() + " height=" + pf.getImageableHeight());
            printJob.setPrintable(new Printable() {
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                Graphics2D g2 = (Graphics2D) graphics;
                final double xScale = settings.getPropertyDouble(PROPERTY_X_SCALE, DEFAULT_X_SCALE);
                final double xTranslate = settings.getPropertyDouble(PROPERTY_X_TRANSLATE, DEFAULT_X_TRANSLATE);
                final double yScale = settings.getPropertyDouble(PROPERTY_Y_SCALE, DEFAULT_Y_SCALE);
                final double yTranslate = settings.getPropertyDouble(PROPERTY_Y_TRANSLATE, DEFAULT_Y_TRANSLATE);
                final double widthScale = (pageFormat.getWidth() / image.getWidth()) * xScale;
                final double heightScale = (pageFormat.getHeight() / image.getHeight()) * yScale;
                System.out.println("Setting scale to " + widthScale + "x" + heightScale);
                final AffineTransform at = AffineTransform.getScaleInstance(widthScale, heightScale);
                System.out.println("Setting translate to " + xTranslate + "x" + yTranslate);
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

    public void printBadges(BadgeSource badgeSource, PrintService printService, File outDir) {
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
                saveBadgeInfo(badgeInfo, outDir);
                badgeSource.reportDone(badgeInfo);
                status = "OK";
            } catch (Exception e) {
                badgeInfo.put(BadgeInfo.ERROR, e.getMessage());
                badgeSource.reportProblem(badgeInfo);
                status = "ERROR - " + e.getMessage();
                LOGGER.log(Level.SEVERE, "Error while printing badge " + badgeInfo, e);
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
            String status = "ERROR - UNKNOWN";
            BadgeInfo badgeInfo = badgeSource.getBadgeToPrint();
            if (badgeInfo == null) {
                return;
            }
            try {
                SVGDocument doc = generateBadge(badgeInfo);
                generatePNG(badgeInfo, doc, outDir);
                saveBadgeInfo(badgeInfo, outDir);
                badgeSource.reportDone(badgeInfo);
                status = "OK";
            } catch (Exception e) {
                badgeInfo.put(BadgeInfo.ERROR, e.getMessage());
                badgeSource.reportProblem(badgeInfo);
                status = "ERROR: " + e.getMessage();
                LOGGER.log(Level.SEVERE, "Error while printing badge " + badgeInfo, e);
            } finally {
                final Object context = badgeInfo.getContext();
                if (context != null && context instanceof BadgeStatusListener) {
                    ((BadgeStatusListener) context).notifyBadgeStatus(badgeInfo, status);
                }
            }
        }
    }

    public SVGDocument generateBadge(BadgeInfo badgeInfo) throws Exception {
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
            for (int i = 0; i < 5; i++) {
                final String numberedKey = (i > 0) ? key + "_" + i : key;
                e = doc.getElementById(numberedKey);
                if (e != null) {
                    final String tag = e.getTagName();
                    if ("image".equals(tag)) {
                        switch (key) {
                            case BadgeInfo.QRCODE:
                                e = doc.getElementById(BadgeInfo.QRCODE);
                                final String qrCodeURL = badgeInfo.get(BadgeInfo.QRCODE);
                                if (e != null && qrCodeURL != null) {
                                    final int qrCodeWidth = Math.round(Float.parseFloat(e.getAttribute("width"))) * 7;
                                    final String qrCodeData = "data:image/png;base64," + BarcodeGenerator.qrCodePNGBase64(qrCodeURL, qrCodeWidth);
                                    t("gen qrcode");
                                    e.setAttributeNS(XLINK_NAMESPACE_URI, ATTRIBUTE_HREF, qrCodeData);
                                    t("set qrcode");
                                }
                                break;
                            case BadgeInfo.BARCODE:
                                e = doc.getElementById(BadgeInfo.BARCODE);
                                final String barcodeValue = badgeInfo.get(BadgeInfo.BARCODE);
                                if (e != null && barcodeValue != null) {
                                    if (e.getTagName().equals("image")) {
                                        final int barcodeWidth = Math.round(Float.parseFloat(e.getAttribute("width"))) * 7;
                                        final int barcodeHeight = Math.round(Float.parseFloat(e.getAttribute("height"))) * 7;
                                        final String barcodeData = "data:image/png;base64," + BarcodeGenerator.code128PNGBase64(barcodeValue, barcodeWidth, barcodeHeight);
                                        t("gen barcode");
                                        e.setAttributeNS(XLINK_NAMESPACE_URI, ATTRIBUTE_HREF, barcodeData);
                                        t("set barcode");
                                    }
                                }
                                break;
                            case BadgeInfo.PICTURE:
                                e = doc.getElementById(BadgeInfo.PICTURE);
                                if (e != null) {
                                    final String picture = badgeInfo.get(BadgeInfo.PICTURE);
                                    final String userId = badgeInfo.get(BadgeInfo.ID_USER);
                                    final String picBase64 = getImage(picture, userId);
                                    e.setAttributeNS(XLINK_NAMESPACE_URI, ATTRIBUTE_HREF, picBase64);
                                    t("set pic");
                                }
                                break;
                        }
                    } else if ("text".equals(tag)) {
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
                }
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

    private static Point2D.Double getScaledSize(double currentWidth, double currentHeight, double maxWidth, double maxHeight) {
        double ratioX =  maxWidth / currentWidth;
        double ratioY = maxHeight / currentHeight;
        double newWidth;
        double newHeight;
        if (ratioX > ratioY){
            newWidth = currentWidth * ratioY;
            newHeight = currentHeight * ratioY;
        } else {
            newWidth = currentWidth * ratioX;
            newHeight = currentHeight * ratioX;
        }
        return new Point2D.Double(newWidth, newHeight);
    }

    private void setupTranscoder(ImageTranscoder t, SVGDocument doc) throws TranscoderException {
        UserAgent userAgent = new UserAgentAdapter();
        DocumentLoader loader = new DocumentLoader(userAgent);
        BridgeContext ctx = new BridgeContext(userAgent, loader);
        ctx.setDynamicState(BridgeContext.DYNAMIC);
        GVTBuilder builder = new GVTBuilder();
        GraphicsNode rootGN = builder.build(ctx, doc);
        Rectangle2D bounds = rootGN.getBounds();

        double pageWidth = settings.getPropertyDouble(PROPERTY_PAGE_WIDTH, DEFAULT_PAGE_WIDTH) * dpi;
        double pageHeight = settings.getPropertyDouble(PROPERTY_PAGE_HEIGHT, DEFAULT_PAGE_HEIGHT) * dpi;
        Point2D.Double scaledSize = getScaledSize(bounds.getWidth(), bounds.getHeight(), pageWidth, pageHeight);
        System.out.println("Target size = " + pageWidth + "x" + pageHeight);
        System.out.println("Scaled size = " + scaledSize.getX() + "x" + scaledSize.getY());

        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) scaledSize.getX());
        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float)scaledSize.getY());
        t.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 25.4f / 300.0f);
    }

    private String getBadgeFilename(BadgeInfo badgeInfo, String ext) {
        String fileName = "";
        final String userId = badgeInfo.get(BadgeInfo.ID_USER);
        final String badgeId = badgeInfo.get(BadgeInfo.ID_BADGE);
        final String type = badgeInfo.get(BadgeInfo.TYPE);
        if (userId != null && badgeId != null) {
            fileName = userId + "-" + badgeId + "-" + type + ext;
        } else if (badgeId != null) {
            fileName = badgeId + "-" + type + ext;
        } else if (userId != null) {
            fileName = userId + "-" + type + ext;
        } else {
            fileName = System.currentTimeMillis() + "-" + type + ext;
        }
        return fileName;
    }

    private void saveBadgeInfo(BadgeInfo badgeInfo, File outDir) {
        String fileName = getBadgeFilename(badgeInfo, ".json");
        File file = new File(outDir, fileName);
        JSONObject json = badgeInfo.toJsonObject();
        try {
            FileOutputStream out = new FileOutputStream(file);
            byte[] jsonBytes = json.toString().getBytes(Charset.forName("UTF-8"));
            out.write(jsonBytes);
            out.close();
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "Error saving json", ioe);
        }
    }

    private File generatePNG(BadgeInfo badgeInfo, SVGDocument doc, File outDir) throws TranscoderException, IOException {
        String fileName = getBadgeFilename(badgeInfo, ".png");
        PNGTranscoder t = new PNGTranscoder();
        setupTranscoder(t, doc);
        // Set the transcoder input and output.
        TranscoderInput input = new TranscoderInput(doc);
        File outFile = new File(outDir, fileName);
        LOGGER.info("Saving badge as " + outFile.getAbsolutePath());
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
        setupTranscoder(t, doc);
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

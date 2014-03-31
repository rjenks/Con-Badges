package com.universalbits.conorganizer.badger.control;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.print.PrintService;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

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

import com.universalbits.conorganizer.badger.model.BadgeInfo;

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

    private final File getBadgeFile(String name) {
        if (badgeDataDir == null) {
            badgeDataDir = new File("badgedata");
            if (!badgeDataDir.exists() || !badgeDataDir.isDirectory()) {
                throw new RuntimeException("badgedata directory not found");
            }
        }
        return new File(badgeDataDir, name);
    }
    
    private final File getBadgeImage(String name) {
    	File image = getBadgeFile(name + ".png");
    	if (!image.exists()) {
    		image = getBadgeFile(name + ".jpg");
    	}
    	return image;
    }

    /**
     * @param args
     */
    /*
    public static void main(String[] args) {
        t("start");
//        final String hostname = getHostname();
        String badgeId = "";
        String jsonStr = "";
        BadgeInfo badgeInfo = null;
        long printDelay = 0;

        try {
            if (args.length > 0 && args[0] != null) {
                printDelay = Long.parseLong(args[0]);
            }
        } catch (NumberFormatException nfe) {
            System.err.println("usage: badgeprinter [printDelay]");
            System.exit(2);
        }

        //preloadTypes();

        File userHome = new File(System.getProperty("user.home"));
        final File outDir = new File(userHome, "badgeprinter");
        outDir.mkdir();

        APIClient client = new APIClient("BadgePrinter");
        
        printerName = client.getProperty("printerName");
        if (printerName == null || "".equals(printerName.trim())) {
            throw new RuntimeException("printerName property not set in ~/BadgePrinter.properties");
        }

        // final URL getUrl = new URL(GET_PRINT_JOB_URL + "?printer=" +
        // hostname);
        final Map<String, String> getBadgeParams = new HashMap<String, String>();
        final Map<String, String> markBadgePrintedParams = new HashMap<String, String>();

        long lastPrintTime = 0;
        while (true) {
            String status = "OK";
            badgeId = "";
            boolean connectionError = false;
            try {
                if (lastPrintTime == 0) {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("name", client.getClientName());
                    params.put("type", "printer");
                    params.put("product", "BadgePrinter");
                    params.put("vendor", "Universal Bits");
                    params.put("version", "1.1");
                    final URL registerUrl = client.getRequestUrl("register", params);
                    String resp = APIClient.getUrlAsString(registerUrl);
                    //System.out.println("register resp=" + resp);
                }
                t("before getUrl");
                final URL getUrl = client.getRequestUrl("get_badge_to_print", getBadgeParams);
                LOGGER.info(getUrl.toString());
                jsonStr = APIClient.getUrlAsString(getUrl);
                LOGGER.info("received jsonStr: " + jsonStr);
                t("after getUrl");

                System.out.println("jsonStr=" + jsonStr);
                if (jsonStr != null && jsonStr.length() > 0) {
                    final JSONObject jsonBadge = new JSONObject(jsonStr);
                    LOGGER.fine(jsonBadge.toString(4));
                    try {
                        badgeId = jsonBadge.getString("id_badge");
                    } catch (JSONException je) {
                        continue;
                    }

                    badgeInfo = new BadgeInfo(jsonBadge);

                    final SVGDocument doc = generateBadge(badgeInfo);
                    final File badge = generatePNG(badgeInfo, doc, outDir);
                    final long curTime = System.currentTimeMillis();
                    final long timeToGenerate = curTime - lastPrintTime;
                    if (timeToGenerate < printDelay) {
                        try {
                            final long delayTime = printDelay - timeToGenerate;
                            LOGGER.info("Pausing for " + delayTime + "ms to wait for printing");
                            Thread.sleep(delayTime);
                        } catch (InterruptedException ie) {
                            LOGGER.info("sleep interrupted");
                        }
                        LOGGER.info("Continuing");
                    }
                    //printBadge(badge);
                    lastPrintTime = System.currentTimeMillis();
                }
            } catch (UnknownHostException uhe) {
                LOGGER.log(Level.SEVERE, "Unknown Host Exception: " + uhe.getMessage());
                connectionError = true;
            } catch (SocketException se) {
                LOGGER.log(Level.SEVERE, "SocketException: " + se.getMessage());
                connectionError = true;
            } catch (SocketTimeoutException ste) {
                LOGGER.log(Level.SEVERE, "SocketTimeoutException: " + ste.getMessage());
                connectionError = true;
            } catch (Exception e) {
                status = "ERROR";
                LOGGER.log(Level.SEVERE, "Error printing badge", e);
            }

            if (badgeInfo != null) {
                boolean markedComplete = false;
                while (!markedComplete) {
                    try {
                        LOGGER.info("Marking badge " + badgeId + " as printed.  Status=" + status);
                        markBadgePrintedParams.put("id_badge", badgeId);
                        markBadgePrintedParams.put("status", status);
                        final URL finishUrl = client.getRequestUrl("mark_badge_printed", markBadgePrintedParams);
                        String finishStr = APIClient.getUrlAsString(finishUrl);
                        LOGGER.info("Received from finishUrl: " + finishStr);
                        markedComplete = true;
                    } catch (UnknownHostException uhe) {
                        LOGGER.log(Level.SEVERE, "Unknown Host Exception: " + uhe.getMessage());
                        connectionError = true;
                    } catch (SocketException se) {
                        LOGGER.log(Level.SEVERE, "SocketException: " + se.getMessage());
                        connectionError = true;
                    } catch (SocketTimeoutException ste) {
                        LOGGER.log(Level.SEVERE, "SocketTimeoutException: " + ste.getMessage());
                        connectionError = true;
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error marking badge " + badgeId + " as complete. type=" + badgeInfo.getType(), e);
                        connectionError = true;
                    }
                }
            }
            
            if (connectionError) {
                Toolkit.getDefaultToolkit().beep();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    LOGGER.info("sleep interrupted");
                }
            }
        }
    }
    */

    private String loadImage(File picFile) throws FileNotFoundException, IOException {
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

    private String getImage(final String picture, final String userId) throws FileNotFoundException, IOException {
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
	    			((BadgeStatusListener)context).notifyBadgeStatus(badgeInfo, status);
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

    private SVGDocument generateBadge(BadgeInfo badgeInfo) throws IOException, URISyntaxException, Exception,
                    FileNotFoundException, TransformerFactoryConfigurationError, TransformerConfigurationException,
                    TransformerException, TranscoderException {
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
        	if (BadgeInfo.QRCODE.equals(key)) {
                e = doc.getElementById(BadgeInfo.QRCODE);
                final String qrCodeURL = badgeInfo.get(BadgeInfo.QRCODE);
                if (e != null && qrCodeURL != null) {
        	        final int qrCodeWidth = Math.round(Float.parseFloat(e.getAttribute("width"))) * 7;
        	        final String qrCodeData = "data:image/png;base64," + BarcodeGenerator.qrCodePNGBase64(qrCodeURL, qrCodeWidth);
        	        t("gen qrcode");
        	        e.setAttributeNS("http://www.w3.org/1999/xlink", "href", qrCodeData);
        	        t("set qrcode");
                }
        	} else if (BadgeInfo.BARCODE.equals(key)) {
                e = doc.getElementById(BadgeInfo.BARCODE);
                if (e != null) {
        	        final String barcodeValue = badgeInfo.get(BadgeInfo.BARCODE);
        	        final int barcodeWidth = Math.round(Float.parseFloat(e.getAttribute("width"))) * 7;
        	        final int barcodeHeight = Math.round(Float.parseFloat(e.getAttribute("height"))) * 7;
        	        final String barcodeData = "data:image/png;base64," + BarcodeGenerator.code128PNGBase64(barcodeValue, barcodeWidth, barcodeHeight);
        	        t("gen barcode");
        	        e.setAttributeNS("http://www.w3.org/1999/xlink", "href", barcodeData);
        	        t("set barcode");
                }
        	} else if (BadgeInfo.PICTURE.equals(key)) {
                e = doc.getElementById(BadgeInfo.PICTURE);
                if (e != null) {
                	String picture = badgeInfo.get(BadgeInfo.PICTURE);
                	String userId = badgeInfo.get(BadgeInfo.ID_USER);
        	        String picBase64 = getImage(picture, userId);
        	        e.setAttributeNS("http://www.w3.org/1999/xlink", "href", picBase64);
        	        t("set pic");
                }
        	} else {
                e = doc.getElementById(key);
                if (e != null) {
                	// Inkscape often puts a tspan element inside the text element for formatting. We need to preserve it.
                    final NodeList children = e.getChildNodes();
                    if (children.getLength() > 0) {
                    	Node eChild = children.item(0);
                    	String nodeName = eChild.getNodeName();
                    	if (eChild instanceof Element && nodeName.equals("tspan")) {
                    		e = (Element)eChild;
                    	}
                    }
                	e.setTextContent(badgeInfo.get(key));
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
        t.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, new Float(25.4f / 300.0f));
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
        t.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, new Float(25.4f / 300.0f));

        TranscoderInput input = new TranscoderInput(doc);
        t.transcode(input, null);
     
        BufferedImage image = t.getBufferedImage();
        t("generate image");
        return image;
    }

    private static byte[] toByteArray(File file) throws FileNotFoundException, IOException {
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
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            return bi;
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

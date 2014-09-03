package com.universalbits.conorganizer.badger.control;
import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;


/**
 * Created by rjenks on 9/1/2014.
 */
public class PrinterUtils {

    public enum Orientation {
        LANDSCAPE(PageFormat.LANDSCAPE),
        PORTRAIT(PageFormat.PORTRAIT);

        private int value;

        private Orientation(int orientation) {
            value = orientation;
        }
    }

    public static PrintService[] getAvailablePrinters() {
        final PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();
        printRequestAttributeSet.add(new Copies(1));
        return PrintServiceLookup.lookupPrintServices(DocFlavor.INPUT_STREAM.PNG, printRequestAttributeSet);
    }

    public static PrinterJob getPrinterJob(PrintService ps, Orientation orientation) throws PrinterException {
        final PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintService(ps);
        final PageFormat pf = printJob.defaultPage();
        pf.setOrientation(orientation.value);
        return printJob;
    }

}

package com.universalbits.conorganizer.badger.ui;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.swing.AbstractAction;
import javax.swing.Action;

import com.universalbits.conorganizer.badger.control.BadgePrinter;
import com.universalbits.conorganizer.badger.control.BadgeSource;

class StartPrintingAction extends AbstractAction {
    /**
     * 
     */
    private final BadgePrinterUI badgePrinterUI;
    private final BadgeSource badgeSource;
    private static final long serialVersionUID = 1L;
    private static final String START_TEXT = "Start Printing";
    private static final String STOP_TEXT = "Stop Printing";
    private boolean running = false;
    BadgePrinter badgePrinter;
    
    public StartPrintingAction(BadgePrinterUI badgePrinterUI, BadgeSource badgeSource) {
        super(START_TEXT);
        this.badgePrinterUI = badgePrinterUI;
        this.badgeSource = badgeSource;
    }
    
    @Override
    public Object getValue(String key) {
        if (key.equals(Action.NAME)) {
            return running ? STOP_TEXT : START_TEXT;
        }
        return super.getValue(key);
    }
    
    private void stop() {
        running = false;
        badgePrinter.stop();
        firePropertyChange(Action.NAME, STOP_TEXT, START_TEXT);
    }
    
    private void start() {
        running = true;
        PrintService ps = null;
        
        firePropertyChange(Action.NAME, START_TEXT, STOP_TEXT);
        
        if (badgePrinterUI.isPrintMode()) {
            final PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
            pras.add(new Copies(1));
            
            final PrintService[] services = PrintServiceLookup.lookupPrintServices(DocFlavor.INPUT_STREAM.PNG, pras);
    
            for (PrintService service : services) {
                final String name = service.getName();
                System.out.println(name);
                // TODO need to 
                if (name.startsWith("HiTi")) {
                    ps = service;
                }
            }
            if (ps == null) {
                System.err.println("Could not find printer");
                this.setEnabled(true);
                return;
            }
        }
        final PrintService printService = ps;
        
        new Thread() {
            public void run() {
                badgePrinter = new BadgePrinter();
                if (badgePrinterUI.isPrintMode()) {
                    badgePrinter.printBadges(badgeSource, printService);
                } else {
                    final File userHome = new File(System.getProperty("user.home"));
                    final File outDir = new File(userHome, "badger");
                    outDir.mkdir();
                    badgePrinter.generateBadgePNGs(badgeSource, outDir);
                }
                StartPrintingAction.this.firePropertyChange(Action.NAME, START_TEXT, STOP_TEXT);
            }
        }.start();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (running) {
            stop();
        } else {
            start();
        }
    }
}
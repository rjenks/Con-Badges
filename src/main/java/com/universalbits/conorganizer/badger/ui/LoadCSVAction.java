package com.universalbits.conorganizer.badger.ui;

import com.universalbits.conorganizer.badger.control.BadgeQueue;
import com.universalbits.conorganizer.badger.control.CSVBadgeLoader;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

class LoadCSVAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final JFrame frame;
    private final BadgeQueue badgeQueue;

    public LoadCSVAction(JFrame frame, BadgeQueue badgeQueue) {
        super("Load Badge From CSV...");
        this.frame = frame;
        this.badgeQueue = badgeQueue;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        JFileChooser fc = new JFileChooser(new File("."));
        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().toLowerCase().endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return "(*.csv) Comma Separated Values";
            }

        };
        fc.addChoosableFileFilter(filter);
        fc.setFileFilter(filter);
        fc.setMultiSelectionEnabled(false);
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            final File file = fc.getSelectedFile();
            try {
                CSVBadgeLoader loader = new CSVBadgeLoader(new FileInputStream(file), badgeQueue);
                new Thread(loader).start();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
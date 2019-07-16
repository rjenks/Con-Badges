package com.universalbits.conorganizer.badger.ui;

import com.universalbits.conorganizer.common.ISettings;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Created by rjenks on 9/1/2014.
 */
public class SettingsAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
    private BadgePrinterUI badgePrinterUI;
    private ISettings settings;

    public SettingsAction(BadgePrinterUI badgePrinterUI, ISettings settings) {
        super("Settings...");
        this.badgePrinterUI = badgePrinterUI;
        this.settings = settings;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        new SettingsDialog(badgePrinterUI, settings);
    }
}

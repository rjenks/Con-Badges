package com.universalbits.conorganizer.badger.ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

/**
 * Created by rjenks on 9/3/2014.
 */
public class NewBadgeAction extends AbstractAction {
    private static final Logger LOGGER = Logger.getLogger(NewBadgeAction.class.getSimpleName());

    private final BadgePrinterUI badgePrinterUI;

    public NewBadgeAction(BadgePrinterUI badgePrinterUI) {
        super("New Badge...");
        this.badgePrinterUI = badgePrinterUI;
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        new NewBadgeDialog(badgePrinterUI);
    }
}

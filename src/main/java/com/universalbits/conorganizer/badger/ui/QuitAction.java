package com.universalbits.conorganizer.badger.ui;

import javax.swing.*;
import java.awt.event.ActionEvent;

class QuitAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public QuitAction() {
        super("Quit");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        System.exit(0);
    }

}
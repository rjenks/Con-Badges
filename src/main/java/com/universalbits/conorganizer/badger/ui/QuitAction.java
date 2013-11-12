package com.universalbits.conorganizer.badger.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

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
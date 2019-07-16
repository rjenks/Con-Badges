package com.universalbits.conorganizer.badger.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.logging.Logger;

import com.universalbits.conorganizer.badger.control.BadgeActivator;
import com.universalbits.conorganizer.badger.control.BadgeActivator.STAGE;

public class BarcodeKeyListener implements KeyListener {
	private static Logger LOGGER = Logger.getLogger(BarcodeKeyListener.class.getSimpleName());
	private final BadgeActivator badgeActivator;
	private StringBuffer barcode = new StringBuffer();
	
	public BarcodeKeyListener(BadgeActivator badgeActivator) {
		this.badgeActivator = badgeActivator;
	}

	public void keyPressed(KeyEvent event) {
	}

	public void keyReleased(KeyEvent event) {
	}

	public void keyTyped(KeyEvent event) {
		LOGGER.info("Key Typed: " + event.getKeyChar());
		if (badgeActivator.getStage() == STAGE.SCAN) {
			char c = event.getKeyChar();
			if (c == '\n') {
				LOGGER.info("Got CR");
				badgeActivator.activate(this.barcode.toString());
				this.barcode.setLength(0);
			} else {
				this.barcode.append(c);
				LOGGER.info("Got '" + c + "'");
			}
//			Is there a reason for this??
//			if (badgeActivator.getStage() == STAGE.DONE) {
//				badgeActivator.setStage(STAGE.SCAN);
//			}
		}
	}
}

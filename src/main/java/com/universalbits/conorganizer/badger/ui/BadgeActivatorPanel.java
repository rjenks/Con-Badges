package com.universalbits.conorganizer.badger.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.universalbits.conorganizer.badger.control.BadgeActivator;
import com.universalbits.conorganizer.badger.control.BadgeActivator.STAGE;

public class BadgeActivatorPanel extends JPanel implements BadgeActivator.StageListener, FocusListener, MouseListener {
	private static final long serialVersionUID = 1L;
	private static Logger LOGGER = Logger.getLogger(BadgeActivatorPanel.class.getSimpleName());

	private final BadgeActivator badgeActivator;
	private Clip successClip;
	private Clip failureClip;
	private final Color defaultColor;

	public BadgeActivatorPanel(final BadgeActivator badgeActivator) {
		this.badgeActivator = badgeActivator;
		this.badgeActivator.setStageListener(this);
		try {
			this.initSounds();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error loading sounds", e);
		}
		this.defaultColor = this.getBackground();
		this.setFocusable(true);
		this.addFocusListener(this);
		this.addMouseListener(this);
	}

	@Override
	public void stageChanged(STAGE oldStage, STAGE newStage) {
		LOGGER.info("stageChanged called");
		SwingUtilities.invokeLater(() -> {
			this.repaint();
		});
		if (newStage == STAGE.DONE) {
			this.successBuz();
		} else if (newStage == STAGE.OOPS) {
			this.failureBuz();
		}
	}

	private void initSounds() throws Exception {
		URL url = getClass().getResource("/sounds/success.wav");
		if (url == null) {
			throw new FileNotFoundException();
		}
		AudioInputStream audio = AudioSystem.getAudioInputStream(url);
		this.successClip = AudioSystem.getClip();
		this.successClip.open(audio);

		url = getClass().getResource("/sounds/failure.wav");
		audio = AudioSystem.getAudioInputStream(url);
		this.failureClip = AudioSystem.getClip();
		this.failureClip.open(audio);
	}

	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		final Graphics2D g = (Graphics2D) graphics;
		g.setFont(new Font("Sans Serif", Font.PLAIN, 20));
		if (this.hasFocus()) {
			switch (badgeActivator.getStage()) {
			case SCAN:
				g.drawString("Scan Barcode", 10, 20);
				break;
			case LOOKUP:
				g.drawString("Looking Up Record...", 10, 20);
				break;
			case PLACE:
				g.drawString("Place Badge over Writer", 10, 20);
				break;
			case PROGRAM:
				g.drawString("Activating the Badge...", 10, 20);
				break;
			case SAVE:
				g.drawString("Saving the Badge...", 10, 20);
				break;
			case DONE:
				g.drawString("Done!", 10, 20);
				break;
			case OOPS:
				g.drawString("Error!", 10, 20);
				break;
			}
		} else {
			g.drawString("Activate badge?", 10, 20);
		}
	}

	private void successBuz() {
		playClip(this.successClip);
	}

	private void failureBuz() {
		playClip(this.failureClip);
	}

	private void playClip(Clip clip) {
		int tries = 0;
		try {
			clip.stop();
			clip.setMicrosecondPosition(0L);
			clip.start();

			System.out.print("Waiting for clip to start");

			tries++;
			while (!clip.isRunning() && tries <= 3) {

				System.out.print(".");
				Thread.sleep(100L);
			}
			System.out.println("ok");

			System.out.print("Waiting for clip to stop");
			while (clip.isRunning()) {
				System.out.print(".");
				Thread.sleep(100L);
			}
			System.out.println("ok");
		} catch (InterruptedException e) {
			LOGGER.log(Level.FINEST, "clip interrupted", e);
		}
	}

	@Override
	public void focusGained(FocusEvent e) {
		SwingUtilities.invokeLater(() -> {
			this.setBackground(new Color(90, 255, 90));
		});
	}

	@Override
	public void focusLost(FocusEvent e) {
		SwingUtilities.invokeLater(() -> {
			this.setBackground(this.defaultColor);
		});
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		this.requestFocus();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

}

package com.universalbits.conorganizer.badger.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;

import com.universalbits.conorganizer.badger.control.BadgeQueue;
import com.universalbits.conorganizer.badger.control.ServerBadgeLoader;

public class ServerConnectAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
	private static final String STOP_TEXT = "Disconnect from Server";
	private static final String START_TEXT = "Connect to Server";
//	private JFrame frame;
	private BadgeQueue queue;
	private boolean running = false;
	private ServerBadgeLoader loader;
	
	public ServerConnectAction(JFrame frame, BadgeQueue queue) {
		super("Connect to Server");
//		this.frame = frame;
		this.queue = queue;
	}
	
	private synchronized void start() {
		running = true;
		loader = new ServerBadgeLoader(queue);
		new Thread(loader).start();
		firePropertyChange(Action.NAME, STOP_TEXT, START_TEXT);
	}
	
	private synchronized void stop() {
		running = false;
		loader.stop();
		loader = null;
		firePropertyChange(Action.NAME, START_TEXT, STOP_TEXT);		
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
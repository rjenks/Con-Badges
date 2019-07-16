package com.universalbits.conorganizer.badger.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.universalbits.conorganizer.badger.control.BadgeQueue;
import com.universalbits.conorganizer.badger.control.ServerBadgeLoader;
import com.universalbits.conorganizer.common.APIClient;
import com.universalbits.conorganizer.common.ISettings;
import com.universalbits.conorganizer.common.Settings;

public class ServerConnectAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    private static final String STOP_TEXT = "Disconnect from Server";
    private static final String START_TEXT = "Connect to Server";
    private JFrame frame;
    private BadgeQueue queue;
    private boolean running = false;
    private ServerBadgeLoader loader;
    private String token;
    private ISettings settings;

    public ServerConnectAction(JFrame frame, ISettings settings, BadgeQueue queue) {
        super("Connect to Server");
        this.frame = frame;
        this.queue = queue;
        this.settings = settings;
        loader = new ServerBadgeLoader(this.queue, new TokenRequiredListener());
    }

    private synchronized void start() {
        running = true;
        final String appName = settings.getProperty(Settings.PROPERTY_APP_NAME);
        final String clientName = settings.getProperty(APIClient.PROPERTY_NAME);
        frame.setTitle(appName + " - " + clientName);
        new Thread(loader).start();
        firePropertyChange(Action.NAME, STOP_TEXT, START_TEXT);
    }

    private synchronized void stop() {
        running = false;
        loader.stop();
        loader = null;
        firePropertyChange(Action.NAME, START_TEXT, STOP_TEXT);
    }

    public ServerBadgeLoader getServerBadgeLoader() {
        return loader;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (running) {
            stop();
        } else {
            start();
        }
    }

    private class TokenRequiredListener implements ServerBadgeLoader.TokenRequiredListener {
        private boolean asking = false;

        @Override
        public String getToken() {
            if ((token == null || token.isEmpty()) && !asking) {
                asking = true;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        token = JOptionPane.showInputDialog(frame, "API Client Token Required");
                        asking = false;
                    }
                });
            }
            return token;
        }

    }

}

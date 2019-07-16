package com.universalbits.conorganizer.badger.control;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.universalbits.conorganizer.badger.model.BadgeInfo;
import com.universalbits.conorganizer.badger.ui.BadgeListModel;

/**
 * Created by rjenks on 8/31/2014.
 */
public class BadgeTypeMonitor {
    private static final Logger LOGGER = Logger.getLogger(BadgeTypeMonitor.class.getSimpleName());
    private BadgeListModel typesList;
    private Path badgeDataPath;
    private WatchService watcher;
//    private WatchKey watchKey;

    public BadgeTypeMonitor(BadgeListModel typesList) {
        this.typesList = typesList;
        try {
            final FileSystem defaultFileSystem = FileSystems.getDefault();
            watcher = defaultFileSystem.newWatchService();
            badgeDataPath = Paths.get(BadgePrinter.BADGE_DATA_DIR);
            File badgeDataFile = badgeDataPath.toFile();
            LOGGER.fine("badgedata path = " + badgeDataFile.getAbsolutePath());
            if (!badgeDataFile.exists()) {
                badgeDataFile.mkdirs();
            }
//            watchKey = 
    		badgeDataPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
            loadTypes();
            new Thread(new MonitorRunnable()).start();
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "Error creating watcher for badge data", ioe);
        }
    }

    private void loadTypes() throws IOException {
        DirectoryStream<Path> dirStream = Files.newDirectoryStream(badgeDataPath);
        for (Path file : dirStream) {
            if (isPropertyFile(file)) {
                String type = getTypeFromFile(file);
                addType(type);
            }
        }
    }

    private void addType(String type) {
        int i;
        LOGGER.info("adding type " + type);
        final BadgeInfo badgeInfo = new BadgeInfo();
        badgeInfo.put(BadgeInfo.TYPE, type);
        for (i = 0; i < typesList.size(); i++) {
            String oType = typesList.get(i).get(BadgeInfo.TYPE);
            if (oType.compareTo(type) > 0) {
                break;
            }
        }
        typesList.add(i, badgeInfo);
    }

    private void removeType(String type) {
        LOGGER.info("removing type " + type);
        for (int i = 0; i < typesList.size(); i++) {
            if (typesList.get(i).get(BadgeInfo.TYPE).equals(type)) {
                LOGGER.info("type " + type + " removed");
                typesList.remove(i);
                break;
            }
        }
    }

    private boolean isPropertyFile(Path file) {
        return file.getFileName().toString().toUpperCase().endsWith(".PROPERTIES");
    }

    private String getTypeFromFile(Path file) {
        String type = file.toFile().getName().toString();
        return type.substring(0, type.lastIndexOf('.'));
    }

    private class MonitorRunnable implements Runnable {
        @Override
        public void run() {
            while(true) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException x) {
                    return;
                }

                for (WatchEvent<?> event: key.pollEvents()) {
                    final WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
					WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    Path filename = ev.context();
                    final Path child = badgeDataPath.resolve(filename);

                    if (isPropertyFile(child)) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                final String type = getTypeFromFile(child);
                                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                    addType(type);
                                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                    removeType(type);
                                }
                            }
                        });
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    LOGGER.log(Level.SEVERE, "badge data folder can no longer be monitored as it no longer exists");
                    break;
                }
            }
        }
    }
}

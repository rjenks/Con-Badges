package com.universalbits.conorganizer.badger.control;

import com.universalbits.conorganizer.badger.model.BadgeInfo;
import com.universalbits.conorganizer.common.APIClient;
import com.universalbits.conorganizer.common.Settings;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerBadgeLoader implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ServerBadgeLoader.class.getName());
    private static final int ERROR_DELAY = 2000;
    private static final String PRINT_STATUS = "___PRINT_STATUS";

    private final APIClient client = new APIClient(Settings.getInstance());
    private final BadgeQueue queue;
    private final TokenRequiredListener tokenRequiredListener;
    private boolean stopped = false;
    private final ServerBadgeNotifier serverBadgeNotifier = new ServerBadgeNotifier();

    public ServerBadgeLoader(BadgeQueue queue, TokenRequiredListener listener) {
        this.queue = queue;
        this.tokenRequiredListener = listener;
        new Thread(serverBadgeNotifier).start();
    }

    public APIClient getAPIClient() {
        return client;
    }

    public void stop() {
        stopped = true;
    }

    @Override
    public void run() {
        final Map<String, String> getBadgeParams = new HashMap<>();
        String jsonStr = "";
        BadgeInfo badgeInfo;
        long lastPrintTime = 0;
        while (!stopped) {
            boolean success = false;
            try {
                if (lastPrintTime == 0) {
                    final Map<String, String> params = new HashMap<>();
                    params.put("name", client.getClientName());
                    params.put("type", "printer");
                    params.put("product", "BadgePrinter");
                    params.put("vendor", "Universal Bits");
                    params.put("version", "1.1");
                    final URL registerUrl = client.getRequestUrl("register", params);
                    APIClient.getUrlAsString(registerUrl);
                }
                if (client.hasToken()) {
                    final URL getUrl = client.getRequestUrl("get_badge_to_print", getBadgeParams);
                    LOGGER.info(getUrl.toString());
                    jsonStr = APIClient.getUrlAsString(getUrl);
                    LOGGER.fine("received jsonStr: " + jsonStr);

                    if (jsonStr != null && jsonStr.length() > 0) {
                        final JSONObject jsonBadge = new JSONObject(jsonStr);
                        if (jsonBadge.has(BadgeInfo.ID_BADGE)) {
                            LOGGER.fine(jsonBadge.toString(4));
                            badgeInfo = new BadgeInfo(jsonBadge);
                            badgeInfo.setContext(serverBadgeNotifier);
                            queue.queueBadge(badgeInfo);
                        }
                    }
                    lastPrintTime = System.currentTimeMillis();
                    success = true;
                } else {
                    String token = tokenRequiredListener.getToken();
                    if (token != null && !token.isEmpty()) {
                        client.setToken(token);
                        success = true;
                    }
                }
            } catch (JSONException je) {
                LOGGER.log(Level.SEVERE, "Error parsing JSON " + jsonStr);
            } catch (UnknownHostException uhe) {
                LOGGER.log(Level.SEVERE, "Unknown Host Exception: " + uhe.getMessage());
            } catch (SocketException se) {
                LOGGER.log(Level.SEVERE, "SocketException: " + se.getMessage());
            } catch (SocketTimeoutException ste) {
                LOGGER.log(Level.SEVERE, "SocketTimeoutException: " + ste.getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error fetching badge", e);
            } finally {
                if (!success) {
                    try {
                        LOGGER.log(Level.SEVERE, "Pausing for " + ERROR_DELAY + "ms to due to exception");
                        Thread.sleep(ERROR_DELAY);
                    } catch (InterruptedException ie) {
                        LOGGER.info("sleep interrupted");
                    }
                }
            }
        }
    }

    private class ServerBadgeNotifier implements Runnable, BadgeStatusListener {
        private final BlockingQueue<BadgeInfo> queue = new LinkedBlockingQueue<>();
        private final Map<String, String> markBadgePrintedParams = new HashMap<>();

        @Override
        public void run() {
            while (!stopped) {
                try {
                    final BadgeInfo badgeInfo = queue.take();
                    final String badgeId = badgeInfo.get(BadgeInfo.ID_BADGE);
                    final String status = badgeInfo.get(PRINT_STATUS);
                    final String type = badgeInfo.get(BadgeInfo.TYPE);
                    boolean markedComplete = false;
                    while (!markedComplete) {
                        try {
                            LOGGER.info("Marking badge " + badgeId + " as printed.  Status=" + status);
                            markBadgePrintedParams.put("id_badge", badgeId);
                            markBadgePrintedParams.put("status", status);
                            final URL finishUrl = client.getRequestUrl("mark_badge_printed", markBadgePrintedParams);
                            String finishStr = APIClient.getUrlAsString(finishUrl);
                            LOGGER.info("Received from finishUrl: " + finishStr);
                            markedComplete = true;
                        } catch (UnknownHostException uhe) {
                            LOGGER.log(Level.SEVERE, "Unknown Host Exception: " + uhe.getMessage());
                        } catch (SocketException se) {
                            LOGGER.log(Level.SEVERE, "SocketException: " + se.getMessage());
                        } catch (SocketTimeoutException ste) {
                            LOGGER.log(Level.SEVERE, "SocketTimeoutException: " + ste.getMessage());
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Error marking badge " + badgeId + " as complete. type=" + type, e);
                        } finally {
                            if (!markedComplete) {
                                try {
                                    LOGGER.log(Level.SEVERE, "Pausing for " + ERROR_DELAY + "ms to due to exception");
                                    Thread.sleep(ERROR_DELAY);
                                } catch (InterruptedException ie) {
                                    LOGGER.info("sleep interrupted");
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    LOGGER.log(Level.INFO, "take interrupted", e);
                }
            }
        }

        @Override
        public void notifyBadgeStatus(BadgeInfo badgeInfo, String status) {
            badgeInfo.put(PRINT_STATUS, status);
            queue.add(badgeInfo);
        }

    }

    public static interface TokenRequiredListener {
        String getToken();
    }

}

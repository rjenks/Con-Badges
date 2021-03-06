package com.universalbits.conorganizer.badger.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class BadgeInfo extends HashMap<String, String> implements Comparable<BadgeInfo> {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_TYPE = "BADGE";

    public static final String TYPE = "TYPE";
    public static final String ID_BADGE = "ID_BADGE";
    public static final String ID_USER = "ID_USER";
    public static final String BARCODE = "BARCODE";
    public static final String QRCODE = "QRCODE";
    public static final String PICTURE = "PICTURE";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String TEMPLATE = "TEMPLATE";
    public static final String ERROR = "__ERROR";

    private String toString;
    private Object context;

    public BadgeInfo() {
        super();
    }

    public BadgeInfo(JSONObject jsonBadge) throws ParseException, JSONException {
        super();
        for (Object objectKey : jsonBadge.keySet()) {
            final String key = objectKey.toString();
            final String value = getJsonString(jsonBadge, key);
            put(key, value);
        }
    }

    public BadgeInfo(Map<String, String> fields) {
        super(fields);
    }

    public void setContext(Object context) {
        this.context = context;
    }

    public Object getContext() {
        return this.context;
    }

    private String getJsonString(JSONObject json, String key) throws JSONException {
        String value = null;
        if (!json.isNull(key)) {
            value = json.get(key).toString();
        }
        return value;
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        for (String key : keySet()) {
            json.put(key, get(key));
        }
        return json;
    }

    @Override
    public String get(Object key) {
        String value = super.get(key);
        if (value == null && TYPE.equals(key)) {
            value = DEFAULT_TYPE;
        }
        return value;
    }

    @Override
    public String put(String key, String value) {
        toString = null;
        return super.put(key, value);
    }

    @Override
    public String toString() {
        if (toString == null) {
            String description = get(DESCRIPTION);
            if (description != null) {
                toString = description;
            } else {
                final StringBuilder b = new StringBuilder();
                final String type = get(TYPE);
                if (type != null) {
                    b.append(type);
                    b.append(" ");
                }
                final String userId = get(ID_USER);
                if (userId != null) {
                    b.append(userId);
                }
                final String badgeId = get(ID_BADGE);
                if (userId != null && badgeId != null) {
                    b.append("-");
                }
                if (badgeId != null) {
                    b.append(badgeId);
                }
                if (userId != null || badgeId != null) {
                    b.append(" ");
                }
                final String error = get(ERROR);
                if (error != null) {
                    b.append(" (ERROR:");
                    b.append(error);
                    b.append(")");
                }
                toString = b.toString();
            }
        }
        return toString;
    }

    @Override
    public int compareTo(BadgeInfo o) {
        final String type = get(TYPE);
        final String oType = o.get(TYPE);
        final String badgeId = get(ID_BADGE);
        final String oBadgeId = o.get(ID_BADGE);
        int result = type.compareTo(oType);
        if (result == 0) {
            result = badgeId.compareTo(oBadgeId);
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof BadgeInfo) ? compareTo((BadgeInfo)o) == 0 : false;
    }
}

package com.universalbits.conorganizer.badger.model;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.universalbits.conorganizer.badger.control.BadgeStatusListener;

public class BadgeInfo extends HashMap<String,String> {
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
	
	public BadgeInfo(Map<String,String> fields) {
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
				toString = b.toString();
			}
		}
		return toString;
	}

}

package com.universalbits.conorganizer.badger.control;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import org.json.JSONObject;

import com.universalbits.conorganizer.common.APIClient;
import com.universalbits.conorganizer.common.Settings;

/**
 * Created by rjenks on 9/1/2014.
 * Test badge:110000-B63036
 */
public class BadgeActivator {
	public static enum STAGE {
		SCAN, LOOKUP, PLACE, PROGRAM, SAVE, DONE, OOPS
	};

	private static Logger LOGGER = Logger.getLogger(BadgeActivator.class.getSimpleName());
	private static final int BLOCKNUM_LOCK = 2;
	private static final int BLOCKNUM_OTP = 3;
	private static final byte[] OTP = { -31, 16, 6, 0 };
	private static final byte[] BLOCK4 = { 3, 46, -47, 1 };
	private static final byte[] BLOCK5 = { 42, 85, 3, 116 };
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	private static final String NFC_TERMINAL = "ACR122";
	private static final int JSON_INDENT = 4;
	private static final int BLOCK_LENGTH = 4;
	private static int MIFARE_ULTRALIGHT = 0x0003;

	private final StringBuffer dataBuffer = new StringBuffer();
	private final StringBuffer uidBuffer = new StringBuffer();
	private final byte[] blockData = new byte[4];
	private final APIClient apiClient = new APIClient(Settings.getInstance());

	private StageListener stageListener;
	private STAGE stage = STAGE.SCAN;

	public BadgeActivator() {
	}

	public STAGE getStage() {
		return this.stage;
	}

	private void setStage(STAGE stage) {
		STAGE oldStage = this.stage;
		this.stage = stage;
		if (this.stageListener != null) {
			this.stageListener.stageChanged(oldStage, this.stage);
			Thread.yield();
		}
	}

	public void setStageListener(StageListener stageListener) {
		this.stageListener = stageListener;
	}

	public void activate(final String lookupBarcode) {

		Runnable runner = () -> {
			final Map<String, String> getBadgeToActivateParams = new HashMap<>();
			final Map<String, String> activateBadgeParams = new HashMap<>();
			try {
				LOGGER.info("Activating Badge for Barcode: " + lookupBarcode);
				this.setStage(STAGE.LOOKUP);

				getBadgeToActivateParams.put("barcode", lookupBarcode);
				URL getUrl = this.apiClient.getRequestUrl("get_badge_to_activate", getBadgeToActivateParams);
				LOGGER.info("Fetching badge info from: " + getUrl);
				String jsonStr = APIClient.getUrlAsString(getUrl);
				LOGGER.info("Recived badge info: " + jsonStr);

				JSONObject jsonBadge = new JSONObject(jsonStr);
				LOGGER.info(jsonBadge.toString(JSON_INDENT));
				String hashcode = jsonBadge.getString("hashcode");
				String currentUID = null;
				if (!jsonBadge.isNull("uid")) {
					currentUID = jsonBadge.getString("uid");
				}
				hashcode = hashcode.trim();
				if (hashcode.length() != 32) {
					throw new RuntimeException("hashcode must be 32 chars");
				}
				this.setStage(STAGE.PLACE);
				final ProgramTagResult tagResult = this.programTag(hashcode, currentUID);
				final String uidHex = tagResult.uid;
				LOGGER.info("uidHex=" + uidHex + " message=" + tagResult.message);
				if (uidHex != null && uidHex.length() == 14) {
					this.setStage(STAGE.SAVE);

					LOGGER.info("Saving badge activation to website");
					activateBadgeParams.put("barcode", lookupBarcode);
					activateBadgeParams.put("uid", uidHex);
					URL storeUrl = this.apiClient.getRequestUrl("activate_badge", activateBadgeParams);
					LOGGER.info("storeUrl = " + storeUrl);
					String status = APIClient.getUrlAsString(storeUrl);
					LOGGER.info("Store Status: " + status);
					if (status != null && status.startsWith("true")) {
						this.setStage(STAGE.DONE);
					} else {
						this.setStage(STAGE.OOPS);
					}
				} else {
					System.err.println("ERROR: uid was " + uidHex);
					this.setStage(STAGE.OOPS);
				}
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error fetching badge", e);
			}
			this.setStage(STAGE.SCAN);
		};
		Thread t = new Thread(runner);
		t.start();
	}

	public ProgramTagResult programTag(String hashcode, String currentUID) throws IOException, InterruptedException {
		ProgramTagResult result = new ProgramTagResult(null, "Unknown Error");
		CardTerminal terminal = null;
		Card card = null;
		try {
			final TerminalFactory factory = TerminalFactory.getDefault();
			final List<CardTerminal> terminals = factory.terminals().list();
			for (CardTerminal discoveredTerminal : terminals) {
				if (discoveredTerminal.getName().contains(NFC_TERMINAL)) {
					terminal = discoveredTerminal;
					break;
				}
			}
			if (terminal == null) {
				result.message = NFC_TERMINAL + " NFC Reader Not Found";
			} else {
				terminal.waitForCardPresent(5000);
				final boolean isCardPresent = terminal.isCardPresent();
				LOGGER.info("Is card present? " + isCardPresent);
				if (isCardPresent) {
					card = terminal.connect("*");
					byte[] historicalBytes = card.getATR().getHistoricalBytes();
					LOGGER.info("Received ATR length=" + historicalBytes.length);
					LOGGER.info("ATR:" + bytesToHex(historicalBytes));
					if (historicalBytes.length >= 11) {
						int tagId = (historicalBytes[9] & 0xff) << 8 | historicalBytes[10];
						LOGGER.info("TagType: " + tagId);
						if (tagId == MIFARE_ULTRALIGHT) {
							LOGGER.info("Tag is a MIFARE_ULTRALIGHT");
							CardChannel channel = card.getBasicChannel();

							// Read OTP block
							readBlock(channel, BLOCKNUM_OTP, blockData);
							this.dataBuffer.setLength(0);
							convertToHexString(this.dataBuffer, blockData, BLOCK_LENGTH);
							LOGGER.fine("Received OTP values " + this.dataBuffer);
							boolean isUnformatted = this.isUnformatted(blockData);
							boolean isWrongFormat = this.isWrongFormat(blockData);
							LOGGER.info("isUnformatted:" + isUnformatted + " isWrongFormat:" + isWrongFormat);

							// Check if the tag is READONLY
							readBlock(channel, BLOCKNUM_LOCK, blockData);
							this.dataBuffer.setLength(0);
							convertToHexString(this.dataBuffer, blockData, BLOCK_LENGTH);
							LOGGER.fine("Received LOCK values " + this.dataBuffer);
							boolean isReadOnly = this.isReadOnly(blockData);
							LOGGER.info("isReadOnly:" + isReadOnly);

							// Read Serial Number (UID)
							this.uidBuffer.setLength(0);
							readBlock(channel, 0, blockData);
							convertToHexString(this.uidBuffer, blockData, 3);
							readBlock(channel, 1, blockData);
							convertToHexString(this.uidBuffer, blockData, BLOCK_LENGTH);
							String uid = this.uidBuffer.toString();
							LOGGER.info("Tag has UID = " + uid);

							boolean uidOK = true;
							if (currentUID != null) {
								while (currentUID.length() < 14) {
									currentUID = "0" + currentUID;
								}
								currentUID = currentUID.toLowerCase();
								if (!currentUID.equalsIgnoreCase(uid)) {
									uidOK = false;
								}
							} else {
								LOGGER.info("No current UID.  New UID=" + uid);
							}

							// Read the existing data (why?? just for logs?)
							for (int i = 4; i < 16; i++) {
								readBlock(channel, i, blockData);
								this.dataBuffer.setLength(0);
								convertToHexString(this.dataBuffer, blockData, BLOCK_LENGTH);
								LOGGER.fine(i + ":" + this.dataBuffer);
							}

							if (!uidOK) {
								result.message = "Badge already activated with a different UID and read-only.  Current UID="
										+ currentUID + " New UID=" + uid;
							} else if (isReadOnly) {
								if (isUnformatted) {
									result.message = "Badge is unformatted and read-only";
								} else if (currentUID == null) {
									result.message = "Badge cannot be programmed because it is read-only";
								} else if (uidOK) {
									result.uid = uid;
									result.message = "Badge already programmed and read-only";
								}
							} else { // Program the tag
								// Format the tag as NDEF
								if (isUnformatted) {
									LOGGER.info("Card is unformatted. Formatting for Mifare Ultralight NDEF");
									writeBlock(channel, BLOCKNUM_OTP, OTP);
								} 

								// Write the static NDEF blocks
								writeBlock(channel, 4, BLOCK4);
								writeBlock(channel, 5, BLOCK5);

								// Validate and write the hashcode portion of the NDEF url
								if ("lv5.us".length() != 6) {
									throw new RuntimeException(
											"Link Domain must be 6 characters long! LINK_DOMAIN=\"lv5.us\"");
								}
								String url = "lv5.us/u/" + hashcode;
								byte[] urlBytes = url.getBytes("US-ASCII");
								for (int pos = 1; pos < urlBytes.length; pos += 4) {
									byte[] newBlockData = Arrays.copyOfRange(urlBytes, pos, pos + 4);
									writeBlock(channel, 6 + pos / 4, newBlockData);
								}

								// Mark the tag as read-only
								readBlock(channel, BLOCKNUM_LOCK, blockData);
								blockData[2] = -1;
								blockData[3] = -1;
								writeBlock(channel, BLOCKNUM_LOCK, blockData);
								result.uid = uid;
								result.message = "Successfully Activated Badge";
							}
						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error reading card", e);
			result.message = "Exception: " + e.getMessage();
		} finally {
			// disconnect
			if (card != null) {
				try {
					card.disconnect(false);
				} catch (CardException ce) {
					LOGGER.log(Level.SEVERE, "Error disconnecting from card", ce);
				}
			}
		}

		return result;
	}

	private void readBlock(CardChannel channel, int blockNum, byte[] blockData) throws CardException {
		if (blockNum < 0 || blockNum > 15) {
			LOGGER.severe("Block number out of range.  blockNum=" + blockNum);
			throw new IllegalArgumentException("blockNum out of range");
		}
		if (blockData.length < 4) {
			LOGGER.severe("blockData must be at least 4 bytes long.  length=" + blockData.length);
			throw new IllegalArgumentException("blockData not long enough");
		}
		final int cla = 0xFF;
		final int ins = 0xB0; // Read Binary Block
		final int p1 = 0x00;
		final int p2 = blockNum;
		final int ne = 4; // Expected response length
		CommandAPDU readBlock = new CommandAPDU(cla, ins, p1, p2, ne);
		ResponseAPDU r = channel.transmit(readBlock);
		byte[] receiveBuffer = r.getBytes();
		for (int i = 0; i < 4; i++) {
			blockData[i] = receiveBuffer[i];
		}
	}

	private void writeBlock(CardChannel channel, int blockNum, byte[] blockData) throws CardException {
		if (blockNum < 0 || blockNum > 15) {
			LOGGER.severe("Block number out of range.  blockNum=" + blockNum);
			throw new IllegalArgumentException("blockNum out of range");
		}
		if (blockData.length < 4) {
			LOGGER.severe("blockData must be at least 4 bytes long.  length=" + blockData.length);
			throw new IllegalArgumentException("blockData not long enough");
		}
		StringBuffer dataBuffer = new StringBuffer();
		convertToHexString(dataBuffer, blockData, 4);
		LOGGER.info("UPDATE: " + blockNum + ":" + dataBuffer);
		final int cla = 0xFF;
		final int ins = 0xD6; // Update Block
		final int p1 = 0x00;
		final int p2 = blockNum;
		final byte[] data = blockData;
		final CommandAPDU updateBlock = new CommandAPDU(cla, ins, p1, p2, data);
		ResponseAPDU r = channel.transmit(updateBlock);
		byte[] receiveBuffer = r.getBytes();
		dataBuffer.setLength(0);
		convertToHexString(dataBuffer, receiveBuffer, receiveBuffer.length);
		LOGGER.info("UPDATE RESPONSE: " + dataBuffer);
		if (receiveBuffer[0] != 0x90 && receiveBuffer[1] != 0x00) {
			throw new RuntimeException("NFC UPDATE Failed response=" + dataBuffer);
		}
	}

	private void convertToHexString(StringBuffer buffer, byte[] data, int length) {
		for (int i = 0; i < length; i++) {
			if ((data[i] & 0xFF) < 16) {
				buffer.append("0");
			}
			buffer.append(Integer.toHexString(data[i] & 0xFF));
		}
	}

	private boolean isReadOnly(byte[] lockData) {
		return (blockData[2] != 0 || blockData[3] != 0);
	}

	private boolean isUnformatted(byte[] otpData) {
		return (otpData[0] == 0 && otpData[1] == 0 && otpData[2] == 0 && otpData[3] == 0);
	}

	private boolean isWrongFormat(byte[] otpData) {
		return (otpData[0] != OTP[0] || otpData[1] != OTP[1] || otpData[2] != OTP[2] || otpData[3] != OTP[3]);
	}

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2 + (int) (Math.floor(bytes.length / 4) * 2)];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			int offset = (j * 2) + (int) (Math.floor(j / 4) * 2);
			hexChars[offset] = HEX_ARRAY[v >>> 4];
			hexChars[offset + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	public class ProgramTagResult {
		private String uid;
		private String message;

		private ProgramTagResult(String uid, String message) {
			this.uid = uid;
			this.message = message;
		}

		public String getUID() {
			return this.uid;
		}

		public String getMessage() {
			return this.message;
		}

		public boolean isError() {
			return this.uid == null;
		}
	}

	public interface StageListener {
		void stageChanged(STAGE oldStage, STAGE newStage);
	}

}

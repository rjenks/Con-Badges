package com.universalbits.conorganizer.badger.ui;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.universalbits.conorganizer.badger.model.BadgeInfo;

public class BadgeDialog extends JDialog {
	
	private static final long serialVersionUID = 1L;
	private static final int EMPTYBORDER_WIDTH = 3;
	
	private final BadgeInfo badgeInfo;
	private final JPanel gridPanel;
	private final HashMap<String,KeyComponents> keyMap = new HashMap<String,KeyComponents>();

	public BadgeDialog(JFrame frame, BadgeInfo badgeInfo) {
		super(frame, true);
		this.badgeInfo = badgeInfo;
		final JComponent contentPane = (JComponent)this.getContentPane();

		gridPanel = new JPanel(new GridLayout(badgeInfo.size() + 1, 3));
		final List<String> keys = new ArrayList<String>();
		keys.addAll(badgeInfo.keySet());
		Collections.sort(keys);
		for (String key : keys) {
			final JTextField keyField = new JTextField(key);
			keyField.setBorder(new EmptyBorder(EMPTYBORDER_WIDTH, EMPTYBORDER_WIDTH, EMPTYBORDER_WIDTH, EMPTYBORDER_WIDTH));
			final JTextField valueField = new JTextField(badgeInfo.get(key));
			valueField.setBorder(new EmptyBorder(EMPTYBORDER_WIDTH, EMPTYBORDER_WIDTH, EMPTYBORDER_WIDTH, EMPTYBORDER_WIDTH));
			keyMap.put(key, new KeyComponents(keyField, valueField));
			gridPanel.add(keyField);
			gridPanel.add(valueField);
			gridPanel.add(new JButton("X"));
		}
		contentPane.add(new JScrollPane(gridPanel));
		setSize(300, 500);
		setLocationRelativeTo(frame);
	}
	
	private class KeyComponents {
		private JComponent keyField;
		private JComponent valueField;
		
		public KeyComponents(JComponent keyField, JComponent valueField) {
			this.keyField = keyField;
			this.valueField = valueField;
		}
		
		public JComponent getKeyField() {
			return keyField;
		}
		
		public JComponent getValueField() {
			return valueField;
		}
	}
	

}

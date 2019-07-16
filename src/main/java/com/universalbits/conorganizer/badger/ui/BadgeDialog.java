package com.universalbits.conorganizer.badger.ui;

import com.universalbits.conorganizer.badger.model.BadgeInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BadgeDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int EMPTY_BORDER_SIZE = 3;

    public BadgeDialog(JFrame frame, BadgeInfo badgeInfo) {
        super(frame, true);
        final JComponent contentPane = (JComponent) this.getContentPane();

        JPanel gridPanel = new JPanel(new GridLayout(badgeInfo.size() + 1, 3));
        final List<String> keys = new ArrayList<>();
        keys.addAll(badgeInfo.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            final JTextField keyField = new JTextField(key);
            keyField.setBorder(new EmptyBorder(EMPTY_BORDER_SIZE, EMPTY_BORDER_SIZE, EMPTY_BORDER_SIZE, EMPTY_BORDER_SIZE));
            final JTextField valueField = new JTextField(badgeInfo.get(key));
            valueField.setBorder(new EmptyBorder(EMPTY_BORDER_SIZE, EMPTY_BORDER_SIZE, EMPTY_BORDER_SIZE, EMPTY_BORDER_SIZE));
            HashMap<String, KeyComponents> keyMap = new HashMap<>();
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

        @SuppressWarnings("unused")
		public JComponent getKeyField() {
            return keyField;
        }

        @SuppressWarnings("unused")
		public JComponent getValueField() {
            return valueField;
        }
    }


}

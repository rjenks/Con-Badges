package com.universalbits.conorganizer.badger.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.universalbits.conorganizer.badger.control.BadgePrinter;
import com.universalbits.conorganizer.badger.model.BadgeInfo;
import com.universalbits.conorganizer.common.ISettings;
import com.universalbits.conorganizer.common.Settings;

public class NewBadgeDialog extends JDialog {
	private static final Logger LOGGER = Logger.getLogger(NewBadgeDialog.class.getSimpleName());
    private static final long serialVersionUID = 1L;
    private static final int EMPTY_BORDER_SIZE = 4;
    private static final String SETTING_NEXT_BADGE_ID = "NEXT_BADGE_ID";

    public NewBadgeDialog(final BadgePrinterUI badgePrinterUI) {
        super(badgePrinterUI.getJFrame(), true);
        final HashMap<String, KeyComponents> fieldMap = new HashMap<>();
        final ISettings settings = Settings.getInstance();
        final JComponent contentPane = (JComponent) this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        final List<String> fieldNames = new ArrayList<>();
        fieldNames.add(BadgeInfo.ID_BADGE);
        fieldNames.add(BadgeInfo.TYPE);
        final String fieldsSetting = settings.getProperty(BadgePrinter.PROPERTY_FIELDS);
        if (fieldsSetting != null) {
            Collections.addAll(fieldNames, fieldsSetting.split("\\s*,\\s*"));
        }
        final GridLayout gridLayout = new GridLayout(0, 1);
        gridLayout.setHgap(4);
        gridLayout.setVgap(4);
        final JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(EMPTY_BORDER_SIZE, EMPTY_BORDER_SIZE, EMPTY_BORDER_SIZE, EMPTY_BORDER_SIZE));
        gridPanel.setBorder(null);
        for (String name : fieldNames) {
            final JLabel labelField = new JLabel(name);
            JComponent valueField = null;
            if (BadgeInfo.TYPE.equals(name)) {
                valueField = new JComboBox<String>(badgePrinterUI.getTypes());
            } else if (BadgeInfo.ID_BADGE.equals(name)) {
                String nextBadgeId = Settings.getInstance().getProperty(SETTING_NEXT_BADGE_ID, "1");
                valueField = new JTextField(nextBadgeId, 30);
            } else {
                valueField = new JTextField("", 30);
            }
            fieldMap.put(name, new KeyComponents(name, labelField, valueField));
            gridPanel.add(labelField);
            gridPanel.add(valueField);
        }
        contentPane.add(new JScrollPane(gridPanel), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        JButton okButton = new JButton(new AbstractAction("Print") {
			private static final long serialVersionUID = 1L;

			@Override
            public void actionPerformed(ActionEvent e) {
                final BadgeInfo badgeInfo = new BadgeInfo();
                String nextBadgeId = "1";
                for (String name : fieldNames) {
                    final KeyComponents fieldData = fieldMap.get(name);
                    if (BadgeInfo.TYPE.equals(name)) {
                        fieldData.requestFocus();
                    }
                    final String value = fieldData.getValue();
                    fieldData.reset();
                    if (BadgeInfo.ID_BADGE.equals(name)) {
                        nextBadgeId = fieldData.getValue();
                    }
                    badgeInfo.put(name, value);
                }
                badgePrinterUI.getBadgeQueue().queueBadge(badgeInfo);
                Settings.getInstance().setProperty(SETTING_NEXT_BADGE_ID, nextBadgeId);
                new Toast(badgeInfo.toString(), 2000);
            }
        });
        getRootPane().setDefaultButton(okButton);
        JButton cancelButton = new JButton(new AbstractAction("Close") {
        	private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        final ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
                dispose();
            }
        };
        final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        //setSize(300, 500);
        setLocationRelativeTo(badgePrinterUI.getJFrame());
        setVisible(true);
    }

    private class KeyComponents {
        private String name;
        private JLabel labelField;
        private JComponent valueField;

        public KeyComponents(String name, JLabel labelField, JComponent valueField) {
            this.name = name;
            this.labelField = labelField;
            this.valueField = valueField;
        	LOGGER.finest("labelField:" + this.labelField + " valueField:" + this.valueField);
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            String value = null;
            if (valueField instanceof JTextField) {
                JTextField textField = (JTextField) valueField;
                value = textField.getText();
            } else if (valueField instanceof JComboBox) {
                @SuppressWarnings("unchecked")
				JComboBox<String> comboBox = (JComboBox<String>)valueField;
                value = "" + comboBox.getSelectedItem();
            }
            return value;
        }

        public void reset() {
            if (BadgeInfo.ID_BADGE.equals(getName())) {
                JTextField valueTextField = (JTextField)valueField;
                try {
                    valueTextField.setText("" + (Integer.parseInt(getValue()) + 1));
                } catch (NumberFormatException nfe) {
                    valueTextField.setText("");
                }
            } else if (BadgeInfo.TYPE.equals(name)) {
                // do nothing
            } else {
                if (valueField instanceof JTextField) {
                    JTextField valueTextField = (JTextField)valueField;
                    valueTextField.setText("");
                }
            }
        }

        public void requestFocus() {
            valueField.requestFocus();
        }

    }


}

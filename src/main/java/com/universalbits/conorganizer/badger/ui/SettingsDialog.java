package com.universalbits.conorganizer.badger.ui;

import com.universalbits.conorganizer.badger.control.BadgePrinter;
import com.universalbits.conorganizer.badger.control.ServerBadgeLoader;
import com.universalbits.conorganizer.common.APIClient;
import com.universalbits.conorganizer.common.ISettings;
import com.universalbits.conorganizer.common.Settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;

/**
 * Created by rjenks on 9/1/2014.
 */
public class SettingsDialog extends JDialog {

    private JFrame frame;
    private BadgePrinterUI badgePrinterUI;
    private ISettings settings;

    private static final String WEST = SpringLayout.WEST;
    private static final String EAST = SpringLayout.EAST;
    private static final String NORTH = SpringLayout.NORTH;
    private static final String SOUTH = SpringLayout.SOUTH;
    private static final int GAP = 5;
    private static final int COLS = 2;

    public SettingsDialog(final BadgePrinterUI badgePrinterUI, final ISettings settings) {
        super(badgePrinterUI.getJFrame(), "Settings", true);
        this.frame = badgePrinterUI.getJFrame();
        this.badgePrinterUI = badgePrinterUI;
        this.settings = settings;

        JPanel contentPane = (JPanel)this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        int numFields = 0;
        final JLabel clientNameLabel = new JLabel("Client Name:");
        final JTextField clientNameField = new JTextField(settings.getProperty(APIClient.PROPERTY_NAME));
        numFields++;
        final JLabel hostLabel = new JLabel("Host URL:");
        final JTextField hostField = new JTextField(settings.getProperty(APIClient.PROPERTY_URL_PREFIX));
        numFields++;
        final JLabel tokenLabel = new JLabel("Token:");
        final JTextField tokenField = new JPasswordField(settings.getProperty(APIClient.PROPERTY_TOKEN));
        numFields++;
        final JLabel widthLabel = new JLabel("Page Width:");
        final JFormattedTextField widthField = new JFormattedTextField(NumberFormat.getNumberInstance());
        widthField.setValue(settings.getPropertyDouble(BadgePrinter.PROPERTY_PAGE_WIDTH, BadgePrinter.DEFAULT_PAGE_WIDTH));
        numFields++;
        final JLabel heightLabel = new JLabel("Page Height:");
        final JFormattedTextField heightField = new JFormattedTextField(NumberFormat.getNumberInstance());
        heightField.setValue(settings.getPropertyDouble(BadgePrinter.PROPERTY_PAGE_HEIGHT, BadgePrinter.DEFAULT_PAGE_HEIGHT));
        numFields++;

        final JLabel xScaleLabel = new JLabel("X Scale:");
        final JFormattedTextField xScaleField = new JFormattedTextField(NumberFormat.getNumberInstance());
        xScaleField.setValue(settings.getPropertyDouble(BadgePrinter.PROPERTY_X_SCALE, BadgePrinter.DEFAULT_X_SCALE));
        numFields++;
        final JLabel yScaleLabel = new JLabel("Y Scale:");
        final JFormattedTextField yScaleField = new JFormattedTextField(NumberFormat.getNumberInstance());
        yScaleField.setValue(settings.getPropertyDouble(BadgePrinter.PROPERTY_Y_SCALE, BadgePrinter.DEFAULT_Y_SCALE));
        numFields++;
        final JLabel xTranslateLabel = new JLabel("X Translate:");
        final JFormattedTextField xTranslateField = new JFormattedTextField(NumberFormat.getNumberInstance());
        xTranslateField.setValue(settings.getPropertyDouble(BadgePrinter.PROPERTY_X_TRANSLATE, BadgePrinter.DEFAULT_X_TRANSLATE));
        numFields++;
        final JLabel yTranslateLabel = new JLabel("Y Translate:");
        final JFormattedTextField yTranslateField = new JFormattedTextField(NumberFormat.getNumberInstance());
        yTranslateField.setValue(settings.getPropertyDouble(BadgePrinter.PROPERTY_Y_TRANSLATE, BadgePrinter.DEFAULT_Y_TRANSLATE));
        numFields++;
        final JLabel fieldsLabel = new JLabel("Fields");
        final JTextField fieldsField = new JTextField(settings.getProperty(BadgePrinter.PROPERTY_FIELDS));
        numFields++;

        SpringLayout springLayout = new SpringLayout();
        JPanel formPanel = new JPanel(springLayout);
        formPanel.add(clientNameLabel);
        formPanel.add(clientNameField);
        formPanel.add(hostLabel);
        formPanel.add(hostField);
        formPanel.add(tokenLabel);
        formPanel.add(tokenField);
        formPanel.add(widthLabel);
        formPanel.add(widthField);
        formPanel.add(heightLabel);
        formPanel.add(heightField);
        formPanel.add(xScaleLabel);
        formPanel.add(xScaleField);
        formPanel.add(yScaleLabel);
        formPanel.add(yScaleField);
        formPanel.add(xTranslateLabel);
        formPanel.add(xTranslateField);
        formPanel.add(yTranslateLabel);
        formPanel.add(yTranslateField);
        formPanel.add(fieldsLabel);
        formPanel.add(fieldsField);

        SpringUtilities.makeCompactGrid(formPanel, numFields, COLS, GAP, GAP, GAP, GAP);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        JButton okButton = new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                settings.setProperty(APIClient.PROPERTY_NAME, clientNameField.getText());
                settings.setProperty(APIClient.PROPERTY_URL_PREFIX, hostField.getText());
                settings.setProperty(APIClient.PROPERTY_TOKEN, tokenField.getText());
                settings.setProperty(BadgePrinter.PROPERTY_PAGE_WIDTH, widthField.getText());
                settings.setProperty(BadgePrinter.PROPERTY_PAGE_HEIGHT, heightField.getText());
                settings.setProperty(BadgePrinter.PROPERTY_X_SCALE, xScaleField.getText());
                settings.setProperty(BadgePrinter.PROPERTY_Y_SCALE, yScaleField.getText());
                settings.setProperty(BadgePrinter.PROPERTY_X_TRANSLATE, xTranslateField.getText());
                settings.setProperty(BadgePrinter.PROPERTY_Y_TRANSLATE, yTranslateField.getText());
                settings.setProperty(BadgePrinter.PROPERTY_FIELDS, fieldsField.getText());
                setVisible(false);
                dispose();
            }
        });
        JButton cancelButton = new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        contentPane.add(formPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(frame);
        setVisible(true);
    }


}

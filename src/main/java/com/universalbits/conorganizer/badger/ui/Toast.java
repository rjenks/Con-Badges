package com.universalbits.conorganizer.badger.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class Toast extends JDialog {

    int delay;
    public Toast(String message, int delay) {
        this.delay = delay;
        setSize(300, 75);
        setUndecorated(true);
        getContentPane().setLayout(new BorderLayout(0, 0));
        JComponent contentPane = (JComponent)getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBackground(Color.WHITE);
        CompoundBorder border = new CompoundBorder(new LineBorder(Color.LIGHT_GRAY, 2), new EmptyBorder(10, 10, 10, 10));
        contentPane.setBorder(border);

        final JLabel toastLabel = new JLabel(message);
        toastLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        toastLabel.setForeground(Color.BLACK);
        contentPane.add(toastLabel, BorderLayout.CENTER);

        setAlwaysOnTop(true);
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(dim.width - getSize().width, dim.height - getSize().height);
        setVisible(true);

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(Toast.this.delay);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            Toast.this.dispose();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}

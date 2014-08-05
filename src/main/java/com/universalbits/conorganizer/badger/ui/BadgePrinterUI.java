package com.universalbits.conorganizer.badger.ui;

import com.universalbits.conorganizer.badger.control.BadgePrinter;
import com.universalbits.conorganizer.badger.model.BadgeInfo;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class BadgePrinterUI {

    private JFrame frame;
    private JTabbedPane tabbedPane;
    private JList<BadgeInfo> pendingList;
    private BadgeListModel pendingListModel;
    private JList<BadgeInfo> problemList;
    private BadgeListModel problemListModel;
    private JList<BadgeInfo> historyList;
    private BadgeListModel historyListModel;
    private QuitAction quitAction;
    private LoadCSVAction loadCSVAction;
    private ServerConnectAction serverConnectAction;
    private StartPrintingAction startPrintingAction;
    private JToolBar toolBar;
    private JToolBar statusBar;
    private JRadioButtonMenuItem printOption;
    private JRadioButtonMenuItem pngOption;

    public BadgePrinterUI() {
        SwingUtilities.invokeLater(new InitRunner());
    }

    public JFrame getJFrame() {
        return frame;
    }

    public boolean isPrintMode() {
        return printOption.isSelected();
    }

    private class InitRunner implements Runnable {
        public void run() {
            try {
                for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                // If Nimbus is not available, you can set the GUI to another look and feel.
            }
            pendingListModel = new BadgeListModel();
            problemListModel = new BadgeListModel();
            historyListModel = new BadgeListModel();
            final UIBadgeSource uiBadgeSource = new UIBadgeSource(pendingListModel, problemListModel, historyListModel);
            final UIBadgeQueue uiBadgeQueue = new UIBadgeQueue(pendingListModel);

            frame = new JFrame(BadgePrinter.APP_NAME);

            startPrintingAction = new StartPrintingAction(BadgePrinterUI.this, uiBadgeSource);
            loadCSVAction = new LoadCSVAction(frame, uiBadgeQueue);
            serverConnectAction = new ServerConnectAction(frame, uiBadgeQueue);
            quitAction = new QuitAction();

            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    quitAction.actionPerformed(null);
                }
            });
            final JComponent contentPane = (JComponent) frame.getRootPane().getContentPane();
            tabbedPane = new JTabbedPane();

            pendingList = new JList<>(pendingListModel);
            pendingList.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        final BadgeInfo badgeInfo = pendingList.getSelectedValue();
                        if (badgeInfo != null) {
                            new BadgeDialog(frame, badgeInfo).setVisible(true);
                        }
                    }
                }
            });
            final JScrollPane pendingScroll = new JScrollPane(pendingList);
            problemList = new JList<>(problemListModel);
            final JScrollPane problemScroll = new JScrollPane(problemList);
            historyList = new JList<>(historyListModel);
            final JScrollPane historyScroll = new JScrollPane(historyList);
            final JMenuBar menuBar = new JMenuBar();
            final JMenu fileMenu = new JMenu("File");
            fileMenu.add(loadCSVAction);
            fileMenu.add(serverConnectAction);
            fileMenu.addSeparator();
            fileMenu.add(startPrintingAction);
            fileMenu.addSeparator();
            fileMenu.add(quitAction);
            menuBar.add(fileMenu);

            final ButtonGroup group = new ButtonGroup();
            printOption = new JRadioButtonMenuItem("Print Badges");
            pngOption = new JRadioButtonMenuItem("Generate PNG Files");
            group.add(printOption);
            group.add(pngOption);
            printOption.setSelected(true);
            final JMenu optionsMenu = new JMenu("Options");
            optionsMenu.add(printOption);
            optionsMenu.add(pngOption);
            menuBar.add(optionsMenu);

            toolBar = new JToolBar();
            statusBar = new JToolBar();

            frame.setJMenuBar(menuBar);
            contentPane.setLayout(new BorderLayout());
            tabbedPane.addTab("Pending", pendingScroll);
            pendingListModel.addListDataListener(new TabTitleUpdateListener("Pending", 0, pendingListModel));
            tabbedPane.addTab("Problems", problemScroll);
            problemListModel.addListDataListener(new TabTitleUpdateListener("Problems", 1, problemListModel));
            tabbedPane.addTab("History", historyScroll);
            historyListModel.addListDataListener(new TabTitleUpdateListener("History", 2, historyListModel));
            contentPane.add(toolBar, BorderLayout.NORTH);
            contentPane.add(tabbedPane, BorderLayout.CENTER);
            contentPane.add(statusBar, BorderLayout.SOUTH);
            frame.setSize(600, 400);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
    }

    private class TabTitleUpdateListener implements ListDataListener {
        private final String title;
        private final int index;
        private final BadgeListModel listModel;

        public TabTitleUpdateListener(String title, int index, BadgeListModel listModel) {
            this.title = title;
            this.index = index;
            this.listModel = listModel;
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            update();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            update();
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            update();
        }

        private void update() {
            final int size = listModel.size();
            final String totalWithCount = size > 0 ? title + " (" + size + ")" : title;
            tabbedPane.setTitleAt(index, totalWithCount);
        }
    }

}

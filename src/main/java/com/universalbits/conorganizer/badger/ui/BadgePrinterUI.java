package com.universalbits.conorganizer.badger.ui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.universalbits.conorganizer.badger.model.BadgeInfo;

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
			
			startPrintingAction = new StartPrintingAction(BadgePrinterUI.this, uiBadgeSource);
			loadCSVAction = new LoadCSVAction(frame, uiBadgeQueue);
			quitAction = new QuitAction();

			frame = new JFrame("Badge Printer");
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent event) {
					quitAction.actionPerformed(null);
				}
			});
			final JComponent contentPane = (JComponent)frame.getRootPane().getContentPane();
			tabbedPane = new JTabbedPane();

			pendingList = new JList<BadgeInfo>(pendingListModel);
			final JScrollPane pendingScroll = new JScrollPane(pendingList);
			problemList = new JList<BadgeInfo>(problemListModel);
			final JScrollPane problemScroll = new JScrollPane(problemList);
			historyList = new JList<BadgeInfo>(historyListModel);
			final JScrollPane historyScroll = new JScrollPane(historyList);
			final JMenuBar menuBar = new JMenuBar();
			final JMenu fileMenu = new JMenu("File");
			fileMenu.add(loadCSVAction);
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
			tabbedPane.addTab("Problems", problemScroll);
			tabbedPane.addTab("History", historyScroll);
			contentPane.add(toolBar, BorderLayout.NORTH);
			contentPane.add(tabbedPane, BorderLayout.CENTER);
			contentPane.add(statusBar, BorderLayout.SOUTH);
			frame.setSize(600, 400);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		}
	}

}

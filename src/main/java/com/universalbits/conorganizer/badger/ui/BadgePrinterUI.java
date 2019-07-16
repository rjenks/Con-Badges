package com.universalbits.conorganizer.badger.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.batik.swing.JSVGCanvas;
import org.w3c.dom.svg.SVGDocument;

import com.universalbits.conorganizer.badger.control.BadgeActivator;
import com.universalbits.conorganizer.badger.control.BadgePrinter;
import com.universalbits.conorganizer.badger.control.BadgeTypeMonitor;
import com.universalbits.conorganizer.badger.control.ServerBadgeLoader;
import com.universalbits.conorganizer.badger.model.BadgeInfo;
import com.universalbits.conorganizer.common.ISettings;
import com.universalbits.conorganizer.common.Settings;

public class BadgePrinterUI {
	public static final String APP_NAME = "Badger";
	private Logger LOGGER = Logger.getLogger(BadgePrinterUI.class.getSimpleName());
	private JFrame frame;
	private JTabbedPane tabbedPane;
	private JList<BadgeInfo> typesList;
	private BadgeListModel typesListModel;
	private JList<BadgeInfo> pendingList;
	private BadgeListModel pendingListModel;
	private JList<BadgeInfo> problemList;
	private BadgeListModel problemListModel;
	private JList<BadgeInfo> historyList;
	private BadgeListModel historyListModel;
	private JSVGCanvas previewPane;
	private QuitAction quitAction;
	private SettingsAction settingsAction;
	private LoadCSVAction loadCSVAction;
	private ServerConnectAction serverConnectAction;
	private StartPrintingAction startPrintingAction;
	private NewBadgeAction newBadgeAction;
	private JToolBar toolBar;
	private JToolBar statusBar;
	private JLabel statusText;
	private JRadioButtonMenuItem printOption;
	private JRadioButtonMenuItem pngOption;
	private BadgePrinter badgePrinter;
	private BadgeActivator badgeActivator;
	private ISettings settings;
	private UIBadgeQueue uiBadgeQueue;
	private BadgeActivatorPanel badgeActivatorPanel;

	public BadgePrinterUI() {
		Settings.init(APP_NAME);
		settings = Settings.getInstance();
		SwingUtilities.invokeLater(new InitRunner());
	}

	public JFrame getJFrame() {
		return frame;
	}

	public boolean isPrintMode() {
		return printOption.isSelected();
	}

	public UIBadgeQueue getBadgeQueue() {
		return uiBadgeQueue;
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

			badgePrinter = new BadgePrinter(settings);
			badgeActivator = new BadgeActivator();

			typesListModel = new BadgeListModel();
			pendingListModel = new BadgeListModel();
			problemListModel = new BadgeListModel();
			historyListModel = new BadgeListModel();
			final UIBadgeSource uiBadgeSource = new UIBadgeSource(pendingListModel, problemListModel, historyListModel);
			uiBadgeQueue = new UIBadgeQueue(pendingListModel);

			frame = new JFrame(APP_NAME);
			frame.setMinimumSize(new Dimension(600, 300));

			newBadgeAction = new NewBadgeAction(BadgePrinterUI.this);
			settingsAction = new SettingsAction(BadgePrinterUI.this, settings);
			startPrintingAction = new StartPrintingAction(BadgePrinterUI.this, uiBadgeSource);
			loadCSVAction = new LoadCSVAction(frame, uiBadgeQueue);
			serverConnectAction = new ServerConnectAction(frame, settings, uiBadgeQueue);
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

			typesList = new JList<>(typesListModel);
			final JScrollPane typesScroll = new JScrollPane(typesList);
			@SuppressWarnings("unused")
			BadgeTypeMonitor typesMonitor = new BadgeTypeMonitor(typesListModel);
			typesList.addListSelectionListener(new PreviewListSelectionListener(typesList));

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
			historyList.addListSelectionListener(new PreviewListSelectionListener(historyList));
			final JScrollPane historyScroll = new JScrollPane(historyList);
			final JMenuBar menuBar = new JMenuBar();
			final JMenu fileMenu = new JMenu("File");
			fileMenu.add(newBadgeAction);
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
			optionsMenu.add(settingsAction);
			optionsMenu.addSeparator();
			optionsMenu.add(printOption);
			optionsMenu.add(pngOption);
			menuBar.add(optionsMenu);

			toolBar = new JToolBar();
			toolBar.setFloatable(false);

			statusBar = new JToolBar();
			statusText = new JLabel("Idle");
			statusText.setBorder(new EmptyBorder(4, 10, 4, 10));
			statusBar.add(statusText);
			statusBar.setFloatable(false);

			frame.setJMenuBar(menuBar);
			contentPane.setLayout(new BorderLayout());
			tabbedPane.setMinimumSize(new Dimension(300, 50));
			tabbedPane.addTab("Types", typesScroll);
			tabbedPane.addTab("Pending", pendingScroll);
			pendingListModel.addListDataListener(new TabTitleUpdateListener("Pending", 1, pendingListModel));
			tabbedPane.addTab("Problems", problemScroll);
			problemListModel.addListDataListener(new TabTitleUpdateListener("Problems", 2, problemListModel));
			tabbedPane.addTab("History", historyScroll);
			historyListModel.addListDataListener(new TabTitleUpdateListener("History", 3, historyListModel));

			previewPane = new JSVGCanvas();

			badgeActivatorPanel = new BadgeActivatorPanel(badgeActivator);
			badgeActivatorPanel.addKeyListener(new BarcodeKeyListener(badgeActivator));

			final JSplitPane leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, badgeActivatorPanel);

			final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, previewPane);

			contentPane.add(toolBar, BorderLayout.NORTH);
			contentPane.add(splitPane, BorderLayout.CENTER);
			contentPane.add(statusBar, BorderLayout.SOUTH);

			frame.setSize(600, 400);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);

		}
	}

	public ServerBadgeLoader getServerBadgeLoader() {
		return serverConnectAction.getServerBadgeLoader();
	}

	public BadgePrinter getBadgePrinter() {
		return badgePrinter;
	}

	public String[] getTypes() {
		final List<String> types = new ArrayList<String>();
		final Enumeration<BadgeInfo> e = typesListModel.elements();
		while (e.hasMoreElements()) {
			final BadgeInfo badgeInfo = e.nextElement();
			types.add(badgeInfo.get(BadgeInfo.TYPE));
		}
		return types.toArray(new String[types.size()]);
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

	private class PreviewListSelectionListener implements ListSelectionListener {
		private JList<BadgeInfo> list;

		private PreviewListSelectionListener(JList<BadgeInfo> list) {
			this.list = list;
		}

		@Override
		public void valueChanged(ListSelectionEvent event) {
			final BadgeInfo badgeInfo = list.getSelectedValue();
			try {
				final SVGDocument doc = badgePrinter.generateBadge(badgeInfo);
				previewPane.setDocument(doc);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error generating preview", e);
			}
		}
	}

}

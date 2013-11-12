package com.universalbits.conorganizer.badger.ui;

import javax.swing.SwingUtilities;

import com.universalbits.conorganizer.badger.control.BadgeQueue;
import com.universalbits.conorganizer.badger.model.BadgeInfo;

class UIBadgeQueue implements BadgeQueue {

	private final BadgeListModel pendingListModel;

	/**
	 * @param badgePrinterUI
	 */
	UIBadgeQueue(BadgeListModel pendingListModel) {
		this.pendingListModel = pendingListModel;
	}

	@Override
	public void queueBadge(final BadgeInfo badgeInfo) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				pendingListModel.addElement(badgeInfo);
			}
		});
	}
	
}
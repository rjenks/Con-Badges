package com.universalbits.conorganizer.badger.ui;

import com.universalbits.conorganizer.badger.control.BadgeQueue;
import com.universalbits.conorganizer.badger.model.BadgeInfo;

import javax.swing.*;

class UIBadgeQueue implements BadgeQueue {

    private final BadgeListModel pendingListModel;

    /**
     * @param pendingListModel Pending Badge Model
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
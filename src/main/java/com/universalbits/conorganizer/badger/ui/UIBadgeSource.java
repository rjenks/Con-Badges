package com.universalbits.conorganizer.badger.ui;

import com.universalbits.conorganizer.badger.control.BadgeSource;
import com.universalbits.conorganizer.badger.model.BadgeInfo;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

class UIBadgeSource implements BadgeSource {

    private final BadgeListModel pendingListModel;
    private final BadgeListModel historyListModel;
    private final BadgeListModel problemListModel;

    /**
     * @param pendingListModel Pending badges
     * @param problemListModel Problem badges
     * @param historyListModel History badges
     */
    UIBadgeSource(BadgeListModel pendingListModel, BadgeListModel problemListModel, BadgeListModel historyListModel) {
        this.pendingListModel = pendingListModel;
        this.problemListModel = problemListModel;
        this.historyListModel = historyListModel;
    }

    @Override
    public BadgeInfo getBadgeToPrint() {
        final BadgeInfo badgeInfo[] = new BadgeInfo[1];
        while (badgeInfo[0] == null) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        if (pendingListModel.getSize() > 0) {
                            badgeInfo[0] = pendingListModel.remove(0);
                        }
                    }
                });
                if (badgeInfo[0] == null) {
                    Thread.sleep(100);
                }
            } catch (InvocationTargetException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return badgeInfo[0];
    }

    @Override
    public void reportProblem(final BadgeInfo badgeInfo) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                problemListModel.addElement(badgeInfo);
            }
        });
    }

    @Override
    public void reportDone(final BadgeInfo badgeInfo) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                historyListModel.addElement(badgeInfo);
            }
        });
    }

}
package com.universalbits.conorganizer.badger.control;

import com.universalbits.conorganizer.badger.model.BadgeInfo;

public interface BadgeStatusListener {
	
	void notifyBadgeStatus(BadgeInfo badgeInfo, String status);

}

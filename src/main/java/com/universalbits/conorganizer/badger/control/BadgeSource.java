package com.universalbits.conorganizer.badger.control;

import com.universalbits.conorganizer.badger.model.BadgeInfo;

public interface BadgeSource {

	BadgeInfo getBadgeToPrint();
	
	void reportProblem(BadgeInfo badgeInfo);
	
	void reportDone(BadgeInfo badgeInfo);
}

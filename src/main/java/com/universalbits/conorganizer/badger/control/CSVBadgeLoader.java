package com.universalbits.conorganizer.badger.control;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import com.universalbits.conorganizer.badger.model.BadgeInfo;

import au.com.bytecode.opencsv.CSVReader;

public class CSVBadgeLoader implements Runnable {
	
	private final CSVReader in;
	private final BadgeQueue queue;
	
	public CSVBadgeLoader(InputStream in, BadgeQueue queue) {
		this.in = new CSVReader(new InputStreamReader(in));
		this.queue = queue;
	}

	@Override
	public void run() {
		int lineNum = 1;
		String line[];
		String mapping[];
		Map<String, String> fields = new HashMap<String, String>();
		try {
			line = in.readNext();
			mapping = new String[line.length];
			fields.clear();
			for (int i = 0; i < line.length; i++) {
				mapping[i] = line[i];
			}
			while ((line = in.readNext()) != null) {		
				for (int i = 0; i < Math.min(mapping.length, line.length); i++) {
					fields.put(mapping[i], line[i].trim());
				}
				queue.queueBadge(new BadgeInfo(fields));
				lineNum++;
			}
		} catch (IOException ioe) {
			System.err.println("Error reading CSV on line " + lineNum);
			ioe.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
			FileInputStream in = new FileInputStream("C:/Users/rjenks/Desktop/WhoFestDFW-2013-Sample.txt");
			BadgeQueue queue = new BadgeQueue() {

				@Override
				public void queueBadge(BadgeInfo badgeInfo) {
					System.out.println(badgeInfo);
				}
				
			};
			CSVBadgeLoader loader = new CSVBadgeLoader(in, queue);
			new Thread(loader).start();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

}

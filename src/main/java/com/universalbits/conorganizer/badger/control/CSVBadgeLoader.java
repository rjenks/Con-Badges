package com.universalbits.conorganizer.badger.control;

import au.com.bytecode.opencsv.CSVReader;
import com.universalbits.conorganizer.badger.model.BadgeInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

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
        Map<String, String> fields = new HashMap<>();
        try {
            line = in.readNext();
            mapping = new String[line.length];
            fields.clear();
            System.arraycopy(line, 0, mapping, 0, line.length);
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

}

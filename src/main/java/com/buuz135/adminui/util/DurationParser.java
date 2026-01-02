package com.buuz135.adminui.util;

import java.util.HashMap;

public class DurationParser {

    public static HashMap<String, Long> DURATIONS = new HashMap<>();

    static {
        DURATIONS.put("d", 86400L);
        DURATIONS.put("h", 3600L);
        DURATIONS.put("m", 60L);
        DURATIONS.put("s", 1L);
    }

    /**
     * Returns time in milliseconds
     */
    public static long parse(String duration){
        var split = duration.split(" ");
        var time = 0;
        for (var s : split) {
            for (String string : DURATIONS.keySet()) {
                if (s.endsWith(string)){
                    time += Long.parseLong(s.replace(string, "")) * DURATIONS.get(string);
                    break;
                }
            }
        }
        return time * 1000L;
    }
}

package org.dslul.openboard.inputmethod.latin.emojisearch;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.xdrop.fuzzywuzzy.Ratio;
import me.xdrop.fuzzywuzzy.ratios.PartialRatio;
import me.xdrop.fuzzywuzzy.ratios.SimpleRatio;

public class EmojiSearch {
    private static final Ratio RATIO = new SimpleRatio();
    private static final Ratio PARTIAL_RATIO = new PartialRatio();
    private static final int CUTOFF = 60;

    private static EmojiSearch instance;

    private static Map<String, List<String>> data;

    private EmojiSearch(Context context) {
        data = loadData(context);
    }

    public static EmojiSearch getInstance() {
        return instance;
    }

    public static void init(Context context) {
        instance = new EmojiSearch(context);
    }

    public List<String> search(String query) {
        List<String> results = new ArrayList<>();
        for (Map.Entry<String, Integer> r :  extract(query.toLowerCase(), false)) {
            results.add(r.getKey());
        }
        return results;
    }

    public List<String> searchExact(String query) {
        List<String> results = new ArrayList<>();
        for (Map.Entry<String, Integer> r :  extract(query.toLowerCase(), true)) {
            results.add(r.getKey());
        }
        return results;
    }

    private static List<Map.Entry<String, Integer>> extract(String query, boolean exact) {
        Map<String, Integer> yields = new HashMap<>();
        for (Map.Entry<String, List<String>> e : data.entrySet()) {
            if (e.getValue().size() == 0) {
                continue;
            }

            int maxScore = 0;
            for (String keyword : e.getValue()) {
            int score = 0;
			if (keyword.equals(query)) {
                score = 100;
				} else if (!exact) {
                    if (keyword.startsWith(query)) {
                        score = 95;
                    } else if (keyword.contains(query)) {
                        score = 90;
                    } else {
			 // TODO: we probably want faster partial search
                        if (query.contains(" ") || keyword.contains(" ")) {
                            score = PARTIAL_RATIO.apply(query, keyword);
                        } else {
                            score = RATIO.apply(query, keyword);
                        }
                    }
                } else if (keyword.startsWith(query + "_")) {
                    // accept partial matches for subsections even in exact mode
                    score = 99;
                }
                maxScore = Math.max(score, maxScore);
                if (maxScore == 100) {
                    break;
                }
            }			

            if (maxScore >= CUTOFF) {
                yields.put(e.getKey(), maxScore);
            }
        }
        // sort
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(yields.entrySet());
        Collections.sort(entries, Collections.reverseOrder((o1, o2) -> {
            return Integer.compare(o1.getValue(), o2.getValue());
        }));

        return entries;
    }

    /**
     * Loads emoji data from data json (grabbed from https://github.com/muan/emojilib)
     */
    private static Map<String, List<String>> loadData(Context context) {
        Map<String, List<String>> map = new HashMap<>();
        try {
            InputStream jsonFile = context.getAssets().open("emoji-en-US.json");
            JsonReader reader = new JsonReader(new InputStreamReader(jsonFile));

            reader.beginObject();
            while (reader.hasNext()) {
                String emoji = reader.nextName();
                List<String> keywords = new ArrayList<>();
                reader.beginArray();
                while (reader.hasNext() && reader.peek() == JsonToken.STRING) {
                    keywords.add(reader.nextString());
                }
                reader.endArray();
                map.put(emoji, keywords);
            }
            reader.endObject();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
}
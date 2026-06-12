package com.smartticket.cache;

import org.springframework.stereotype.Component;

@Component
public class TokenEstimator {
    public int estimate(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chineseChars = 0, englishWords = 0;
        StringBuilder word = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseChars++;
                if (word.length() > 0) { englishWords++; word.setLength(0); }
            } else if (Character.isWhitespace(c)) {
                if (word.length() > 0) { englishWords++; word.setLength(0); }
            } else {
                word.append(c);
            }
        }
        if (word.length() > 0) englishWords++;
        return (int)((chineseChars * 1.5 + englishWords * 1.3) * 1.1); // 10% safety margin
    }
}

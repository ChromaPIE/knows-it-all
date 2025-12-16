package cr.chromapie.knowsitall.api;

import java.util.List;

public final class TokenEstimator {

    private TokenEstimator() {}

    public static int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int cjkChars = 0;
        int otherChars = 0;

        for (char c : text.toCharArray()) {
            if (isCJK(c)) {
                cjkChars++;
            } else {
                otherChars++;
            }
        }

        int cjkTokens = (int) Math.ceil(cjkChars / 1.5);
        int otherTokens = (int) Math.ceil(otherChars / 3.5);

        return cjkTokens + otherTokens;
    }

    public static int estimate(ChatMessage message) {
        if (message == null) {
            return 0;
        }
        return estimate(message.getContent()) + 4;
    }

    public static int estimate(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        int total = 3;
        for (ChatMessage msg : messages) {
            total += estimate(msg);
        }
        return total;
    }

    private static boolean isCJK(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            || block == Character.UnicodeBlock.HIRAGANA
            || block == Character.UnicodeBlock.KATAKANA
            || block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.HANGUL_JAMO;
    }
}

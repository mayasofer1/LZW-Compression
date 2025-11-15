package lzw;

import java.io.ByteArrayOutputStream;
import java.util.*;
import static lzw.BitManager.*;

/**
 * ImprovedLZW – Variable-width packing (no reset):
 * - אותו אלגוריתם כמו Regular (מילון גדל ללא איפוס).
 * - כותבים את הקודים ברוחב משתנה: מתחיל ב-9 ביט, גדל כשצריך עד לרוחב המינימלי
 * הדרוש.
 * - התוצאה תמיד ≤ בביטים מה-Regular שכותב ברוחב אחיד = bitsNeeded(maxCode).
 */
public class ImprovedLZW {

    private static final int START_BITS = 9; // רוחב כתיבה התחלתי
    private static final int FIRST_FREE = 256; // 0..255 תווי בסיס

    /* ================= Compress ================= */
    public static ByteArrayOutputStream Compress(String text) {
        List<Boolean> outBits = new ArrayList<>();

        // מקרה ריק
        if (text == null || text.isEmpty()) {
            writeHeader(outBits, START_BITS, 0);
            return convertBitsToBytes(outBits);
        }

        // ===== שלב 1: בדיוק כמו Regular – מפיקים את קודי ה-LZW =====
        Map<String, Integer> dict = new HashMap<>();
        for (int i = 0; i < 256; i++)
            dict.put(String.valueOf((char) i), i);
        int nextCode = FIRST_FREE;

        List<Integer> codes = new ArrayList<>(text.length() / 2 + 16);

        String w = "";
        for (char ch : text.toCharArray()) {
            String wc = w + ch;
            if (dict.containsKey(wc)) {
                w = wc;
            } else {
                codes.add(dict.get(w));
                dict.put(wc, nextCode++);
                w = String.valueOf(ch);
            }
        }
        if (!w.isEmpty())
            codes.add(dict.get(w));

        // ===== שלב 2: כותבים את הקודים ברוחב משתנה =====
        // כותרת: שומרים את הרוחב ההתחלתי (9) + מונה קודים (כמו אצלך)
        writeHeader(outBits, START_BITS, codes.size());

        int writeBits = START_BITS;
        int growAt = (1 << writeBits); // מתי נעלה רוחב
        int simulatedNext = FIRST_FREE;

        for (int code : codes) {
            // כותבים את הקוד ברוחב הנוכחי
            writeFixedBits(outBits, code, writeBits);

            // כמו באלגוריתם: בכל פליטה (מלבד הראשונה) נוצר ערך מילון חדש.
            // הסימולציה הזו תואמת בדיוק את גדילת המילון בלי צורך לדעת את המחרוזת.
            simulatedNext++;
            if (simulatedNext == growAt) {
                writeBits++;
                growAt = (1 << writeBits);
            }
        }

        System.out.println(
                "Improved LZW: startWidth=" + START_BITS +
                        ", codes=" + codes.size() +
                        ", finalWidth=" + writeBits +
                        ", totalBits=" + outBits.size());

        return convertBitsToBytes(outBits);
    }

    /* ================= Decompress ================= */
    public static String Decompress(ByteArrayOutputStream compressed) {
        List<Boolean> bits = byteStreamToBitStream(compressed);
        if (bits.size() < 36)
            return "";

        int[] hdr = readHeader(bits);
        int readBits = hdr[0]; // מצופה 9
        int codeCount = hdr[1];
        if (readBits <= 0 || codeCount <= 0)
            return "";

        // קוראים את הקודים אחד-אחד ברוחב משתנה (מסונכרן עם ההצפנה)
        int cursor = 36;
        int growAt = (1 << readBits);
        int simulatedNext = FIRST_FREE;

        List<Integer> codes = new ArrayList<>(codeCount);
        for (int i = 0; i < codeCount; i++) {
            if (cursor + readBits > bits.size())
                return "";
            int code = readFixedBits(bits, cursor, readBits);
            cursor += readBits;
            codes.add(code);

            simulatedNext++;
            if (simulatedNext == growAt) {
                readBits++;
                growAt = (1 << readBits);
            }
        }

        // דקומפרסיה רגילה לפי הקודים (כמו Regular)
        Map<Integer, String> dict = new HashMap<>();
        for (int i = 0; i < 256; i++)
            dict.put(i, String.valueOf((char) i));
        int nextCode = FIRST_FREE;

        Iterator<Integer> it = codes.iterator();
        String w = dict.get(it.next());
        if (w == null)
            return "";
        StringBuilder out = new StringBuilder(w);

        while (it.hasNext()) {
            int k = it.next();
            String entry;
            if (dict.containsKey(k)) {
                entry = dict.get(k);
            } else if (k == nextCode) {
                entry = w + w.charAt(0);
            } else {
                return ""; // זרימה לא תקינה
            }

            out.append(entry);
            dict.put(nextCode++, w + entry.charAt(0));
            w = entry;
        }
        return out.toString();
    }
}

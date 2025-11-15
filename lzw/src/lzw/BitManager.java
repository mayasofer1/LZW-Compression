package lzw;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class BitManager {

    // ממיר ByteArrayOutputStream לרשימת ביטים
    public static List<Boolean> byteStreamToBitStream(ByteArrayOutputStream byteStream) {
        List<Boolean> bitStream = new ArrayList<>();
        for (byte b : byteStream.toByteArray()) {
            int ub = b & 0xFF;
            for (int i = 7; i >= 0; i--) {
                bitStream.add(((ub >> i) & 1) == 1);
            }
        }
        return bitStream;
    }

    // ממיר רשימת ביטים לבייטים
    public static ByteArrayOutputStream convertBitsToBytes(List<Boolean> bitStream) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        int bitIndex = 0, currentByte = 0;
        for (Boolean bit : bitStream) {
            currentByte = (currentByte << 1) | (bit ? 1 : 0);
            bitIndex++;
            if (bitIndex == 8) {
                byteOut.write(currentByte);
                bitIndex = 0;
                currentByte = 0;
            }
        }
        if (bitIndex > 0) {
            currentByte <<= (8 - bitIndex); // השלמה באפסים
            byteOut.write(currentByte);
        }
        return byteOut;
    }

    // ממיר רשימת אינטים לרשימת ביטים ברוחב קבוע לכל ערך
    public static List<Boolean> integerArrayToBitStream(List<Integer> values, int bitsNeeded) {
        List<Boolean> bitStream = new ArrayList<>();
        int bitMask = (bitsNeeded >= 32) ? -1 : ((1 << bitsNeeded) - 1);
        for (int value : values) {
            int val = value & bitMask;
            for (int i = bitsNeeded - 1; i >= 0; i--) {
                bitStream.add(((val >> i) & 1) == 1);
            }
        }
        return bitStream;
    }

    // קריאה בטוחה לפי מונה קודים (מומלץ ל-LZW)
    public static List<Integer> BitStreamToIntegerArray(List<Boolean> bitStream, int segmentSize, int codeCount) {
        List<Integer> integerArray = new ArrayList<>(codeCount);
        int idx = 0;
        for (int k = 0; k < codeCount; k++) {
            int v = 0;
            for (int i = 0; i < segmentSize; i++) {
                v = (v << 1) | (bitStream.get(idx++) ? 1 : 0);
            }
            integerArray.add(v);
        }
        return integerArray;
    }

    // כתיבה/קריאה של מספר קבוע ביטים
    public static void writeFixedBits(List<Boolean> out, int value, int bits) {
        for (int i = bits - 1; i >= 0; i--) {
            out.add(((value >> i) & 1) == 1);
        }
    }

    public static int readFixedBits(List<Boolean> in, int fromIndex, int bits) {
        int v = 0;
        for (int i = 0; i < bits; i++) {
            v = (v << 1) | (in.get(fromIndex + i) ? 1 : 0);
        }
        return v;
    }

    // כותרת רגילה: 4 ביט רוחב + 32 ביט מונה קודים
    // שימו לב: 0 בכותרת מייצג בפועל רוחב 16 ביט (מיפוי 16↔0 כדי להתאים ל-4 ביט).
    public static void writeHeader(List<Boolean> out, int bitsNeeded, int codeCount) {
        int store = (bitsNeeded == 16) ? 0 : (bitsNeeded & 0x0F); // 16→0; 4 ביט בלבד
        writeFixedBits(out, store, 4);
        writeFixedBits(out, codeCount, 32);
    }

    public static int[] readHeader(List<Boolean> in) {
        int b = readFixedBits(in, 0, 4);
        int bitsNeeded = (b == 0) ? 16 : b; // 0→16
        int codeCount = readFixedBits(in, 4, 32);
        return new int[] { bitsNeeded, codeCount };
    }
}

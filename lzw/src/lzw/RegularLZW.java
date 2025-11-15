package lzw;

import java.io.ByteArrayOutputStream;
import java.util.*;
import static lzw.BitManager.*;

public class RegularLZW {

    public static ByteArrayOutputStream Compress(String Text) {
        // Handle empty/null input: write an empty header and return
        if (Text == null || Text.isEmpty()) {
            List<Boolean> bitStream = new ArrayList<>();
            writeHeader(bitStream, 8, 0); // width=8 (placeholder), codeCount=0
            return convertBitsToBytes(bitStream);
        }

        // ---- Standard LZW with 16-bit dictionary cap ----
        final int MAX_BITS = 16;
        final int MAX_CODE = (1 << MAX_BITS) - 1; // 65535

        // Initialize dictionary with single-byte entries 0..255
        int nextCode = 256;
        Map<String, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++)
            dictionary.put(String.valueOf((char) i), i);

        String w = "";
        List<Integer> codes = new ArrayList<>();

        // Core LZW compression loop
        for (char c : Text.toCharArray()) {
            String wc = w + c;
            if (dictionary.containsKey(wc)) {
                // If the extended sequence exists, keep extending
                w = wc;
            } else {
                // Output the code for current sequence w
                codes.add(dictionary.get(w));
                // Add new sequence wc to dictionary if under 16-bit cap
                if (nextCode <= MAX_CODE) {
                    dictionary.put(wc, nextCode++);
                }
                // Start a new sequence from the current character
                w = String.valueOf(c);
            }
        }
        // Flush the last sequence
        if (!w.isEmpty())
            codes.add(dictionary.get(w));

        // Compute fixed bit width based on the maximum code produced (clamped to 16)
        int maxCode = Collections.max(codes);
        int bitsNeeded = Math.max(8, 32 - Integer.numberOfLeadingZeros(maxCode));
        if (bitsNeeded > MAX_BITS)
            bitsNeeded = MAX_BITS;

        // Serialize: header + all codes with fixed width
        List<Boolean> bitStream = new ArrayList<>();
        writeHeader(bitStream, bitsNeeded, codes.size());
        bitStream.addAll(integerArrayToBitStream(codes, bitsNeeded));

        System.out.println("Regular LZW: width=" + bitsNeeded +
                ", codes=" + codes.size() +
                ", totalBits=" + bitStream.size());
        return convertBitsToBytes(bitStream);
    }

    public static String Decompress(ByteArrayOutputStream compressed) {
        // Convert bytes back to bits; minimal header length check (36 bits)
        List<Boolean> allBits = byteStreamToBitStream(compressed);
        if (allBits.size() < 36)
            return "";

        // Read header: [bitsNeeded, codeCount]
        int[] hdr = readHeader(allBits);
        int bitsNeeded = hdr[0];
        int codeCount = hdr[1];

        // Compatibility: if header wrote 0 for width (since 16 didn't fit in 4 bits),
        // treat as 16
        if (bitsNeeded == 0)
            bitsNeeded = 16;

        if (bitsNeeded <= 0 || codeCount < 0)
            return "";
        if (codeCount == 0)
            return "";

        // Slice out data bits and decode them into integer codes
        List<Boolean> dataBits = allBits.subList(36, allBits.size());
        List<Integer> codes = BitStreamToIntegerArray(dataBits, bitsNeeded, codeCount);
        if (codes.isEmpty())
            return "";

        // Initialize reverse dictionary with 0..255 single-byte strings
        int nextCode = 256;
        Map<Integer, String> dict = new HashMap<>();
        for (int i = 0; i < 256; i++)
            dict.put(i, String.valueOf((char) i));

        // Seed with the first code
        Iterator<Integer> it = codes.iterator();
        String w = dict.get(it.next());
        if (w == null)
            return "";
        StringBuilder out = new StringBuilder(w);

        // Core LZW decompression loop
        while (it.hasNext()) {
            int k = it.next();
            // If k exists in dict, take it; else it's the special "w + firstChar(w)" case
            String entry = dict.containsKey(k) ? dict.get(k) : (w + w.charAt(0));
            out.append(entry);
            // Add new dictionary entry built from previous output and first char of current
            // entry
            dict.put(nextCode++, w + entry.charAt(0));
            w = entry;
        }
        return out.toString();
    }
}

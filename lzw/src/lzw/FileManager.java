package lzw;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileManager {
    private final Path path;
    public FileManager(String filePath) { this.path = Path.of(filePath); }

    // טקסט
    public String readFileAsText() {
        try { return Files.readString(path); }
        catch (IOException e) { e.printStackTrace(); return null; }
    }
    public void writeFileAsText(String content) {
        try { Files.writeString(path, content); }
        catch (IOException e) { e.printStackTrace(); }
    }

    // בינארי
    public byte[] readFileAsBytes() {
        try { return Files.readAllBytes(path); }
        catch (IOException e) { e.printStackTrace(); return null; }
    }
    public void writeFileAsBytes(byte[] data) {
        try { Files.write(path, data); }
        catch (IOException e) { e.printStackTrace(); }
    }
    public void writeFileFromStream(ByteArrayOutputStream stream) {
        writeFileAsBytes(stream.toByteArray());
    }
}

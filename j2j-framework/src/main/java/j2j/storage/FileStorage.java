package j2j.storage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

/**
 * Low-level file I/O for JSONL storage.
 *   - Append lines to the storage file
 *   - Read all lines from the storage file
 * Each line in the file is one serialized object:
 */
public class FileStorage {

    private final Path filePath;

    /**
     * @param filePath path to the JSONL storage file.
     *                 File will be created automatically on first write if it doesn't exist.
     */
    public FileStorage(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("filePath must not be null");
        }
        this.filePath = filePath;
    }

    /**
     * Appends a list of JSON lines to the storage file.
     *
     * @param lines list of serialized JSON strings to write
     * @throws FileStorageException if writing fails
     */
    public void appendLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) return;

        try (BufferedWriter writer = Files.newBufferedWriter(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new FileStorageException("Failed to write to storage file: " + filePath, e);
        }
    }

    /**
     * Reads all lines from the storage file.
     * Returns an empty list if the file does not exist yet.
     *
     * @return list of raw JSON strings, one per object
     * @throws FileStorageException if reading fails
     */
    public List<String> readAllLines() {
        if (!Files.exists(filePath)) {
            return Collections.emptyList();
        }

        try {
            return Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read storage file: " + filePath, e);
        }
    }
}
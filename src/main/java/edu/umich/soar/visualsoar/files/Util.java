package edu.umich.soar.visualsoar.files;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Util {
  private static final Logger LOGGER = Logger.getLogger(Util.class.getName());

  @FunctionalInterface
  public interface Writer {
    void write(OutputStream out);
  }

  /**
   * Write to the destination file from {@code writer} as atomically as possible.
   */
  public void Save(Path destination, Writer writer) throws java.io.IOException {
    // Write to a temp file first, then make the final changes via a rename,
    // which can often be done atomically. This prevents issues such as overwriting a file with a
    // incomplete data, as could be the case if we hit an IO exception in the middle of writing the
    // file (especially relevant for folks using network drives!)
    String tempFilename = destination.toAbsolutePath() + ".temp";
    Path tempPath = Paths.get(tempFilename);

    try (FileOutputStream output = new FileOutputStream(tempPath.toFile())) {
      writer.write(output);
    }

    try {
      Files.move(tempPath, destination, REPLACE_EXISTING, ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      LOGGER.warning(
          "Cannot write "
              + destination
              + " atomically  ("
              + e.getMessage()
              + "); falling back to non-atomic write");
      Files.move(tempPath, destination, REPLACE_EXISTING);
    }
  }
}

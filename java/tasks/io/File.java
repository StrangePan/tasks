package tasks.io;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** An interface for interfacing with file system files. */
public interface File {

  /** Opens an input stream to read the contents of the file. */
  InputStream openInputStream();

  /** Opens an output stream to write to the contents of the file. */
  OutputStream openOutputStream();

  static File fromPath(String path) {
    return new File() {

      @Override
      public InputStream openInputStream() {
        try {
          return new FileInputStream(path);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }

      @Override
      public OutputStream openOutputStream() {
        try {
          return new FileOutputStream(path);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    };
  }
}

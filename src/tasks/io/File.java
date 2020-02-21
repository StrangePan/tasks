package tasks.io;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

/** An interface for interfacing with file system files. */
public interface File {

  Writer openWriter();

  Reader openReader();

  static File fromPath(String path) {
    return new File() {

      @Override
      public Reader openReader() {
        try {
          return new FileReader(path);
        } catch (FileNotFoundException ex) {
          return new StringReader("");
        }
      }

      @Override
      public Writer openWriter() {
        try {
          return new FileWriter(path);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    };
  }
}

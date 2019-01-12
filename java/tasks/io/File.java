package tasks.io;

import java.io.InputStream;
import java.io.OutputStream;

/** An interface for interfacing with file system files. */
public interface File {

  /** Opens an input stream to read the contents of the file. */
  InputStream openInputStream();

  /** Opens an output stream to write to the contents of the file. */
  OutputStream openOutputStream();
}

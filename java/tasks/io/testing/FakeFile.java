package tasks.io.testing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import tasks.io.File;

/** In-memory file for testing file system interactions */
public final class FakeFile implements File {

  private byte[] contents = new byte[0];
  private ByteArrayOutputStream lastOutputStream = null;

  @Override
  public synchronized InputStream openInputStream() {
    return new ByteArrayInputStream(contents());
  }

  @Override
  public synchronized OutputStream openOutputStream() {
    lastOutputStream = new ByteArrayOutputStream();
    return lastOutputStream;
  }

  /** Copies the contents of the file into a new byte array and returns it. */
  public synchronized byte[] contents() {
    if (lastOutputStream != null) {
      contents = lastOutputStream.toByteArray();
    }
    return Arrays.copyOf(contents, contents.length);
  }

  /**
   * Sets the contents of the file. Any open output streams will be disconnected from the file such
   * that any additional writes will be ignored.
   *
   * @param contents the new contents of the file
   */
  public synchronized void setContents(byte[] contents) {
    lastOutputStream = null;
    this.contents = Arrays.copyOf(Objects.requireNonNull(contents), contents.length);
  }
}

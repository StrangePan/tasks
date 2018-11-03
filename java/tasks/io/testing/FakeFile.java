package tasks.io.testing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import tasks.io.File;

/** In-memory file for easy IO testing. */
public final class FakeFile implements File {

  private byte[] contents = new byte[0];
  private ByteArrayOutputStream lastOutputStream = null;

  @Override
  public synchronized InputStream openForRead() {
    return new ByteArrayInputStream(contents());
  }

  @Override
  public synchronized OutputStream openForWrite() {
    lastOutputStream = new ByteArrayOutputStream();
    return lastOutputStream;
  }

  public synchronized byte[] contents() {
    if (lastOutputStream != null) {
      contents = lastOutputStream.toByteArray();
      lastOutputStream = null;
    }
    return contents;
  }

  public synchronized void setContents(byte[] contents) {
    lastOutputStream = null;
    this.contents = Arrays.copyOf(Objects.requireNonNull(contents), contents.length);
  }
}

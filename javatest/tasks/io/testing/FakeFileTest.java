package tasks.io.testing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class FakeFileTest {

  private final byte[] testContents = "this was a triumph".getBytes(StandardCharsets.UTF_8);
  private final FakeFile underTest = new FakeFile();

  @Test
  public void init_contentsAreEmpty() {
    assertEquals(0, underTest.contents().length);
  }

  @Test
  public void setContents_didSetContents() {
    underTest.setContents(testContents);

    assertArrayEquals(testContents, underTest.contents());
  }

  @Test
  public void read_didMatchSetContents() throws IOException {
    underTest.setContents(testContents);

    try (InputStream inputStream = underTest.openForRead()) {
      assertArrayEquals(testContents, inputStream.readAllBytes());
    }
  }

  @Test
  public void write_didSetContents() throws IOException {
    try (OutputStream outputStream = underTest.openForWrite()) {
      outputStream.write(testContents);
    }

    assertArrayEquals(testContents, underTest.contents());
  }

  @Test
  public void write_thenRead_didMatchContents() throws IOException {
    try (OutputStream outputStream = underTest.openForWrite()) {
      outputStream.write(testContents);
    }

    try (InputStream inputStream = underTest.openForRead()) {
      assertArrayEquals(testContents, inputStream.readAllBytes());
    }
  }
}

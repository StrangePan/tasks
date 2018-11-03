package tasks.io;

import java.io.InputStream;
import java.io.OutputStream;

public interface File {

  InputStream openForRead();

  OutputStream openForWrite();
}

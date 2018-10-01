package tasks.data;

import java.io.IOException;
import omnia.data.structure.Collection;
import tasks.Task;

public interface TaskWriter extends AutoCloseable {

  void write(Collection<Task> tasks) throws IOException;
}

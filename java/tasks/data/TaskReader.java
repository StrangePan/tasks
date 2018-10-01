package tasks.data;

import java.io.IOException;
import omnia.data.structure.Collection;
import tasks.Task;

public interface TaskReader extends AutoCloseable {

  Collection<Task> read() throws IOException;
}

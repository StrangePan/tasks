package tasks.data;

import java.io.IOException;
import omnia.data.structure.Graph;
import tasks.Task;

public interface TaskReader extends AutoCloseable {

  Graph<Task> read() throws IOException;
}

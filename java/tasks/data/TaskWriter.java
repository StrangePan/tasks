package tasks.data;

import omnia.data.structure.Graph;
import tasks.Task;

public interface TaskWriter extends AutoCloseable {

  void write(Graph<Task> taskGraph);
}

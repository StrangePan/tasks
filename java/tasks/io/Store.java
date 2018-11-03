package tasks.io;

public interface Store<T> {

  T retrieveBlocking();

  void storeBlocking(T data);
}

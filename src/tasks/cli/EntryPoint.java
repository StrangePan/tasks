package tasks.cli;

/** CLI entry point into the Tasks application. */
public final class EntryPoint {
  public static void main(String[] args) {
    new Application(args).run();
  }
}

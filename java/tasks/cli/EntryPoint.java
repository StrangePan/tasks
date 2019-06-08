package tasks.cli;

import tasks.cli.arg.CliArguments;
/** CLI entry point into the Tasks application. */
public final class EntryPoint {

  public static void main(String[] args) {
    CliArguments arguments;

    try {
      arguments = CliArguments.parse(args);
    } catch (CliArguments.ArgumentFormatException e) {
      System.out.println(e.getMessage());
      return;
    }

    System.out.println(arguments);
  }


}

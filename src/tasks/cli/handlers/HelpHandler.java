package tasks.cli.handlers;

import tasks.cli.arg.HelpArguments;

public final class HelpHandler implements ArgumentHandler<HelpArguments> {
  @Override
  public void handle(HelpArguments arguments) {
    System.out.println("success!");
    // TODO
  }
}

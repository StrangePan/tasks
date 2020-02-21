package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;

import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.HelpArguments;

public final class HelpHandler implements ArgumentHandler<HelpArguments> {
  @Override
  public void handle(HelpArguments arguments) {
    System.out.println(
        "Commands: "
            + ImmutableList.<String>builder()
            .add("add")
            .add("amend")
            .add("complete")
            .add("help")
            .add("list, ls")
            .add("remove, rm")
            .add("reopen")
            .build()
            .stream()
            .map(line -> "\n  " + line)
            .collect(joining()));
  }
}

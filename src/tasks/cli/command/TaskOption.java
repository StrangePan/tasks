package tasks.cli.command;

import java.util.Optional;

public final class TaskOption extends Option {
  public TaskOption(String longName, String shortName, String description, Parameter.Repeatable repeatable) {
    super(longName, shortName, description, repeatable, Optional.of("task"));
  }

  @Override
  public org.apache.commons.cli.Option toCliOption() {
    return org.apache.commons.cli.Option.builder(shortName().orElse(null))
        .longOpt(longName())
        .desc(description())
        .optionalArg(false)
        .numberOfArgs(1)
        .build();
  }
}

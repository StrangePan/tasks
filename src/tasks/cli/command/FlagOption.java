package tasks.cli.command;

import java.util.Optional;

public final class FlagOption extends Option {
  public FlagOption(String longName, String shortName, String description, Parameter.Repeatable repeatable) {
    super(longName, shortName, description, repeatable, Optional.empty());
  }

  public FlagOption(String longName, String description, Parameter.Repeatable repeatable) {
    super(longName, description, repeatable, Optional.empty());
  }

  @Override
  public org.apache.commons.cli.Option toCliOption() {
    return org.apache.commons.cli.Option.builder(shortName().orElse(null))
        .longOpt(longName())
        .desc(description())
        .numberOfArgs(0)
        .build();
  }
}

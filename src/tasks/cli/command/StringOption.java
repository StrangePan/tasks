package tasks.cli.command;

import java.util.Optional;

public final class StringOption extends Option {
  public StringOption(
      String longName,
      String shortName,
      String description,
      Parameter.Repeatable repeatable,
      String semanticDescription) {
    super(longName, shortName, description, repeatable, Optional.of(semanticDescription));
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

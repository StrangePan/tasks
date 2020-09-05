package tasks.cli.arg.registration;

import static java.util.Objects.requireNonNull;

import tasks.cli.arg.CliArguments;

public abstract class Parameter {
  private final String description;
  private final Repeatable repeatable;

  Parameter(String description, Repeatable repeatable) {
    this.description = requireNonNull(description);
    this.repeatable = repeatable;
  }

  public String description() {
    return description;
  }

  public boolean isRepeatable() {
    return repeatable == Repeatable.REPEATABLE;
  }

  public enum Repeatable {
    REPEATABLE,
    NOT_REPEATABLE,
  }
}

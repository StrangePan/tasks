package tasks.cli.command;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

public abstract class Option {
  private final String longName;
  private final Optional<String> shortName;
  private final String description;
  private final Parameter.Repeatable repeatable;
  private final Optional<String> parameterRepresentation;

  protected Option(
      String longName,
      String shortName,
      String description,
      Parameter.Repeatable repeatable,
      Optional<String> parameterRepresentation) {
    this(longName, Optional.of(shortName), description, repeatable, parameterRepresentation);
  }

  protected Option(
      String longName,
      String description,
      Parameter.Repeatable repeatable,
      Optional<String> parameterRepresentation) {
    this(longName, Optional.empty(), description, repeatable, parameterRepresentation);
  }

  private Option(
      String longName,
      Optional<String> shortName,
      String description,
      Parameter.Repeatable repeatable,
      Optional<String> parameterRepresentation) {
    this.longName = requireNonNull(longName);
    this.shortName = requireNonNull(shortName);
    this.description = requireNonNull(description);
    this.repeatable = repeatable;
    this.parameterRepresentation = requireNonNull(parameterRepresentation);
  }

  public String longName() {
    return longName;
  }

  public Optional<String> shortName() {
    return shortName;
  }

  public String description() {
    return description;
  }

  public boolean isRepeatable() {
    return repeatable == Parameter.Repeatable.REPEATABLE;
  }

  public Optional<String> parameterRepresentation() {
    return parameterRepresentation;
  }

  public abstract org.apache.commons.cli.Option toCliOption();
}

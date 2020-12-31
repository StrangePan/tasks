package tasks.cli.feature.help;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

/** Model for parsed Help command arguments. */
public final class HelpArguments {
  private final Optional<String> mode;

  HelpArguments() {
    this(Optional.empty());
  }

  HelpArguments(String mode) {
    this(Optional.of(mode));
  }

  private HelpArguments(Optional<String> mode) {
    this.mode = requireNonNull(mode);
  }

  /** The optional command for which the user is requesting help. */
  public Optional<String> mode() {
    return mode;
  }
}

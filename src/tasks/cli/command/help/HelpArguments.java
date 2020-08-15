package tasks.cli.command.help;

import static java.util.Objects.requireNonNull;
import static tasks.cli.arg.CliArguments.Parameter.Repeatable.NOT_REPEATABLE;
import static tasks.cli.arg.CliUtils.tryParse;

import java.util.Optional;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;

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

  public Optional<String> mode() {
    return mode;
  }
}

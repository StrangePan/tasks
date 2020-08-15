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

  private HelpArguments() {
    this(Optional.empty());
  }

  private HelpArguments(String mode) {
    this(Optional.of(mode));
  }

  private HelpArguments(Optional<String> mode) {
    this.mode = requireNonNull(mode);
  }

  public static CliArguments.CommandRegistration registration(Memoized<Set<String>> validModes) {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.HELP)
        .canonicalName("help")
        .aliases()
        .parameters(ImmutableList.of(new CliArguments.StringParameter("command", NOT_REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new HelpArguments.Parser(validModes))
        .helpDocumentation("Retrieve the help documentation for a specific command.");
  }

  public Optional<String> mode() {
    return mode;
  }

  public static final class Parser implements CliArguments.Parser<HelpArguments> {
    private final Memoized<Set<String>> validModes;

    public Parser(Memoized<Set<String>> validModes) {
      this.validModes = requireNonNull(validModes);
    }

    @Override
    public HelpArguments parse(List<? extends String> args) {
      List<String> parsedArgs = List.masking(tryParse(args, new Options()).getArgList());

      Optional<String> mode =
          parsedArgs.count() > 1
              ? Optional.of(parsedArgs.itemAt(1))
              : Optional.empty();

      mode.ifPresent(this::assertModeIsValid);

      return mode.map(HelpArguments::new).orElseGet(HelpArguments::new);
    }

    private void assertModeIsValid(String mode) {
      if (!validModes.value().contains(mode)) {
        throw new CliArguments.ArgumentFormatException(
            String.format("Command mode not recognized: %s", mode));
      }
    }
  }
}

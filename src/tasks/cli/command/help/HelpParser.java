package tasks.cli.command.help;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import tasks.cli.arg.CliArguments;

/** Command line argument parser for the Help command. */
public final class HelpParser implements CliArguments.CommandParser<HelpArguments> {
  private final Memoized<Set<String>> validModes;

  public HelpParser(Memoized<Set<String>> validModes) {
    this.validModes = requireNonNull(validModes);
  }

  @Override
  public HelpArguments parse(CommandLine commandLine) {
    List<String> parsedArgs = ImmutableList.copyOf(commandLine.getArgList());

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

package tasks.cli.command.help;

import static java.util.Objects.requireNonNull;
import static tasks.cli.arg.CliUtils.tryParse;

import java.util.Optional;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.Set;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliArguments;

public final class HelpParser implements CliArguments.Parser<HelpArguments> {
  private final Memoized<Set<String>> validModes;

  public HelpParser(Memoized<Set<String>> validModes) {
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

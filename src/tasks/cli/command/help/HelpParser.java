package tasks.cli.command.help;

import java.util.Optional;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import tasks.cli.parser.CommandParser;

/** Command line argument parser for the Help command. */
public final class HelpParser implements CommandParser<HelpArguments> {
  public HelpParser() {}

  @Override
  public HelpArguments parse(CommandLine commandLine) {
    List<String> parsedArgs = ImmutableList.copyOf(commandLine.getArgList());

    Optional<String> mode =
        parsedArgs.count() > 0
            ? Optional.of(parsedArgs.itemAt(0))
            : Optional.empty();

    return mode.map(HelpArguments::new).orElseGet(HelpArguments::new);
  }
}

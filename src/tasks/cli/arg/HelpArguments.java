package tasks.cli.arg;

import static tasks.cli.arg.CliUtils.assertNoExtraArgs;
import static tasks.cli.arg.CliUtils.tryParse;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public final class HelpArguments {
  private HelpArguments() {}

  static HelpArguments parse(String[] args) {
    /*
    First arg is assumed to be "help" or an alias thereof
    No other unclassified arg allowed
    */
    CommandLine commandLine = tryParse(args, new Options());

    assertNoExtraArgs(commandLine);

    return new HelpArguments();
  }
}

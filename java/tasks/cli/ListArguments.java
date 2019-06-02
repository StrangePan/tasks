package tasks.cli;

import static tasks.cli.CliUtils.assertNoExtraArgs;
import static tasks.cli.CliUtils.tryParse;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public final class ListArguments {
  private final boolean isAllSet;

  private ListArguments(boolean isAllSet) {
    this.isAllSet = isAllSet;
  }

  public boolean isAllSet() {
    return isAllSet;
  }

  static ListArguments parse(String[] args) {
    /*
    First arg is assumed to be "ls" or an alias thereof
    No other unclassified args allowed
    optional --all flag
    */
    Options options = new Options();
    options.addOption(
        Option.builder("a")
            .longOpt("all")
            .desc("Setting this flag lists all tasks, including blocked tasks and completed tasks.")
            .numberOfArgs(0)
            .build());

    CommandLine commandLine = tryParse(args, options);
    assertNoExtraArgs(commandLine);

    boolean isAllSet = commandLine.hasOption("a");

    return new ListArguments(isAllSet);
  }
}

package tasks.cli.command.common;

import org.apache.commons.cli.CommandLine;

public final class CommonParser {
  public CommonParser() {}

  public <T> CommonArguments<T> parse(CommandLine commandLine, T specificArguments) {
    return new CommonArguments<>(specificArguments, true);
  }
}

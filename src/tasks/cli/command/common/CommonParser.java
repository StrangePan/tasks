package tasks.cli.command.common;

import org.apache.commons.cli.CommandLine;
import tasks.cli.arg.CliUtils;

public final class CommonParser {
  public CommonParser() {}

  public <T> CommonArguments<T> parse(CommandLine commandLine, T specificArguments) {
    return new CommonArguments<>(
        specificArguments,
        !CliUtils.getFlagPresence(commandLine, StripColors.STRIP_COLORS_OPTION.value()));
  }
}

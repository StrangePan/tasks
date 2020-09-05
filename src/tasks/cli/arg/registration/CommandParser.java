package tasks.cli.arg.registration;

import org.apache.commons.cli.CommandLine;

public interface CommandParser<T> {
  T parse(CommandLine commandLine);
}

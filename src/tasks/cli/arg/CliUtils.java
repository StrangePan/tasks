package tasks.cli.arg;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static omnia.data.stream.Collectors.toList;

import java.util.Optional;
import omnia.data.structure.List;
import omnia.data.structure.mutable.ArrayList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import tasks.Task;
class CliUtils {
  static CommandLine tryParse(String[] args, Options options) {
    try {
      return new DefaultParser().parse(options, args, /* stopAtNonOption= */ false);
    } catch (ParseException e) {
      throw new CliArguments.ArgumentFormatException("Unable to parse arguments: " + e.getMessage(), e);
    }
  }

  static List<Task.Id> parseTaskIds(List<String> taskStrings) {
    List<Task.Id> taskIds;
    try {
      taskIds = taskStrings.stream().map(Task.Id::parse).collect(toList());
    } catch (Task.Id.IdFormatException ex) {
      throw new CliArguments.ArgumentFormatException("Invalid task ID", ex);
    }
    return taskIds;
  }

  static List<String> getOptionValues(CommandLine commandLine, String opt) {
    return ArrayList.of(
        Optional.ofNullable(requireNonNull(commandLine).getOptionValues(requireNonNull(opt)))
            .orElse(new String[0]));
  }

  static Optional<String> getSingleOptionValue(CommandLine commandLine, String opt) {
    if (commandLine.getOptionValues(opt).length > 1) {
      throw new CliArguments.ArgumentFormatException("Too many values provided for parameter '" + opt + "'");
    }
    return Optional.ofNullable(commandLine.getOptionValue(opt));
  }

  static void assertNoExtraArgs(CommandLine commandLine) {
    if (commandLine.getArgList().size() > 1) {
      String unexpectedArgs =
          commandLine.getArgList()
              .stream()
              .skip(1)
              .map(s -> "\"" + s + "\"")
              .collect(joining(", "));

      throw new CliArguments.ArgumentFormatException("Unexpected arguments given: " + unexpectedArgs);
    }
  }
}

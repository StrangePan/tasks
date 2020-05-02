package tasks.cli.arg;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableMap;

/** Data structure for arguments passed into the command line. */
public final class CliArguments {

  private final Map<CliMode, Function<String[], Object>> argsParsers =
      ImmutableMap.<CliMode, Function<String[], Object>>builder()
          .put(CliMode.HELP, HelpArguments::parse)
          .put(CliMode.LIST, ListArguments::parse)
          .put(CliMode.INFO, InfoArguments::parse)
          .put(CliMode.ADD, AddArguments::parse)
          .put(CliMode.REMOVE, RemoveArguments::parse)
          .put(CliMode.AMEND, AmendArguments::parse)
          .put(CliMode.COMPLETE, CompleteArguments::parse)
          .put(CliMode.REOPEN, ReopenArguments::parse)
          .build();

  public CliArguments() {}

  public Object parse(String[] args) {

    // Parameter validation
    requireNonNull(args);
    if (Arrays.stream(args).anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("arg cannot contain null");
    }

    List<String> argsList = ImmutableList.<String>builder().addAll(args).build();

    // Determine what mode we're in. This will affect what flags are available and what they mean.
    String modeArgument = argsList.isPopulated() ? argsList.itemAt(0) : "";
    CliMode mode = modeFromArgument(modeArgument);

    return argsParsers.valueOf(mode).map(f -> f.apply(args)).orElseThrow(AssertionError::new);
  }

  private static CliMode modeFromArgument(String arg) {
    switch (arg) {
      case "":
      case "help":
        return CliMode.HELP;
      case "list":
      case "ls":
        return CliMode.LIST;
      case "add":
        return CliMode.ADD;
      case "amend":
        return CliMode.AMEND;
      case "info":
        return CliMode.INFO;
      case "remove":
      case "rm":
        return CliMode.REMOVE;
      case "complete":
        return CliMode.COMPLETE;
      case "reopen":
        return CliMode.REOPEN;
      default:
        throw new ArgumentFormatException("unrecognized mode " + arg);
    }
  }

  public static final class ArgumentFormatException extends RuntimeException {
    ArgumentFormatException(String reason) {
      super(reason);
    }

    ArgumentFormatException(String reason, Throwable cause) {
      super(reason, cause);
    }
  }
}

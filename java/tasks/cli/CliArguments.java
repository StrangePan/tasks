package tasks.cli;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Objects;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;

/** Data structure for arguments passed into the command line. */
public final class CliArguments {
  private final Mode mode;

  private CliArguments(Mode mode) {
    this.mode = requireNonNull(mode);
  }

  @Override
  public String toString() {
    return "CliArguments {mode = " + mode + "}";
  }

  public static CliArguments parse(String[] args) {

    // Parameter validation
    requireNonNull(args);
    if (Arrays.stream(args).anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("args cannot contain null");
    }

    List<String> argsList = ImmutableList.of(args);

    // Determine what mode we're in. This will affect what flags are available and what they mean.
    Mode mode = modeFromArgument(argsList.isPopulated() ? argsList.itemAt(0) : "");

    return new CliArguments(mode);
  }

  public enum Mode {
    HELP,
    LIST,
    ADD,
    REMOVE,
    COMPLETE,
    UNCOMPLETE,
  }

  private static Mode modeFromArgument(String arg) {
    switch (arg) {
      case "":
      case "help":
        return Mode.HELP;
      case "list":
      case "ls":
        return Mode.LIST;
      case "add":
        return Mode.ADD;
      case "remove":
      case "rm":
        return Mode.REMOVE;
      case "complete":
        return Mode.COMPLETE;
      case "uncomplete":
        return Mode.UNCOMPLETE;
      default:
        throw new ArgumentFormatException("unrecognized mode " + arg);
    }
  }

  public static final class ArgumentFormatException extends RuntimeException {
    ArgumentFormatException(String reason) {
      super(reason);
    }
  }
}

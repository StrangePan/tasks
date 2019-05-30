package tasks.cli;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** Data structure for arguments passed into the command line. */
public final class CliArguments {

  public static final Map<Mode, Function<String[], CommandLine>> MODE_TO_PARSER_MAP =
      ImmutableMap.<Mode, Function<String[], CommandLine>>builder()
          .put(Mode.HELP, CliArguments::parseHelp)
          .put(Mode.LIST, CliArguments::parseList)
          .put(Mode.INFO, CliArguments::parseInfo)
          .put(Mode.ADD, CliArguments::parseAdd)
          .put(Mode.REMOVE, CliArguments::parseRemove)
          .put(Mode.AMEND, CliArguments::parseAmend)
          .put(Mode.COMPLETE, CliArguments::parseComplete)
          .put(Mode.UNCOMPLETE, CliArguments::parseUncomplete)
          .build();

  private final Mode mode;

  private CliArguments(Mode mode) {
    this.mode = requireNonNull(mode);
  }

  @Override
  public String toString() {
    return "CliArguments {mode = " + mode + "}";
  }

  public enum Mode {
    HELP,
    LIST,
    INFO,
    ADD,
    REMOVE,
    AMEND,
    COMPLETE,
    UNCOMPLETE,
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

    CommandLine commandLine = MODE_TO_PARSER_MAP.valueOf(mode).map(f -> f.apply(args)).orElse(null);

    System.out.println(stringify(commandLine));

    return new CliArguments(mode);
  }

  private static CommandLine parseHelp(String[] args) {
    return tryParse(args, new Options());
  }

  private static CommandLine parseList(String[] args) {
    Options options = new Options();
    options.addOption(
        Option.builder("a")
            .longOpt("all")
            .desc("Setting this flag lists all tasks, including blocked tasks and completed tasks.")
            .numberOfArgs(0)
            .build());

    return tryParse(args, options);
  }

  private static CommandLine parseInfo(String[] args) {
    return tryParse(args, new Options());
  }

  private static CommandLine parseAdd(String[] args) {
    Options options = new Options();
    options.addOption(
        Option.builder("a")
            .longOpt("after")
            .desc("The tasks this one comes after. This list of tasks will be blocking this task.")
            .optionalArg(false)
            .numberOfArgs(1)
            .build());
    options.addOption(
        Option.builder("b")
            .longOpt("before")
            .desc("The tasks this one comes before. This list of tasks will be unblocked by this " +
                "task.")
            .optionalArg(false)
            .numberOfArgs(1)
            .build());

    return tryParse(args, options);
  }

  private static CommandLine parseRemove(String[] args) {
    return tryParse(args, new Options());
  }

  private static CommandLine parseComplete(String[] args) {
    return tryParse(args, new Options());
  }

  private static CommandLine parseUncomplete(String[] args) {
    return tryParse(args, new Options());
  }

  private static CommandLine parseAmend(String[] args) {
    Options options = new Options();
    options.addOption(
        Option.builder("m")
            .longOpt("description")
            .desc("Edit the description of the task. Leave blank to open in an editor")
            .numberOfArgs(1)
            .optionalArg(true)
            .build());
    options.addOption(
        Option.builder("aa")
            .longOpt("addafter")
            .desc("Add this task as blocking another task.")
            .numberOfArgs(1)
            .optionalArg(false)
            .build());
    options.addOption(
        Option.builder("ra")
            .longOpt("rmafter")
            .desc("Remove this task from blocking another task.")
            .numberOfArgs(1)
            .optionalArg(false)
            .build());
    options.addOption(
        Option.builder("ab")
            .longOpt("addbefore")
            .desc("Add another task as blocking this task.")
            .numberOfArgs(1)
            .optionalArg(false)
            .build());
    options.addOption(
        Option.builder("rb")
            .longOpt("rmbefore")
            .desc("Remove another task as blocking this task.")
            .numberOfArgs(1)
            .optionalArg(false)
            .build());

    return tryParse(args, options);
  }

  private static CommandLine tryParse(String[] args, Options options) {
    try {
      return new DefaultParser().parse(options, args, /* stopAtNonOption= */ false);
    } catch (ParseException e) {
      throw new ArgumentFormatException("Unable to parse arguments: " + e.getMessage(), e);
    }
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

    ArgumentFormatException(String reason, Throwable cause) {
      super(reason, cause);
    }
  }

  private static String stringify(CommandLine commandLine) {
    StringBuilder string = new StringBuilder();
    commandLine.getArgList().stream().map(s -> s + "\n").forEachOrdered(string::append);
    Arrays.stream(commandLine.getOptions())
        .filter(o -> commandLine.hasOption(o.getOpt()))
        .map(o -> o.getOpt() + " = " + commandLine.getOptionValue(o.getOpt()) + "\n")
        .forEachOrdered(string::append);
    return string.toString();
  }
}

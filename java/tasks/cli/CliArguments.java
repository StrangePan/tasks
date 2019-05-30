package tasks.cli;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toList;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.mutable.ArrayList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import tasks.Task;

/** Data structure for arguments passed into the command line. */
public final class CliArguments {

  public static final Map<Mode, Function<String[], CommandLine>> MODE_TO_PARSER =
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

  public static final Map<Mode, Function<CommandLine, Object>> COMMAND_LINE_TO_MODE_ARGUMENTS =
      ImmutableMap.<Mode, Function<CommandLine, Object>>builder()
          .put(Mode.ADD, AddArguments::parseFrom)
          .build();

  private final Mode mode;
  private final Object arguments;

  private CliArguments(Mode mode, Object arguments) {
    this.mode = requireNonNull(mode);
    this.arguments = requireNonNull(arguments);
  }

  /**
   * Get the mode-specific arguments.
   *
   * <p>Will be one of {@link AddArguments}.
   */
  public Object getArguments() {
    return arguments;
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

    CommandLine commandLine = MODE_TO_PARSER.valueOf(mode).map(f -> f.apply(args)).orElse(null);
    Object modeArguments =
        COMMAND_LINE_TO_MODE_ARGUMENTS.valueOf(mode).map(f -> f.apply(commandLine)).orElse(null);

    System.out.println(stringify(commandLine));

    return new CliArguments(mode, modeArguments);
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

  public static final class AddArguments {
    private final String description;
    private final List<Task.Id> blockingTasks;
    private final List<Task.Id> blockedTasks;

    private AddArguments(
        String description, List<Task.Id> blockingTasks, List<Task.Id> blockedTasks) {
      this.description = description;
      this.blockingTasks = blockingTasks;
      this.blockedTasks = blockedTasks;
    }

    /** The description of the task. */
    public String description() {
      return description;
    }

    /** List of task IDs that are blocking this new task in the order specified in the CLI. */
    public List<Task.Id> blockingTasks() {
      return blockingTasks;
    }

    /** List of task IDs that are blocked by this new task in the order specified in the CLI. */
    public List<Task.Id> blockedTasks() {
      return blockedTasks;
    }

    private static AddArguments parseFrom(CommandLine commandLine) {
      // 1st param assumed to be "add" or an alias for it
      // 2nd param must be description
      // 3+ params not supported
      // optional befores
      // optional afters

      List<String> argsList = List.masking(commandLine.getArgList());
      if (argsList.count() < 2) {
        throw new ArgumentFormatException("Task description not defined");
      }
      if (argsList.count() > 2) {
        throw new ArgumentFormatException("Unexpected extra arguments");
      }

      List<Task.Id> afterTasks = parseTaskIds(getOptionValues(commandLine, "a"));
      List<Task.Id> beforeTasks = parseTaskIds(getOptionValues(commandLine, "b"));

      return new AddArguments(argsList.itemAt(1), afterTasks, beforeTasks);
    }
  }

  private static List<Task.Id> parseTaskIds(List<String> taskStrings) {
    List<Task.Id> taskIds;
    try {
      taskIds = taskStrings.stream().map(Long::parseLong).map(Task.Id::from).collect(toList());
    } catch (NumberFormatException ex) {
      throw new ArgumentFormatException("Invalid task ID", ex);
    }
    if (taskIds.stream().anyMatch(id -> id.asLong() < 0)) {
      throw new ArgumentFormatException("Invalid task ID");
    }
    return taskIds;
  }

  private static List<String> getOptionValues(CommandLine commandLine, String opt) {
    return ArrayList.of(
        Optional.ofNullable(requireNonNull(commandLine).getOptionValues(requireNonNull(opt)))
            .orElse(new String[0]));
  }
}

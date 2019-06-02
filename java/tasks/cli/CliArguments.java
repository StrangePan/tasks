package tasks.cli;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
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
import tasks.Task.Id.IdFormatException;

/** Data structure for arguments passed into the command line. */
public final class CliArguments {

  private static final Map<Mode, Function<String[], Object>> ARGS_TO_MODEL =
  ImmutableMap.<Mode, Function<String[], Object>>builder()
          .put(Mode.HELP, HelpArguments::parse)
          .put(Mode.LIST, ListArguments::parse)
          .put(Mode.INFO, InfoArguments::parse)
          .put(Mode.ADD, AddArguments::parse)
          .put(Mode.REMOVE, RemoveArguments::parse)
          .put(Mode.AMEND, AmendArguments::parse)
          .put(Mode.COMPLETE, CompleteArguments::parse)
          .put(Mode.REOPEN, ReopenArguments::parse)
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
    REOPEN,
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

    Object modeArguments = ARGS_TO_MODEL.valueOf(mode).map(f -> f.apply(args)).orElse(null);

    return new CliArguments(mode, modeArguments);
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
      case "reopen":
        return Mode.REOPEN;
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

  private static abstract class SimpleArguments {
    private final List<Task.Id> tasks;

    private SimpleArguments(List<Task.Id> tasks) {
      this.tasks = tasks;
    }

    public List<Task.Id> tasks() {
      return tasks;
    }

    private static <T extends SimpleArguments> T parse(
        String[] args, Function<List<Task.Id>, T> constructor) {
      return parseFrom(tryParse(args, new Options()), constructor);
    }

    private static <T extends SimpleArguments> T parseFrom(
        CommandLine commandLine, Function<List<Task.Id>, T> constructor) {
      /*
      1st param assumed to be "remove" or an alias for it.
      2nd+ params must be task IDs
      */

      List<String> argsList = List.masking(commandLine.getArgList());
      if (argsList.count() < 2) {
        throw new ArgumentFormatException("No task IDs specified");
      }

      List<Task.Id> taskIds = parseTaskIds(argsList.stream().skip(1).collect(toList()));

      return constructor.apply(taskIds);
    }
  }

  public static final class HelpArguments {
    private HelpArguments() {}

    private static HelpArguments parse(String[] args) {
      /*
      First arg is assumed to be "help" or an alias thereof
      No other unclassified args allowed
      */
      CommandLine commandLine = tryParse(args, new Options());

      assertNoExtraArgs(commandLine);

      return new HelpArguments();
    }
  }

  public static final class ListArguments {
    private final boolean isAllSet;

    private ListArguments(boolean isAllSet) {
      this.isAllSet = isAllSet;
    }

    public boolean isAllSet() {
      return isAllSet;
    }

    private static ListArguments parse(String[] args) {
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

  public static final class InfoArguments extends SimpleArguments {
    private InfoArguments(List<Task.Id> taskIds) {
      super(taskIds);
    }

    private static InfoArguments parse(String[] args) {
      return SimpleArguments.parse(args, InfoArguments::new);
    }
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

    private static AddArguments parse(String[] args) {
      /*
      1st param assumed to be "add" or an alias for it
      2nd param must be description
      3+ params not supported
      optional befores
      optional afters
      */

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

      CommandLine commandLine = tryParse(args, options);

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

  public static final class RemoveArguments extends SimpleArguments {
    private RemoveArguments(List<Task.Id> tasks) {
      super(tasks);
    }

    private static RemoveArguments parse(String[] args) {
      return SimpleArguments.parse(args, RemoveArguments::new);
    }
  }

  public static final class AmendArguments {
    private final Optional<String> description;
    private final List<Task.Id> blockingTasks;
    private final List<Task.Id> blockingTasksToAdd;
    private final List<Task.Id> blockingTasksToRemove;
    private final List<Task.Id> blockedTasks;
    private final List<Task.Id> blockedTasksToAdd;
    private final List<Task.Id> blockedTasksToRemove;

    private AmendArguments(
        Optional<String> description,
        List<Task.Id> blockingTasks,
        List<Task.Id> blockingTasksToAdd,
        List<Task.Id> blockingTasksToRemove,
        List<Task.Id> blockedTasks,
        List<Task.Id> blockedTasksToAdd,
        List<Task.Id> blockedTasksToRemove) {
      this.description = description;
      this.blockingTasks = blockingTasks;
      this.blockingTasksToAdd = blockingTasksToAdd;
      this.blockingTasksToRemove = blockingTasksToRemove;
      this.blockedTasks = blockedTasks;
      this.blockedTasksToAdd = blockedTasksToAdd;
      this.blockedTasksToRemove = blockedTasksToRemove;
    }

    public Optional<String> description() {
      return description;
    }

    public List<Task.Id> blockingTasks() {
      return blockingTasks;
    }

    public List<Task.Id> blockingTasksToAdd() {
      return blockingTasksToAdd;
    }

    public List<Task.Id> blockingTasksToRemove() {
      return blockingTasksToRemove;
    }

    public List<Task.Id> blockedTasks() {
      return blockedTasks;
    }

    public List<Task.Id> blockedTasksToAdd() {
      return blockedTasksToAdd;
    }

    public List<Task.Id> blockedTasksToRemove() {
      return blockedTasksToRemove;
    }

    private static AmendArguments parse(String[] args) {
      /*
      1st param assumed to be "amend" or an alias for it
      2nd param must a task ID
      3+ params not supported
      optional description replacement
      optional after=, after+=, and after-=. after= cannot be used with after+= and after-=.
      optional before=, before+=, and before-=. before= cannot be used with before+= and before-=.
      */
      Options options = new Options();
      options.addOption(
          Option.builder("m")
              .longOpt("description")
              .desc("Edit the description of the task. Leave blank to open in an editor")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());
      options.addOption(
          Option.builder("a")
              .longOpt("after")
              .desc("Sets this task as blocked by another task. Clears the previous blocking tasks.")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());
      options.addOption(
          Option.builder("aa")
              .longOpt("addafter")
              .desc("Adds another task as blocking this one.")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());
      options.addOption(
          Option.builder("ra")
              .longOpt("rmafter")
              .desc("Removes another task as blocking this one.")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());
      options.addOption(
          Option.builder("b")
              .longOpt("before")
              .desc("Sets this task as blocking another task. Clears the previous blocked tasks.")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());
      options.addOption(
          Option.builder("ab")
              .longOpt("addbefore")
              .desc("Adds another task as being blocked by this one.")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());
      options.addOption(
          Option.builder("rb")
              .longOpt("rmbefore")
              .desc("Removes another task as being blocked by this one.")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());

      CommandLine commandLine = tryParse(args, options);

      List<String> argsList = List.masking(commandLine.getArgList());
      if (argsList.count() < 2) {
        throw new ArgumentFormatException("Task not specified");
      }
      if (argsList.count() > 2) {
        throw new ArgumentFormatException("Unexpected extra arguments");
      }

      Optional<String> description1 = getSingleOptionValue(commandLine, "m");
      List<Task.Id> afterTasks = parseTaskIds(getOptionValues(commandLine, "a"));
      List<Task.Id> afterTasksToAdd = parseTaskIds(getOptionValues(commandLine, "aa"));
      List<Task.Id> afterTasksToRemove = parseTaskIds(getOptionValues(commandLine, "ra"));
      List<Task.Id> beforeTasks = parseTaskIds(getOptionValues(commandLine, "b"));
      List<Task.Id> beforeTasksToAdd = parseTaskIds(getOptionValues(commandLine, "ab"));
      List<Task.Id> beforeTasksToRemove = parseTaskIds(getOptionValues(commandLine, "rb"));

      if (afterTasks.isPopulated()
          && (afterTasksToAdd.isPopulated() || afterTasksToRemove.isPopulated())) {
        throw new ArgumentFormatException(
            "--after cannot be use with --addafter or --rmafter");
      }

      if (beforeTasks.isPopulated()
          && (beforeTasksToAdd.isPopulated() || beforeTasksToRemove.isPopulated())) {
        throw new ArgumentFormatException(
            "--before cannot be use with --addbefore or --rmbefore");
      }

      if (!description1.isPresent()
          && !afterTasks.isPopulated()
          && !afterTasksToAdd.isPopulated()
          && !afterTasksToRemove.isPopulated()
          && !beforeTasks.isPopulated()
          && !beforeTasksToAdd.isPopulated()
          && !beforeTasksToRemove.isPopulated()) {
        throw new ArgumentFormatException("Nothing to amend");
      }

      return new AmendArguments(
          description1,
          afterTasks,
          afterTasksToAdd,
          afterTasksToRemove,
          beforeTasks,
          beforeTasksToAdd,
          beforeTasksToRemove);
    }
  }

  public static final class CompleteArguments extends SimpleArguments {
    private CompleteArguments(List<Task.Id> tasks) {
      super(tasks);
    }

    private static CompleteArguments parse(String[] args) {
      return SimpleArguments.parse(args, CompleteArguments::new);
    }
  }

  public static final class ReopenArguments extends SimpleArguments {
    private ReopenArguments(List<Task.Id> tasks) {
      super(tasks);
    }

    private static ReopenArguments parse(String[] args) {
      return SimpleArguments.parse(args, ReopenArguments::new);
    }
  }

  private static List<Task.Id> parseTaskIds(List<String> taskStrings) {
    List<Task.Id> taskIds;
    try {
      taskIds = taskStrings.stream().map(Task.Id::parse).collect(toList());
    } catch (IdFormatException ex) {
      throw new ArgumentFormatException("Invalid task ID", ex);
    }
    return taskIds;
  }

  private static List<String> getOptionValues(CommandLine commandLine, String opt) {
    return ArrayList.of(
        Optional.ofNullable(requireNonNull(commandLine).getOptionValues(requireNonNull(opt)))
            .orElse(new String[0]));
  }

  private static Optional<String> getSingleOptionValue(CommandLine commandLine, String opt) {
    if (commandLine.getOptionValues(opt).length > 1) {
      throw new ArgumentFormatException("Too many values provided for parameter '" + opt + "'");
    }
    return Optional.ofNullable(commandLine.getOptionValue(opt));
  }

  private static void assertNoExtraArgs(CommandLine commandLine) {
    if (commandLine.getArgList().size() > 1) {
      String unexpectedArgs =
          commandLine.getArgList()
              .stream()
              .skip(1)
              .map(s -> "\"" + s + "\"")
              .collect(joining(", "));

      throw new ArgumentFormatException("Unexpected arguments given: " + unexpectedArgs);
    }
  }
}

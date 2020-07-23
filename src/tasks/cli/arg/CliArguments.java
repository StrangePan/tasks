package tasks.cli.arg;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static omnia.data.stream.Collectors.toImmutableMap;
import static omnia.data.stream.Collectors.toImmutableSet;

import io.reactivex.Single;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.Pair;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableMap;
import tasks.model.TaskStore;

/** Data structure for arguments passed into the command line. */
public final class CliArguments {

  private static final boolean REPEATABLE = true;
  private static final boolean NOT_REPEATABLE = false;

  private static Collection<CommandRegistration> createCommandModeRegistry(
      Memoized<TaskStore> taskStore, Memoized<Set<String>> validModes) {
    return new RegistryBuilder()
        .register(
            CommandRegistration.builder()
                .cliMode(CliMode.HELP)
                .canonicalName("help")
                .aliases()
                .parameters(ImmutableList.of(new StringParameter(NOT_REPEATABLE)))
                .options(ImmutableList.empty())
                .parser(() -> new HelpArguments.Parser(validModes))
                .helpDocumentation("Retrieve the help documentation for a specific command."))
        .register(
            CommandRegistration.builder()
                .cliMode(CliMode.LIST)
                .canonicalName("list")
                .aliases("ls", "l")
                .parameters(ImmutableList.empty())
                .options(
                    ImmutableList.of(
                        new FlagOption(
                            "blocked",
                            "b",
                            "Setting this flag lists tasks that are uncompleted, but blocked by "
                                + "other tasks",
                            NOT_REPEATABLE),
                        new FlagOption(
                            "completed",
                            "c",
                            "Setting this flag lists tasks that are already marked as completed.",
                            NOT_REPEATABLE),
                        new FlagOption(
                            "unblocked",
                            "u",
                            "Setting this flag lists tasks that are unblocked. This is the "
                                + "default.",
                            NOT_REPEATABLE),
                        new FlagOption(
                            "all",
                            "a",
                            "Setting this flag lists all tags.",
                            NOT_REPEATABLE)))
                .parser(() -> ListArguments::parse)
                .helpDocumentation(
                    "List all tasks that are currently unblocked. Can be configured with flags to "
                        + "list blocked tasks, completed tasks, or all tasks."))
        .register(
            CommandRegistration.builder()
                .cliMode(CliMode.INFO)
                .canonicalName("info")
                .aliases("i")
                .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
                .options(ImmutableList.empty())
                .parser(() -> new InfoArguments.Parser(taskStore))
                .helpDocumentation("Lists "))
        .register(
            CommandRegistration.builder()
                .cliMode(CliMode.ADD)
                .canonicalName("add")
                .aliases()
                .parameters(ImmutableList.of(new StringParameter(NOT_REPEATABLE)))
                .options(
                    ImmutableList.of(
                        new TaskOption(
                            "after",
                            "a",
                            "The tasks this one comes after. This list empty tasks will be "
                                + "blocking this task.",
                            REPEATABLE),
                        new TaskOption(
                            "before",
                            "b",
                            "The tasks this one comes before. This list empty tasks will be "
                                + "unblocked by this task.",
                            REPEATABLE)))
                .parser(() -> new AddArguments.Parser(taskStore))
                .helpDocumentation("Create a new task."))
        .register(
            CommandRegistration.builder()
                .cliMode(CliMode.REMOVE)
                .canonicalName("remove")
                .aliases("rm")
                .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
                .options(ImmutableList.empty())
                .parser(() -> new RemoveArguments.Parser(taskStore))
                .helpDocumentation(
                    "Delete a task altogether. THIS CANNOT BE UNDONE. It is recommended that tasks "
                        + "be marked as completed rather than deleted, or amended if their content "
                        + "needs to change."))
        .register(
            CommandRegistration.builder()
                .cliMode(CliMode.AMEND)
                .canonicalName("amend")
                .aliases()
                .parameters(ImmutableList.of(new TaskParameter(NOT_REPEATABLE)))
                .options(
                    ImmutableList.of(
                        new StringOption(
                            "description",
                            "m",
                            "Edit the task label.",
                            NOT_REPEATABLE),
                        new TaskOption(
                            "after",
                            "a",
                            "Sets this task as blocked by another task. Clears the previous "
                                + "blocking tasks.",
                            REPEATABLE),
                        new TaskOption(
                            "addafter",
                            "aa",
                            "Adds another task as blocking this one.",
                            REPEATABLE),
                        new TaskOption(
                            "rmafter",
                            "ra",
                            "Removes another task as blocking this one.",
                            REPEATABLE),
                        new TaskOption(
                            "before",
                            "b",
                            "Sets this task as blocking another task. Clears the previous "
                                + "blocked tasks.",
                            REPEATABLE),
                        new TaskOption(
                            "addbefore",
                            "ab",
                            "Adds another task as being blocked by this one.",
                            REPEATABLE),
                        new TaskOption(
                            "rmbefore",
                            "rb",
                            "Removes another task as being blocked by this one.",
                            REPEATABLE)))
                .parser(() -> new AmendArguments.Parser(taskStore))
                .helpDocumentation(
                    "Makes changes to an existing task. Can be used to change the task "
                        + "description, or to add/remove blocking/blocked tasks."))
        .register(
            CommandRegistration.builder()
                .cliMode(CliMode.COMPLETE)
                .canonicalName("complete")
                .aliases()
                .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
                .options(ImmutableList.empty())
                .parser(() -> new CompleteArguments.Parser(taskStore))
                .helpDocumentation(
                    "Mark one or more tasks as complete. This can be undone with the reopen "
                        + "command."))
        .register(
            CommandRegistration.builder()
                .cliMode(CliMode.REOPEN)
                .canonicalName("reopen")
                .aliases()
                .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
                .options(ImmutableList.empty())
                .parser(() -> new ReopenArguments.Parser(taskStore))
                .helpDocumentation(
                    "Reopens one or more completed tasks. This can be undone with the complete "
                        + "command."))
        .build();
  }

  private final Collection<CommandRegistration> registrations;
  private final Map<String, CommandRegistration> registrationsIndexedByAliases;
  private final CommandRegistration fallback;

  public CliArguments(Memoized<TaskStore> taskStore) {
    registrations = createCommandModeRegistry(taskStore, memoize(this::modeNamesAndAliases));

    registrationsIndexedByAliases =
        registrations.stream()
            .flatMap(
                registration ->
                    registration.canonicalNameAndAliases()
                        .stream()
                        .map(alias -> Pair.of(alias, registration)))
            .collect(toImmutableMap());

    //noinspection OptionalGetWithoutIsPresent
    this.fallback =
        registrations.stream()
            .filter(registration -> registration.cliMode() == CliMode.HELP)
            .findFirst()
            .get();
  }

  private Set<String> modeNamesAndAliases() {
    return registrationsIndexedByAliases.keys();
  }

  public Object parse(String[] args) {
    // Parameter validation
    requireNonNull(args);
    if (Arrays.stream(args).anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("arg cannot contain null");
    }

    return Single.just(
        Pair.of(
            args,
            Optional.of(args)
                .filter(a -> a.length > 0)
                .map(a -> a[0])
                .flatMap(this::registrationFromArgument)))
        .map(
            pair -> pair.second().isPresent()
                ? pair.map(Function.identity(), Optional::get)
                : Pair.of(new String[0], fallback))
        .map(pair -> pair.second().parserSupplier().parse(pair.first()))
        .blockingGet();
  }

  private Optional<CommandRegistration> registrationFromArgument(String arg) {
    return registrationsIndexedByAliases.valueOf(arg);
  }

  public Set<CommandDocumentation> commandDocumentation() {
    return registrations.stream()
        .map(CliArguments::toCommandDocumentation)
        .collect(toImmutableSet());
  }

  private static CommandDocumentation toCommandDocumentation(CommandRegistration registration) {
    return new CommandDocumentation(
        registration.canonicalName(),
        ImmutableList.copyOf(registration.aliases()),
        registration.description(),
        registration.options().stream()
            .map(CliArguments::toOptionDocumentation)
            .collect(toImmutableSet()));
  }

  private static CommandDocumentation.OptionDocumentation toOptionDocumentation(Option option) {
    return new CommandDocumentation.OptionDocumentation(
        option.longName(),
        option.shortName(),
        option.description(),
        option.isRepeatable());
  }

  public static final class ArgumentFormatException extends RuntimeException {
    ArgumentFormatException(String reason) {
      super(reason);
    }

    ArgumentFormatException(String reason, Throwable cause) {
      super(reason, cause);
    }
  }

  interface Parser<T> {
    T parse(String[] args);
  }

  private static final class RegistryBuilder {
    private final MutableMap<CliMode, CommandRegistration> registeredHandlers = HashMap.create();

     RegistryBuilder register(CommandRegistration registration) {
       requireNonNull(registration);
       requireUnique(registration.cliMode());
       registeredHandlers.putMapping(registration.cliMode(), registration);
      return this;
    }

    private void requireUnique(CliMode mode) {
      if (registeredHandlers.keys().contains(mode)) {
        throw new IllegalStateException("Duplication registration for " + mode);
      }
    }

    Set<CommandRegistration> build() {
      return ImmutableSet.copyOf(registeredHandlers.values());
    }
  }

  private static final class CommandRegistration {
    private final CliMode cliMode;
    private final String canonicalName;
    private final Collection<String> aliases;
    private final String description;
    private final Collection<Parameter> parameters;
    private final Collection<Option> options;
    private final Memoized<Parser<?>> parser;

    private CommandRegistration(
        CliMode cliMode,
        String canonicalName,
        Collection<String> aliases,
        String description,
        Collection<Parameter> parameters,
        Collection<Option> options,
        Supplier<? extends Parser<?>> parserSupplier) {
      requireNonNull(cliMode);
      requireNonNull(canonicalName);
      requireNonNull(aliases);
      requireNonNull(description);
      requireNonNull(parameters);
      requireNonNull(options);
      requireNonNull(parserSupplier);

      if (aliases.contains(canonicalName)) {
        throw new IllegalArgumentException("aliases cannot contain the canonical name");
      }
      if (ImmutableSet.copyOf(aliases).count() < aliases.count()) {
        throw new IllegalArgumentException("aliases cannot contain duplicates: " + aliases);
      }

      this.cliMode = cliMode;
      this.canonicalName = canonicalName;
      this.aliases = ImmutableList.copyOf(aliases);
      this.description = description;
      this.parameters = parameters;
      this.options = ImmutableList.copyOf(options);
      this.parser = memoize(parserSupplier);
    }

    CliMode cliMode() {
      return cliMode;
    }

    String canonicalName() {
      return canonicalName;
    }

    Collection<String> aliases() {
      return aliases;
    }

    String description() {
      return description;
    }

    Collection<Parameter> parameters() {
      return parameters;
    }

    Collection<Option> options() {
      return options;
    }

    List<String> canonicalNameAndAliases() {
      return ImmutableList.<String>builder().add(canonicalName).addAll(aliases).build();
    }

    Parser<?> parserSupplier() {
      return parser.value();
    }

    interface Builder0 {
      Builder1 cliMode(CliMode cliMode);
    }

    interface Builder1 {
      Builder2 canonicalName(String canonicalName);
    }

    interface Builder2 {
      Builder3 aliases(String...aliases);
    }

    interface Builder3 {
      Builder4 parameters(Collection<Parameter> parameters);
    }

    interface Builder4 {
      Builder5 options(Collection<Option> options);
    }

    interface Builder5 {
      Builder6 parser(Supplier<? extends Parser<?>> parserSupplier);
    }

    interface Builder6 {
      CommandRegistration helpDocumentation(String description);
    }

    static Builder0 builder() {
      return cliMode ->
          (Builder1) canonicalName ->
            (Builder2) aliases ->
                (Builder3) parameters ->
                    (Builder4) arguments ->
                        (Builder5) parserSupplier ->
                            (Builder6) description ->
                                new CommandRegistration(
                                    cliMode,
                                    canonicalName,
                                    ImmutableList.copyOf(aliases),
                                    description,
                                    parameters,
                                    arguments,
                                    parserSupplier);
    }
  }

  private static class Option {
    private final String longName;
    private final String shortName;
    private final String description;
    private final boolean repeatable;

    Option(String longName, String shortName, String description, boolean repeatable) {
      this.longName = longName;
      this.shortName = shortName;
      this.description = description;
      this.repeatable = repeatable;
    }

    String longName() {
      return longName;
    }

    String shortName() {
      return shortName;
    }

    String description() {
      return description;
    }

    boolean isRepeatable() {
      return repeatable;
    }
  }

  private static class TaskOption extends Option {
    TaskOption(String longName, String shortName, String description, boolean repeatable) {
      super(longName, shortName, description, repeatable);
    }
  }

  private static class StringOption extends Option {
    StringOption(String longName, String shortName, String description, boolean repeatable) {
      super(longName, shortName, description, repeatable);
    }
  }

  private static class FlagOption extends Option {
    FlagOption(String longName, String shortName, String description, boolean repeatable) {
      super(longName, shortName, description, repeatable);
    }
  }

  private static class Parameter {
    private final boolean repeatable;

    Parameter(boolean repeatable) {
      this.repeatable = repeatable;
    }

    boolean isRepeatable() {
      return repeatable;
    }
  }

  private static class TaskParameter extends Parameter {
    TaskParameter(boolean repeatable) {
      super(repeatable);
    }
  }

  private static class StringParameter extends Parameter {
    StringParameter(boolean repeatable) {
      super(repeatable);
    }
  }
}

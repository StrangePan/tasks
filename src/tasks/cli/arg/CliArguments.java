package tasks.cli.arg;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static omnia.data.stream.Collectors.toImmutableMap;
import static omnia.data.stream.Collectors.toImmutableSet;

import io.reactivex.Single;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import omnia.algorithm.ListAlgorithms;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableMap;
import omnia.data.structure.tuple.Tuple;
import org.apache.commons.cli.CommandLine;
import tasks.cli.command.add.AddCommand;
import tasks.cli.command.blockers.BlockersCommand;
import tasks.cli.command.complete.CompleteCommand;
import tasks.cli.command.help.HelpCommand;
import tasks.cli.command.info.InfoCommand;
import tasks.cli.command.list.ListCommand;
import tasks.cli.command.remove.RemoveCommand;
import tasks.cli.command.reopen.ReopenCommand;
import tasks.cli.command.reword.RewordCommand;
import tasks.model.Task;
import tasks.model.TaskStore;

/** Data structure for arguments passed into the command line. */
public final class CliArguments {

  private static Collection<CommandRegistration> createCommandModeRegistry(
      Memoized<Set<String>> validModes,
      Memoized<Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return new RegistryBuilder()
        .register(AddCommand.registration(taskParser))
        .register(BlockersCommand.registration(taskParser))
        .register(CompleteCommand.registration(taskParser))
        .register(HelpCommand.registration(validModes))
        .register(InfoCommand.registration(taskParser))
        .register(ListCommand.registration())
        .register(RemoveCommand.registration(taskParser))
        .register(ReopenCommand.registration(taskParser))
        .register(RewordCommand.registration(taskParser))
        .build();
  }

  private final Collection<CommandRegistration> registrations;
  private final Map<String, CommandRegistration> registrationsIndexedByAliases;
  private final CommandRegistration fallback;

  public CliArguments(Memoized<TaskStore> taskStore) {
    registrations =
        createCommandModeRegistry(
            memoize(this::modeNamesAndAliases),
            memoize(() -> CliUtils.taskListParser(taskStore)));

    registrationsIndexedByAliases =
        registrations.stream()
            .flatMap(
                registration ->
                    registration.canonicalNameAndAliases()
                        .stream()
                        .map(alias -> Tuple.of(alias, registration)))
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

  public Object parse(List<? extends String> args) {
    // Parameter validation
    requireNonNull(args);

    return Single.just(
        Tuple.<List<? extends String>, Optional<CommandRegistration>>of(
            ListAlgorithms.sublistOf(args, 1, args.count()),
            args.stream().findFirst().flatMap(this::registrationFromArgument)))
        .map(
            couple -> couple.second().isPresent()
                ? couple.mapSecond(Optional::get)
                : Tuple.of(ImmutableList.<String>empty(), fallback))
        .map(couple -> couple.mapFirst(first -> CliUtils.tryParse(first, CliUtils.toOptions(couple.second().options()))))
        .map(couple -> couple.second().commandParserSupplier().parse(couple.first()))
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
        toParameterRepresentation(registration),
        registration.description(),
        registration.options().stream()
            .map(CliArguments::toOptionDocumentation)
            .collect(toImmutableSet()));
  }

  private static Optional<String> toParameterRepresentation(CommandRegistration registration) {
    return registration.parameters().isPopulated()
        ? Optional.of(
            registration.parameters().stream()
              .map(CliArguments::toParameterRepresentation)
              .collect(Collectors.joining(" ")))
        : Optional.empty();
  }

  private static String toParameterRepresentation(Parameter parameter) {
    return "<" + parameter.description() + (parameter.isRepeatable() ? "..." : "") + ">";
  }

  private static Optional<String> toParameterRepresentation(Option option) {
    return option.parameterRepresentation().map(rep -> "<" + rep + ">");
  }

  private static CommandDocumentation.OptionDocumentation toOptionDocumentation(Option option) {
    return new CommandDocumentation.OptionDocumentation(
        option.longName(),
        option.shortName(),
        option.description(),
        option.isRepeatable(),
        toParameterRepresentation(option));
  }

  public static final class ArgumentFormatException extends RuntimeException {
    public ArgumentFormatException(String reason) {
      super(reason);
    }

    ArgumentFormatException(String reason, Throwable cause) {
      super(reason, cause);
    }
  }

  public interface Parser<T> {
    T parse(List<? extends String> commandLine);
  }

  public interface CommandParser<T> {
    T parse(CommandLine commandLine);
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

  public static final class CommandRegistration {
    private final CliMode cliMode;
    private final String canonicalName;
    private final Collection<String> aliases;
    private final String description;
    private final Collection<Parameter> parameters;
    private final Collection<Option> options;
    private final Memoized<CommandParser<?>> parser;

    private CommandRegistration(
        CliMode cliMode,
        String canonicalName,
        Collection<String> aliases,
        String description,
        Collection<Parameter> parameters,
        Collection<Option> options,
        Supplier<? extends CommandParser<?>> commandParserSupplier) {
      requireNonNull(cliMode);
      requireNonNull(canonicalName);
      requireNonNull(aliases);
      requireNonNull(description);
      requireNonNull(parameters);
      requireNonNull(options);
      requireNonNull(commandParserSupplier);

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
      this.parser = memoize(commandParserSupplier);
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

    CommandParser<?> commandParserSupplier() {
      return parser.value();
    }

    public interface Builder0 {
      Builder1 cliMode(CliMode cliMode);
    }

    public interface Builder1 {
      Builder2 canonicalName(String canonicalName);
    }

    public interface Builder2 {
      Builder3 aliases(String...aliases);
    }

    public interface Builder3 {
      Builder4 parameters(Collection<Parameter> parameters);
    }

    public interface Builder4 {
      Builder5 options(Collection<Option> options);
    }

    public interface Builder5 {
      Builder6 parser(Supplier<? extends CommandParser<?>> commandParserSupplier);
    }

    public interface Builder6 {
      CommandRegistration helpDocumentation(String description);
    }

    public static Builder0 builder() {
      return cliMode ->
          (Builder1) canonicalName ->
            (Builder2) aliases ->
                (Builder3) parameters ->
                    (Builder4) arguments ->
                        (Builder5) commandParserSupplier ->
                            (Builder6) description ->
                                new CommandRegistration(
                                    cliMode,
                                    canonicalName,
                                    ImmutableList.copyOf(aliases),
                                    description,
                                    parameters,
                                    arguments,
                                    commandParserSupplier);
    }
  }

  public abstract static class Option {
    private final String longName;
    private final String shortName;
    private final String description;
    private final Parameter.Repeatable repeatable;
    private final Optional<String> parameterRepresentation;

    Option(
        String longName,
        String shortName,
        String description,
        Parameter.Repeatable repeatable,
        Optional<String> parameterRepresentation) {
      this.longName = requireNonNull(longName);
      this.shortName = requireNonNull(shortName);
      this.description = requireNonNull(description);
      this.repeatable = repeatable;
      this.parameterRepresentation = requireNonNull(parameterRepresentation);
    }

    public String longName() {
      return longName;
    }

    public String shortName() {
      return shortName;
    }

    public String description() {
      return description;
    }

    public boolean isRepeatable() {
      return repeatable == Parameter.Repeatable.REPEATABLE;
    }

    public Optional<String> parameterRepresentation() {
      return parameterRepresentation;
    }

    public abstract org.apache.commons.cli.Option toCliOption();
  }

  public static class TaskOption extends Option {
    public TaskOption(String longName, String shortName, String description, Parameter.Repeatable repeatable) {
      super(longName, shortName, description, repeatable, Optional.of("task"));
    }

    @Override
    public org.apache.commons.cli.Option toCliOption() {
      return org.apache.commons.cli.Option.builder(shortName())
          .longOpt(longName())
          .desc(description())
          .optionalArg(false)
          .numberOfArgs(1)
          .build();
    }
  }

  public static class StringOption extends Option {
    public StringOption(
        String longName,
        String shortName,
        String description,
        Parameter.Repeatable repeatable,
        String semanticDescription) {
      super(longName, shortName, description, repeatable, Optional.of(semanticDescription));
    }

    @Override
    public org.apache.commons.cli.Option toCliOption() {
      return org.apache.commons.cli.Option.builder(shortName())
          .longOpt(longName())
          .desc(description())
          .optionalArg(false)
          .numberOfArgs(1)
          .build();
    }
  }

  public static class FlagOption extends Option {
    public FlagOption(String longName, String shortName, String description, Parameter.Repeatable repeatable) {
      super(longName, shortName, description, repeatable, Optional.empty());
    }

    @Override
    public org.apache.commons.cli.Option toCliOption() {
      return org.apache.commons.cli.Option.builder(shortName())
          .longOpt(longName())
          .desc(description())
          .numberOfArgs(0)
          .build();
    }
  }

  public abstract static class Parameter {
    private final String description;
    private final Repeatable repeatable;

    Parameter(String description, Repeatable repeatable) {
      this.description = requireNonNull(description);
      this.repeatable = repeatable;
    }

    String description() {
      return description;
    }

    public boolean isRepeatable() {
      return repeatable == Repeatable.REPEATABLE;
    }

    public enum Repeatable {
      REPEATABLE,
      NOT_REPEATABLE,
    }
  }

  public static class TaskParameter extends Parameter {
    public TaskParameter(Repeatable repeatable) {
      super("task", repeatable);
    }
  }

  public static class StringParameter extends Parameter {
    public StringParameter(String semanticName, Repeatable repeatable) {
      super(semanticName, repeatable);
    }
  }
}

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
import java.util.stream.Collectors;
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
import tasks.cli.command.add.AddArguments;
import tasks.cli.command.amend.AmendArguments;
import tasks.cli.command.complete.CompleteArguments;
import tasks.cli.command.help.HelpArguments;
import tasks.cli.command.info.InfoArguments;
import tasks.cli.command.list.ListArguments;
import tasks.cli.command.remove.RemoveArguments;
import tasks.cli.command.reopen.ReopenArguments;
import tasks.model.TaskStore;

/** Data structure for arguments passed into the command line. */
public final class CliArguments {

  private static Collection<CommandRegistration> createCommandModeRegistry(
      Memoized<TaskStore> taskStore, Memoized<Set<String>> validModes) {
    return new RegistryBuilder()
        .register(AddArguments.registration(taskStore))
        .register(AmendArguments.registration(taskStore))
        .register(CompleteArguments.registration(taskStore))
        .register(HelpArguments.registration(validModes))
        .register(InfoArguments.registration(taskStore))
        .register(ListArguments.registration())
        .register(RemoveArguments.registration(taskStore))
        .register(ReopenArguments.registration(taskStore))
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

  public static final class CommandRegistration {
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
      Builder6 parser(Supplier<? extends Parser<?>> parserSupplier);
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
      return repeatable == Parameter.Repeatable.REPEATABLE;
    }

    Optional<String> parameterRepresentation() {
      return parameterRepresentation;
    }
  }

  public static class TaskOption extends Option {
    public TaskOption(String longName, String shortName, String description, Parameter.Repeatable repeatable) {
      super(longName, shortName, description, repeatable, Optional.of("task"));
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
  }

  public static class FlagOption extends Option {
    public FlagOption(String longName, String shortName, String description, Parameter.Repeatable repeatable) {
      super(longName, shortName, description, repeatable, Optional.empty());
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

    boolean isRepeatable() {
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

package tasks.cli.arg;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static omnia.data.stream.Collectors.toImmutableList;
import static omnia.data.stream.Collectors.toImmutableMap;
import static omnia.data.stream.Collectors.toImmutableSet;

import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Optional;
import java.util.stream.Collectors;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableMap;
import omnia.data.structure.tuple.Couple;
import omnia.data.structure.tuple.Tuple;
import org.apache.commons.cli.CommandLine;
import tasks.cli.arg.registration.CommandRegistration;
import tasks.cli.arg.registration.Option;
import tasks.cli.arg.registration.Parameter;
import tasks.cli.command.add.AddCommand;
import tasks.cli.command.blockers.BlockersCommand;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.command.common.CommonOptions;
import tasks.cli.command.common.CommonParser;
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
      Memoized<Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return new RegistryBuilder()
        .register(AddCommand.registration(taskParser))
        .register(BlockersCommand.registration(taskParser))
        .register(CompleteCommand.registration(taskParser))
        .register(HelpCommand.registration())
        .register(InfoCommand.registration(taskParser))
        .register(ListCommand.registration())
        .register(RemoveCommand.registration(taskParser))
        .register(ReopenCommand.registration(taskParser))
        .register(RewordCommand.registration(taskParser))
        .build();
  }

  private final CommonParser commonArgumentsParser = new CommonParser();
  private final CommandRegistration fallback = HelpCommand.registration();

  private final Collection<CommandRegistration> registrations;
  private final Map<String, CommandRegistration> registrationsIndexedByAliases;

  public CliArguments(Memoized<TaskStore> taskStore) {
    registrations = createCommandModeRegistry(memoize(() -> CliUtils.taskListParser(taskStore)));

    registrationsIndexedByAliases =
        registrations.stream()
            .flatMap(
                registration ->
                    registration.canonicalNameAndAliases()
                        .stream()
                        .map(alias -> Tuple.of(alias, registration)))
            .collect(toImmutableMap());
  }

  public CommonArguments<?> parse(List<? extends String> args) {
    requireNonNull(args);
    return Single.just(argsAndOptionalRegistration(args))
        .map(this::resolveRegistrationOrUseFallback)
        .map(CliArguments::parseToCommandLine)
        .map(this::parseToArguments)
        .blockingGet();
  }

  private Couple<List<? extends String>, Optional<CommandRegistration>> argsAndOptionalRegistration(
      List<? extends String> args) {
    return Tuple.of(
        args.stream().skip(1).collect(toImmutableList()),
        args.stream().findFirst().flatMap(this::registrationFromArgument));
  }

  private Couple<List<? extends String>, CommandRegistration> resolveRegistrationOrUseFallback(
      Couple<List<? extends String>, Optional<CommandRegistration>> argsAndOptionalRegistration) {
    return argsAndOptionalRegistration.second().isPresent()
        ? argsAndOptionalRegistration.mapSecond(Optional::get)
        : Tuple.of(ImmutableList.empty(), fallback);
  }

  private static Couple<CommandLine, CommandRegistration> parseToCommandLine(Couple<List<? extends String>, CommandRegistration> argsAndCommand) {
    Collection<Option> commonAndSpecificOptions =
        ImmutableSet.<Option>builder()
            .addAll(argsAndCommand.second().options())
            .addAll(CommonOptions.OPTIONS.value())
            .build();
    return argsAndCommand
        .mapFirst(first -> CliUtils.tryParse(first, CliUtils.toOptions(commonAndSpecificOptions)));
  }

  private CommonArguments<?> parseToArguments(
      Couple<CommandLine, CommandRegistration> commandLineAndRegistration) {
    return commonArgumentsParser.parse(
        commandLineAndRegistration.first(),
        commandLineAndRegistration
            .second()
            .commandParserSupplier()
            .parse(commandLineAndRegistration.first()));
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

  public static CommandDocumentation.OptionDocumentation toOptionDocumentation(Option option) {
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

  private static final class RegistryBuilder {
    private final ImmutableSet.Builder<CommandRegistration> registeredCommands =
        ImmutableSet.builder();

    RegistryBuilder register(CommandRegistration registration) {
      requireNonNull(registration);
      registeredCommands.add(registration);
      return this;
    }

    Set<CommandRegistration> build() {
      Set<CommandRegistration> registeredCommands = this.registeredCommands.build();
      requireNamesAndAliasesAreUnique(registeredCommands);
      return registeredCommands;
    }

    private static void requireNamesAndAliasesAreUnique(Set<CommandRegistration> commands) {
      List<String> duplicates =
          Observable.fromIterable(commands)
              .flatMap(
                  command ->
                      Observable.just(command.canonicalName())
                          .concatWith(Observable.fromIterable(command.aliases())))
              .groupBy(name -> name)
              .flatMapMaybe(names -> names.skip(1).reduce((original, duplicate) -> original))
              .sorted(String::compareToIgnoreCase)
              .<ImmutableList.Builder<String>>collectInto(
                  ImmutableList.builder(), ImmutableList.Builder::add)
              .map(ImmutableList.Builder::build)
              .blockingGet();

      if (duplicates.isPopulated()) {
        throw new IllegalStateException(
            "two or more commands use these aliases: "
                + duplicates.stream().collect(Collectors.joining(",", "[", "]")));
      }
    }
  }

}

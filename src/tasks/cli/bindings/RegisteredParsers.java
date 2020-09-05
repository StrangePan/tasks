package tasks.cli.bindings;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static omnia.data.stream.Collectors.toImmutableList;
import static omnia.data.stream.Collectors.toImmutableMap;
import static omnia.data.stream.Collectors.toImmutableSet;

import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Optional;
import java.util.stream.Collectors;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.tuple.Couple;
import omnia.data.structure.tuple.Tuple;
import org.apache.commons.cli.CommandLine;
import tasks.cli.command.Command;
import tasks.cli.command.Option;
import tasks.cli.command.Parameter;
import tasks.cli.parser.ParserUtil;
import tasks.cli.parser.Parser;
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
public final class RegisteredParsers {

  private static Collection<Command> createCommandModeRegistry(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
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
  private final Command fallback = HelpCommand.registration();

  private final Collection<Command> registrations;
  private final Map<String, Command> registrationsIndexedByAliases;

  public RegisteredParsers(Memoized<TaskStore> taskStore) {
    registrations = createCommandModeRegistry(memoize(() -> ParserUtil.taskListParser(taskStore)));

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
        .map(RegisteredParsers::parseToCommandLine)
        .map(this::parseToArguments)
        .blockingGet();
  }

  private Couple<List<? extends String>, Optional<Command>> argsAndOptionalRegistration(
      List<? extends String> args) {
    return Tuple.of(
        args.stream().skip(1).collect(toImmutableList()),
        args.stream().findFirst().flatMap(this::registrationFromArgument));
  }

  private Couple<List<? extends String>, Command> resolveRegistrationOrUseFallback(
      Couple<List<? extends String>, Optional<Command>> argsAndOptionalRegistration) {
    return argsAndOptionalRegistration.second().isPresent()
        ? argsAndOptionalRegistration.mapSecond(Optional::get)
        : Tuple.of(ImmutableList.empty(), fallback);
  }

  private static Couple<CommandLine, Command> parseToCommandLine(Couple<List<? extends String>, Command> argsAndCommand) {
    Collection<Option> commonAndSpecificOptions =
        ImmutableSet.<Option>builder()
            .addAll(argsAndCommand.second().options())
            .addAll(CommonOptions.OPTIONS.value())
            .build();
    return argsAndCommand
        .mapFirst(first -> ParserUtil.tryParse(first, ParserUtil.toOptions(commonAndSpecificOptions)));
  }

  private CommonArguments<?> parseToArguments(
      Couple<CommandLine, Command> commandLineAndRegistration) {
    return commonArgumentsParser.parse(
        commandLineAndRegistration.first(),
        commandLineAndRegistration
            .second()
            .commandParserSupplier()
            .parse(commandLineAndRegistration.first()));
  }

  private Optional<Command> registrationFromArgument(String arg) {
    return registrationsIndexedByAliases.valueOf(arg);
  }

  public Set<CommandDocumentation> commandDocumentation() {
    return registrations.stream()
        .map(RegisteredParsers::toCommandDocumentation)
        .collect(toImmutableSet());
  }

  private static CommandDocumentation toCommandDocumentation(Command registration) {
    return new CommandDocumentation(
        registration.canonicalName(),
        ImmutableList.copyOf(registration.aliases()),
        toParameterRepresentation(registration),
        registration.description(),
        registration.options().stream()
            .map(RegisteredParsers::toOptionDocumentation)
            .collect(toImmutableSet()));
  }

  private static Optional<String> toParameterRepresentation(Command registration) {
    return registration.parameters().isPopulated()
        ? Optional.of(
            registration.parameters().stream()
              .map(RegisteredParsers::toParameterRepresentation)
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

  private static final class RegistryBuilder {
    private final ImmutableSet.Builder<Command> registeredCommands =
        ImmutableSet.builder();

    RegistryBuilder register(Command registration) {
      requireNonNull(registration);
      registeredCommands.add(registration);
      return this;
    }

    Set<Command> build() {
      Set<Command> registeredCommands = this.registeredCommands.build();
      requireNamesAndAliasesAreUnique(registeredCommands);
      return registeredCommands;
    }

    private static void requireNamesAndAliasesAreUnique(Set<Command> commands) {
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

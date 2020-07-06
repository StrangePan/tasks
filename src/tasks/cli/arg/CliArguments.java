package tasks.cli.arg;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static omnia.data.stream.Collectors.toImmutableMap;
import static omnia.data.stream.Collectors.toImmutableSet;

import java.util.Arrays;
import java.util.Objects;
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

  private static Collection<ModeRegistration> createCommandModeRegistry(
      Memoized<TaskStore> taskStore, Memoized<Set<String>> validModes) {
    return new RegistryBuilder()
        .register(
            ModeRegistration.builder()
                .cliMode(CliMode.HELP)
                .canonicalName("help")
                .aliases("")
                .parser(() -> new HelpArguments.Parser(validModes))
                .helpDocumentation(Output::empty))
        .register(
            ModeRegistration.builder()
                .cliMode(CliMode.LIST)
                .canonicalName("list")
                .aliases("ls", "l")
                .parser(() -> ListArguments::parse)
                .helpDocumentation(Output::empty))
        .register(
            ModeRegistration.builder()
                .cliMode(CliMode.INFO)
                .canonicalName("info")
                .aliases()
                .parser(() -> new InfoArguments.Parser(taskStore))
                .helpDocumentation(Output::empty))
        .register(
            ModeRegistration.builder()
                .cliMode(CliMode.ADD)
                .canonicalName("add")
                .aliases()
                .parser(() -> new AddArguments.Parser(taskStore))
                .helpDocumentation(Output::empty))
        .register(
            ModeRegistration.builder()
                .cliMode(CliMode.REMOVE)
                .canonicalName("remove")
                .aliases("rm")
                .parser(() -> new RemoveArguments.Parser(taskStore))
                .helpDocumentation(Output::empty))
        .register(
            ModeRegistration.builder()
                .cliMode(CliMode.AMEND)
                .canonicalName("amend")
                .aliases()
                .parser(() -> new AmendArguments.Parser(taskStore))
                .helpDocumentation(Output::empty))
        .register(
            ModeRegistration.builder()
                .cliMode(CliMode.COMPLETE)
                .canonicalName("complete")
                .aliases()
                .parser(() -> new CompleteArguments.Parser(taskStore))
                .helpDocumentation(Output::empty))
        .register(
            ModeRegistration.builder()
                .cliMode(CliMode.REOPEN)
                .canonicalName("reopen")
                .aliases()
                .parser(() -> new ReopenArguments.Parser(taskStore))
                .helpDocumentation(Output::empty))
        .build();
  }

  private final Collection<ModeRegistration> registrations;
  private final Map<String, ModeRegistration> registrationsIndexedByAliases;

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

    List<String> argsList = ImmutableList.copyOf(args);

    // Determine what mode we're in. This will affect what flags are available and what they mean.
    String modeArgument = argsList.isPopulated() ? argsList.itemAt(0) : "";
    CliMode mode = modeFromArgument(modeArgument);

    return registrationsIndexedByAliases.valueOf(mode)
        .map(ModeRegistration::parserSupplier)
        .map(parser -> parser.parse(args))
        .orElseThrow(AssertionError::new);
  }

  private CliMode modeFromArgument(String arg) {
    return registrationsIndexedByAliases.valueOf(arg)
        .map(ModeRegistration::cliMode)
        .orElseThrow(() -> new ArgumentFormatException("unrecognized mode " + arg));
  }

  public Set<CommandDocumentation> commandDocumentation() {
    return registrations.stream()
        .map(
            registration ->
                new CommandDocumentation(
                    registration.canonicalName(),
                    ImmutableList.copyOf(registration.aliases())))
        .collect(toImmutableSet());
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
    private final MutableMap<CliMode, ModeRegistration> registeredHandlers = HashMap.create();

     RegistryBuilder register(ModeRegistration registration) {
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

    Set<ModeRegistration> build() {
      return ImmutableSet.copyOf(registeredHandlers.values());
    }
  }

  private static final class ModeRegistration {
    private final CliMode cliMode;
    private final String canonicalName;
    private final Collection<String> aliases;
    private final Memoized<Parser<?>> parser;
    private final Memoized<Output> helpDocumentation;

    private ModeRegistration(
        CliMode cliMode,
        String canonicalName,
        Collection<String> aliases,
        Supplier<? extends Parser<?>> parserSupplier,
        Supplier<? extends Output> helpDocumentation) {
      requireNonNull(cliMode);
      requireNonNull(canonicalName);
      requireNonNull(aliases);
      requireNonNull(parserSupplier);
      requireNonNull(helpDocumentation);

      if (aliases.contains(canonicalName)) {
        throw new IllegalArgumentException("aliases cannot contain the canonical name");
      }
      if (ImmutableSet.copyOf(aliases).count() < aliases.count()) {
        throw new IllegalArgumentException("aliases cannot contain duplicates: " + aliases);
      }

      this.cliMode = cliMode;
      this.canonicalName = canonicalName;
      this.aliases = ImmutableList.copyOf(aliases);
      this.parser = memoize(parserSupplier);
      this.helpDocumentation = memoize(helpDocumentation);
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

    List<String> canonicalNameAndAliases() {
      return ImmutableList.<String>builder().add(canonicalName).addAll(aliases).build();
    }

    Parser<?> parserSupplier() {
      return parser.value();
    }

    Output helpDocumentation() {
      return helpDocumentation.value();
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
      Builder4 parser(Supplier<? extends Parser<?>> parserSupplier);
    }

    interface Builder4 {
      ModeRegistration helpDocumentation(Supplier<? extends Output> helpDocumentation);
    }

    static Builder0 builder() {
      return cliMode ->
          (Builder1) canonicalName ->
            (Builder2) aliases ->
                (Builder3) parserSupplier ->
                    (Builder4) helpDocumentation ->
                        new ModeRegistration(
                            cliMode,
                            canonicalName,
                            ImmutableList.copyOf(aliases),
                            parserSupplier,
                            helpDocumentation);
    }
  }
}

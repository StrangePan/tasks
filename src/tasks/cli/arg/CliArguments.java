package tasks.cli.arg;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static omnia.data.stream.Collectors.toImmutableMap;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableMap;
import tasks.model.TaskStore;

/** Data structure for arguments passed into the command line. */
public final class CliArguments {

  private static final Map<String, CliMode> modeAliases =
      ImmutableMap.<String, CliMode>builder()
          .putAll(aliasesFor(CliMode.HELP, "", "help"))
          .putAll(aliasesFor(CliMode.LIST, "list", "ls", "l"))
          .putAll(aliasesFor(CliMode.ADD, "add"))
          .putAll(aliasesFor(CliMode.AMEND, "amend"))
          .putAll(aliasesFor(CliMode.INFO, "info"))
          .putAll(aliasesFor(CliMode.REMOVE, "remove", "rm"))
          .putAll(aliasesFor(CliMode.COMPLETE, "complete"))
          .putAll(aliasesFor(CliMode.REOPEN, "reopen"))
          .build();

  private static Map<String, CliMode> aliasesFor(CliMode mode, String...aliases) {
    return Arrays.stream(aliases).collect(toImmutableMap(alias -> alias, unused -> mode));
  }

  private static Map<CliMode, ModeRegistration> createModeParserRegistry(
      Memoized<TaskStore> taskStore, Memoized<Set<String>> validModes) {
    return new RegistryBuilder()
        .register(CliMode.HELP, () -> new HelpArguments.Parser(validModes), Output::empty)
        .register(CliMode.LIST, () -> ListArguments::parse, Output::empty)
        .register(CliMode.INFO, () -> new InfoArguments.Parser(taskStore), Output::empty)
        .register(CliMode.ADD, () -> new AddArguments.Parser(taskStore), Output::empty)
        .register(CliMode.REMOVE, () -> new RemoveArguments.Parser(taskStore), Output::empty)
        .register(CliMode.AMEND, () -> new AmendArguments.Parser(taskStore), Output::empty)
        .register(CliMode.COMPLETE, () -> new CompleteArguments.Parser(taskStore), Output::empty)
        .register(CliMode.REOPEN, () -> new ReopenArguments.Parser(taskStore), Output::empty)
        .build();
  }

  private final Map<CliMode, ModeRegistration> registeredModes;

  public CliArguments(Memoized<TaskStore> taskStore) {
    registeredModes = createModeParserRegistry(taskStore, memoize(modeAliases::keys));
  }

  public Object parse(String[] args) {

    // Parameter validation
    requireNonNull(args);
    if (Arrays.stream(args).anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("arg cannot contain null");
    }

    List<String> argsList = ImmutableList.<String>builder().addAll(args).build();

    // Determine what mode we're in. This will affect what flags are available and what they mean.
    String modeArgument = argsList.isPopulated() ? argsList.itemAt(0) : "";
    CliMode mode = modeFromArgument(modeArgument);

    return registeredModes.valueOf(mode)
        .map(ModeRegistration::parserSupplier)
        .map(parser -> parser.parse(args))
        .orElseThrow(AssertionError::new);
  }

  private static CliMode modeFromArgument(String arg) {
    return modeAliases.valueOf(arg)
        .orElseThrow(() -> new ArgumentFormatException("unrecognized mode " + arg));
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

     RegistryBuilder register(
         CliMode mode,
         Supplier<? extends Parser<?>> parserSupplier,
         Supplier<? extends Output> helpDocumentation) {
      requireNonNull(mode);
      requireNonNull(parserSupplier);
      requireUnique(mode);
       registeredHandlers.putMapping(
           mode,
           new ModeRegistration(parserSupplier, helpDocumentation));
      return this;
    }

    private void requireUnique(CliMode mode) {
      if (registeredHandlers.keys().contains(mode)) {
        throw new IllegalStateException("Duplication registration for " + mode);
      }
    }

    ImmutableMap<CliMode, ModeRegistration> build() {
      return ImmutableMap.copyOf(registeredHandlers);
    }
  }

  private static final class ModeRegistration {
    private final Memoized<Parser<?>> parser;
    private final Memoized<Output> helpDocumentation;

    ModeRegistration(
        Supplier<? extends Parser<?>> parserSupplier,
        Supplier<? extends Output> helpDocumentation) {
      this.parser = memoize(requireNonNull(parserSupplier));
      this.helpDocumentation = memoize(requireNonNull(helpDocumentation));
    }

    Parser<?> parserSupplier() {
      return parser.value();
    }

    Output helpDocumentation() {
      return helpDocumentation.value();
    }
  }
}

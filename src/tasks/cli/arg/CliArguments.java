package tasks.cli.arg;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableMap;
import tasks.model.TaskStore;

/** Data structure for arguments passed into the command line. */
public final class CliArguments {

  private final Map<CliMode, Parser<?>> registeredParsers;

  public CliArguments(Memoized<TaskStore> taskStore) {
    registeredParsers = createArgParsersRegistry(taskStore);
  }

  private static Map<CliMode, Parser<?>> createArgParsersRegistry(Memoized<TaskStore> taskStore) {
    return new RegistryBuilder()
        .register(CliMode.HELP, () -> HelpArguments::parse)
        .register(CliMode.LIST, () -> ListArguments::parse)
        .register(CliMode.INFO, () -> new InfoArguments.Parser(taskStore))
        .register(CliMode.ADD, () -> new AddArguments.Parser(taskStore))
        .register(CliMode.REMOVE, () -> new RemoveArguments.Parser(taskStore))
        .register(CliMode.AMEND, () -> new AmendArguments.Parser(taskStore))
        .register(CliMode.COMPLETE, () -> new CompleteArguments.Parser(taskStore))
        .register(CliMode.REOPEN, () -> new ReopenArguments.Parser(taskStore))
        .build();
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

    return registeredParsers.valueOf(mode)
        .map(parser -> parser.parse(args))
        .orElseThrow(AssertionError::new);
  }

  private static CliMode modeFromArgument(String arg) {
    switch (arg) {
      case "":
      case "help":
        return CliMode.HELP;
      case "list":
      case "ls":
      case "l":
        return CliMode.LIST;
      case "add":
        return CliMode.ADD;
      case "amend":
        return CliMode.AMEND;
      case "info":
        return CliMode.INFO;
      case "remove":
      case "rm":
        return CliMode.REMOVE;
      case "complete":
        return CliMode.COMPLETE;
      case "reopen":
        return CliMode.REOPEN;
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

  interface Parser<T> {
    T parse(String[] args);
  }

  private static final class RegistryBuilder {
    private final MutableMap<CliMode, Parser<?>> registeredHandlers = HashMap.create();

     RegistryBuilder register(CliMode mode, Supplier<? extends Parser<?>> parserSupplier) {
      requireNonNull(mode);
      requireNonNull(parserSupplier);
      requireUnique(mode);
      Memoized<? extends Parser<?>> memoizedParser = memoize(parserSupplier);
      registeredHandlers.putMapping(mode, s -> memoizedParser.value().parse(s));
      return this;
    }

    private void requireUnique(CliMode mode) {
      if (registeredHandlers.keys().contains(mode)) {
        throw new IllegalStateException("Duplication registration for " + mode);
      }
    }

    ImmutableMap<CliMode, Parser<?>> build() {
      return ImmutableMap.copyOf(registeredHandlers);
    }
  }
}

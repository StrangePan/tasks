package tasks.cli.arg;

import static java.util.Objects.requireNonNull;

import omnia.data.structure.List;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;

public final class CommandDocumentation {
  private final String canonicalName;
  private final ImmutableList<String> aliases;
  private final ImmutableSet<ArgumentDocumentation> arguments;

  public CommandDocumentation(
      String canonicalName, List<String> aliases, Set<ArgumentDocumentation> arguments) {
    this.canonicalName = requireNonNull(canonicalName);
    this.aliases = ImmutableList.copyOf(requireNonNull(aliases));
    this.arguments = ImmutableSet.copyOf(requireNonNull(arguments));
  }

  public String canonicalName() {
    return canonicalName;
  }

  public ImmutableList<String> aliases() {
    return aliases;
  }

  public ImmutableSet<ArgumentDocumentation> arguments() {
    return arguments;
  }

  public static final class ArgumentDocumentation {
    private final String canonicalName;
    private final String shortFlag;
    private final String description;

    public ArgumentDocumentation(
        String canonicalName,
        String shortFlag,
        String description) {
      this.canonicalName = requireNonNull(canonicalName);
      this.shortFlag = requireNonNull(shortFlag);
      this.description = requireNonNull(description);
    }

    public String canonicalName() {
      return canonicalName;
    }

    public String shortFlag() {
      return shortFlag;
    }

    public String description() {
      return description;
    }
  }
}

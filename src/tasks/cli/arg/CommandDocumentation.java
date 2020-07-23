package tasks.cli.arg;

import static java.util.Objects.requireNonNull;

import omnia.data.structure.List;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;

public final class CommandDocumentation {
  private final String canonicalName;
  private final ImmutableList<String> aliases;
  private final ImmutableSet<OptionDocumentation> options;

  public CommandDocumentation(
      String canonicalName, List<String> aliases, Set<OptionDocumentation> options) {
    this.canonicalName = requireNonNull(canonicalName);
    this.aliases = ImmutableList.copyOf(requireNonNull(aliases));
    this.options = ImmutableSet.copyOf(requireNonNull(options));
  }

  public String canonicalName() {
    return canonicalName;
  }

  public ImmutableList<String> aliases() {
    return aliases;
  }

  public ImmutableSet<OptionDocumentation> options() {
    return options;
  }

  public static final class OptionDocumentation {
    private final String canonicalName;
    private final String shortFlag;
    private final String description;

    public OptionDocumentation(
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

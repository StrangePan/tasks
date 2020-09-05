package tasks.cli.arg;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import omnia.data.structure.List;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;

public final class CommandDocumentation {
  private final String canonicalName;
  private final ImmutableList<String> aliases;
  private final Optional<String> parameterRepresentation;
  private final String description;
  private final ImmutableSet<OptionDocumentation> options;

  public CommandDocumentation(
      String canonicalName,
      List<String> aliases,
      Optional<String> parameterRepresentation,
      String description,
      Set<OptionDocumentation> options) {
    this.canonicalName = requireNonNull(canonicalName);
    this.aliases = ImmutableList.copyOf(requireNonNull(aliases));
    this.parameterRepresentation = requireNonNull(parameterRepresentation);
    this.description = requireNonNull(description);
    this.options = ImmutableSet.copyOf(requireNonNull(options));
  }

  public String canonicalName() {
    return canonicalName;
  }

  public ImmutableList<String> aliases() {
    return aliases;
  }

  public Optional<String> parameterRepresentation() {
    return parameterRepresentation;
  }

  public String description() {
    return description;
  }

  public ImmutableSet<OptionDocumentation> options() {
    return options;
  }

  public static final class OptionDocumentation {
    private final String canonicalName;
    private final Optional<String> shortFlag;
    private final String description;
    private final boolean repeatable;
    private final Optional<String> parameterRepresentation;

    public OptionDocumentation(
        String canonicalName,
        Optional<String> shortFlag,
        String description,
        boolean repeatable,
        Optional<String> parameterRepresentation) {
      this.canonicalName = requireNonNull(canonicalName);
      this.shortFlag = requireNonNull(shortFlag);
      this.description = requireNonNull(description);
      this.repeatable = repeatable;
      this.parameterRepresentation = requireNonNull(parameterRepresentation);
    }

    public String canonicalName() {
      return canonicalName;
    }

    public Optional<String> shortFlag() {
      return shortFlag;
    }

    public String description() {
      return description;
    }

    public boolean isRepeatable() {
      return repeatable;
    }

    public Optional<String> parameterRepresentation() {
      return parameterRepresentation;
    }
  }
}

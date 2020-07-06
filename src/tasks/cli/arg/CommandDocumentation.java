package tasks.cli.arg;

import static java.util.Objects.requireNonNull;

import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;

public final class CommandDocumentation {
  private final String canonicalName;
  private final ImmutableList<String> aliases;

  public CommandDocumentation(String canonicalName, List<String> aliases) {
    this.canonicalName = requireNonNull(canonicalName);
    this.aliases = ImmutableList.copyOf(requireNonNull(aliases));
  }

  public String canonicalName() {
    return canonicalName;
  }

  public ImmutableList<String> aliases() {
    return aliases;
  }
}

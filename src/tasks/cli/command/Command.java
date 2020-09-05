package tasks.cli.command;

import static java.util.Objects.requireNonNull;

import omnia.data.structure.Collection;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;

public final class Command {
  private final String canonicalName;
  private final Collection<String> aliases;
  private final String description;
  private final Collection<Parameter> parameters;
  private final Collection<Option> options;

  private Command(
      String canonicalName,
      Collection<String> aliases,
      String description,
      Collection<Parameter> parameters,
      Collection<Option> options) {
    requireNonNull(canonicalName);
    requireNonNull(aliases);
    requireNonNull(description);
    requireNonNull(parameters);
    requireNonNull(options);

    if (aliases.contains(canonicalName)) {
      throw new IllegalArgumentException("aliases cannot contain the canonical name");
    }
    if (ImmutableSet.copyOf(aliases).count() < aliases.count()) {
      throw new IllegalArgumentException("aliases cannot contain duplicates: " + aliases);
    }

    this.canonicalName = canonicalName;
    this.aliases = ImmutableList.copyOf(aliases);
    this.description = description;
    this.parameters = parameters;
    this.options = ImmutableList.copyOf(options);
  }

  public String canonicalName() {
    return canonicalName;
  }

  public Collection<String> aliases() {
    return aliases;
  }

  public String description() {
    return description;
  }

  public Collection<Parameter> parameters() {
    return parameters;
  }

  public Collection<Option> options() {
    return options;
  }

  public List<String> canonicalNameAndAliases() {
    return ImmutableList.<String>builder().add(canonicalName).addAll(aliases).build();
  }

  public interface Builder0 {
    Builder1 canonicalName(String canonicalName);
  }

  public interface Builder1 {
    Builder2 aliases(String... aliases);
  }

  public interface Builder2 {
    Builder3 parameters(Collection<Parameter> parameters);
  }

  public interface Builder3 {
    Builder4 options(Collection<Option> options);
  }

  public interface Builder4 {
    Command helpDocumentation(String description);
  }

  public static Builder0 builder() {
    return canonicalName ->
        (Builder1) aliases ->
            (Builder2) parameters ->
                (Builder3) arguments ->
                    (Builder4) description ->
                        new Command(
                            canonicalName,
                            ImmutableList.copyOf(aliases),
                            description,
                            parameters,
                            arguments);
  }
}

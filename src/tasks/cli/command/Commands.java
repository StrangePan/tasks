package tasks.cli.command;

import java.util.Optional;
import omnia.data.structure.Collection;

public interface Commands {

  /** Gets all registered commands. */
  Collection<Command> getAllCommands();

  /**
   * Finds and returns the only command that matches the provided string (presumed to be userInput),
   * or the empty Optional if there are no matches. Compares command canonical names and aliases.
   */
  Optional<Command> getMatchingCommand(String userInput);
}

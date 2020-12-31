package tasks.cli.feature.help;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Comparator.comparing;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.stream.Collectors.joining;
import static omnia.data.stream.Collectors.toImmutableList;
import static tasks.cli.handler.testing.HandlerTestUtils.commonArgs;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.cli.command.Command;
import tasks.cli.command.Commands;
import tasks.cli.command.common.CommonArguments;

@RunWith(JUnit4.class)
public final class HelpHandlerTest {

  private static final Command TEST_COMMAND_ALPHA = generateCommand("alpha");
  private static final Command TEST_COMMAND_BETA = generateCommand("beta");
  private static final Command TEST_COMMAND_GAMMA = generateCommand("gamma");

  private static final Commands TEST_COMMANDS =
      new Commands() {
        private final ImmutableMap<String, Command> commands =
            ImmutableMap.<String, Command>builder()
                .putMapping(TEST_COMMAND_ALPHA.canonicalName(), TEST_COMMAND_ALPHA)
                .putMapping(TEST_COMMAND_BETA.canonicalName(), TEST_COMMAND_BETA)
                .putMapping(TEST_COMMAND_GAMMA.canonicalName(), TEST_COMMAND_GAMMA)
                .build();

        @Override
        public Collection<Command> getAllCommands() {
          return commands.values()
              .stream()
              .sorted(comparing(Command::canonicalName))
              .collect(toImmutableList());
        }

        @Override
        public Optional<Command> getMatchingCommand(String userInput) {
          return commands.valueOf(userInput);
        }
      };

  private static Command generateCommand(String name) {
    return Command.builder()
        .canonicalName(name)
        .aliases(name + "_alias")
        .parameters(ImmutableSet.empty())
        .options(ImmutableSet.empty())
        .helpDocumentation(name + " help documentation");
  }

  private final HelpHandler underTest = new HelpHandler(Memoized.just(TEST_COMMANDS));

  @Test
  public void handle_withNoArguments_listsAllCommands() {
    assertOutputListsAllCommands(underTest.handle(helpArgs()).blockingGet().renderWithoutCodes());
  }

  @Test
  public void handle_forUnrecognizedCommand_listsAllCommands() {
    assertOutputListsAllCommands(
        underTest.handle(helpArgs("zeta")).blockingGet().renderWithoutCodes());
  }

  @Test
  public void handle_forSpecificCommand_printsCommandDetails() {
    String output =
        underTest.handle(helpArgs(TEST_COMMAND_ALPHA.canonicalName()))
            .blockingGet()
            .renderWithoutCodes();

    assertThat(output).contains(TEST_COMMAND_ALPHA.canonicalName());
    assertThat(output).contains(TEST_COMMAND_ALPHA.aliases().iterator().next());
    assertThat(output).contains(TEST_COMMAND_ALPHA.description());

    assertThat(output).doesNotContain(TEST_COMMAND_BETA.canonicalName());
    assertThat(output).doesNotContain(TEST_COMMAND_GAMMA.canonicalName());
  }

  @Test
  public void handle_withUnrecognizedArguments_listsAllCommands() {
    assertOutputListsAllCommands(
        underTest.handle(helpArgs("zeta")).blockingGet().renderWithoutCodes());
  }

  private static CommonArguments<HelpArguments> helpArgs() {
    return commonArgs(new HelpArguments());
  }

  private static CommonArguments<HelpArguments> helpArgs(String command) {
    return commonArgs(new HelpArguments(command));
  }

  private static void assertOutputListsAllCommands(String output) {
    assertThat(output).matches(
        Pattern.compile(
            "Commands:\\s+" +
                TEST_COMMANDS.getAllCommands()
                    .stream()
                    .map(
                        command -> command.canonicalNameAndAliases()
                            .stream()
                            .collect(joining(", ")))
                    .map(Pattern::quote)
                    .map(s -> "\\s+" + s)
                    .collect(joining()) +
                "\\s*",
            MULTILINE));
  }
}
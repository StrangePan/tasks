package me.strangepan.tasks.cli.feature.help

import java.util.stream.Collectors as JavaCollectors
import omnia.data.stream.Collectors as OmniaCollectors
import com.google.common.truth.Truth
import java.util.Comparator
import java.util.Optional
import java.util.regex.Pattern
import omnia.data.cache.Memoized.Companion.just
import omnia.data.structure.Collection
import omnia.data.structure.immutable.ImmutableMap.Companion.builder
import omnia.data.structure.immutable.ImmutableSet
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import me.strangepan.tasks.cli.command.Command
import me.strangepan.tasks.cli.command.Command.Companion.builder
import me.strangepan.tasks.cli.command.Commands
import me.strangepan.tasks.cli.command.common.CommonArguments
import me.strangepan.tasks.cli.handler.testing.HandlerTestUtils

@RunWith(JUnit4::class)
class HelpHandlerTest {
  private val underTest = HelpHandler(just(TEST_COMMANDS))

  @Test
  fun handle_withNoArguments_listsAllCommands() {
    assertOutputListsAllCommands(underTest.handle(helpArgs()).blockingGet().renderWithoutCodes())
  }

  @Test
  fun handle_forUnrecognizedCommand_listsAllCommands() {
    assertOutputListsAllCommands(
        underTest.handle(helpArgs("zeta")).blockingGet().renderWithoutCodes())
  }

  @Test
  fun handle_forSpecificCommand_printsCommandDetails() {
    val output = underTest.handle(helpArgs(TEST_COMMAND_ALPHA.canonicalName()))
        .blockingGet()
        .renderWithoutCodes()
    Truth.assertThat(output).contains(TEST_COMMAND_ALPHA.canonicalName())
    Truth.assertThat(output).contains(TEST_COMMAND_ALPHA.aliases().iterator().next())
    Truth.assertThat(output).contains(TEST_COMMAND_ALPHA.description())
    Truth.assertThat(output).doesNotContain(TEST_COMMAND_BETA.canonicalName())
    Truth.assertThat(output).doesNotContain(TEST_COMMAND_GAMMA.canonicalName())
  }

  @Test
  fun handle_withUnrecognizedArguments_listsAllCommands() {
    assertOutputListsAllCommands(
        underTest.handle(helpArgs("zeta")).blockingGet().renderWithoutCodes())
  }

  companion object {
    private val TEST_COMMAND_ALPHA = generateCommand("alpha")
    private val TEST_COMMAND_BETA = generateCommand("beta")
    private val TEST_COMMAND_GAMMA = generateCommand("gamma")
    private val TEST_COMMANDS: Commands = object : Commands {
      private val commands = builder<String, Command>()
          .putMapping(TEST_COMMAND_ALPHA.canonicalName(), TEST_COMMAND_ALPHA)
          .putMapping(TEST_COMMAND_BETA.canonicalName(), TEST_COMMAND_BETA)
          .putMapping(TEST_COMMAND_GAMMA.canonicalName(), TEST_COMMAND_GAMMA)
          .build()
      override val allCommands: Collection<Command>
        get() = commands.values()
            .stream()
            .sorted(Comparator.comparing(Command::canonicalName))
            .collect(OmniaCollectors.toImmutableList())

      override fun getMatchingCommand(userInput: String): Optional<Command> {
        return commands.valueOf(userInput)
      }
    }

    private fun generateCommand(name: String): Command {
      return builder()
          .canonicalName(name)
          .aliases(name + "_alias")
          .parameters(ImmutableSet.empty())
          .options(ImmutableSet.empty())
          .helpDocumentation("$name help documentation")
    }

    private fun helpArgs(): CommonArguments<HelpArguments> {
      return HandlerTestUtils.commonArgs(HelpArguments())
    }

    private fun helpArgs(command: String): CommonArguments<HelpArguments> {
      return HandlerTestUtils.commonArgs(HelpArguments(command))
    }

    private fun assertOutputListsAllCommands(output: String) {
      Truth.assertThat(output).matches(
          Pattern.compile(
              "Commands:\\s+" +
                  TEST_COMMANDS.allCommands
                      .stream()
                      .map { command: Command ->
                        command.canonicalNameAndAliases()
                            .stream()
                            .collect(JavaCollectors.joining(", "))
                      }
                      .map { s: String -> Pattern.quote(s) }
                      .map { s: String -> "\\s+$s" }
                      .collect(JavaCollectors.joining()) +
                  "\\s*",
              Pattern.MULTILINE))
    }
  }
}
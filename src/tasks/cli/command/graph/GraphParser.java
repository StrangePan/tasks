package tasks.cli.command.graph;

import static tasks.cli.parser.ParserUtil.assertNoExtraArgs;

import org.apache.commons.cli.CommandLine;
import tasks.cli.parser.CommandParser;
import tasks.cli.parser.ParserUtil;

/** Parser for graph/xl command. */
public final class GraphParser implements CommandParser<GraphArguments> {

  @Override
  public GraphArguments parse(CommandLine commandLine) {
    /*
     * No non-flag parameters allowed
     * optional --completed flag
     * optional --all flag
     */
    assertNoExtraArgs(commandLine);

    boolean isAllSet = ParserUtil.getFlagPresence(commandLine, GraphCommand.ALL_OPTION.value());
    boolean isCompletedSet =
        isAllSet || ParserUtil.getFlagPresence(commandLine, GraphCommand.COMPLETED_OPTION.value());
    boolean isUncompletedSet = isAllSet || !isCompletedSet;

    return new GraphArguments(isCompletedSet, isUncompletedSet);
  }
}

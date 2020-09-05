package tasks.cli.command.common;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.arg.CliArguments.Parameter.Repeatable.NOT_REPEATABLE;

import omnia.data.cache.Memoized;
import tasks.cli.arg.CliArguments;

public final class StripColors {
  private StripColors() {}

  public static Memoized<CliArguments.FlagOption> STRIP_COLORS_OPTION =
      memoize(
          () -> new CliArguments.FlagOption(
              "no-color",
              "Removes all color and formatting codes from the output. This is useful when "
                  + "attempting to pipe the output into another command.",
              NOT_REPEATABLE));
}

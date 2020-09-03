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
              "nocolor",
              "c",
              "When this flag is present, all color and formatting codes are stripped from the "
                  + "output. This is useful when attempting to pipe the output.",
              NOT_REPEATABLE));
}

package tasks.cli.command.common;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.NOT_REPEATABLE;

import omnia.data.cache.Memoized;
import tasks.cli.command.FlagOption;

public final class StripColors {
  private StripColors() {}

  public static Memoized<FlagOption> STRIP_COLORS_OPTION =
      memoize(
          () -> new FlagOption(
              "no-color",
              "Removes all color and formatting codes from the output. This is useful when "
                  + "attempting to pipe the output into another command.",
              NOT_REPEATABLE));
}

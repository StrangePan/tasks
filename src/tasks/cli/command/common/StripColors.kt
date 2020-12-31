package tasks.cli.command.common

import java.util.function.Supplier
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import tasks.cli.command.FlagOption
import tasks.cli.command.Parameter

object StripColors {
  var STRIP_COLORS_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "no-color", "Removes all color and formatting codes from the output. This is useful when "
            + "attempting to pipe the output into another command.",
            Parameter.Repeatable.NOT_REPEATABLE)
      })
}
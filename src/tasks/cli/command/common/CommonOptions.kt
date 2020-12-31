package tasks.cli.command.common

import omnia.data.cache.Memoized
import omnia.data.structure.immutable.ImmutableList
import tasks.cli.command.Option

object CommonOptions {
  val OPTIONS: Memoized<ImmutableList<Option>> = Memoized.memoize { ImmutableList.of(StripColors.STRIP_COLORS_OPTION.value()) }
}
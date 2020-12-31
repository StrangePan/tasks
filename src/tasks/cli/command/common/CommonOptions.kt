package tasks.cli.command.common;

import static omnia.data.cache.Memoized.memoize;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Option;

public final class CommonOptions {

  public static final Memoized<ImmutableList<Option>> OPTIONS =
      memoize(() -> ImmutableList.of(StripColors.STRIP_COLORS_OPTION.value()));
}

package tasks.data.text;

import omnia.data.structure.immutable.ImmutableSet;
import omnia.string.Escapist;

final class Utils {

  private Utils() {}

  private static final Escapist ESCAPIST =
      new Escapist('\\', ImmutableSet.<Character>builder().add(';').build());

  static Escapist escapist() {
    return ESCAPIST;
  }
}

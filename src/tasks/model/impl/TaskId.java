package tasks.model.impl;

import java.util.Objects;

final class TaskId {

  static final int TO_STRING_BASE = Character.MAX_RADIX;
  static final int TO_STRING_MAX_LENGTH = (int) Math.floor(log(Long.MAX_VALUE, TO_STRING_BASE));
  static final long MAX_ID_VALUE = (long) Math.pow(TO_STRING_BASE, TO_STRING_MAX_LENGTH);

  private final long id;

  TaskId(long id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TaskId && ((TaskId) other).id == id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return forceToMaxLength(Long.toString(asLong(), TO_STRING_BASE));
  }

  private static String forceToMaxLength(String s) {
    return "0".repeat(Math.max(0, TO_STRING_MAX_LENGTH - s.length())).concat(s);
  }

  static TaskId parse(String string) throws NumberFormatException {
    return new TaskId(Long.parseUnsignedLong(string, TO_STRING_BASE));
  }

  long asLong() {
    return id;
  }

  private static double log(double value, double base) {
    return Math.log(value) / Math.log(base);
  }
}

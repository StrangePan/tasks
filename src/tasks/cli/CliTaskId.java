package tasks.cli;

import java.util.Objects;

public final class CliTaskId {
  private final long id;

  public static CliTaskId from(long id) {
    return new CliTaskId(id);
  }

  public static CliTaskId parse(String serializedId) throws IdFormatException {
    try {
      return CliTaskId.from(Long.parseLong(serializedId, Character.MAX_RADIX));
    } catch (NumberFormatException ex) {
      throw new IdFormatException("Unable to parse numerical representation ID", ex);
    }
  }

  private CliTaskId(long id) {
    this.id = id;
  }

  public long asLong() {
    return id;
  }

  public String serialize() {
    return Long.toString(id, Character.MAX_RADIX);
  }

  @Override
  public String toString() {
    return "Id" + Long.toString(id, Character.MAX_RADIX);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof CliTaskId && ((CliTaskId) other).id == id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public static final class IdFormatException extends RuntimeException {
    private IdFormatException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}

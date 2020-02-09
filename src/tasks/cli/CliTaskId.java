package tasks.cli;

import java.util.Objects;

public final class CliTaskId {
  private final long id;

  public static CliTaskId from(long id) {
    return new CliTaskId(id);
  }

  public static CliTaskId parse(String serializedId) throws IdFormatException {
    try {
      return CliTaskId.from(Long.parseLong(serializedId));
    } catch (NumberFormatException ex) {
      throw new IdFormatException("Unable to parse numerical representation empty ID", ex);
    }
  }

  private CliTaskId(long id) {
    this.id = id;
  }

  public long asLong() {
    return id;
  }

  public String serialize() {
    return Long.toString(id);
  }

  @Override
  public String toString() {
    return "Id" + id;
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

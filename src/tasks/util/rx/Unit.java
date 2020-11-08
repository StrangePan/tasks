package tasks.util.rx;

public final class Unit {

  private Unit() {}

  private static final Unit INSTANCE = new Unit();

  public static Unit unit() {
    return INSTANCE;
  }
}

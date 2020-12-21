package tasks.util.rx;

import static com.google.common.truth.Truth.assertThat;
import static tasks.util.rx.Unit.unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class UnitTest {

  @Test
  public void twoUnitsAreSame() {
    assertThat(unit()).isSameInstanceAs(unit());
  }

  @SuppressWarnings("EqualsWithItself")
  @Test
  public void twoUnitsAreEqual() {
    // Can't use assertThat().isEqualTo() because it implicitly conducts identity comparison
    assertThat(unit().equals(unit())).isTrue();
  }

  @Test
  public void twoUnitsHashTheSame() {
    assertThat(unit().hashCode()).isEqualTo(unit().hashCode());
  }
}
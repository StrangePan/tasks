package tasks.util.rx;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class MaybesTest {

  @Test
  public void fromOptional_whenPresent_emitsValue() {
    Maybes.fromOptional(Optional.of(132)).test().assertValue(132).assertComplete();
  }

  @Test
  public void fromOptional_whenEmpty_completes() {
    Maybes.fromOptional(Optional.empty()).test().assertNoValues().assertComplete();
  }
}
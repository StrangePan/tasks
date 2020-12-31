package tasks.util.rx

import java.util.Optional
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tasks.util.rx.Maybes.fromOptional

@RunWith(JUnit4::class)
class MaybesTest {
  @Test
  fun fromOptional_whenPresent_emitsValue() {
    fromOptional(Optional.of(132)).test().assertValue(132).assertComplete()
  }

  @Test
  fun fromOptional_whenEmpty_completes() {
    fromOptional(Optional.empty<Any>()).test().assertNoValues().assertComplete()
  }
}
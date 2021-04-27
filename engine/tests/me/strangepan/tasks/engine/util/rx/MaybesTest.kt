package me.strangepan.tasks.engine.util.rx

import java.util.Optional
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MaybesTest {
  @Test
  fun fromOptional_whenPresent_emitsValue() {
    Maybes.fromOptional(Optional.of(132)).test().assertValue(132).assertComplete()
  }

  @Test
  fun fromOptional_whenEmpty_completes() {
    Maybes.fromOptional(Optional.empty<Any>()).test().assertNoValues().assertComplete()
  }
}
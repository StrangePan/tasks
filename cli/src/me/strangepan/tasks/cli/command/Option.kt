package me.strangepan.tasks.cli.command

import java.util.Optional
import org.apache.commons.cli.Option

abstract class Option private constructor(
    private val longName: String,
    private val shortName: Optional<String>,
    private val description: String,
    private val repeatable: Parameter.Repeatable,
    private val parameterRepresentation: Optional<String>) {

  protected constructor(
      longName: String,
      shortName: String,
      description: String,
      repeatable: Parameter.Repeatable,
      parameterRepresentation: Optional<String>) : this(longName, Optional.of<String>(shortName), description, repeatable, parameterRepresentation)

  protected constructor(
      longName: String,
      description: String,
      repeatable: Parameter.Repeatable,
      parameterRepresentation: Optional<String>) : this(longName, Optional.empty<String>(), description, repeatable, parameterRepresentation)

  fun longName(): String {
    return longName
  }

  fun shortName(): Optional<String> {
    return shortName
  }

  fun description(): String {
    return description
  }

  fun isRepeatable(): Boolean {
    return repeatable == Parameter.Repeatable.REPEATABLE
  }

  fun parameterRepresentation(): Optional<String> {
    return parameterRepresentation
  }

  abstract fun toCliOption(): Option
}
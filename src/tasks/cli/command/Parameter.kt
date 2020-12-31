package tasks.cli.command

abstract class Parameter internal constructor(private val description: String, private val repeatable: Repeatable) {

  fun description(): String {
    return description
  }

  fun isRepeatable(): Boolean {
    return repeatable == Repeatable.REPEATABLE
  }

  enum class Repeatable {
    REPEATABLE, NOT_REPEATABLE
  }
}
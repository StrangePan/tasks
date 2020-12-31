package tasks.cli.parser

class ParserException : RuntimeException {
  constructor(reason: String) : super(reason)
  constructor(reason: String, cause: Throwable) : super(reason, cause)

  companion object {
    private const val serialVersionUID = -3922266917865193312L
  }
}
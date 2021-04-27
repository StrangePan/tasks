package me.strangepan.tasks.cli.handler

class HandlerException : RuntimeException {
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)

  companion object {
    private const val serialVersionUID = 374445314238057858L
  }
}
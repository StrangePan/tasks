package tasks.cli

/** CLI entry point into the Tasks application.  */
object EntryPoint {
  @JvmStatic
  fun main(args: Array<String>) {
    Application(args).run()
  }
}
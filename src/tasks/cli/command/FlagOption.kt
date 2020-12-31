package tasks.cli.command

import java.util.Optional

class FlagOption : Option {
  constructor(longName: String, shortName: String, description: String, repeatable: Parameter.Repeatable) : super(longName, shortName, description, repeatable, Optional.empty<String>())
  constructor(longName: String, description: String, repeatable: Parameter.Repeatable) : super(longName, description, repeatable, Optional.empty<String>())

  override fun toCliOption(): org.apache.commons.cli.Option {
    return org.apache.commons.cli.Option.builder(shortName().orElse(null))
        .longOpt(longName())
        .desc(description())
        .numberOfArgs(0)
        .build()
  }
}
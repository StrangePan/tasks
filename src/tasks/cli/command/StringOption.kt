package tasks.cli.command

import java.util.Optional

class StringOption(
    longName: String,
    shortName: String,
    description: String,
    repeatable: Parameter.Repeatable,
    semanticDescription: String) : Option(longName, shortName, description, repeatable, Optional.of(semanticDescription)) {
  override fun toCliOption(): org.apache.commons.cli.Option {
    return org.apache.commons.cli.Option.builder(shortName().orElse(null))
        .longOpt(longName())
        .desc(description())
        .optionalArg(false)
        .numberOfArgs(1)
        .build()
  }
}
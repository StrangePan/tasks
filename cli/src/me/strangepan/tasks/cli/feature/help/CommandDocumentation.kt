package me.strangepan.tasks.cli.feature.help

import java.util.Optional
import omnia.data.structure.List
import omnia.data.structure.Set
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.immutable.ImmutableSet

class CommandDocumentation(
    private val canonicalName: String,
    aliases: List<String>,
    private val parameterRepresentation: Optional<String>,
    private val description: String,
    options: Set<OptionDocumentation>) {
  private val aliases: ImmutableList<String> = ImmutableList.copyOf(aliases)
  private val options: ImmutableSet<OptionDocumentation> = ImmutableSet.copyOf(options)

  fun canonicalName(): String {
    return canonicalName
  }

  fun aliases(): ImmutableList<String> {
    return aliases
  }

  fun parameterRepresentation(): Optional<String> {
    return parameterRepresentation
  }

  fun description(): String {
    return description
  }

  fun options(): ImmutableSet<OptionDocumentation> {
    return options
  }

  class OptionDocumentation(
      private val canonicalName: String,
      private val shortFlag: Optional<String>,
      private val description: String,
      val isRepeatable: Boolean,
      private val parameterRepresentation: Optional<String>) {

    fun canonicalName(): String {
      return canonicalName
    }

    fun shortFlag(): Optional<String> {
      return shortFlag
    }

    fun description(): String {
      return description
    }

    fun parameterRepresentation(): Optional<String> {
      return parameterRepresentation
    }
  }

}
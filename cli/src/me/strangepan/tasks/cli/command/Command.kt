package me.strangepan.tasks.cli.command

import omnia.data.structure.Collection
import omnia.data.structure.List
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.immutable.ImmutableSet

class Command private constructor(
    private val canonicalName: String,
    private val aliases: Collection<String>,
    private val description: String,
    private val parameters: Collection<Parameter>,
    private val options: Collection<Option>) {

  fun canonicalName(): String {
    return canonicalName
  }

  fun aliases(): Collection<String> {
    return aliases
  }

  fun description(): String {
    return description
  }

  fun parameters(): Collection<Parameter> {
    return parameters
  }

  fun options(): Collection<Option> {
    return options
  }

  fun canonicalNameAndAliases(): List<String> {
    return ImmutableList.builder<String>().add(canonicalName).addAll(aliases).build()
  }

  interface Builder0 {
    fun canonicalName(canonicalName: String): Builder1
  }

  interface Builder1 {
    fun aliases(vararg aliases: String): Builder2
  }

  interface Builder2 {
    fun parameters(parameters: Collection<Parameter>): Builder3
  }

  interface Builder3 {
    fun options(options: Collection<Option>): Builder4
  }

  interface Builder4 {
    fun helpDocumentation(description: String): Command
  }

  companion object {
    @JvmStatic
    fun builder(): Builder0 {
      return object : Builder0 {
        override fun canonicalName(canonicalName: String): Builder1 {
          return object : Builder1 {
            override fun aliases(vararg aliases: String): Builder2 {
              return object : Builder2 {
                override fun parameters(parameters: Collection<Parameter>): Builder3 {
                  return object : Builder3 {
                    override fun options(options: Collection<Option>): Builder4 {
                      return object : Builder4 {
                        override fun helpDocumentation(description: String): Command {
                          return Command(
                              canonicalName,
                              ImmutableList.copyOf(aliases),
                              description,
                              ImmutableList.copyOf(parameters),
                              ImmutableList.copyOf(options))
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  init {
    require(!aliases.contains(canonicalName)) { "aliases cannot contain the canonical name" }
    require(ImmutableSet.copyOf(aliases).count() >= aliases.count()) { "aliases cannot contain duplicates: $aliases" }
  }
}
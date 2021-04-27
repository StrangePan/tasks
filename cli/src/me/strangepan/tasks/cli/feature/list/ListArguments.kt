package me.strangepan.tasks.cli.feature.list

/** Model for parsed List command arguments.  */
class ListArguments internal constructor(
    /**
     * Whether or not to list all unblocked me.strangepan.tasks.engine.tasks. Defaults to `true` if no other flags are
     * set.
     */
    val isUnblockedSet: Boolean,
    /**
     * Whether or not to list all blocked me.strangepan.tasks.engine.tasks. Defaults to `false` unless set by the user
     * specifically or via the [ListCommand.ALL_OPTION] flag.
     */
    val isBlockedSet: Boolean,
    /**
     * Whether or not to list all completed me.strangepan.tasks.engine.tasks. Defaults to `false` unless set by the user
     * specifically or via the [ListCommand.ALL_OPTION] flag.
     */
    val isCompletedSet: Boolean,
    /**
     * Whether or not to filter the result so that only started me.strangepan.tasks.engine.tasks are listed. If this flag is set,
     * then only started me.strangepan.tasks.engine.tasks should be included in the output. Defaults to false.
     *
     *
     * Unlike other flags, this one is subtractive. Setting this flag can always reduce the
     * output.
     */
    val isStartedSet: Boolean)
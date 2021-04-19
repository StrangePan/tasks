package me.strangepan.tasks.android.engine


class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}

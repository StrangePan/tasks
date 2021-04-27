package me.strangepan.tasks.engine.io

interface Store<T> {
  fun retrieveBlocking(): T
  fun storeBlocking(data: T)
}
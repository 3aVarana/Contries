package com.example.contris.domain

import kotlinx.coroutines.CoroutineDispatcher

/** Abstraction over coroutine dispatchers so repositories and ViewModels are testable. */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

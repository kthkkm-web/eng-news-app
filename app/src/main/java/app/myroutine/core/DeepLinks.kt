package app.myroutine.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 위젯·알림에서 들어온 이동 요청. NavHost 가 소비 후 비운다. */
object DeepLinks {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending
    fun push(route: String?) { if (!route.isNullOrBlank()) _pending.value = route }
    fun consume() { _pending.value = null }
}

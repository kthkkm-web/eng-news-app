package app.myroutine.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 기록/루틴/할 일이 바뀌면 신호를 보낸다. 위젯·알림 갱신이 이것을 구독한다. */
@Singleton
class DataChanges @Inject constructor() {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<Unit> = _events
    fun notifyChanged() { _events.tryEmit(Unit) }
}

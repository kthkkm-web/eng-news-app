package app.myroutine.core

import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** 테스트에서 시간을 바꿀 수 있도록 주입한다. */
interface TimeProvider {
    fun nowMillis(): Long
    fun nowDateTime(): LocalDateTime
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun nowDateTime(): LocalDateTime = LocalDateTime.now()
}

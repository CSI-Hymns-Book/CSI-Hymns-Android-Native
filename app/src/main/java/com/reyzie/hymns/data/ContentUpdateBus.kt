package com.reyzie.hymns.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Notifies UI layers when background content sync updates local files. */
object ContentUpdateBus {
    private val _hymnsUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val hymnsUpdated: SharedFlow<Unit> = _hymnsUpdated.asSharedFlow()

    private val _keerthanesUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val keerthanesUpdated: SharedFlow<Unit> = _keerthanesUpdated.asSharedFlow()

    private val _orderUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val orderUpdated: SharedFlow<Unit> = _orderUpdated.asSharedFlow()

    fun notifyHymnsUpdated() {
        _hymnsUpdated.tryEmit(Unit)
    }

    fun notifyKeerthanesUpdated() {
        _keerthanesUpdated.tryEmit(Unit)
    }

    fun notifyOrderUpdated() {
        _orderUpdated.tryEmit(Unit)
    }

    fun notifyFrom(result: ContentSyncResult) {
        if (result.hymnsUpdated) notifyHymnsUpdated()
        if (result.keerthanesUpdated) notifyKeerthanesUpdated()
        if (result.orderUpdated) notifyOrderUpdated()
    }
}

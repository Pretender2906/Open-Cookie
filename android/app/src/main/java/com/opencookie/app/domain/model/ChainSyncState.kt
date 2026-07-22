package com.opencookie.app.domain.model

enum class ChainSyncState {
    Syncing,
    Ready,
    Unavailable,
}

fun chainSyncState(
    isOnline: Boolean,
    configLoaded: Boolean,
    lastRefreshMs: Long,
): ChainSyncState = when {
    !isOnline -> ChainSyncState.Unavailable
    configLoaded -> ChainSyncState.Ready
    lastRefreshMs == 0L -> ChainSyncState.Syncing
    else -> ChainSyncState.Unavailable
}

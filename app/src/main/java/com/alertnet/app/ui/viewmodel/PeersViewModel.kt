package com.alertnet.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertnet.app.mesh.DiscoveryState
import com.alertnet.app.mesh.MeshManager
import com.alertnet.app.mesh.MeshStats
import com.alertnet.app.model.*
import kotlinx.coroutines.flow.*

/**
 * ViewModel for the peers/discovery screen and mesh stats.
 *
 * Exposes the unified [NearbyUser] list (deduplicated, clean names)
 * split into connected and nearby-only sections for the UI.
 */
class PeersViewModel(
    private val meshManager: MeshManager
) : ViewModel() {

    /** All nearby users (unified, deduplicated) */
    val nearbyUsers: StateFlow<List<NearbyUser>> = meshManager.nearbyUsers

    /** Connected users (filtered from nearbyUsers) */
    val connectedUsers: StateFlow<List<NearbyUser>> = nearbyUsers
        .map { users -> users.filter { it.status == UserStatus.CONNECTED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Nearby-only users (not yet connected) */
    val nearbyOnlyUsers: StateFlow<List<NearbyUser>> = nearbyUsers
        .map { users -> users.filter { it.status != UserStatus.CONNECTED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Currently active mesh peers (raw, for backward compatibility) */
    val peers: StateFlow<List<MeshPeer>> = meshManager.activePeers

    /** Real-time mesh statistics */
    val meshStats: StateFlow<MeshStats> = meshManager.meshStats

    /** Current discovery state machine phase */
    val discoveryState: StateFlow<DiscoveryState> = meshManager.discoveryState

    /** Which discovery source is currently active */
    val activeDiscoverySource: StateFlow<ConnectionType?> = meshManager.activeDiscoverySource

    /** Whether any scan is currently active (derived from discoveryState) */
    val isDiscovering: StateFlow<Boolean> = discoveryState
        .map { it != DiscoveryState.IDLE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Trigger an immediate peer discovery sweep.
     * Cancels any ongoing scan cycle and starts a fresh BLE → WiFi scan.
     */
    fun refreshPeers() {
        meshManager.peerDiscoveryManager.requestScan()
    }

    /**
     * Connect to a nearby user — auto-initiates WiFi Direct connection.
     */
    fun connectToUser(userId: String) {
        meshManager.connectToUser(userId)
    }
}

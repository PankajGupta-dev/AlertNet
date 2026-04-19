package com.alertnet.app.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alertnet.app.AlertNetApplication
import com.alertnet.app.ui.screen.ChatScreen
import com.alertnet.app.ui.screen.LocationPrivacySettingsScreen
import com.alertnet.app.ui.screen.MeshMapScreen
import com.alertnet.app.ui.screen.MeshStatsScreen
import com.alertnet.app.ui.screen.PeersScreen
import com.alertnet.app.ui.viewmodel.*

/**
 * Navigation graph for AlertNet.
 *
 * Routes:
 * - "peers" → Nearby Users screen (home) — WhatsApp-style
 * - "chat/{peerId}/{peerName}" → Chat with a specific peer
 * - "stats" → Mesh network statistics
 * - "mesh_map?focusLat={}&focusLon={}&pinType={}" → Offline map with peer locations
 * - "location_privacy_settings" → Location broadcast & local map toggles
 */
@Composable
fun NavGraph(app: AlertNetApplication) {
    val navController = rememberNavController()
    val factory = remember { ViewModelFactory(app) }

    NavHost(
        navController = navController,
        startDestination = "peers"
    ) {
        composable("peers") {
            val viewModel: PeersViewModel = viewModel(factory = factory)
            val connectedUsers by viewModel.connectedUsers.collectAsState()
            val nearbyUsers by viewModel.nearbyOnlyUsers.collectAsState()
            val meshStats by viewModel.meshStats.collectAsState()
            val isDiscovering by viewModel.isDiscovering.collectAsState()
            val discoveryState by viewModel.discoveryState.collectAsState()
            val activeSource by viewModel.activeDiscoverySource.collectAsState()

            PeersScreen(
                connectedUsers = connectedUsers,
                nearbyUsers = nearbyUsers,
                meshStats = meshStats,
                isDiscovering = isDiscovering,
                discoveryState = discoveryState,
                activeSource = activeSource,
                onUserClick = { user ->
                    // Auto-initiate connection if not already connected
                    viewModel.connectToUser(user.id)
                    val name = user.name.replace("/", "-")
                    navController.navigate("chat/${user.id}/$name")
                },
                onRefresh = { viewModel.refreshPeers() },
                onStatsClick = { navController.navigate("stats") },
                onMapClick = { navController.navigate("mesh_map") },
                onLocationSettingsClick = { navController.navigate("location_privacy_settings") }
            )
        }

        composable(
            route = "chat/{peerId}/{peerName}",
            arguments = listOf(
                navArgument("peerId") { type = NavType.StringType },
                navArgument("peerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: return@composable
            val peerName = backStackEntry.arguments?.getString("peerName") ?: "Peer"
            val chatVm: ChatViewModel = viewModel(factory = factory)
            val locationShareVm: LocationShareViewModel = viewModel(factory = factory)

            ChatScreen(
                peerId = peerId,
                peerName = peerName,
                viewModel = chatVm,
                locationShareViewModel = locationShareVm,
                onBack = { navController.popBackStack() },
                onViewOnMap = { lat, lon ->
                    navController.navigate(
                        "mesh_map?focusLat=$lat&focusLon=$lon&pinType=SHARED_LOCATION"
                    )
                }
            )
        }

        composable("stats") {
            val viewModel: PeersViewModel = viewModel(factory = factory)
            val meshStats by viewModel.meshStats.collectAsState()

            MeshStatsScreen(
                stats = meshStats,
                deviceId = app.deviceId,
                onBack = { navController.popBackStack() }
            )
        }

        // ─── Mesh Map ────────────────────────────────────────────
        composable(
            route = "mesh_map?focusLat={focusLat}&focusLon={focusLon}&pinType={pinType}",
            arguments = listOf(
                navArgument("focusLat") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("focusLon") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("pinType") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val focusLat = backStackEntry.arguments?.getString("focusLat")?.toDoubleOrNull()
            val focusLon = backStackEntry.arguments?.getString("focusLon")?.toDoubleOrNull()
            val pinType = backStackEntry.arguments?.getString("pinType")
            val mapVm: MeshMapViewModel = viewModel(factory = factory)

            MeshMapScreen(
                focusLat = focusLat,
                focusLon = focusLon,
                pinType = pinType,
                viewModel = mapVm,
                onBack = { navController.popBackStack() }
            )
        }

        // ─── Location Privacy Settings ───────────────────────────
        composable("location_privacy_settings") {
            val privacyVm: LocationPrivacyViewModel = viewModel(factory = factory)

            LocationPrivacySettingsScreen(
                viewModel = privacyVm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}


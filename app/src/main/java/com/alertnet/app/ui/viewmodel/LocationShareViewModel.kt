package com.alertnet.app.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertnet.app.model.LocationSharePayload
import com.alertnet.app.repository.SettingsRepository
import com.alertnet.app.service.LocationForegroundService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for LocationShareBottomSheet.
 *
 * Fetches a one-shot GPS location on init (does NOT broadcast to mesh).
 * Exposes the location state for the preview map and send button.
 */
class LocationShareViewModel(
    private val settingsRepository: SettingsRepository,
    private val locationService: LocationForegroundService?
) : ViewModel() {

    sealed class LocationState {
        data object Acquiring : LocationState()
        data class Ready(val payload: LocationSharePayload) : LocationState()
        data object Failed : LocationState()
    }

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Acquiring)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    val isBroadcastingToMesh: StateFlow<Boolean> = settingsRepository
        .observeMeshLocationBroadcast()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        fetchLocation()
    }

    private fun fetchLocation() {
        if (locationService == null) {
            _locationState.value = LocationState.Failed
            return
        }

        locationService.requestOneShotForShare { location ->
            _locationState.value = if (location != null) {
                LocationState.Ready(
                    LocationSharePayload(
                        lat = location.latitude,
                        lon = location.longitude,
                        accuracyMeters = location.accuracy,
                        altitudeMeters = if (location.hasAltitude()) location.altitude.toFloat() else null,
                        timestampEpochSec = location.time / 1000
                    )
                )
            } else {
                LocationState.Failed
            }
        }
    }
}

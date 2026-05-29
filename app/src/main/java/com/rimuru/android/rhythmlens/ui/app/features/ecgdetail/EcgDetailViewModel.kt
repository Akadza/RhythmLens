package com.rimuru.android.rhythmlens.ui.app.features.ecgdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.rimuru.android.rhythmlens.ui.navigation.EcgDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class EcgDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val destination = savedStateHandle.toRoute<EcgDetailDestination>()
    val ecgId: String = destination.ecgId

    private val _effect = Channel<EcgDetailEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: EcgDetailEvent) {
        when (event) {
            EcgDetailEvent.BackClicked -> {
                _effect.trySend(EcgDetailEffect.NavigateBack)
            }

            EcgDetailEvent.CompareClicked -> {
                _effect.trySend(EcgDetailEffect.NavigateToComparison(ecgId))
            }

            EcgDetailEvent.SyntheticClicked -> {
                _effect.trySend(EcgDetailEffect.NavigateToSyntheticImage(ecgId))
            }

            EcgDetailEvent.ExportClicked -> {
                _effect.trySend(EcgDetailEffect.NavigateToExport(ecgId))
            }

            EcgDetailEvent.DoctorConclusionClicked -> {
                _effect.trySend(EcgDetailEffect.OpenDoctorConclusion(ecgId))
            }

            EcgDetailEvent.DeleteClicked -> {
                _effect.trySend(EcgDetailEffect.ConfirmDelete(ecgId))
            }
        }
    }
}

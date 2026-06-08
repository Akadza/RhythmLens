package com.rimuru.android.rhythmlens.ui.app.features.syntheticimage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rimuru.android.rhythmlens.domain.usecase.GenerateSyntheticImageUseCase
import com.rimuru.android.rhythmlens.ui.navigation.SyntheticImageDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyntheticImageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val generateSyntheticImageUseCase: GenerateSyntheticImageUseCase
) : ViewModel() {

    private val destination = savedStateHandle.toRoute<SyntheticImageDestination>()

    private val _uiState = MutableStateFlow(
        SyntheticImageUiState(
            ecgId = destination.ecgId,
            isLoading = true
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<SyntheticImageEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadSyntheticImage(force = false)
    }

    fun onEvent(event: SyntheticImageEvent) {
        when (event) {
            SyntheticImageEvent.BackClicked -> {
                _effect.trySend(SyntheticImageEffect.NavigateBack)
            }

            SyntheticImageEvent.RetryClicked -> {
                loadSyntheticImage(force = true)
            }

            SyntheticImageEvent.SaveClicked -> {
                _uiState.value.imageUri?.let { imageUri ->
                    _effect.trySend(SyntheticImageEffect.SaveImage(imageUri))
                }
            }

            SyntheticImageEvent.ShareClicked -> {
                _uiState.value.imageUri?.let { imageUri ->
                    _effect.trySend(SyntheticImageEffect.ShareImage(imageUri))
                }
            }
        }
    }

    private fun loadSyntheticImage(force: Boolean) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            runCatching {
                generateSyntheticImageUseCase(destination.ecgId, force = force)
            }.onSuccess { imageUri ->
                _uiState.update { state ->
                    state.copy(
                        imageUri = imageUri,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Не удалось сформировать изображение ЭКГ"
                    )
                }
            }
        }
    }
}

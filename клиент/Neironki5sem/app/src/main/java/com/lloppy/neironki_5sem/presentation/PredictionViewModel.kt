package com.lloppy.neironki_5sem.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloppy.neironki_5sem.data.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class PredictionViewModel : ViewModel() {
    private val api = ApiService.create()

    private val _imageBytes = MutableStateFlow<ByteArray?>(null)
    val imageBytes: StateFlow<ByteArray?> = _imageBytes

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun sendImage(bytes: ByteArray) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "capture.jpg", body)
                val resp = api.predictFromNpy(part)
                if (resp.isSuccessful) {
                    _imageBytes.value = resp.body()?.bytes()
                } else {
                    _error.value = "Server error: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}
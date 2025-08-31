package com.example.bixi.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bixi.AppSession
import com.example.bixi.enums.AttendanceType
import com.example.bixi.helper.ApiStatus
import com.example.bixi.models.api.AttendanceRequest
import com.example.bixi.models.api.Position
import com.example.bixi.services.RetrofitClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

class TimekeepingViewModel : BaseViewModel() {

    private val _attendanceStatus = MutableLiveData<AttendanceType>()
    val attendanceStatus: LiveData<AttendanceType> = _attendanceStatus

    init {
        _attendanceStatus.postValue(AttendanceType.fromValue(AppSession.user!!.attendanceStatus))
    }

    fun sendAction(coordinates: LatLng) {
        setLoading(true)
        viewModelScope.launch {
            try {
                val response = RetrofitClient.attendance(AttendanceRequest(AppSession.user!!.user.id,
                    _attendanceStatus.value!!.toString(),
                    Position(coordinates.latitude, coordinates.longitude)))
                if (response.success) {
                    _attendanceStatus.postValue(if(_attendanceStatus.value == AttendanceType.START) AttendanceType.STOP else AttendanceType.START )
                    _sendResponseCode.postValue(response.statusCode)
                } else {
                    _sendResponseCode.postValue(response.statusCode)
                }

            } catch (e: Exception) {
                Log.e("API", "Exception: ${e.message}")
                _sendResponseCode.postValue(ApiStatus.SERVER_ERROR.code)
            }
        }
    }
}
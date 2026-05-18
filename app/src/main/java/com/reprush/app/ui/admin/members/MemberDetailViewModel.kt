package com.reprush.app.ui.admin.members

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.model.MembershipPackage
import com.reprush.app.data.repository.AttendanceRecord
import com.reprush.app.data.repository.AttendanceRepository
import com.reprush.app.data.repository.MemberDetail
import com.reprush.app.data.repository.MemberRepository
import com.reprush.app.data.repository.PackageRepository
import com.reprush.app.data.repository.PaymentRecord
import com.reprush.app.data.repository.PaymentRepository
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val packageRepository: PackageRepository,
    private val paymentRepository: PaymentRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _member = MutableLiveData<MemberDetail>()
    val member: LiveData<MemberDetail> = _member

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operationResult = MutableLiveData<Result<Unit>>()
    val operationResult: LiveData<Result<Unit>> = _operationResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _payments = MutableLiveData<List<PaymentRecord>>()
    val payments: LiveData<List<PaymentRecord>> = _payments

    private val _attendance = MutableLiveData<List<AttendanceRecord>>()
    val attendance: LiveData<List<AttendanceRecord>> = _attendance

    private val _activePackages = MutableLiveData<List<MembershipPackage>>()
    val activePackages: LiveData<List<MembershipPackage>> = _activePackages

    fun loadMember(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            when (val result = memberRepository.getMemberById(uid)) {
                is Result.Success -> {
                    val memberData = result.data
                    val pkgName = memberData.packageId?.let { pkgId ->
                        when (val pkgResult = packageRepository.getPackageById(pkgId)) {
                            is Result.Success -> pkgResult.data.name
                            is Result.Error -> null
                        }
                    }
                    _member.postValue(memberData.copy(packageName = pkgName))
                }
                is Result.Error -> _error.postValue(result.message)
            }
            _isLoading.postValue(false)
        }
    }

    fun loadPayments(memberId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = paymentRepository.getPaymentsForMember(memberId)) {
                is Result.Success -> _payments.postValue(result.data)
                is Result.Error -> _payments.postValue(emptyList())
            }
        }
    }

    fun loadAttendance(memberId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = attendanceRepository.getMemberAttendance(memberId)) {
                is Result.Success -> _attendance.postValue(result.data)
                is Result.Error -> _attendance.postValue(emptyList())
            }
        }
    }

    fun suspendMember(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = memberRepository.suspendMember(uid)
            _operationResult.postValue(result)
            if (result is Result.Success) loadMember(uid)
            else _isLoading.postValue(false)
        }
    }

    fun reactivateMember(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = memberRepository.reactivateMember(uid)
            _operationResult.postValue(result)
            if (result is Result.Success) loadMember(uid)
            else _isLoading.postValue(false)
        }
    }

    fun removeMember(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = memberRepository.removeMember(uid)
            _operationResult.postValue(result)
            _isLoading.postValue(false)
        }
    }

    fun loadActivePackages() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = packageRepository.getActivePackages()) {
                is Result.Success -> _activePackages.postValue(result.data)
                is Result.Error -> {}
            }
        }
    }

    fun assignPackage(uid: String, pkg: MembershipPackage) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = memberRepository.assignPackage(uid, pkg.id, pkg.durationDays)
            _operationResult.postValue(result)
            if (result is Result.Success) loadMember(uid)
            else _isLoading.postValue(false)
        }
    }
}
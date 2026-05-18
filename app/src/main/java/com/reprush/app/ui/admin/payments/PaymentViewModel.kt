package com.reprush.app.ui.admin.payments

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.model.MembershipPackage
import com.reprush.app.data.repository.Member
import com.reprush.app.data.repository.MemberRepository
import com.reprush.app.data.repository.PackageRepository
import com.reprush.app.data.repository.PaymentRecord
import com.reprush.app.data.repository.PaymentRepository
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val memberRepository: MemberRepository,
    private val packageRepository: PackageRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _members = MutableLiveData<List<Member>>()
    val members: LiveData<List<Member>> = _members

    private val _activePackages = MutableLiveData<List<MembershipPackage>>()
    val activePackages: LiveData<List<MembershipPackage>> = _activePackages

    private val _operationResult = MutableLiveData<Result<String>?>(null)
    val operationResult: LiveData<Result<String>?> = _operationResult

    private val _voidResult = MutableLiveData<Result<Unit>?>(null)
    val voidResult: LiveData<Result<Unit>?> = _voidResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _paymentLog = MutableLiveData<List<PaymentRecord>>()
    val paymentLog: LiveData<List<PaymentRecord>> = _paymentLog

    private val _memberPayments = MutableLiveData<List<PaymentRecord>>()
    val memberPayments: LiveData<List<PaymentRecord>> = _memberPayments

    private val _receipt = MutableLiveData<PaymentRecord?>()
    val receipt: LiveData<PaymentRecord?> = _receipt

    private val _monthlyRevenue = MutableLiveData<Double>()
    val monthlyRevenue: LiveData<Double> = _monthlyRevenue

    private val _yearlyRevenue = MutableLiveData<Double>()
    val yearlyRevenue: LiveData<Double> = _yearlyRevenue

    fun loadMembers() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = memberRepository.getMembers("active")) {
                is Result.Success -> _members.postValue(result.data)
                is Result.Error -> _members.postValue(emptyList())
            }
        }
    }

    fun loadActivePackages() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = packageRepository.getActivePackages()) {
                is Result.Success -> _activePackages.postValue(result.data)
                is Result.Error -> _activePackages.postValue(emptyList())
            }
        }
    }

    fun recordPayment(
        memberId: String,
        memberName: String,
        packageId: String,
        packageName: String,
        amount: Double,
        paymentMethod: String,
        paymentDate: String,
        periodStart: String,
        periodEnd: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val recordedBy = auth.currentUser?.uid ?: ""
            val result = paymentRepository.recordPayment(
                memberId, packageId, amount, paymentMethod, paymentDate,
                periodStart, periodEnd, recordedBy
            )
            _operationResult.postValue(result)

            if (result is Result.Success) {
                val paymentId = result.data
                try {
                    paymentRepository.writeReceiptNotification(
                        memberId, paymentId, memberName, packageName, amount, periodStart, periodEnd
                    )
                } catch (e: Exception) {
                    Log.w("PaymentViewModel", "Failed to write receipt notification", e)
                }
            }
            _isLoading.postValue(false)
        }
    }

    fun voidPayment(paymentId: String, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = paymentRepository.voidPayment(paymentId, reason)
            _voidResult.postValue(result)
            _isLoading.postValue(false)
        }
    }

    fun loadPaymentLog(startDate: String? = null, endDate: String? = null, memberId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            when (val result = paymentRepository.getPaymentLog(startDate, endDate, memberId)) {
                is Result.Success -> {
                    val payments = enrichPaymentsWithNames(result.data)
                    _paymentLog.postValue(payments)
                }
                is Result.Error -> _paymentLog.postValue(emptyList())
            }
            _isLoading.postValue(false)
        }
    }

    fun loadMemberPayments(memberId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = paymentRepository.getPaymentsForMember(memberId)) {
                is Result.Success -> {
                    val payments = enrichPaymentsWithNames(result.data)
                    _memberPayments.postValue(payments)
                }
                is Result.Error -> _memberPayments.postValue(emptyList())
            }
        }
    }

    fun loadReceipt(paymentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = paymentRepository.getPaymentById(paymentId)) {
                is Result.Success -> {
                    val payment = result.data
                    val memberName = getMemberName(payment.memberId)
                    val packageName = getPackageName(payment.packageId)
                    _receipt.postValue(payment.copy(memberName = memberName, packageName = packageName))
                }
                is Result.Error -> _receipt.postValue(null)
            }
        }
    }

    fun loadRevenueStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val now = YearMonth.now()
            when (val result = paymentRepository.getMonthlyRevenue(now)) {
                is Result.Success -> _monthlyRevenue.postValue(result.data)
                is Result.Error -> _monthlyRevenue.postValue(0.0)
            }
            when (val result = paymentRepository.getYearlyRevenue(now.year)) {
                is Result.Success -> _yearlyRevenue.postValue(result.data)
                is Result.Error -> _yearlyRevenue.postValue(0.0)
            }
        }
    }

    fun clearOperationResult() {
        _operationResult.postValue(null)
    }

    fun clearVoidResult() {
        _voidResult.postValue(null)
    }

    private suspend fun enrichPaymentsWithNames(payments: List<PaymentRecord>): List<PaymentRecord> {
        val memberIds = payments.map { it.memberId }.distinct()
        val packageIds = payments.map { it.packageId }.distinct()
        val memberNames = mutableMapOf<String, String>()
        val packageNames = mutableMapOf<String, String>()

        for (id in memberIds) {
            memberNames[id] = getMemberName(id)
        }
        for (id in packageIds) {
            packageNames[id] = getPackageName(id)
        }

        return payments.map { p ->
            p.copy(
                memberName = memberNames[p.memberId] ?: p.memberId,
                packageName = packageNames[p.packageId] ?: p.packageId
            )
        }
    }

    private suspend fun getMemberName(memberId: String): String {
        return when (val result = memberRepository.getMemberById(memberId)) {
            is Result.Success -> result.data.displayName
            is Result.Error -> memberId
        }
    }

    private suspend fun getPackageName(packageId: String): String {
        if (packageId.isEmpty()) return ""
        return when (val result = packageRepository.getPackageById(packageId)) {
            is Result.Success -> result.data.name
            is Result.Error -> packageId
        }
    }
}

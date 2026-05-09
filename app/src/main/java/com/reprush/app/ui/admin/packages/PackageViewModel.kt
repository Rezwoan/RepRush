package com.reprush.app.ui.admin.packages

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.model.MembershipPackage
import com.reprush.app.data.repository.PackageRepository
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PackageViewModel @Inject constructor(
    private val repository: PackageRepository
) : ViewModel() {

    private val _packages = MutableLiveData<List<MembershipPackage>>()
    val packages: LiveData<List<MembershipPackage>> = _packages

    private val _activePackages = MutableLiveData<List<MembershipPackage>>()
    val activePackages: LiveData<List<MembershipPackage>> = _activePackages

    private val _operationResult = MutableLiveData<Result<*>>()
    val operationResult: LiveData<Result<*>> = _operationResult

    fun loadPackages() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getAllPackages()
            if (result is Result.Success) {
                _packages.postValue(result.data)
            }
            _operationResult.postValue(result)
        }
    }

    fun loadActivePackages() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getActivePackages()
            if (result is Result.Success) {
                _activePackages.postValue(result.data)
            }
            _operationResult.postValue(result)
        }
    }

    fun createPackage(name: String, price: Double, durationDays: Int, description: String?) {
        val pkg = MembershipPackage(
            name = name,
            price = price,
            durationDays = durationDays,
            description = description
        )
        viewModelScope.launch(Dispatchers.IO) {
            _operationResult.postValue(repository.createPackage(pkg))
        }
    }

    fun updatePackage(pkg: MembershipPackage) {
        viewModelScope.launch(Dispatchers.IO) {
            _operationResult.postValue(repository.updatePackage(pkg))
        }
    }

    fun deactivatePackage(packageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _operationResult.postValue(repository.deactivatePackage(packageId))
        }
    }
}

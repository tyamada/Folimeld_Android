package com.tyamada.folimeld.ui.main

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.tyamada.folimeld.domain.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    val isSupporter: StateFlow<Boolean> = billingRepository.isSupporter
    val supportProductPrice: StateFlow<String?> = billingRepository.supportProductPrice

    init {
        billingRepository.startBillingConnection()
    }

    override fun onCleared() {
        super.onCleared()
        billingRepository.terminateBillingConnection()
    }

    fun onSupportClick(activity: Activity) {
        billingRepository.launchBillingFlow(activity)
    }

    fun onRefreshClick() {
        billingRepository.queryPurchases()
    }
}

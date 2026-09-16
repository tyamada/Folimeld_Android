package com.tyamada.folimeld.domain.repository

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {
    val isSupporter: StateFlow<Boolean>
    val supportProductPrice: StateFlow<String?>

    fun startBillingConnection()
    fun terminateBillingConnection()
    fun launchBillingFlow(activity: Activity)
    fun queryPurchases()
}

package com.tyamada.folimeld.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.tyamada.folimeld.domain.repository.BillingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BillingRepository, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _isSupporter = MutableStateFlow(false)
    override val isSupporter: StateFlow<Boolean> = _isSupporter.asStateFlow()

    private val _supportProductPrice = MutableStateFlow<String?>(null)
    override val supportProductPrice: StateFlow<String?> = _supportProductPrice.asStateFlow()

    private var productDetails: ProductDetails? = null

    companion object {
        private const val PRODUCT_ID = "supporter_pack"
    }

    override fun startBillingConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request
            }
        })
    }

    override fun terminateBillingConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    private fun queryProductDetails() {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = queryProductDetailsResult.productDetailsList
                productDetails = productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
                productDetails?.let {
                    _supportProductPrice.value = it.oneTimePurchaseOfferDetails?.formattedPrice
                }
            }
        }
    }

    override fun queryPurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
    }

    override fun launchBillingFlow(activity: Activity) {
        val details = productDetails ?: return
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases)
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val isPurchased = purchases.any { purchase ->
            purchase.products.contains(PRODUCT_ID) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        
        if (isPurchased) {
            _isSupporter.value = true
            // Acknowledge the purchase if necessary
            purchases.forEach { purchase ->
                if (purchase.products.contains(PRODUCT_ID) && !purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
        } else {
            _isSupporter.value = false
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _isSupporter.value = true
            }
        }
    }
}

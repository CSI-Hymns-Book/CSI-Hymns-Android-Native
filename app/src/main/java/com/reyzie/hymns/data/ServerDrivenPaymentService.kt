package com.reyzie.hymns.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ServerDrivenPaymentService(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun startCheckout(
        gateway: PaymentGatewayRow,
        amount: Double,
        currency: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rawUrl = gateway.edgeFunctionUrl
            val edgeUrl = if (!rawUrl.isNullOrBlank()) {
                rawUrl
            } else if (gateway.name.equals("razorpay", ignoreCase = true)) {
                "https://xwrlyeyxrqjlyeefyfls.supabase.co/functions/v1/razorpay-checkout"
            } else {
                "https://xwrlyeyxrqjlyeefyfls.supabase.co/functions/v1/adyen-checkout"
            }

            val jsonBody = JSONObject().apply {
                put("amount", amount)
                put("currency", currency)
                put("returnUrl", "https://csihymns.app/donation_result")
                put("callback_url", "https://csihymns.app/donation_result")
                put("description", "CSI Hymns Support Donation")
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(edgeUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseText = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errObj = try { JSONObject(responseText) } catch (e: Exception) { null }
                val msg = errObj?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "Server request failed (HTTP ${response.code})"
                return@withContext Result.failure(Exception(msg))
            }

            val json = JSONObject(responseText)
            val hostedUrl = json.optString("hostedUrl").ifBlank { json.optString("url") }

            if (hostedUrl.isBlank()) {
                return@withContext Result.failure(Exception("Payment URL missing from response"))
            }

            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(hostedUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ServerDrivenPayment", "Error launching checkout", e)
            Result.failure(e)
        }
    }
}

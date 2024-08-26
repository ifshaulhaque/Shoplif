package com.ifsha.shoplif.screens.onboarding

import android.content.Context
import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(): ViewModel() {
    fun sendSMS(phoneNumber: String, message: String) {
        if (phoneNumber.isNotBlank() && message.isNotBlank()) {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        }
    }

    fun readLastSmsFromNumber(phoneNumber: String, context: Context): String {
        // Normalize the input phone number to a standard format for comparison
        val normalizedPhoneNumber = PhoneNumberUtils.normalizeNumber(phoneNumber)

        val smsUri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")
        val sortOrder = "date DESC"

        var smsContent = "No SMS found"

        val cursor = context.contentResolver.query(smsUri, projection, null, null, sortOrder)
        cursor?.use {
            while (it.moveToNext()) {
                val addressIndex = it.getColumnIndexOrThrow("address")
                val smsAddress = it.getString(addressIndex)

                // Normalize the SMS address for proper comparison
                val normalizedSmsAddress = PhoneNumberUtils.normalizeNumber(smsAddress)

                // Check if normalized numbers match
                if (PhoneNumberUtils.compare(context, normalizedPhoneNumber, normalizedSmsAddress)) {
                    val bodyIndex = it.getColumnIndexOrThrow("body")
                    smsContent = it.getString(bodyIndex)
                    break
                }
            }
        }

        return smsContent
    }
}
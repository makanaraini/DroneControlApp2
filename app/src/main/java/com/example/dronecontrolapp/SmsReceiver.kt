package com.example.dronecontrolapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.dronecontrolapp.viewmodel.DroneViewModel

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                val sender = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.messageBody

                // Process the telemetry data
                if (sender == "+1234567890") { // Replace with the drone's phone number
                    updateTelemetry(context, messageBody)
                }
            }
        }
    }

    private fun updateTelemetry(context: Context, telemetry: String) {
        // Parse and update telemetry data in the app
        // You might want to use a ViewModel or a shared state to update the UI
        val droneViewModel: DroneViewModel = ViewModelProvider(context as ComponentActivity).get(DroneViewModel::class.java)
        droneViewModel.updateTelemetryFromSms(telemetry)
    }
} 
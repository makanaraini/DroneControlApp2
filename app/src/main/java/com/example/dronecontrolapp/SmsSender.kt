package com.example.dronecontrolapp

import android.telephony.SmsManager

object SmsSender {
    fun sendCommand(phoneNumber: String, command: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phoneNumber, null, command, null, null)
    }
} 
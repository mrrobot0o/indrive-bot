package com.indriver.bot.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class InDriverAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "InDriverBot"
        const val PACKAGE_INDRIVER = "sinet.startup.inDriver"
        
        @Volatile
        private var instance: InDriverAccessibilityService? = null
        
        fun getInstance(): InDriverAccessibilityService? = instance
    }

    private var totalOrdersDetected = 0
    private var ordersAccepted = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
            packageNames = arrayOf(PACKAGE_INDRIVER)
        }
        serviceInfo = info
        
        Log.d(TAG, "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        if (packageName != PACKAGE_INDRIVER) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> handleNotification(event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> handleWindowChange(event)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
    }

    private fun handleNotification(event: AccessibilityEvent) {
        val text = event.text?.joinToString(" ") ?: ""
        Log.d(TAG, "Notification: $text")
        
        if (text.contains("طلب", ignoreCase = true) || text.contains("order", ignoreCase = true)) {
            totalOrdersDetected++
            Log.d(TAG, "New order detected! Total: $totalOrdersDetected")
            tryAcceptOrder()
        }
    }

    private fun handleWindowChange(event: AccessibilityEvent) {
        // Check for order-related screens
        val className = event.className?.toString() ?: ""
        if (className.contains("Order", ignoreCase = true)) {
            tryAcceptOrder()
        }
    }

    private fun tryAcceptOrder() {
        val root = rootInActiveWindow ?: return
        
        val acceptTexts = listOf("قبول", "Accept", "Принять", "ACEPTAR", "ACCEPT")
        
        for (text in acceptTexts) {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                if (node.isClickable) {
                    val accepted = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (accepted) {
                        ordersAccepted++
                        showToast("✅ Order Accepted!")
                        Log.d(TAG, "Order accepted! Total: $ordersAccepted")
                        return
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun getStats(): Pair<Int, Int> = Pair(totalOrdersDetected, ordersAccepted)

    fun resetStats() {
        totalOrdersDetected = 0
        ordersAccepted = 0
    }
}

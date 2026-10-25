package com.smarthealth.companion.model

enum class DataSource(val label: String) {
    WEARABLE("ESP32-S3 Wearable"),
    MANUAL("Manual Entry"),
    IMPORTED("Imported Data"),
    SIMULATED("Simulated Demo")
}

enum class DataQualityStatus(val label: String) {
    EXCELLENT("Excellent Signal"),
    GOOD("Good Signal"),
    MARGINAL("Marginal Quality"),
    POOR("Poor Quality / Motion Artifact"),
    MISSING_OR_MALFORMED("Missing or Malformed")
}

enum class BaselineComparison(val label: String) {
    WITHIN_BASELINE("Within Personal Baseline"),
    UNUSUAL_CHANGE("Unusual Change Detected"),
    REVIEW_QUALITY("Review Measurement Quality"),
    INSUFFICIENT_DATA("Insufficient Historical Data"),
    URGENT_CONCERN("Possible Urgent Concern")
}

enum class BleConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    DISCOVERING_SERVICES,
    NOTIFICATION_READY,
    RECONNECTING,
    ERROR
}

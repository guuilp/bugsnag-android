package com.bugsnag.android.internal

/**
 * Stores internal metrics for Bugsnag use.
 */
interface InternalMetrics {
    /**
     * Returns a map that can be merged with the top-level JSON report.
     */
    fun toJsonableMap(): Map<String, Any>

    fun setConfigDifferences(differences: Map<String, Any>)

    fun setCallbackCounts(newCallbackCounts: Map<String, Int>)

    fun notifyAddCallback(callback: String)

    fun notifyRemoveCallback(callback: String)
}

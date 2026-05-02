package com.rimuru.android.rhythmlens.ui.navigation

import kotlinx.serialization.Serializable

// Top-level destinations (для Bottom Navigation)
@Serializable
sealed class BottomNavDestination {
    @Serializable
    data object Home : BottomNavDestination()

    @Serializable
    data object History : BottomNavDestination()

    @Serializable
    data object Patients : BottomNavDestination()   // только для врача

    @Serializable
    data object Profile : BottomNavDestination()
}

// Детальные экраны
@Serializable
data class EcgDetailDestination(val ecgId: String)

@Serializable
data class ComparisonDestination(val ecgIds: List<String>)

@Serializable
object ScanDestination

@Serializable
data class SyntheticImageDestination(val ecgId: String)
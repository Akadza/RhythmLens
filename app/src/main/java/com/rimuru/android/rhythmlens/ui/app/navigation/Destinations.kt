package com.rimuru.android.rhythmlens.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object AuthDestination

@Serializable
data object MainDestination

@Serializable
sealed class BottomNavDestination {

    @Serializable
    data object Home : BottomNavDestination()

    @Serializable
    data object History : BottomNavDestination()

    @Serializable
    data object Patients : BottomNavDestination()

    @Serializable
    data object Profile : BottomNavDestination()
}

@Serializable
data class EcgDetailDestination(
    val ecgId: String
)

@Serializable
data class ComparisonDestination(
    val baseEcgId: String,
    val comparedEcgId: String? = null
)

@Serializable
data object ScanDestination

@Serializable
data class SyntheticImageDestination(
    val ecgId: String
)

@Serializable
data class ExportDestination(
    val ecgId: String
)

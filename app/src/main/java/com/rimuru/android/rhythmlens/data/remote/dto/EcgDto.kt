package com.rimuru.android.rhythmlens.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EcgRecordDto(
    val id: String,
    @SerialName("owner_user_id")
    val ownerUserId: String,
    @SerialName("uploaded_by_user_id")
    val uploadedByUserId: String,
    val status: String,
    @SerialName("storage_dir")
    val storageDir: String,
    @SerialName("digitized_csv_path")
    val digitizedCsvPath: String? = null,
    @SerialName("digitization_metadata_path")
    val digitizationMetadataPath: String? = null,
    @SerialName("completed_csv_path")
    val completedCsvPath: String? = null,
    @SerialName("completion_plot_path")
    val completionPlotPath: String? = null,
    @SerialName("analysis_json_path")
    val analysisJsonPath: String? = null,
    @SerialName("analysis_report_path")
    val analysisReportPath: String? = null,
    @SerialName("top_predictions")
    val topPredictions: List<EcgPredictionDto> = emptyList(),
    @SerialName("synthetic_image_path")
    val syntheticImagePath: String? = null,
    @SerialName("error_message")
    val errorMessage: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class EcgPredictionDto(
    val label: String,
    val probability: Double,
    @SerialName("detected")
    val isDetected: Boolean
)

@Serializable
data class EcgSignalDto(
    @SerialName("ecg_id")
    val ecgId: String,
    @SerialName("sampling_rate")
    val samplingRate: Int,
    @SerialName("duration_seconds")
    val durationSeconds: Double,
    val leads: List<EcgSignalLeadDto> = emptyList()
)

@Serializable
data class EcgSignalLeadDto(
    val lead: String,
    val origin: String,
    val segments: List<EcgSignalSegmentDto> = emptyList()
)

@Serializable
data class EcgSignalSegmentDto(
    val origin: String,
    @SerialName("start_sample_index")
    val startSampleIndex: Int,
    val voltage: List<Double> = emptyList()
)

@Serializable
data class DoctorConclusionDto(
    @SerialName("ecg_id")
    val ecgId: String,
    @SerialName("doctor_id")
    val doctorId: String,
    val text: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class SaveDoctorConclusionRequestDto(
    val text: String
)

@Serializable
data class SyntheticImageDto(
    @SerialName("ecg_id")
    val ecgId: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("image_path")
    val imagePath: String,
    val layout: String,
    @SerialName("rhythm_lead")
    val rhythmLead: String,
    val format: String
)

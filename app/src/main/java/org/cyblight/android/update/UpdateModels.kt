package org.cyblight.android.update

data class AppUpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val assetName: String,
)

enum class UpdateStatus {
    Idle,
    Available,
    Downloading,
    ReadyToInstall,
    Error,
}

data class UpdateUiState(
    val visible: Boolean = false,
    val versionName: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val status: UpdateStatus = UpdateStatus.Idle,
    val progress: Float = 0f,
    val errorMessage: String? = null,
)

data class ManualUpdateCheckState(
    val visible: Boolean = false,
    val checking: Boolean = false,
    val upToDate: Boolean = false,
    val errorMessage: String? = null,
)

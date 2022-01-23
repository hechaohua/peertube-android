package net.schueller.peertube.feature_video.presentation.video_play

import net.schueller.peertube.feature_video.domain.model.Description

data class VideoDescriptionState(
    val isLoading: Boolean = false,
    val description: Description? = null,
    val error: String = ""
)

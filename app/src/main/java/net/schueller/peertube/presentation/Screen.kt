package net.schueller.peertube.presentation

sealed class Screen(val route: String) {
    object VideoListScreen: Screen("video_list_screen")
    object VideoPlayScreen: Screen("video_play_screen")
    object VideoDescriptionScreen: Screen("video_description_screen")
    object SettingsScreen: Screen("settings_screen")
    object MeScreen: Screen("me_screen")
}
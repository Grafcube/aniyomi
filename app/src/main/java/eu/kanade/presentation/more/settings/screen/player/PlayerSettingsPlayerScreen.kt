package eu.kanade.presentation.more.settings.screen.player

import android.content.pm.ActivityInfo
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.preference.asState
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.data.torrentServer.TorrentServerPreferences
import eu.kanade.tachiyomi.data.torrentServer.service.TorrentServerService
import eu.kanade.tachiyomi.ui.player.JUST_PLAYER
import eu.kanade.tachiyomi.ui.player.MPV_KT
import eu.kanade.tachiyomi.ui.player.MPV_KT_PREVIEW
import eu.kanade.tachiyomi.ui.player.MPV_PLAYER
import eu.kanade.tachiyomi.ui.player.MPV_REMOTE
import eu.kanade.tachiyomi.ui.player.MX_PLAYER
import eu.kanade.tachiyomi.ui.player.MX_PLAYER_FREE
import eu.kanade.tachiyomi.ui.player.MX_PLAYER_PRO
import eu.kanade.tachiyomi.ui.player.NEXT_PLAYER
import eu.kanade.tachiyomi.ui.player.VLC_PLAYER
import eu.kanade.tachiyomi.ui.player.WEB_VIDEO_CASTER
import eu.kanade.tachiyomi.ui.player.X_PLAYER
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object PlayerSettingsPlayerScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_player_internal

    @Composable
    override fun getPreferences(): List<Preference> {
        val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val torrentServerPreferences = remember { Injekt.get<TorrentServerPreferences>() }
        val deviceSupportsPip = basePreferences.deviceHasPip()

        return listOfNotNull(
            Preference.PreferenceItem.ListPreference(
                pref = playerPreferences.progressPreference(),
                title = stringResource(MR.strings.pref_progress_mark_as_seen),
                entries = persistentMapOf(
                    1.00F to stringResource(MR.strings.pref_progress_100),
                    0.95F to stringResource(MR.strings.pref_progress_95),
                    0.90F to stringResource(MR.strings.pref_progress_90),
                    0.85F to stringResource(MR.strings.pref_progress_85),
                    0.80F to stringResource(MR.strings.pref_progress_80),
                    0.75F to stringResource(MR.strings.pref_progress_75),
                    0.70F to stringResource(MR.strings.pref_progress_70),
                ),
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = playerPreferences.preserveWatchingPosition(),
                title = stringResource(MR.strings.pref_preserve_watching_position),
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = playerPreferences.playerFullscreen(),
                title = stringResource(MR.strings.pref_player_fullscreen),
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = playerPreferences.hideControls(),
                title = stringResource(MR.strings.pref_player_hide_controls),
            ),
            getVolumeAndBrightnessGroup(playerPreferences = playerPreferences),
            getOrientationGroup(playerPreferences = playerPreferences),
            if (deviceSupportsPip) getPipGroup(playerPreferences = playerPreferences) else null,
            getExternalPlayerGroup(
                playerPreferences = playerPreferences,
                basePreferences = basePreferences,
            ),
            getTorrentServerGroup(torrentServerPreferences),
        )
    }

    @Composable
    private fun getVolumeAndBrightnessGroup(playerPreferences: PlayerPreferences): Preference.PreferenceGroup {
        val enableVolumeBrightnessGestures = playerPreferences.gestureVolumeBrightness()
        val rememberPlayerBrightness = playerPreferences.rememberPlayerBrightness()
        val rememberPlayerVolume = playerPreferences.rememberPlayerVolume()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_volume_brightness),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = enableVolumeBrightnessGestures,
                    title = stringResource(MR.strings.enable_volume_brightness_gestures),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = rememberPlayerBrightness,
                    title = stringResource(MR.strings.pref_remember_brightness),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = rememberPlayerVolume,
                    title = stringResource(MR.strings.pref_remember_volume),
                ),
            ),
        )
    }

    @Composable
    private fun getOrientationGroup(playerPreferences: PlayerPreferences): Preference.PreferenceGroup {
        val defaultPlayerOrientationType = playerPreferences.defaultPlayerOrientationType()
        val adjustOrientationVideoDimensions = playerPreferences.adjustOrientationVideoDimensions()
        val defaultPlayerOrientationPortrait = playerPreferences.defaultPlayerOrientationPortrait()
        val defaultPlayerOrientationLandscape = playerPreferences.defaultPlayerOrientationLandscape()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_player_orientation),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    pref = defaultPlayerOrientationType,
                    title = stringResource(MR.strings.pref_default_player_orientation),
                    entries = persistentMapOf(
                        ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR to stringResource(
                            MR.strings.rotation_free,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT to stringResource(
                            MR.strings.rotation_portrait,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT to stringResource(
                            MR.strings.rotation_reverse_portrait,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE to stringResource(
                            MR.strings.rotation_landscape,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE to stringResource(
                            MR.strings.rotation_reverse_landscape,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT to stringResource(
                            MR.strings.rotation_sensor_portrait,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE to stringResource(
                            MR.strings.rotation_sensor_landscape,
                        ),
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = adjustOrientationVideoDimensions,
                    title = stringResource(MR.strings.pref_adjust_orientation_video_dimensions),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = defaultPlayerOrientationPortrait,
                    title = stringResource(MR.strings.pref_default_portrait_orientation),
                    entries = persistentMapOf(
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT to stringResource(
                            MR.strings.rotation_portrait,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT to stringResource(
                            MR.strings.rotation_reverse_portrait,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT to stringResource(
                            MR.strings.rotation_sensor_portrait,
                        ),
                    ),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = defaultPlayerOrientationLandscape,
                    title = stringResource(MR.strings.pref_default_landscape_orientation),
                    entries = persistentMapOf(
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE to stringResource(
                            MR.strings.rotation_landscape,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE to stringResource(
                            MR.strings.rotation_reverse_landscape,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE to stringResource(
                            MR.strings.rotation_sensor_landscape,
                        ),
                    ),
                ),
            ),
        )
    }

    @Composable
    private fun getPipGroup(playerPreferences: PlayerPreferences): Preference.PreferenceGroup {
        val enablePip = playerPreferences.enablePip()
        val pipEpisodeToasts = playerPreferences.pipEpisodeToasts()
        val pipOnExit = playerPreferences.pipOnExit()
        val pipReplaceWithPrevious = playerPreferences.pipReplaceWithPrevious()

        val isPipEnabled by enablePip.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_pip),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = enablePip,
                    title = stringResource(MR.strings.pref_enable_pip),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = pipEpisodeToasts,
                    title = stringResource(MR.strings.pref_pip_episode_toasts),
                    enabled = isPipEnabled,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = pipOnExit,
                    title = stringResource(MR.strings.pref_pip_on_exit),
                    enabled = isPipEnabled,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = pipReplaceWithPrevious,
                    title = stringResource(MR.strings.pref_pip_replace_with_previous),
                    enabled = isPipEnabled,
                ),
            ),
        )
    }

    @Composable
    private fun getExternalPlayerGroup(
        playerPreferences: PlayerPreferences,
        basePreferences: BasePreferences,
    ): Preference.PreferenceGroup {
        val alwaysUseExternalPlayer = playerPreferences.alwaysUseExternalPlayer()
        val externalPlayerPreference = playerPreferences.externalPlayerPreference()

        val pm = basePreferences.context.packageManager
        val installedPackages = pm.getInstalledPackages(0)
        val supportedPlayers = installedPackages.filter { it.packageName in externalPlayers }

        val packageNames = supportedPlayers.map { it.packageName }
        val packageNamesReadable = supportedPlayers
            .map { pm.getApplicationLabel(it.applicationInfo!!).toString() }

        val packageNamesMap: Map<String, String> =
            packageNames.zip(packageNamesReadable)
                .toMap()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_external_player),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = alwaysUseExternalPlayer,
                    title = stringResource(MR.strings.pref_always_use_external_player),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = externalPlayerPreference,
                    title = stringResource(MR.strings.pref_external_player_preference),
                    entries = (mapOf("" to "None") + packageNamesMap).toPersistentMap(),
                ),
            ),
        )
    }

    @Composable
    private fun getTorrentServerGroup(
        torrentServerPreferences: TorrentServerPreferences,
    ): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val trackersPref = torrentServerPreferences.trackers()
        val trackers by trackersPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_torrentserver),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.EditTextPreference(
                    pref = torrentServerPreferences.port(),
                    title = stringResource(MR.strings.pref_torrentserver_port),
                    onValueChanged = {
                        try {
                            Integer.parseInt(it)
                            TorrentServerService.stop()
                            true
                        } catch (e: Exception) {
                            false
                        }
                    },
                ),
                Preference.PreferenceItem.MultiLineEditTextPreference(
                    pref = torrentServerPreferences.trackers(),
                    title = stringResource(MR.strings.pref_torrent_trackers),
                    subtitle = trackersPref.asState(scope).value
                        .lines().take(2)
                        .joinToString(
                            separator = "\n",
                            postfix = if (trackersPref.asState(scope).value.lines().size > 2) "\n..." else "",
                        ),
                    onValueChanged = {
                        TorrentServerService.stop()
                        true
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_reset_torrent_trackers_string),
                    enabled = remember(trackers) { trackers != trackersPref.defaultValue() },
                    onClick = {
                        trackersPref.delete()
                    },
                ),
            ),
        )
    }

    @Composable
    private fun SkipIntroLengthDialog(
        initialSkipIntroLength: Int,
        onDismissRequest: () -> Unit,
        onValueChanged: (skipIntroLength: Int) -> Unit,
    ) {
        val skipIntroLengthValue by rememberSaveable { mutableStateOf(initialSkipIntroLength) }
        var newLength = 0
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = stringResource(MR.strings.pref_intro_length)) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    content = {
                        WheelTextPicker(
                            modifier = Modifier.align(Alignment.Center),
                            items = remember { 0..255 }.map {
                                stringResource(
                                    MR.strings.seconds_short,
                                    it,
                                )
                            }.toImmutableList(),
                            onSelectionChanged = {
                                newLength = it
                            },
                            startIndex = skipIntroLengthValue,
                        )
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = { onValueChanged(newLength) }) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
        )
    }
}

val externalPlayers = listOf(
    MPV_PLAYER,
    MX_PLAYER,
    MX_PLAYER_FREE,
    MX_PLAYER_PRO,
    VLC_PLAYER,
    MPV_KT,
    MPV_KT_PREVIEW,
    MPV_REMOTE,
    JUST_PLAYER,
    NEXT_PLAYER,
    X_PLAYER,
    WEB_VIDEO_CASTER,
)

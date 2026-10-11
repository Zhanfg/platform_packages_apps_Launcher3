/*
 * Copyright (C) 2026 The uwuAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.launcher3.BuildConfig
import com.android.launcher3.LauncherFiles
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.R
import com.android.launcher3.SessionCommitReceiver
import com.android.launcher3.util.SettingsCache
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.framework.compose.localNavController
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold
import com.android.settingslib.spa.widget.ui.Category

// Keep translation and opacity on the same clock when navigating Settings pages.
private const val SETTINGS_NAV_DURATION_MS = 300

private const val MAIN_ROUTE = "main"
private const val GRID_ROUTE = "grid"
private const val DOCK_ROUTE = "dock"
private const val SEARCH_ROUTE = "search"
private const val DRAWER_ROUTE = "drawer"
private const val FOLDER_ROUTE = "folder"
private const val ICON_PACK_ROUTE = "icon_pack"
private const val AT_A_GLANCE_ROUTE = "at_a_glance"

@Composable
fun LauncherSettingsScreen(startRoute: String? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedIconPack by remember {
        mutableStateOf(LauncherPrefs.get(context).get(LauncherPrefs.ICON_PACK_PACKAGE))
    }

    androidx.compose.runtime.CompositionLocalProvider(navController.localNavController()) {
        NavHost(
            navController = navController,
            startDestination = startRoute ?: MAIN_ROUTE,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(SETTINGS_NAV_DURATION_MS),
                    initialOffset = { it / 5 },
                ) + fadeIn(tween(SETTINGS_NAV_DURATION_MS))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(SETTINGS_NAV_DURATION_MS),
                    targetOffset = { it / 5 },
                ) + fadeOut(tween(SETTINGS_NAV_DURATION_MS))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(SETTINGS_NAV_DURATION_MS),
                    initialOffset = { it / 5 },
                ) + fadeIn(tween(SETTINGS_NAV_DURATION_MS))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(SETTINGS_NAV_DURATION_MS),
                    targetOffset = { it / 5 },
                ) + fadeOut(tween(SETTINGS_NAV_DURATION_MS))
            },
        ) {
            composable(MAIN_ROUTE) {
                SettingsScaffold(
                    title = context.getString(R.string.derived_app_name),
                    isFirstLayerPageWhenEmbedded = true,
                ) { padding ->
                    MainSettingsContent(
                        contentPadding = padding,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        selectedIconPack = selectedIconPack,
                        onOpenRoute = { navController.navigate(it) },
                        onOpenGrid = { navController.navigate(GRID_ROUTE) },
                        onOpenDock = { navController.navigate(DOCK_ROUTE) },
                        onOpenSearch = { navController.navigate(SEARCH_ROUTE) },
                        onOpenDrawer = { navController.navigate(DRAWER_ROUTE) },
                         onOpenFolder = { navController.navigate(FOLDER_ROUTE) },
                         onOpenIconPack = { navController.navigate(ICON_PACK_ROUTE) },
                    )
                }
            }
            composable(GRID_ROUTE) {
                SettingsScaffold(
                    title = context.getString(R.string.home_screen_grid),
                    actions = {
                        ResetAction {
                            resetGridSettings(context)
                            recreateSettings(context)
                        }
                    },
                ) { padding ->
                    GridSizeSettingsContent(contentPadding = padding)
                }
            }
            composable(DOCK_ROUTE) {
                SettingsScaffold(
                    title = context.getString(R.string.dock_settings),
                    actions = {
                        ResetAction {
                            resetDockSettings(context)
                            recreateSettings(context)
                        }
                    },
                ) { padding ->
                    DockSettingsContent(padding)
                }
            }
            composable(SEARCH_ROUTE) {
                SettingsScaffold(
                    title = context.getString(R.string.search_bar_settings),
                    actions = {
                        ResetAction {
                            resetSearchSettings(context)
                            recreateSettings(context)
                        }
                    },
                ) { padding ->
                    SearchSettingsContent(padding)
                }
            }
            composable(DRAWER_ROUTE) {
                SettingsScaffold(
                    title = context.getString(R.string.drawer_settings),
                    actions = {
                        ResetAction {
                            resetLayoutSettings(context, LayoutSettingsKind.Drawer)
                            recreateSettings(context)
                        }
                    },
                ) { padding ->
                    LayoutSettingsContent(LayoutSettingsKind.Drawer, padding)
                }
            }
            composable(FOLDER_ROUTE) {
                SettingsScaffold(
                    title = context.getString(R.string.folder_settings),
                    actions = {
                        ResetAction {
                            resetLayoutSettings(context, LayoutSettingsKind.Folder)
                            recreateSettings(context)
                        }
                    },
                ) { padding ->
                    LayoutSettingsContent(LayoutSettingsKind.Folder, padding)
                }
            }
            composable(ICON_PACK_ROUTE) {
                SettingsScaffold(title = context.getString(R.string.icon_pack_title)) { padding ->
                    IconSettingsContent(
                        contentPadding = padding,
                        selectedPackage = selectedIconPack,
                        onSelectedPackageChange = { selectedIconPack = it },
                    )
                }
            }
            composable(AT_A_GLANCE_ROUTE) {
                SettingsScaffold(
                    title = context.getString(R.string.at_a_glance_title),
                    actions = {
                        ResetAction {
                            resetAtAGlanceSettings(context)
                            recreateSettings(context)
                        }
                    },
                ) { padding ->
                    AtAGlanceSettingsContent(padding)
                }
            }
        }
    }
}

@Composable
private fun ResetAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.RestartAlt,
            contentDescription = stringResource(R.string.action_reset),
        )
    }
}

@Composable
private fun SettingsSearchBar(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(min = 72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_search),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    innerTextField()
                },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = stringResource(R.string.clear_search),
                    )
                }
            }
        }
    }
}

private data class SettingsSearchResult(
    val title: Int,
    val route: String,
    val keywords: String,
)

private val SETTINGS_SEARCH_RESULTS = listOf(
    SettingsSearchResult(R.string.home_screen_grid, GRID_ROUTE, "grid rows columns"),
    SettingsSearchResult(R.string.dock_settings, DOCK_ROUTE, "dock icons pages labels background"),
    SettingsSearchResult(R.string.search_bar_settings, SEARCH_ROUTE, "search provider qsb"),
    SettingsSearchResult(R.string.drawer_settings, DRAWER_ROUTE, "drawer applications"),
    SettingsSearchResult(R.string.folder_settings, FOLDER_ROUTE, "folder"),
    SettingsSearchResult(R.string.dock_rows, DOCK_ROUTE, "dock rows"),
    SettingsSearchResult(R.string.dock_pages, DOCK_ROUTE, "dock pages"),
    SettingsSearchResult(R.string.dock_background, DOCK_ROUTE, "dock background"),
    SettingsSearchResult(R.string.search_provider, SEARCH_ROUTE, "search provider"),
    SettingsSearchResult(R.string.show_dock_search, SEARCH_ROUTE, "search bar"),
    SettingsSearchResult(R.string.force_website_search, SEARCH_ROUTE, "website search"),
    SettingsSearchResult(R.string.match_drawer_search, SEARCH_ROUTE, "drawer search"),
    SettingsSearchResult(
        R.string.smartspacer_title,
        AT_A_GLANCE_ROUTE,
        "smartspace smartspacer download",
    ),
    SettingsSearchResult(
        R.string.at_a_glance_title,
        AT_A_GLANCE_ROUTE,
        "at a glance smartspace status",
    ),
    SettingsSearchResult(R.string.icon_pack_title, ICON_PACK_ROUTE, "icons icon pack theme"),
)

@Composable
private fun SettingsSearchResults(query: String, onOpenRoute: (String) -> Unit) {
    val context = LocalContext.current
    val normalizedQuery = query.trim().lowercase()
    val results = SETTINGS_SEARCH_RESULTS.filter { result ->
        val title = context.getString(result.title)
        "$title ${result.keywords}".lowercase().contains(normalizedQuery)
    }

    Category(title = stringResource(R.string.settings_search_results)) {
        if (results.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_search_no_results),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            results.forEach { result ->
                Preference(
                    model = object : PreferenceModel {
                        override val title = context.getString(result.title)
                        override val onClick = { onOpenRoute(result.route) }
                    }
                )
            }
        }
    }
}

@Composable
private fun MainSettingsContent(
    contentPadding: PaddingValues,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedIconPack: String,
    onOpenRoute: (String) -> Unit,
    onOpenGrid: () -> Unit,
    onOpenDock: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenFolder: () -> Unit,
    onOpenIconPack: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsSearchBar(searchQuery, onSearchQueryChange)

        if (searchQuery.isNotBlank()) {
            SettingsSearchResults(searchQuery, onOpenRoute)
        } else {
             Category(title = context.getString(R.string.settings_general_section)) {
                 NotificationDotsPreference(context)

                 AddIconsToHomePreference(context)

             }
             Category(title = context.getString(R.string.settings_layout_section)) {

             Preference(
                 model = object : PreferenceModel {
                     override val title = context.getString(R.string.at_a_glance_title)
                     override val onClick = { onOpenRoute(AT_A_GLANCE_ROUTE) }
                 }
             )
              Preference(
                  model =
                     object : PreferenceModel {
                         override val title = context.getString(R.string.home_screen_grid)
                         override val onClick = onOpenGrid
                     }
             )
             Preference(
                 model = object : PreferenceModel {
                     override val title = context.getString(R.string.icon_pack_title)
                     override val onClick = onOpenIconPack
                 }
             )
            Preference(
                model =
                    object : PreferenceModel {
                        override val title = context.getString(R.string.dock_settings)
                        override val onClick = onOpenDock
                    }
            )
            Preference(
                model =
                    object : PreferenceModel {
                        override val title = context.getString(R.string.search_bar_settings)
                        override val onClick = onOpenSearch
                    }
            )
            Preference(
                model =
                    object : PreferenceModel {
                        override val title = context.getString(R.string.drawer_settings)
                        override val onClick = onOpenDrawer
                    }
            )
            Preference(
                model =
                    object : PreferenceModel {
                        override val title = context.getString(R.string.folder_settings)
                        override val onClick = onOpenFolder
                    }
            )

                if (BuildConfig.IS_STUDIO_BUILD) {
                Preference(
                    model =
                        object : PreferenceModel {
                            override val title = context.getString(R.string.developer_options_title)
                            override val onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                )
                            }
                        }
                )
                }
            }
        }
    }
}

@Composable
private fun NotificationDotsPreference(context: Context) {
    val settingsCache = remember { SettingsCache.INSTANCE.get(context) }
    val enabled = remember { mutableStateOf(settingsCache.getValue(SettingsCache.NOTIFICATION_BADGING_URI)) }

    Preference(
        model =
            object : PreferenceModel {
                override val title = context.getString(R.string.notification_dots_title)
                override val summary = {
                    context.getString(
                        if (enabled.value) R.string.notification_dots_desc_on
                        else R.string.notification_dots_desc_off
                    )
                }
                override val onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_SETTINGS))
                }
            }
    )
}

@Composable
private fun AddIconsToHomePreference(context: Context) {
    val sharedPrefs =
        remember {
            context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
        }
    var enabled by remember {
        mutableStateOf(sharedPrefs.getBoolean(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY, true))
    }

    SwitchPreference(
        model =
            object : SwitchPreferenceModel {
                override val title = context.getString(R.string.auto_add_shortcuts_label)
                override val summary = { context.getString(R.string.auto_add_shortcuts_description) }
                override val checked = { enabled }
                override val onCheckedChange = { newChecked: Boolean ->
                    enabled = newChecked
                    sharedPrefs
                        .edit()
                        .putBoolean(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY, newChecked)
                        .apply()
                }
            }
    )
}

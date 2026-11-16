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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startRoute = intent.getStringExtra(EXTRA_START_ROUTE)
        setContent { LauncherExpressiveSettingsTheme { LauncherSettingsScreen(startRoute) } }
    }

    companion object {
        const val EXTRA_START_ROUTE = "com.android.launcher3.settings.extra.START_ROUTE"
        const val SEARCH_ROUTE = "search"
        const val FIXED_LANDSCAPE_MODE = "pref_fixed_landscape_mode"
        const val SMARTSPACER_PREFERENCE_KEY = "pref_enable_smartspacer"
        const val SHOW_AT_A_GLANCE_PREFERENCE_KEY = "pref_show_at_a_glance"
        const val SMARTSPACER_INSTALL_PROMPT_DISMISSED_PREFERENCE_KEY =
            "pref_smartspacer_install_prompt_dismissed"
    }
}

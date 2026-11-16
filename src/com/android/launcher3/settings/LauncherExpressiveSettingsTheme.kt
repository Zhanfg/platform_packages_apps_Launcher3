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

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import com.android.settingslib.spa.framework.theme.SettingsTheme

/**
 * Keep SettingsLib's platform color and content locals while enabling the
 * Material 3 Expressive motion scheme for Launcher-owned Compose content.
 *
 * This does not replace SettingsLib components, the ROM color palette, or the
 * Quickstep/View animation pipeline.
 */
@Composable
internal fun LauncherExpressiveSettingsTheme(content: @Composable () -> Unit) {
    SettingsTheme {
        MaterialExpressiveTheme(
            colorScheme = MaterialTheme.colorScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

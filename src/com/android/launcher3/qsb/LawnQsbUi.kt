/*
 * Copyright (C) 2026 Lawnchair
 * Copyright (C) 2026 The uwuAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.android.launcher3.qsb

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.LayerDrawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.android.compose.ui.graphics.painter.rememberDrawablePainter
import com.android.launcher3.R
import com.android.launcher3.util.Themes

enum class QsbIconId { SEARCH, MIC, LENS }

enum class ThemingMethod { TINT, THEME_BY_LAYER_ID }

@Immutable
data class QsbIconState(
    val id: QsbIconId,
    @param:DrawableRes val resId: Int,
    val themed: Boolean,
    val contentDescription: String,
    val method: ThemingMethod = ThemingMethod.THEME_BY_LAYER_ID,
    val visible: Boolean = true,
)

@Immutable
data class QsbStyle(
    val backgroundAlpha: Float,
    @param:ColorInt val backgroundColor: Int,
    @param:ColorInt val strokeColor: Int,
    val strokeWidthPx: Float,
    val cornerRadiusPx: Float,
)

@Immutable
data class QsbState(
    val contentDescription: String,
    val startIcon: QsbIconState,
    val endIcons: List<QsbIconState>,
)

@Immutable
data class QsbActions(
    val onQsbClick: () -> Unit,
    val onQsbLongClick: (() -> Unit)? = null,
    val onEndIconClick: (QsbIconId) -> Unit,
)

@Composable
fun rememberHotseatQsbState(
    searchProvider: QsbSearchProvider,
    themed: Boolean,
    showMic: Boolean,
    showLens: Boolean,
): QsbState {
    val searchLabel = stringResource(R.string.label_search)
    val voiceLabel = stringResource(R.string.label_voice_search)
    val lensLabel = stringResource(R.string.label_lens)
    return remember(searchProvider, themed, showMic, showLens, searchLabel, voiceLabel, lensLabel) {
        val iconRes = if (themed) searchProvider.themedIcon else searchProvider.icon
        val isGoogle = searchProvider == QsbSearchProvider.GOOGLE ||
            searchProvider == QsbSearchProvider.GOOGLE_GO ||
            searchProvider == QsbSearchProvider.PIXEL_SEARCH
        QsbState(
            contentDescription = searchLabel,
            startIcon = QsbIconState(
                QsbIconId.SEARCH,
                iconRes,
                themed || iconRes == R.drawable.ic_qsb_search,
                searchLabel,
                searchProvider.themingMethod,
            ),
            endIcons = listOf(
                QsbIconState(
                    QsbIconId.MIC,
                    if (isGoogle) R.drawable.ic_mic_color else R.drawable.ic_mic_flat,
                    (isGoogle && themed) || !isGoogle,
                    voiceLabel,
                    if (isGoogle) ThemingMethod.THEME_BY_LAYER_ID else ThemingMethod.TINT,
                    showMic,
                ),
                QsbIconState(
                    QsbIconId.LENS,
                    R.drawable.ic_lens_color,
                    themed,
                    lensLabel,
                    ThemingMethod.THEME_BY_LAYER_ID,
                    showLens,
                ),
            ),
        )
    }
}

fun buildQsbStyle(
    context: Context,
    themed: Boolean,
    backgroundAlpha: Int,
    backgroundColor: Int,
    cornerRadius: Float,
    strokeColor: Int,
    strokeWidth: Float,
) = QsbStyle(
    backgroundAlpha = backgroundAlpha / 100f,
    backgroundColor = if (themed) LauncherExpressiveSearchColors.container(context) else backgroundColor,
    strokeColor = if (strokeColor == 0) Themes.getColorAccent(context) else strokeColor,
    strokeWidthPx = strokeWidth,
    cornerRadiusPx = getHotseatQsbCornerRadius(context, cornerRadius),
)

fun getHotseatQsbCornerRadius(context: Context, factor: Float): Float {
    val resources = context.resources
    val height = resources.getDimension(R.dimen.uwu_qsb_widget_height)
    val padding = resources.getDimension(R.dimen.uwu_qsb_widget_vertical_padding)
    return (height - 2 * padding) / 2 * factor
}

@Composable
fun LawnQsbUi(
    state: QsbState,
    style: QsbStyle,
    actions: QsbActions,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val shape = RoundedCornerShape(with(density) { style.cornerRadiusPx.toDp() })
    val strokeWidth = with(density) { style.strokeWidthPx.toDp() }
    val container = modifier
        .fillMaxWidth()
        .semantics { contentDescription = state.contentDescription }
        .clip(shape)
        .background(Color(style.backgroundColor).copy(alpha = style.backgroundAlpha), shape)
        .combinedClickable(
            onClick = actions.onQsbClick,
            onLongClick = actions.onQsbLongClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = MaterialTheme.colorScheme.onSurface),
        )
        .then(
            if (style.strokeWidthPx > 0f) Modifier.border(strokeWidth, Color(style.strokeColor), shape)
            else Modifier,
        )

    Row(container, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.requiredWidth(dimensionResource(R.dimen.uwu_qsb_icon_width)).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            ThemedImage(state.startIcon, Modifier.size(24.dp))
        }
        Spacer(Modifier.weight(1f))
        AnimatedContent(
            targetState = state.endIcons.filter(QsbIconState::visible),
            transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
            label = "endIcons",
            contentAlignment = Alignment.CenterEnd,
        ) { icons ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                icons.forEachIndexed { index, icon ->
                    QsbIcon(
                        icon,
                        shape,
                        { actions.onEndIconClick(icon.id) },
                        if (index == icons.lastIndex) Modifier.offset(x = (-6).dp) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun QsbIcon(icon: QsbIconState, shape: Shape, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier.requiredWidth(dimensionResource(R.dimen.uwu_qsb_icon_width))
            .fillMaxHeight()
            .clip(shape)
            .clickable(
                role = Role.Button,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.onSurface),
            )
            .padding(dimensionResource(R.dimen.uwu_qsb_icon_padding)),
        contentAlignment = Alignment.Center,
    ) {
        ThemedIcon(icon, Modifier.fillMaxSize())
    }
}

@Composable
private fun ThemedImage(icon: QsbIconState, modifier: Modifier) {
    Image(rememberThemedIconPainter(icon), icon.contentDescription, modifier)
}

@Composable
private fun ThemedIcon(icon: QsbIconState, modifier: Modifier) {
    Icon(rememberThemedIconPainter(icon), icon.contentDescription, modifier, tint = Color.Unspecified)
}

@Composable
private fun rememberThemedIconPainter(icon: QsbIconState) = run {
    val context = LocalContext.current
    val drawable = remember(context, icon) {
        requireNotNull(ResourcesCompat.getDrawable(context.resources, icon.resId, context.theme)).mutate().also {
            if (!icon.themed) return@also
            if (icon.method == ThemingMethod.THEME_BY_LAYER_ID && it is LayerDrawable) {
                val isNight = context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                val colors = if (isNight) {
                    intArrayOf(
                        context.getColor(android.R.color.system_accent3_100),
                        context.getColor(android.R.color.system_accent1_400),
                        context.getColor(android.R.color.system_accent2_10),
                        context.getColor(android.R.color.system_accent1_200),
                    )
                } else {
                    intArrayOf(
                        context.getColor(android.R.color.system_accent3_400),
                        context.getColor(android.R.color.system_accent1_500),
                        context.getColor(android.R.color.system_accent2_300),
                        context.getColor(android.R.color.system_accent1_600),
                    )
                }
                val ids = intArrayOf(
                    R.id.qsbIconTintPrimary,
                    R.id.qsbIconTintSecondary,
                    R.id.qsbIconTintTertiary,
                    R.id.qsbIconTintQuaternary,
                )
                for (index in 0 until it.numberOfLayers) {
                    ids.indexOf(it.getId(index)).takeIf { found -> found >= 0 }
                        ?.let { found -> it.getDrawable(index).setTint(colors[found]) }
                }
            } else {
                it.setTint(Themes.getColorAccent(context))
            }
        }
    }
    rememberDrawablePainter(drawable)
}

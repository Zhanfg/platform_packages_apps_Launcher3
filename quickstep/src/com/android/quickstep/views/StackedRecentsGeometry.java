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
package com.android.quickstep.views;

/**
 * Platform-independent geometry for overlapping cards in the Recents overview.
 *
 * The caller supplies coordinates along the current PagedOrientationHandler's primary axis.
 * No Android classes, view references or global state are used, so the math can be verified on
 * the host JVM before wiring it into Android 17's gesture and task animation pipeline.
 */
public final class StackedRecentsGeometry {
    private static final float CARD_STEP_FRACTION = 0.29f;
    private static final float SCALE_LOSS_PER_PAGE = 0.075f;
    private static final float MAX_STACK_DISTANCE = 3.0f;

    private StackedRecentsGeometry() {}

    /** Result relative to the native, unmodified Quickstep card transform. */
    public static final class Transform {
        public final float primaryOffsetPx;
        public final float scaleFactor;
        public final float depthPx;

        private Transform(float primaryOffsetPx, float scaleFactor, float depthPx) {
            this.primaryOffsetPx = primaryOffsetPx;
            this.scaleFactor = scaleFactor;
            this.depthPx = depthPx;
        }
    }

    /**
     * Computes one card's placement relative to the viewport center.
     *
     * @param centerDistancePx the signed native card-center minus viewport-center distance
     * @param pageStridePx the native distance between neighboring page centers, always positive
     * @param cardSizePx the size of the card along the primary scrolling axis, always positive
     * @return a translation delta, multiplicative scale, and Z elevation; never NaN or infinity
     */
    public static Transform calculate(
            float centerDistancePx, float pageStridePx, float cardSizePx) {
        if (!Float.isFinite(centerDistancePx)
                || !Float.isFinite(pageStridePx)
                || !Float.isFinite(cardSizePx)
                || pageStridePx <= 0f
                || cardSizePx <= 0f) {
            return new Transform(0f, 1f, 0f);
        }

        final float pageDistance = centerDistancePx / pageStridePx;
        if (!Float.isFinite(pageDistance)) {
            return new Transform(0f, 1f, 0f);
        }
        final float limitedDistance =
                Math.min(Math.abs(pageDistance), MAX_STACK_DISTANCE);
        final float direction = Math.signum(pageDistance);
        final float targetCenterDistance =
                direction * limitedDistance * cardSizePx * CARD_STEP_FRACTION;
        final float translation = targetCenterDistance - centerDistancePx;
        final float scale = 1f - limitedDistance * SCALE_LOSS_PER_PAGE;
        final float depth = (MAX_STACK_DISTANCE - limitedDistance) * 2f;
        return new Transform(translation, scale, depth);
    }
}

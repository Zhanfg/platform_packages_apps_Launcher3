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

/** Executable JVM regression checks, requiring no Android runtime. */
public final class StackedRecentsGeometryTest {
    private static final float EPS = 0.0005f;

    public static void main(String[] args) {
        focusedCardKeepsNativePosition();
        adjacentCardOverlapsAndShrinks();
        signedDistanceMirrorsForRtl();
        depthAndScaleAreBounded();
        fractionalScrollProducesContinuousMovement();
        farCardsDoNotPileUpAtTheStackEdge();
        stackExitHasNoJump();
        swipeThroughExitMaintainsDirectionSymmetry();
        invalidInputsAreAlwaysIdentity();
        System.out.println("StackedRecentsGeometry: 9 host tests passed");
    }

    private static void focusedCardKeepsNativePosition() {
        StackedRecentsGeometry.Transform t =
                StackedRecentsGeometry.calculate(0f, 100f, 80f);
        near(0f, t.primaryOffsetPx);
        near(1f, t.scaleFactor);
        near(6f, t.depthPx);
    }

    private static void adjacentCardOverlapsAndShrinks() {
        StackedRecentsGeometry.Transform t =
                StackedRecentsGeometry.calculate(100f, 100f, 80f);
        near(-76.8f, t.primaryOffsetPx);
        near(0.925f, t.scaleFactor);
        near(4f, t.depthPx);
    }

    private static void signedDistanceMirrorsForRtl() {
        StackedRecentsGeometry.Transform left =
                StackedRecentsGeometry.calculate(-100f, 100f, 80f);
        StackedRecentsGeometry.Transform right =
                StackedRecentsGeometry.calculate(100f, 100f, 80f);
        near(-left.primaryOffsetPx, right.primaryOffsetPx);
        near(left.scaleFactor, right.scaleFactor);
        near(left.depthPx, right.depthPx);
    }

    private static void depthAndScaleAreBounded() {
        for (int i = -100; i <= 100; ++i) {
            StackedRecentsGeometry.Transform t =
                    StackedRecentsGeometry.calculate(i * 100f, 100f, 80f);
            check(t.scaleFactor >= 0.775f && t.scaleFactor <= 1f);
            check(t.depthPx >= 0f && t.depthPx <= 6f);
        }
    }

    private static void fractionalScrollProducesContinuousMovement() {
        StackedRecentsGeometry.Transform left =
                StackedRecentsGeometry.calculate(49.99f, 100f, 80f);
        StackedRecentsGeometry.Transform right =
                StackedRecentsGeometry.calculate(50.01f, 100f, 80f);
        check(Math.abs(right.primaryOffsetPx - left.primaryOffsetPx) < 0.05f);
        check(Math.abs(right.scaleFactor - left.scaleFactor) < 0.0001f);
    }

    private static void farCardsDoNotPileUpAtTheStackEdge() {
        for (float pages : new float[] {4f, 5f, 8f, 100f}) {
            for (float sign : new float[] {-1f, 1f}) {
                StackedRecentsGeometry.Transform t =
                        StackedRecentsGeometry.calculate(sign * pages * 100f, 100f, 80f);
                near(0f, t.primaryOffsetPx);
                near(1f, t.scaleFactor);
                near(0f, t.depthPx);
            }
        }
        StackedRecentsGeometry.Transform near =
                StackedRecentsGeometry.calculate(300f, 100f, 80f);
        check(300f + near.primaryOffsetPx < 100f);
        StackedRecentsGeometry.Transform far =
                StackedRecentsGeometry.calculate(400f, 100f, 80f);
        check(400f + far.primaryOffsetPx > 300f);
    }

    private static void stackExitHasNoJump() {
        for (float boundary : new float[] {3f, 4f}) {
            float d = 0.001f;
            StackedRecentsGeometry.Transform left =
                    StackedRecentsGeometry.calculate((boundary - d) * 100f, 100f, 80f);
            StackedRecentsGeometry.Transform right =
                    StackedRecentsGeometry.calculate((boundary + d) * 100f, 100f, 80f);
            check(Math.abs(left.primaryOffsetPx - right.primaryOffsetPx) < 0.15f);
            check(Math.abs(left.scaleFactor - right.scaleFactor) < 0.001f);
            check(Math.abs(left.depthPx - right.depthPx) < 0.01f);
        }
    }

    private static void swipeThroughExitMaintainsDirectionSymmetry() {
        for (int steps = 300; steps <= 400; ++steps) {
            float x = steps / 100f * 100f;
            StackedRecentsGeometry.Transform right =
                    StackedRecentsGeometry.calculate(x, 100f, 80f);
            StackedRecentsGeometry.Transform left =
                    StackedRecentsGeometry.calculate(-x, 100f, 80f);
            near(-left.primaryOffsetPx, right.primaryOffsetPx);
            near(left.scaleFactor, right.scaleFactor);
            near(left.depthPx, right.depthPx);
            check(right.scaleFactor >= 0.775f && right.scaleFactor <= 1f);
        }
    }

    private static void invalidInputsAreAlwaysIdentity() {
        float[][] cases = {
                {0f, 0f, 80f},
                {0f, -2f, 80f},
                {1f, 100f, 0f},
                {Float.NaN, 100f, 80f},
                {1f, Float.POSITIVE_INFINITY, 80f},
                {1f, 100f, Float.NEGATIVE_INFINITY},
        };
        for (float[] input : cases) {
            StackedRecentsGeometry.Transform t =
                    StackedRecentsGeometry.calculate(input[0], input[1], input[2]);
            near(0f, t.primaryOffsetPx);
            near(1f, t.scaleFactor);
            near(0f, t.depthPx);
        }
    }

    private static void near(float expected, float actual) {
        check(Float.isFinite(actual) && Math.abs(expected - actual) < EPS);
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Stacked Recents geometry test failed");
    }
}

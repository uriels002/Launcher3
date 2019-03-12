/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.quickstep.util;

import static com.android.quickstep.TouchInteractionService.BACKGROUND_EXECUTOR;
import static com.android.quickstep.TouchInteractionService.MAIN_THREAD_EXECUTOR;
import static com.android.systemui.shared.system.RemoteAnimationTargetCompat.MODE_CLOSING;

import android.graphics.Rect;

import com.android.systemui.shared.recents.model.ThumbnailData;
import com.android.systemui.shared.system.RecentsAnimationControllerCompat;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;

import java.util.function.Consumer;

/**
 * Extension of {@link RemoteAnimationTargetSet} with additional information about swipe
 * up animation
 */
public class SwipeAnimationTargetSet extends RemoteAnimationTargetSet {

    private final boolean mShouldMinimizeSplitScreen;
    private final Consumer<SwipeAnimationTargetSet> mOnFinishListener;


    public final RecentsAnimationControllerCompat controller;
    public final Rect homeContentInsets;
    public final Rect minimizedHomeBounds;

    public SwipeAnimationTargetSet(RecentsAnimationControllerCompat controller,
            RemoteAnimationTargetCompat[] targets, Rect homeContentInsets,
            Rect minimizedHomeBounds, boolean shouldMinimizeSplitScreen,
            Consumer<SwipeAnimationTargetSet> onFinishListener) {
        super(targets, MODE_CLOSING);
        this.controller = controller;
        this.homeContentInsets = homeContentInsets;
        this.minimizedHomeBounds = minimizedHomeBounds;
        this.mShouldMinimizeSplitScreen = shouldMinimizeSplitScreen;
        this.mOnFinishListener = onFinishListener;
    }

    public void finishController(boolean toRecents, Runnable callback) {
        mOnFinishListener.accept(this);
        BACKGROUND_EXECUTOR.execute(() -> {
            controller.setInputConsumerEnabled(false);
            controller.finish(toRecents);

            if (callback != null) {
                MAIN_THREAD_EXECUTOR.execute(callback);
            }
        });
    }

    public void enableInputConsumer() {
        BACKGROUND_EXECUTOR.submit(() -> {
            controller.hideCurrentInputMethod();
            controller.setInputConsumerEnabled(true);
        });
    }

    public void setWindowThresholdCrossed(boolean thresholdCrossed) {
        BACKGROUND_EXECUTOR.execute(() -> {
            controller.setAnimationTargetsBehindSystemBars(!thresholdCrossed);
            if (mShouldMinimizeSplitScreen && thresholdCrossed) {
                // NOTE: As a workaround for conflicting animations (Launcher animating the task
                // leash, and SystemUI resizing the docked stack, which resizes the task), we
                // currently only set the minimized mode, and not the inverse.
                // TODO: Synchronize the minimize animation with the launcher animation
                controller.setSplitScreenMinimized(thresholdCrossed);
            }
        });
    }

    public ThumbnailData screenshotTask(int taskId) {
        return controller != null ? controller.screenshotTask(taskId) : null;
    }

    public interface SwipeAnimationListener {

        void onRecentsAnimationStart(SwipeAnimationTargetSet targetSet);

        void onRecentsAnimationCanceled();
    }

    public interface SwipeAnimationFinishListener {

        void onSwipeAnimationFinished(SwipeAnimationTargetSet targetSet);
    }
}

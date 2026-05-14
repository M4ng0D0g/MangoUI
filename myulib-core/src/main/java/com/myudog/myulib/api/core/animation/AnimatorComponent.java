package com.myudog.myulib.api.core.animation;

import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;

import java.util.Objects;

public final class AnimatorComponent<T> {
    private final AnimationSpec<T> spec;
    private final AnimationTarget<T> target;
    private PlaybackState state = PlaybackState.IDLE;
    private long elapsedMillis;
    private long completedCycles;
    private boolean forward = true;
    private T currentValue;

    public AnimatorComponent(AnimationSpec<T> spec, AnimationTarget<T> target) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.target = Objects.requireNonNull(target, "target");
        this.currentValue = spec.startValue();
    }

    public AnimationSpec<T> spec() {
        return spec;
    }

    public PlaybackState state() {
        return state;
    }

    public long elapsedMillis() {
        return elapsedMillis;
    }

    public long completedCycles() {
        return effectiveCompletedCycles();
    }

    public boolean forward() {
        return spec.playMode() != PlayMode.PING_PONG || (effectiveCompletedCycles() % 2L) == 0L;
    }

    public T currentValue() {
        return currentValue;
    }

    public void play() {
        if (state == PlaybackState.PLAYING) {
            return;
        }
        if (state == PlaybackState.IDLE || state == PlaybackState.STOPPED || state == PlaybackState.FINISHED) {
            resetTimeline();
            applyCurrentValue();
        }
        state = PlaybackState.PLAYING;
        DebugLogManager.INSTANCE.log(DebugFeature.ANIMATION, "play");
    }

    public void pause() {
        if (state == PlaybackState.PLAYING) {
            state = PlaybackState.PAUSED;
            DebugLogManager.INSTANCE.log(DebugFeature.ANIMATION, "pause");
        }
    }

    public void resume() {
        if (state == PlaybackState.PAUSED) {
            state = PlaybackState.PLAYING;
            DebugLogManager.INSTANCE.log(DebugFeature.ANIMATION, "resume");
        }
    }

    public void stop() {
        resetTimeline();
        state = PlaybackState.STOPPED;
        applyCurrentValue();
        DebugLogManager.INSTANCE.log(DebugFeature.ANIMATION, "stop");
    }

    public void seek(long newElapsedMillis) {
        if (newElapsedMillis < 0L) {
            throw new IllegalArgumentException("newElapsedMillis must be >= 0");
        }
        elapsedMillis = newElapsedMillis;
        syncTimelineState();
        applyCurrentValue();
        DebugLogManager.INSTANCE.log(DebugFeature.ANIMATION, "seek=" + newElapsedMillis);
    }

    public T tick(long deltaMillis) {
        if (deltaMillis < 0L) {
            throw new IllegalArgumentException("deltaMillis must be >= 0");
        }
        if (state != PlaybackState.PLAYING) {
            return currentValue;
        }
        if (spec.durationMillis() <= 0L) {
            currentValue = spec.endValue();
            target.apply(currentValue);
            state = spec.playMode() == PlayMode.ONCE ? PlaybackState.FINISHED : PlaybackState.PLAYING;
            return currentValue;
        }

        elapsedMillis += deltaMillis;
        syncTimelineState();
        applyCurrentValue();
        return currentValue;
    }

    private void applyCurrentValue() {
        if (spec.durationMillis() <= 0L) {
            currentValue = spec.endValue();
            target.apply(currentValue);
            if (state == PlaybackState.PLAYING && spec.playMode() == PlayMode.ONCE) {
                state = PlaybackState.FINISHED;
            }
            return;
        }

        syncTimelineState();
        switch (spec.playMode()) {
            case ONCE -> {
                long duration = spec.durationMillis();
                long clampedElapsed = Math.min(elapsedMillis, duration);
                double progress = (double) clampedElapsed / (double) duration;
                currentValue = spec.valueAt(progress);
                target.apply(currentValue);
                if (elapsedMillis >= duration) {
                    state = PlaybackState.FINISHED;
                }
            }
            case LOOP -> {
                long duration = spec.durationMillis();
                completedCycles = elapsedMillis / duration;
                long cycleElapsed = elapsedMillis % duration;
                currentValue = spec.valueAt((double) cycleElapsed / (double) duration);
                target.apply(currentValue);
            }
            case PING_PONG -> {
                long duration = spec.durationMillis();
                completedCycles = elapsedMillis / duration;
                long cycleElapsed = elapsedMillis % duration;
                forward = completedCycles % 2L == 0L;
                double progress = (double) cycleElapsed / (double) duration;
                currentValue = spec.valueAt(forward ? progress : 1.0 - progress);
                target.apply(currentValue);
            }
        }
    }

    private void syncTimelineState() {
        long duration = spec.durationMillis();
        if (duration <= 0L) {
            completedCycles = 0L;
            forward = true;
            return;
        }

        completedCycles = elapsedMillis / duration;
        forward = spec.playMode() != PlayMode.PING_PONG || (completedCycles % 2L) == 0L;
    }

    private long effectiveCompletedCycles() {
        long duration = spec.durationMillis();
        if (duration <= 0L) {
            return 0L;
        }
        return elapsedMillis / duration;
    }

    private void resetTimeline() {
        elapsedMillis = 0L;
        completedCycles = 0L;
        forward = true;
        currentValue = spec.startValue();
    }
}


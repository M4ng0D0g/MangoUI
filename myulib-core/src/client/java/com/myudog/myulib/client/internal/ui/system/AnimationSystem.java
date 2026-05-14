package com.myudog.myulib.client.internal.ui.system;

import com.myudog.myulib.api.core.animation.AnimatorComponent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * AnimationSystem
 *
 * 系統：客戶端 UI 系統 (Client UI - Internal)
 * 角色：UI 動畫的調度與更新引擎。
 * 類型：System / Manager
 *
 * 此系統負責管理所有註冊的 {@link AnimatorComponent}。
 * 透過掛載於客戶端的渲染循環或特定更新事件，驅動所有動畫實例進行 Tick 累加與值更新。
 * 支援透過 String ID 進行動畫的播放、暫停、恢復與停止操作。
 */
public final class AnimationSystem {

    /** 儲存所有已註冊的動畫組件，按 ID 映射。 */
    private final Map<String, AnimatorComponent<?>> animators = new LinkedHashMap<>();

    /** 總累計流逝毫秒數。 */
    private long totalTickedMillis;

    public long totalTickedMillis() {
        return totalTickedMillis;
    }

    public int size() {
        return animators.size();
    }

    public boolean contains(String id) {
        return animators.containsKey(normalize(id));
    }

    /**
     * 註冊一個新的動畫組件。
     *
     * @param id       動畫唯一識別碼
     * @param animator 動畫組件實例
     * @return 該動畫的控制句柄 (Handle)
     */
    public <T> Handle<T> register(String id, AnimatorComponent<T> animator) {
        String key = normalize(id);
        Objects.requireNonNull(animator, "animator");
        if (animators.containsKey(key)) {
            throw new IllegalArgumentException("Animation already registered: " + key);
        }
        animators.put(key, animator);
        return new Handle<>(this, key);
    }

    /**
     * 獲取指定 ID 的動畫組件。
     */
    public <T> AnimatorComponent<T> animator(String id) {
        return castAnimator(requireAnimator(id));
    }

    /**
     * 註銷指定 ID 的動畫組件。
     */
    public void unregister(String id) {
        animators.remove(normalize(id));
    }

    /**
     * 清空所有動畫。
     */
    public void clear() {
        animators.clear();
        totalTickedMillis = 0L;
    }

    /**
     * 開始播放動畫。
     */
    public boolean play(String id) {
        AnimatorComponent<?> animator = animators.get(normalize(id));
        if (animator == null) {
            return false;
        }
        animator.play();
        return true;
    }

    /**
     * 暫停動畫。
     */
    public boolean pause(String id) {
        AnimatorComponent<?> animator = animators.get(normalize(id));
        if (animator == null) {
            return false;
        }
        animator.pause();
        return true;
    }

    /**
     * 恢復播放暫停的動畫。
     */
    public boolean resume(String id) {
        AnimatorComponent<?> animator = animators.get(normalize(id));
        if (animator == null) {
            return false;
        }
        animator.resume();
        return true;
    }

    /**
     * 停止動畫。
     */
    public boolean stop(String id) {
        AnimatorComponent<?> animator = animators.get(normalize(id));
        if (animator == null) {
            return false;
        }
        animator.stop();
        return true;
    }

    /**
     * 驅動所有動畫進行更新。
     *
     * @param deltaMillis 自上次更新以來流逝的毫秒數
     */
    public void tick(long deltaMillis) {
        if (deltaMillis < 0L) {
            throw new IllegalArgumentException("deltaMillis must be >= 0");
        }
        totalTickedMillis += deltaMillis;
        for (AnimatorComponent<?> animator : animators.values()) {
            animator.tick(deltaMillis);
        }
    }

    private AnimatorComponent<?> requireAnimator(String id) {
        String key = normalize(id);
        AnimatorComponent<?> animator = animators.get(key);
        if (animator == null) {
            throw new IllegalArgumentException("Unknown animation: " + key);
        }
        return animator;
    }

    private static String normalize(String id) {
        return Objects.requireNonNull(id, "id");
    }

    @SuppressWarnings("unchecked")
    private static <T> AnimatorComponent<T> castAnimator(AnimatorComponent<?> animator) {
        return (AnimatorComponent<T>) animator;
    }

    /**
     * 控制句柄 (Handle)
     * 提供對特定動畫實例的便捷控制介面。
     */
    public static final class Handle<T> {
        private final AnimationSystem system;
        private final String id;

        private Handle(AnimationSystem system, String id) {
            this.system = system;
            this.id = id;
        }

        public String id() {
            return id;
        }

        public AnimatorComponent<T> animator() {
            return system.animator(id);
        }

        public void play() {
            system.play(id);
        }

        public void pause() {
            system.pause(id);
        }

        public void resume() {
            system.resume(id);
        }

        public void stop() {
            system.stop(id);
        }

        public void unregister() {
            system.unregister(id);
        }
    }
}


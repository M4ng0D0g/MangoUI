package com.myudog.myulib.client.gui.base;

import com.myudog.myulib.api.core.animation.AnimationSpec;
import com.myudog.myulib.api.core.animation.AnimatorComponent;
import com.myudog.myulib.api.core.animation.Interpolators;

/**
 * AnimatedProperty
 * 
 * 封裝了一個可動畫化的數值。
 */
public class AnimatedProperty<T> {
    private T value;
    private AnimatorComponent<T> animator;

    public AnimatedProperty(T initialValue) {
        this.value = initialValue;
    }

    public void animateTo(T targetValue, long durationMillis) {
        // 這裡需要一個簡單的方法來建立 Spec，暫時簡化
        // 假設我們只支援 Float/Double 等基本類型，或者開發者傳入 Spec
    }
    
    // 為了展示，我們實作一個具體的 Float 版本
    public static class FloatProperty extends AnimatedProperty<Float> {
        public FloatProperty(Float initialValue) {
            super(initialValue);
        }
        
        public void animateTo(Float targetValue, long durationMillis) {
            AnimationSpec<Float> spec = AnimationSpec.builder(Interpolators.FLOAT)
                .startValue(getValue())
                .endValue(targetValue)
                .durationMillis(durationMillis)
                .build();
            
            // 這裡需要一個機制來播放動畫並更新 getValue()
        }
    }

    public T getValue() {
        return value;
    }
}

package com.myudog.myulib.client.gui;

import com.myudog.myulib.client.gui.layout.FlexContainer;
import com.myudog.myulib.client.gui.widgets.atomic.Box;
import com.myudog.myulib.client.gui.widgets.atomic.Label;
import com.myudog.myulib.client.gui.widgets.interactive.MyuButton;
import net.minecraft.network.chat.Component;

/**
 * MyuUI
 * 
 * 供開發者使用的 DSL 入口點。
 */
public final class MyuUI {
    private MyuUI() {}

    public static Box box() {
        return new Box();
    }

    public static Label label(String text) {
        return label(Component.literal(text));
    }

    public static Label label(Component text) {
        Label label = new Label();
        label.setText(text);
        return label;
    }

    public static MyuButton button(String text, Runnable onClick) {
        return new MyuButton(Component.literal(text), onClick);
    }

    public static FlexContainer column() {
        return new FlexContainer(FlexContainer.Direction.VERTICAL);
    }

    public static FlexContainer row() {
        return new FlexContainer(FlexContainer.Direction.HORIZONTAL);
    }
}

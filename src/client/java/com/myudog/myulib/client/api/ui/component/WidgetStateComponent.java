package com.myudog.myulib.client.api.ui.component;

import com.myudog.myulib.api.ecs.IComponent;

public class WidgetStateComponent implements IComponent {
    public boolean visible = true;
    public boolean enabled = true;
    public boolean hovered;
    public boolean pressed;
    public boolean focused;
}

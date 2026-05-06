package com.myudog.myulib.client.api.ui.component;

import com.myudog.myulib.api.ecs.IComponent;

public class WidgetInstanceComponent implements IComponent {
    public String widgetId;
    public Object widget;
    public boolean dirty = true;
}

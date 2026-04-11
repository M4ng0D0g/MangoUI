package com.myudog.myulib.internal.field;

import com.myudog.myulib.api.field.FieldAdminService;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;

public class DefaultFieldManager {
    public static void install() { FieldManager.install(); }
    public static void register(FieldDefinition field) { FieldAdminService.create(field); }
    public static FieldDefinition get(String fieldId) { return FieldAdminService.get(fieldId); }
}

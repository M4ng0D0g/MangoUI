package com.myudog.myulib.client.api.field;

import com.mojang.blaze3d.vertex.PoseStack;
import com.myudog.myulib.MyulibFramework;
import com.myudog.myulib.api.core.hologram.HologramDefinition;
import com.myudog.myulib.api.core.hologram.HologramStyle;
import com.myudog.myulib.api.core.hologram.network.HologramNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Modifier;

public final class FieldVisualizationClientRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyulibFramework.MOD_ID);
    private static final List<HologramDefinition> ACTIVE_ENTRIES = new ArrayList<>();
    private static volatile boolean installed;

    private FieldVisualizationClientRenderer() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;

        ClientPlayNetworking.registerGlobalReceiver(HologramNetworking.HologramPayload.TYPE,
                (HologramNetworking.HologramPayload payload, ClientPlayNetworking.Context context) -> {
                    context.client().execute(() -> {
                        synchronized (ACTIVE_ENTRIES) {
                            ACTIVE_ENTRIES.clear();
                            ACTIVE_ENTRIES.addAll(payload.entries());
                        }
                    });
                });

        installRenderHook();
    }

    private static void installRenderHook() {
        List<String> classNames = List.of(
                "net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents",
                "net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents"
        );
        List<String> fieldNames = List.of(
                "AFTER_ENTITIES",
                "LAST",
                "BEFORE_DEBUG_RENDER",
                "END_MAIN",
                "BEFORE_TRANSLUCENT",
                "AFTER_TRANSLUCENT"
        );
        for (String className : classNames) {
            for (String fieldName : fieldNames) {
                if (tryRegisterWorldRenderEvent(className, fieldName)) {
                    LOGGER.info("[Myulib] Field visualization renderer hook installed: {}#{}", className, fieldName);
                    return;
                }
            }
        }
        LOGGER.warn("[Myulib] Field visualization renderer hook not installed: no supported WorldRenderEvents API found.");
    }

    private static boolean tryRegisterWorldRenderEvent(String className, String fieldName) {
        try {
            Class<?> eventsClass = Class.forName(className);
            Field eventField = eventsClass.getField(fieldName);
            Object event = eventField.get(null);
            Method register = findMethod(event.getClass(), "register", 1);
            if (register == null) {
                return false;
            }
            Class<?> callbackType = register.getParameterTypes()[0];
            Object callback = Proxy.newProxyInstance(
                    callbackType.getClassLoader(),
                    new Class<?>[]{callbackType},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            if ("hashCode".equals(method.getName())) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(method.getName())) {
                                return proxy == (args == null || args.length == 0 ? null : args[0]);
                            }
                            if ("toString".equals(method.getName())) {
                                return "FieldVisualizationRenderCallbackProxy";
                            }
                        }
                        if (args != null && args.length >= 1) {
                            render(args[0]);
                        }
                        if (method.getReturnType() == boolean.class) {
                            return Boolean.TRUE;
                        }
                        return null;
                    }
            );
            register.invoke(event, callback);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void render(Object context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        List<HologramDefinition> snapshot;
        synchronized (ACTIVE_ENTRIES) {
            snapshot = List.copyOf(ACTIVE_ENTRIES);
        }

        PoseStack poseStack = extractPoseStack(context);
        Object lineBuffer = extractLineBuffer(context);
        Vec3 cameraPos = extractCameraPos(minecraft);
        if (poseStack == null || lineBuffer == null || cameraPos == null) {
            return;
        }

        AABB fallbackBox = snapshot.isEmpty() ? buildFallbackBox(minecraft) : null;
        if (snapshot.isEmpty() && fallbackBox == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Minimal viable guarantee: always render one white translucent region.
        AABB primary = fallbackBox != null ? fallbackBox : snapshot.getFirst().bounds();
        invokeLineBox(poseStack, lineBuffer, primary, 1.0f, 1.0f, 1.0f, 1.0f);
        drawVirtualBlockProjection(poseStack, lineBuffer, primary, 1.0f, 1.0f, 1.0f, 0.35f);

        for (HologramDefinition entry : snapshot) {
            AABB box = entry.bounds();
            HologramStyle style = entry.style();

            if (style.showLines()) {
                invokeLineBox(poseStack, lineBuffer, box, 0.2f, 0.95f, 1.0f, 1.0f);
            }
            if (style.showPoints()) {
                drawCorners(poseStack, lineBuffer, box, 1.0f, 1.0f, 0.2f, 1.0f);
            }
            if (style.showAxes()) {
                drawAxes(poseStack, lineBuffer, box);
            }
            if (style.showFaces()) {
                drawVirtualBlockProjection(poseStack, lineBuffer, box, 0.9f, 0.4f, 1.0f, 0.55f);
            }
            if (style.showName()) {
                drawNameMarker(minecraft, poseStack, lineBuffer, box, entry.id().toString(), 0.4f, 1.0f, 0.5f, 1.0f);
            }
        }

        poseStack.popPose();
    }

    private static AABB buildFallbackBox(Minecraft minecraft) {
        if (minecraft.player == null) {
            return null;
        }
        Vec3 center = minecraft.player.position();
        return new AABB(
                center.x - 1.5,
                center.y,
                center.z - 1.5,
                center.x + 1.5,
                center.y + 2.0,
                center.z + 1.5
        );
    }

    private static PoseStack extractPoseStack(Object context) {
        for (String methodName : List.of("matrices", "matrixStack")) {
            try {
                Method method = context.getClass().getMethod(methodName);
                Object result = method.invoke(context);
                if (result instanceof PoseStack poseStack) {
                    return poseStack;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static Vec3 extractCameraPos(Minecraft minecraft) {
        try {
            Object camera = minecraft.gameRenderer.getMainCamera();
            for (Method method : camera.getClass().getMethods()) {
                if (method.getParameterCount() == 0 && Vec3.class.isAssignableFrom(method.getReturnType())) {
                    Object result = method.invoke(camera);
                    if (result instanceof Vec3 vec3) {
                        return vec3;
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Object extractLineBuffer(Object context) {
        try {
            Method consumersMethod = findMethod(context.getClass(), "consumers", 0);
            if (consumersMethod == null) {
                return null;
            }
            Object consumers = consumersMethod.invoke(context);
            if (consumers == null) {
                return null;
            }
            Object lineLayer = resolveLineLayer();
            if (lineLayer == null) {
                return null;
            }
            for (Method method : consumers.getClass().getMethods()) {
                if (!method.getName().equals("getBuffer") || method.getParameterCount() != 1) {
                    continue;
                }
                if (!method.getParameterTypes()[0].isInstance(lineLayer)) {
                    continue;
                }
                return method.invoke(consumers, lineLayer);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Object resolveLineLayer() {
        try {
            Class<?> renderTypeClass = Class.forName("net.minecraft.client.renderer.RenderType");
            Method lines = renderTypeClass.getMethod("lines");
            return lines.invoke(null);
        } catch (Throwable ignored) {
        }
        try {
            Class<?> renderPipelinesClass = Class.forName("net.minecraft.client.renderer.RenderPipelines");
            Field lines = renderPipelinesClass.getField("LINES");
            return lines.get(null);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void invokeLineBox(PoseStack poseStack, Object lineBuffer, AABB box, float r, float g, float b, float a) {
        try {
            for (Method method : LevelRenderer.class.getMethods()) {
                if (!method.getName().equals("renderLineBox") || !Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 7
                        && params[0].isInstance(poseStack)
                        && params[1].isInstance(lineBuffer)
                        && params[2].isAssignableFrom(AABB.class)) {
                    method.invoke(null, poseStack, lineBuffer, box, r, g, b, a);
                    return;
                }
                if (params.length == 13 && params[0].isInstance(poseStack) && params[1].isInstance(lineBuffer)) {
                    method.invoke(null,
                            poseStack,
                            lineBuffer,
                            box.minX,
                            box.minY,
                            box.minZ,
                            box.maxX,
                            box.maxY,
                            box.maxZ,
                            r,
                            g,
                            b,
                            a);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void drawCorners(PoseStack poseStack, Object lineBuffer, AABB box, float r, float g, float b, float a) {
        double size = 0.08;
        for (Vec3 corner : cornersOf(box)) {
            AABB marker = new AABB(
                    corner.x - size,
                    corner.y - size,
                    corner.z - size,
                    corner.x + size,
                    corner.y + size,
                    corner.z + size
            );
            invokeLineBox(poseStack, lineBuffer, marker, r, g, b, a);
        }
    }

    private static void drawAxes(PoseStack poseStack, Object lineBuffer, AABB box) {
        double x0 = box.minX;
        double y0 = box.minY;
        double z0 = box.minZ;
        invokeLineSegment(poseStack, lineBuffer, x0, y0, z0, box.maxX, y0, z0, 1.0f, 0.25f, 0.25f, 1.0f);
        invokeLineSegment(poseStack, lineBuffer, x0, y0, z0, x0, box.maxY, z0, 0.25f, 1.0f, 0.25f, 1.0f);
        invokeLineSegment(poseStack, lineBuffer, x0, y0, z0, x0, y0, box.maxZ, 0.25f, 0.65f, 1.0f, 1.0f);
    }

    private static void drawFaceGuides(PoseStack poseStack, Object lineBuffer, AABB box, float r, float g, float b, float a) {
        invokeLineSegment(poseStack, lineBuffer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        invokeLineSegment(poseStack, lineBuffer, box.minX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        invokeLineSegment(poseStack, lineBuffer, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
        invokeLineSegment(poseStack, lineBuffer, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        invokeLineSegment(poseStack, lineBuffer, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, a);
        invokeLineSegment(poseStack, lineBuffer, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
    }

    private static void drawVirtualBlockProjection(PoseStack poseStack,
                                                   Object lineBuffer,
                                                   AABB box,
                                                   float r,
                                                   float g,
                                                   float b,
                                                   float a) {
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.ceil(box.maxX) - 1;
        int maxY = (int) Math.ceil(box.maxY) - 1;
        int maxZ = (int) Math.ceil(box.maxZ) - 1;

        if (maxX < minX || maxY < minY || maxZ < minZ) {
            return;
        }

        long sx = (long) maxX - minX + 1L;
        long sy = (long) maxY - minY + 1L;
        long sz = (long) maxZ - minZ + 1L;
        // Hard cap to avoid client spikes on accidental huge projections.
        long shellEstimate = sx * sy * 2L + sx * sz * 2L + sy * sz * 2L;
        if (shellEstimate > 4096L) {
            drawFaceGuides(poseStack, lineBuffer, box, r, g, b, a);
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onShell = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    if (!onShell) {
                        continue;
                    }
                    AABB blockBox = new AABB(x, y, z, x + 1.0, y + 1.0, z + 1.0);
                    invokeLineBox(poseStack, lineBuffer, blockBox, r, g, b, a);
                }
            }
        }
    }

    private static void drawNameMarker(Minecraft minecraft,
                                       PoseStack poseStack,
                                       Object lineBuffer,
                                       AABB box,
                                       String id,
                                       float r,
                                       float g,
                                       float b,
                                       float a) {
        double cx = (box.minX + box.maxX) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        double top = box.maxY;
        if (!drawFloatingText(minecraft, id, cx, top + 0.5, cz)) {
            // Fallback if the floating text renderer API differs in this runtime.
            invokeLineSegment(poseStack, lineBuffer, cx, top, cz, cx, top + 0.5, cz, r, g, b, a);
        }
    }

    private static boolean drawFloatingText(Minecraft minecraft, String text, double x, double y, double z) {
        try {
            Class<?> debugRendererClass = Class.forName("net.minecraft.client.renderer.debug.DebugRenderer");
            for (Method method : debugRendererClass.getMethods()) {
                if (!"renderFloatingText".equals(method.getName()) || !Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length >= 7
                        && params[0] == String.class
                        && params[1] == double.class
                        && params[2] == double.class
                        && params[3] == double.class
                        && params[4] == int.class
                        && params[5] == float.class
                        && params[6] == boolean.class) {
                    Object[] args = new Object[params.length];
                    args[0] = text == null ? "" : text;
                    args[1] = x;
                    args[2] = y;
                    args[3] = z;
                    args[4] = 0xA0FFD0;
                    args[5] = 0.02f;
                    args[6] = Boolean.FALSE;
                    for (int i = 7; i < params.length; i++) {
                        if (params[i] == boolean.class) {
                            args[i] = Boolean.FALSE;
                        } else if (params[i] == float.class) {
                            args[i] = 0.0f;
                        } else if (params[i] == int.class) {
                            args[i] = 0;
                        } else if (params[i] == double.class) {
                            args[i] = 0.0d;
                        } else if (!params[i].isPrimitive() && minecraft != null && params[i].isInstance(minecraft.font)) {
                            args[i] = minecraft.font;
                        } else if (!params[i].isPrimitive()) {
                            args[i] = null;
                        }
                    }
                    method.invoke(null, args);
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void invokeLineSegment(PoseStack poseStack,
                                          Object lineBuffer,
                                          double x1,
                                          double y1,
                                          double z1,
                                          double x2,
                                          double y2,
                                          double z2,
                                          float r,
                                          float g,
                                          float b,
                                          float a) {
        try {
            for (Method method : LevelRenderer.class.getMethods()) {
                if (!method.getName().equals("renderLineBox") || !Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 13 && params[0].isInstance(poseStack) && params[1].isInstance(lineBuffer)) {
                    method.invoke(null, poseStack, lineBuffer, x1, y1, z1, x2, y2, z2, r, g, b, a);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static List<Vec3> cornersOf(AABB box) {
        return List.of(
                new Vec3(box.minX, box.minY, box.minZ),
                new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ),
                new Vec3(box.minX, box.maxY, box.maxZ),
                new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.maxZ)
        );
    }

    private static Method findMethod(Class<?> type, String name, int argCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        return null;
    }
}







package com.myudog.myulib.api.timer;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
final class TimerSystemTest {
    @BeforeEach
    void reset() {
        resetTimerManagerState();
    }
    @Test
    void timerLifecycleUpdatesSnapshotAndTriggersBindings() {
        Identifier timerId = Identifier.fromNamespaceAndPath("tests", "countdown");
        AtomicInteger started = new AtomicInteger();
        AtomicInteger elapsed = new AtomicInteger();
        AtomicInteger remaining = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();
        TimerDefinition timer = new TimerDefinition(timerId, 3L, TimerMode.COUNT_DOWN, true)
                .onStarted(snapshot -> started.incrementAndGet())
                .onElapsedTick(1L, snapshot -> elapsed.incrementAndGet())
                .onRemainingTick(2L, snapshot -> remaining.incrementAndGet())
                .onCompleted(snapshot -> completed.incrementAndGet());
        TimerManager.INSTANCE.register(timer);
        int instanceId = TimerManager.INSTANCE.createInstance(
                timerId,
                42L,
                new RespawnTimerPayload(UUID.fromString("00000000-0000-0000-0000-000000000999"), true),
                true,
                null
        );
        assertTrue(TimerManager.INSTANCE.isRunning(instanceId, null), "Auto-started timer should be running immediately after creation");
        TimerSnapshot snapshot = TimerManager.INSTANCE.getSnapshot(instanceId);
        assertEquals(TimerStatus.RUNNING, snapshot.status(), "Created timer should start in the RUNNING state");
        assertEquals(0L, snapshot.elapsedTicks(), "Timer should start with zero elapsed ticks");
        assertEquals(3L, snapshot.remainingTicks(), "Timer should start with full remaining ticks");
        assertEquals(0.0d, snapshot.progress(), 1.0e-9, "Timer progress should start at zero");
        TimerManager.INSTANCE.update(null);
        snapshot = TimerManager.INSTANCE.getSnapshot(instanceId);
        assertEquals(1L, snapshot.elapsedTicks(), "One update should advance elapsed ticks by one");
        assertEquals(2L, snapshot.remainingTicks(), "One update should reduce remaining ticks by one");
        assertEquals(1, started.get(), "Start action should fire when the timer auto-starts");
        assertEquals(1, elapsed.get(), "Elapsed binding should fire at tick 1");
        assertEquals(1, remaining.get(), "Remaining binding should fire when the remaining tick matches");
        TimerManager.INSTANCE.update(null);
        TimerManager.INSTANCE.update(null);
        assertEquals(1, completed.get(), "Completion action should fire exactly once");
        assertTrue(TimerManager.INSTANCE.isStopped(instanceId, null), "Auto-stop should leave the timer in STOPPED state");
        assertEquals(TimerStatus.STOPPED, TimerManager.INSTANCE.getInstance(instanceId, null).status,
                "The underlying timer instance should be marked STOPPED after auto-stop-on-complete");
    }
    @Test
    void timerMutatorsAndPayloadValidationWork() {
        Identifier timerId = Identifier.fromNamespaceAndPath("tests", "cooldown");
        RespawnTimerPayload payload = new RespawnTimerPayload(
                UUID.fromString("00000000-0000-0000-0000-000000000555"),
                false
        );
        TimerDefinition timer = new TimerDefinition(timerId, 10L, TimerMode.COUNT_UP, false);
        TimerManager.INSTANCE.register(timer);
        int instanceId = TimerManager.INSTANCE.createInstance(timerId, 99L, payload, false, null);
        assertFalse(TimerManager.INSTANCE.isRunning(instanceId, null), "Timer should not auto-start when autoStart is false");
        TimerManager.INSTANCE.start(instanceId);
        assertTrue(TimerManager.INSTANCE.isRunning(instanceId, null), "Timer should become running after start");
        TimerManager.INSTANCE.setElapsedTicks(instanceId, 4L);
        assertEquals(4L, TimerManager.INSTANCE.getSnapshot(instanceId).elapsedTicks(), "Elapsed ticks should be set directly");
        TimerManager.INSTANCE.setRemainingTicks(instanceId, 3L);
        TimerSnapshot snapshot = TimerManager.INSTANCE.getSnapshot(instanceId);
        assertEquals(7L, snapshot.elapsedTicks(), "Remaining tick setter should update elapsed ticks consistently");
        assertEquals(3L, snapshot.remainingTicks(), "Remaining tick setter should update remaining ticks consistently");
        TimerPayload replacement = new SoundTimerPayload("minecraft:block.note_block.harp", 1.0f, 1.0f);
        TimerManager.INSTANCE.setPayload(instanceId, replacement);
        assertEquals(replacement, TimerManager.INSTANCE.getSnapshot(instanceId).payload(), "Payload setter should replace the payload");
        TimerManager.INSTANCE.pause(instanceId);
        assertTrue(TimerManager.INSTANCE.isPaused(instanceId, null), "Pause should move the timer to PAUSED");
        TimerManager.INSTANCE.resume(instanceId);
        assertTrue(TimerManager.INSTANCE.isRunning(instanceId, null), "Resume should move the timer back to RUNNING");
        TimerManager.INSTANCE.stop(instanceId);
        assertTrue(TimerManager.INSTANCE.isStopped(instanceId, null), "Stop should move the timer to STOPPED");
        TimerManager.INSTANCE.reset(instanceId, true);
        snapshot = TimerManager.INSTANCE.getSnapshot(instanceId);
        assertEquals(TimerStatus.IDLE, snapshot.status(), "Reset should return the timer to IDLE");
        assertEquals(0L, snapshot.elapsedTicks(), "Reset should clear elapsed ticks");
        assertNull(snapshot.payload(), "Reset with clearPayload should remove the payload");
    }
    @Test
    void timerSnapshotRequiresPayloadWhenMissing() {
        TimerSnapshot snapshot = new TimerSnapshot(
                1,
                2L,
                null,
                TimerStatus.IDLE,
                0L,
                0L,
                null,
                0L
        );
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                snapshot::requirePayload,
                "requirePayload should reject missing payloads"
        );
        assertEquals("Timer payload is missing", ex.getMessage(), "Missing payload should explain the problem");
        assertDoesNotThrow(() -> snapshot.payloadAs(), "payloadAs should simply return null when no payload exists");
    }
    private static void resetTimerManagerState() {
        try {
            clearStaticMap("TIMERS");
            clearStaticMap("INSTANCES");
            resetAtomicInteger("NEXT_INSTANCE_ID", 1);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to reset TimerManager state for tests", ex);
        }
    }
    @SuppressWarnings("unchecked")
    private static void clearStaticMap(String fieldName) throws ReflectiveOperationException {
        Field field = TimerManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Map<Object, Object>) field.get(null)).clear();
    }
    private static void resetAtomicInteger(String fieldName, int value) throws ReflectiveOperationException {
        Field field = TimerManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((AtomicInteger) field.get(null)).set(value);
    }

    private record RespawnTimerPayload(UUID playerId, boolean allowSkip) implements TimerPayload {}

    private record SoundTimerPayload(String soundId, float volume, float pitch) implements TimerPayload {}
}

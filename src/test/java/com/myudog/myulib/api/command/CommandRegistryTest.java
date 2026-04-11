package com.myudog.myulib.api.command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
final class CommandRegistryTest {
    @BeforeEach
    void reset() {
        CommandRegistry.clear();
    }
    @Test
    void contextAndResultTypesNormalizeInputsAndRemainImmutable() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("name", "builder");
        CommandContext context = new CommandContext(null, null, source);
        source.put("name", "changed");
        assertEquals("", context.sourceId(), "Null sourceId should normalize to an empty string");
        assertEquals("", context.commandName(), "Null commandName should normalize to an empty string");
        assertEquals("builder", context.arguments().get("name"), "CommandContext should defensively copy arguments");
        assertThrows(UnsupportedOperationException.class,
                () -> context.arguments().put("x", "y"),
                "CommandContext arguments should be immutable");
        CommandResult success = CommandResult.success("ok");
        assertTrue(success.success(), "CommandResult.success(...) should mark success");
        assertEquals("ok", success.message(), "CommandResult.success(...) should preserve the message");
        CommandResult failure = CommandResult.failure("nope");
        assertFalse(failure.success(), "CommandResult.failure(...) should mark failure");
        assertEquals("nope", failure.message(), "CommandResult.failure(...) should preserve the message");
    }
    @Test
    void registryExecutesRegisteredCommandsAndExplainsMissingCommands() {
        CommandRegistry.register("echo", context -> CommandResult.success("Hello " + context.arguments().getOrDefault("name", "world")));
        CommandResult executed = CommandRegistry.execute(new CommandContext("console", "echo", Map.of("name", "builder")));
        assertTrue(executed.success(), "Registered command should succeed");
        assertEquals("Hello builder", executed.message(), "Registered command should return the handler message");
        CommandResult missing = CommandRegistry.execute(new CommandContext("console", "missing", Map.of()));
        assertFalse(missing.success(), "Unknown command should fail");
        assertEquals("Unknown command: missing", missing.message(), "Missing command should explain which name was not found");
        assertEquals(1, CommandRegistry.snapshot().size(), "Registry snapshot should expose the registered command");
    }
}

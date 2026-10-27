package io.github.lichao10.stardewfishingautofish;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;

/** Inspect actual upstream bytecode without initializing the game's signed/remapped classes. */
class UpstreamContractTest {
    private static final String ROOT = "com/bonker/stardewfishing/";
    private record Members(Map<String, String> fields, Map<String, String> methods) {}
    private Members read(String name) throws Exception {
        Map<String, String> fields = new HashMap<>(), methods = new HashMap<>();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(ROOT + name + ".class")) {
            assertNotNull(input, name);
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public FieldVisitor visitField(int access, String n, String d, String signature, Object value) {
                    fields.put(n, d); return null;
                }
                @Override public MethodVisitor visitMethod(int access, String n, String d, String signature, String[] exceptions) {
                    methods.put(n + d, d); return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return new Members(fields, methods);
    }
    @Test void pinnedMinigameHasControllerFields() throws Exception {
        var fields = read("client/FishingMinigame").fields();
        assertEquals("D", fields.get("bobberVelocity"));
        assertEquals("D", fields.get("fishVelocity"));
        assertEquals("I", fields.get("fishTarget"));
        assertEquals("Z", fields.get("fishIsIdle"));
        assertEquals("Z", fields.get("hasTreasureBobber"));
        assertEquals("L" + ROOT + "common/networking/S2CStartMinigamePacket;", fields.get("packet"));
    }
    @Test void pinnedScreenHasExpectedInputAndState() throws Exception {
        var screen = read("client/FishingScreen");
        assertEquals("L" + ROOT + "client/FishingMinigame;", screen.fields().get("minigame"));
        assertEquals("L" + ROOT + "client/FishingScreen$Status;", screen.fields().get("status"));
        assertTrue(screen.methods().containsKey("setInputDown(Z)V"));
    }
    @Test void pinnedPacketContainsControllerParameters() throws Exception {
        var methods = read("common/networking/S2CStartMinigamePacket").methods();
        assertTrue(methods.containsKey("topSpeed()F"));
        assertTrue(methods.containsKey("lineStrength()F"));
        assertTrue(methods.containsKey("barSize()I"));
    }
}

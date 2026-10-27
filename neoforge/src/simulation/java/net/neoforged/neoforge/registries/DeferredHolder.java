package net.neoforged.neoforge.registries;
/** Test-only holder for muted audio; never shipped in the mod JAR. */
public class DeferredHolder<R, T extends R> {
    public T get() { return null; }
}

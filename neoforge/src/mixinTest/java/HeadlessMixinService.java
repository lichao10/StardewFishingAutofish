import java.io.*;
import java.net.URL;
import java.util.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.*;
import org.spongepowered.asm.launch.platform.container.*;

/** Test-only service: transform real installed classes without launching Minecraft/GL. */
public class HeadlessMixinService extends MixinServiceAbstract
        implements IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {
    public String getName() { return "StardewFishingAutofishHeadlessTest"; }
    public boolean isValid() { return true; }
    public MixinEnvironment.Phase getInitialPhase() { return MixinEnvironment.Phase.DEFAULT; }
    public IClassProvider getClassProvider() { return this; }
    public IClassBytecodeProvider getBytecodeProvider() { return this; }
    public ITransformerProvider getTransformerProvider() { return this; }
    public IClassTracker getClassTracker() { return this; }
    public IMixinAuditTrail getAuditTrail() { return null; }
    public IFeatureValidator getFeatureValidator() { return IFeatureValidator.ALLOW_ALL; }
    public IAdviceProvider getAdviceProvider() { return IAdviceProvider.GENERIC; }
    public Collection<String> getPlatformAgents() { return Collections.emptyList(); }
    public IContainerHandle getPrimaryContainer() { return new ContainerHandleVirtual("headless-test"); }
    public InputStream getResourceAsStream(String name) { return getClass().getClassLoader().getResourceAsStream(name); }
    public URL[] getClassPath() { return new URL[0]; }
    public Class<?> findClass(String n) throws ClassNotFoundException { return findClass(n, false); }
    public Class<?> findClass(String n, boolean init) throws ClassNotFoundException { return Class.forName(n, init, getClass().getClassLoader()); }
    public Class<?> findAgentClass(String n, boolean init) throws ClassNotFoundException { return findClass(n, init); }
    public ClassNode getClassNode(String n) throws ClassNotFoundException, IOException { return getClassNode(n, false); }
    public ClassNode getClassNode(String n, boolean transforms) throws ClassNotFoundException, IOException {
        return getClassNode(n, transforms, 0);
    }
    public ClassNode getClassNode(String n, boolean transforms, int flags) throws ClassNotFoundException, IOException {
        try (InputStream in = getResourceAsStream(n.replace('.', '/') + ".class")) {
            if (in == null) throw new ClassNotFoundException(n);
            ClassNode node = new ClassNode(); new ClassReader(in).accept(node, flags); return node;
        }
    }
    public Collection<ITransformer> getTransformers() { return Collections.emptyList(); }
    public Collection<ITransformer> getDelegatedTransformers() { return Collections.emptyList(); }
    public void addTransformerExclusion(String n) {}
    public void registerInvalidClass(String n) {}
    public boolean isClassLoaded(String n) { return false; }
    public String getClassRestrictions(String n) { return ""; }
    public IMixinTransformer transformer() { return getInternal(IMixinTransformerFactory.class).createTransformer(); }
}

import java.util.*;
import org.spongepowered.asm.service.*;
public class TestProperties implements IGlobalPropertyService {
    private record Key(String name) implements IPropertyKey {}
    private final Map<IPropertyKey,Object> values = new HashMap<>();
    public IPropertyKey resolveKey(String n) { return new Key(n); }
    @SuppressWarnings("unchecked") public <T> T getProperty(IPropertyKey k) { return (T)values.get(k); }
    public void setProperty(IPropertyKey k, Object v) { values.put(k,v); }
    public <T> T getProperty(IPropertyKey k,T fallback) { T v=getProperty(k); return v==null?fallback:v; }
    public String getPropertyString(IPropertyKey k,String fallback) { Object v=values.get(k);return v==null?fallback:v.toString(); }
}

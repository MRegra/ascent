package dev.sobatista.ascent.meter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads classes under a target package from their bytecode, rewritten by {@link InstructionCounter}; everything else
 * (the JDK, {@link Counters}) is delegated to the parent so the counters stay shared. This is the hook that lets us
 * run the <em>unchanged</em> solver and still measure exactly what it did — the whole idea behind instrumentation.
 */
public final class InstrumentingClassLoader extends ClassLoader {
    private final String targetPrefix;

    public InstrumentingClassLoader(String targetPrefix, ClassLoader parent) {
        super(parent);
        this.targetPrefix = targetPrefix;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                c = name.startsWith(targetPrefix) ? defineInstrumented(name) : super.loadClass(name, false);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    private Class<?> defineInstrumented(String name) throws ClassNotFoundException {
        String resource = name.replace('.', '/') + ".class";
        try (InputStream in = getParent().getResourceAsStream(resource)) {
            if (in == null) {
                throw new ClassNotFoundException(name);
            }
            byte[] transformed = InstructionCounter.instrument(in.readAllBytes());
            return defineClass(name, transformed, 0, transformed.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }
}

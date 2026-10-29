package rikka.hidden.compat.util;

import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Last-resort helpers for hidden methods whose return type changed between Android versions.
 * <p>
 * The JVM/ART links a call by name + parameter types + return type, so a stub compiled with
 * the old return type throws {@link NoSuchMethodError} after the platform changed it (this is
 * what happened to {@code IPackageManager#getInstalledPackages} in Android 17). Reflection
 * looks a method up by name and parameter types only, so it keeps working.
 */
public final class ReflectCompat {

    private static final Map<String, Method> CACHE = new ConcurrentHashMap<>();

    private ReflectCompat() {
    }

    @NonNull
    public static Method findMethod(@NonNull Class<?> cls, @NonNull String name, @NonNull Class<?>... parameterTypes) throws NoSuchMethodException {
        String key = cls.getName() + "#" + name + Arrays.toString(parameterTypes);
        Method method = CACHE.get(key);
        if (method != null) {
            return method;
        }

        NoSuchMethodException firstError = null;
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                method = c.getDeclaredMethod(name, parameterTypes);
                break;
            } catch (NoSuchMethodException e) {
                if (firstError == null) firstError = e;
            }
        }
        if (method == null) {
            for (Class<?> i : cls.getInterfaces()) {
                try {
                    method = i.getMethod(name, parameterTypes);
                    break;
                } catch (NoSuchMethodException ignored) {
                }
            }
        }
        if (method == null) {
            throw firstError != null ? firstError : new NoSuchMethodException(key);
        }
        method.setAccessible(true);
        CACHE.put(key, method);
        return method;
    }

    /**
     * Calls {@code target.name(args)} with the given parameter types, whatever the return type is.
     *
     * @throws RemoteException       if the binder call failed
     * @throws NoSuchMethodError     if no such method exists on this platform
     */
    @Nullable
    public static Object invokeAnyReturnType(@NonNull Object target, @NonNull String name, @NonNull Class<?>[] parameterTypes, Object... args) throws RemoteException {
        Method method;
        try {
            method = findMethod(target.getClass(), name, parameterTypes);
        } catch (NoSuchMethodException e) {
            NoSuchMethodError error = new NoSuchMethodError(target.getClass().getName() + "#" + name);
            error.initCause(e);
            throw error;
        }
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RemoteException) throw (RemoteException) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new RuntimeException(cause);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}

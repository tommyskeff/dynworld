package dev.tommyjs.dynworld.nms;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Reflect {

    private static final String VERSION =
        Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    private Reflect() {
    }

    public static String version() {
        return VERSION;
    }

    public static Class<?> nms(String name) {
        return forName("net.minecraft.server." + VERSION + "." + name);
    }

    public static Class<?> craft(String name) {
        return forName("org.bukkit.craftbukkit." + VERSION + "." + name);
    }

    public static Class<?> forName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Missing class " + name + " (server " + VERSION + ")", e);
        }
    }

    public static Method method(Class<?> clazz, String name, Class<?>... params) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
            }
        }

        throw new IllegalStateException("Missing method " + clazz.getSimpleName() + "#" + name);
    }

    public static Constructor<?> constructor(Class<?> clazz, Class<?>... params) {
        try {
            Constructor<?> c = clazz.getDeclaredConstructor(params);
            c.setAccessible(true);
            return c;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Missing constructor for " + clazz.getSimpleName(), e);
        }
    }

    public static Field field(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }

        throw new IllegalStateException("Missing field " + clazz.getSimpleName() + "#" + name);
    }

    public static Object staticField(Class<?> clazz, String name) {
        try {
            return field(clazz, name).get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read " + clazz.getSimpleName() + "#" + name, e);
        }
    }

    public static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Invoke failed: " + method, e);
        }
    }

    public static Object construct(Constructor<?> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Construct failed: " + constructor, e);
        }
    }

    public static Object get(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Field read failed: " + field, e);
        }
    }

    public static void set(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Field write failed: " + field, e);
        }
    }

}

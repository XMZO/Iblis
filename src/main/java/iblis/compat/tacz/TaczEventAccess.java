package iblis.compat.tacz;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

/** Cached, fail-fast access to TACZ's optional public Forge events. */
final class TaczEventAccess {
    private TaczEventAccess() {
    }

    static Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Missing TACZ API type " + name, exception);
        }
    }

    static Class<? extends Event> eventType(String name) {
        return type(name).asSubclass(Event.class);
    }

    static Method method(Class<?> owner, String name, Class<?>... parameters) {
        try {
            return owner.getMethod(name, parameters);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(
                    "Missing TACZ API method " + owner.getName() + "#" + name, exception);
        }
    }

    static Object call(Method method, Object receiver, Object... arguments) {
        try {
            return method.invoke(receiver, arguments);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access TACZ API method " + method, exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof LinkageError linkage) {
                throw linkage;
            }
            throw new IllegalStateException("TACZ API method failed " + method, cause);
        }
    }

    static void listen(Class<? extends Event> eventType, Consumer<Object> listener) {
        listen(eventType, EventPriority.NORMAL, listener);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void listen(Class<? extends Event> eventType, EventPriority priority,
                       Consumer<Object> listener) {
        MinecraftForge.EVENT_BUS.addListener(
                priority, false, (Class) eventType, (Consumer) listener);
    }
}

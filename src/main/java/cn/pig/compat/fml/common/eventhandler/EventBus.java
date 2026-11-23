package cn.pig.compat.fml.common.eventhandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

   
                                                                   
                         
   
public class EventBus {
    private final Map<Object, List<Listener>> byOwner = new ConcurrentHashMap<Object, List<Listener>>();
    private final Map<Class<?>, List<Listener>> byEvent = new ConcurrentHashMap<Class<?>, List<Listener>>();

    public void register(Object target) {
        if (target == null || this.byOwner.containsKey(target)) {
            return;
        }
        List<Listener> listeners = new ArrayList<Listener>();
        for (Method method : allMethods(target.getClass())) {
            try {
                SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
                if (annotation == null) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1 || !Event.class.isAssignableFrom(params[0])) {
                    continue;
                }
                method.setAccessible(true);
                Listener listener = new Listener(target, method, annotation);
                listeners.add(listener);
                List<Listener> bucket = this.byEvent.get(params[0]);
                if (bucket == null) {
                    bucket = new ArrayList<Listener>();
                    this.byEvent.put(params[0], bucket);
                }
                bucket.add(listener);
            } catch (Throwable ignored) {
                                                   
            }
        }
        this.byOwner.put(target, listeners);
    }

    public void unregister(Object object) {
        List<Listener> listeners = this.byOwner.remove(object);
        if (listeners == null) {
            return;
        }
        for (Listener listener : listeners) {
            List<Listener> bucket = this.byEvent.get(listener.eventType);
            if (bucket != null) {
                bucket.remove(listener);
            }
        }
    }

                        
    public boolean post(Event event) {
        if (event == null) {
            return false;
        }
                                            
                                                                             
        List<Listener> listeners = new ArrayList<Listener>();
        for (Class<?> c = event.getClass(); c != null && Event.class.isAssignableFrom(c); c = c.getSuperclass()) {
            List<Listener> registered = this.byEvent.get(c);
            if (registered == null || registered.isEmpty()) {
                continue;
            }
            synchronized (registered) {
                listeners.addAll(registered);
            }
        }
        if (listeners.isEmpty()) {
            return event.isCanceled();
        }
        Collections.sort(listeners, BY_PRIORITY);
        for (Listener listener : listeners) {
            if (event.isCanceled() && !listener.receiveCanceled) {
                continue;
            }
            try {
                listener.method.invoke(listener.target, event);
            } catch (Throwable t) {
            }
            if (event.hasResult() && event.getResult() != Event.Result.DEFAULT) {
                break;
            }
        }
        return event.isCanceled();
    }

    private static List<Method> allMethods(Class<?> type) {
        List<Method> methods = new ArrayList<Method>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                                                       
                                                             
            try {
                for (Method method : c.getDeclaredMethods()) {
                    methods.add(method);
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            for (Method method : type.getMethods()) {
                if (!methods.contains(method)) {
                    methods.add(method);
                }
            }
        } catch (Throwable ignored) {
        }
        return methods;
    }

    public boolean hasListeners(Class<?> eventType) {
        List<Listener> list = this.byEvent.get(eventType);
        return list != null && !list.isEmpty();
    }

    private static final Comparator<Listener> BY_PRIORITY = new Comparator<Listener>() {
        @Override
        public int compare(Listener a, Listener b) {
            return a.priority.ordinal() - b.priority.ordinal();
        }
    };

    private static final class Listener {
        final Object target;
        final Method method;
        final Class<?> eventType;
        final EventPriority priority;
        final boolean receiveCanceled;

        Listener(Object target, Method method, SubscribeEvent annotation) {
            this.target = target;
            this.method = method;
            this.eventType = method.getParameterTypes()[0];
            this.priority = annotation.priority();
            this.receiveCanceled = annotation.receiveCanceled();
        }
    }
}

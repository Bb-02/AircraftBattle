package com.aircraftwar.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * 轻量事件总线（队列式）：post() 将事件入队，调用方在合适时机调用 drain() 在同一线程处理事件，避免订阅者阻塞。
 */
public class EventBus {
    private static final EventBus INSTANCE = new EventBus();

    private final Map<Class<?>, List<Consumer<?>>> handlers = new HashMap<>();
    private final ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<>();

    private EventBus() {}

    public static EventBus getDefault() {
        return INSTANCE;
    }

    public synchronized <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    public synchronized <T> void unsubscribe(Class<T> eventType, Consumer<T> handler) {
        List<Consumer<?>> list = handlers.get(eventType);
        if (list != null) list.remove(handler);
    }

    /**
     * 将事件放入队列，稍后由 drain() 处理（线程安全）
     */
    public void post(Object event) {
        if (event == null) return;
        queue.offer(event);
    }

    /**
     * 在调用线程处理队列中的事件（非并发）。推荐在主循环中调用一次/帧处理。
     */
    @SuppressWarnings("unchecked")
    public void drain() {
        Object evt;
        while ((evt = queue.poll()) != null) {
            List<Consumer<?>> list;
            synchronized (this) {
                list = handlers.get(evt.getClass());
                if (list == null) continue;
                list = new ArrayList<>(list);
            }
            for (Consumer handler : list) {
                try {
                    handler.accept(evt);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

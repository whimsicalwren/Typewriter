package dev.wren.typewriter.internal;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class EventReceiver<T extends Event> implements Consumer<T> {
    public static <T extends Event & IModBusEvent> void addModBusListener(TypewriterBase<?> owner, Class<? super T> evtClass, Consumer<? super T> listener) {
        EventReceiver.<T>addModBusListener(owner, EventPriority.NORMAL, evtClass, listener);
    }

    @SuppressWarnings("unchecked")
    static <T extends Event & IModBusEvent> void updateWaitingModBusListeners(TypewriterBase<?> owner) {
        for (Map.Entry<Class<?>, List<Pair<EventPriority, Consumer<?>>>> waitingListener : waitingListenerTable.get().row(owner).entrySet()) {
            for (Pair<EventPriority, Consumer<?>> pair : waitingListener.getValue()) {
                EventReceiver.<T>addListener(owner.getModEventBus(), pair.getKey(), (Class<? super T>) waitingListener.getKey(), (Consumer<? super T>) pair.getValue());
            }
        }
        addModBusListener(owner, FMLLoadCompleteEvent.class, EventReceiver::onLoadComplete);
    }

    public static <T extends Event & IModBusEvent> void addModBusListener(TypewriterBase<?> owner, EventPriority priority, Class<? super T> evtClass, Consumer<? super T> listener) {
        if (owner.getModEventBus() == null) {
            waitingListenerTable.add(owner, evtClass, priority, listener);
        } else {
            EventReceiver.<T>addListener(owner.getModEventBus(), priority, evtClass, listener);
        }
    }

    public static <T extends Event> void addNeoBusListener(Class<? super T> evtClass, Consumer<? super T> listener) {
        EventReceiver.<T>addListener(NeoForge.EVENT_BUS, EventPriority.NORMAL, evtClass, listener);
    }

    public static <T extends Event> void addNeoBusListener(EventPriority priority, Class<? super T> evtClass, Consumer<? super T> listener) {
        EventReceiver.<T>addListener(NeoForge.EVENT_BUS, priority, evtClass, listener);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> void addListener(IEventBus bus, EventPriority priority, Class<? super T> evtClass, Consumer<? super T> listener) {
        bus.addListener(priority, false, (Class<T>) evtClass, new EventReceiver<>(bus, listener));
    }

    private static final WaitingListenerTable waitingListenerTable = new WaitingListenerTable();

    private final IEventBus bus;
    private final Consumer<? super T> listener;
    private final AtomicBoolean consumed;

    public EventReceiver(IEventBus bus, Consumer<? super T> listener) {
        this.bus = bus;
        this.listener = listener;
        this.consumed = new AtomicBoolean();
    }

    @Override
    public void accept(T event) {
        if (consumed.compareAndSet(false, true)) {
            listener.accept(event);
            unregister(bus, this, event.getClass());
        }
    }


    public static synchronized void unregister(TypewriterBase<?> owner, Object listener, Class<? extends Event> event) {
        unregister(owner.getModEventBus(), listener, event);
    }

    private static synchronized void unregister(IEventBus bus, Object listener, Class<? extends Event> event) {
        unregister.add(Triple.of(bus, listener, event));
    }

    private static final List<Triple<IEventBus, Object, Class<? extends Event>>> unregister = new ArrayList<>();


    private static void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            unregister.forEach(t -> t.getLeft().unregister(t.getMiddle()));
            unregister.clear();
        });
    }

    private static class WaitingListenerTable {
        private final Table<TypewriterBase<?>, Class<?>, List<Pair<EventPriority, Consumer<?>>>> waitingListeners;

        WaitingListenerTable() {
            waitingListeners = HashBasedTable.create();
        }

        public Table<TypewriterBase<?>, Class<?>, List<Pair<EventPriority, Consumer<?>>>> get() {
            return waitingListeners;
        }

        public void add(TypewriterBase<?> owner, Class<?> eventClass, Pair<EventPriority, Consumer<?>> listener) {
            if (waitingListeners.get(owner, eventClass) == null) {
                waitingListeners.put(owner, eventClass, new ArrayList<>());
            }
            waitingListeners.get(owner, eventClass).add(listener);
        }

        public void add(TypewriterBase<?> owner, Class<?> eventClass, EventPriority priority, Consumer<?> consumer) {
            add(owner, eventClass, Pair.of(priority, consumer));
        }
    }
}
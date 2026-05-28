package dev.wren.typewriter.internal;

import com.google.common.collect.*;
import com.mojang.serialization.Codec;
import dev.wren.typewriter.builder.Builder;
import dev.wren.typewriter.internal.function.LazySupplier;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IParent;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IProvideData;
import dev.wren.typewriter.internal.waytoomanyinterfaces.ITransformable;
import dev.wren.typewriter.prov.DataProviderInitializer;
import dev.wren.typewriter.prov.TypewriterDataProvider;
import dev.wren.typewriter.prov.TypewriterProvider;
import dev.wren.typewriter.spec.Specification;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A base typewriter class.
 */
@ApiStatus.Internal
public abstract class TypewriterBase<SELF extends TypewriterBase<SELF>> implements ITransformable<SELF> {
    public static Logger log = LogManager.getLogger("typewriter");

    protected String modId;
    protected String currentName;
    protected TypewriterDataProvider provider;
    private final DataProviderInitializer initializer = new DataProviderInitializer();

    protected IEventBus modEventBus;
    protected LazySupplier<Boolean> doDatagen = LazySupplier.of(DatagenModLoader.isRunningDataGen());

    protected final Table<ResourceKey<? extends Registry<?>>, String, SpecRegister<?, ?>> specRegisters = HashBasedTable.create();
    private final Set<ResourceKey<? extends Registry<?>>> completedSpecRegisters = new HashSet<>();

    protected final Multimap<Pair<String, ResourceKey<? extends Registry<?>>>, Consumer<?>> onRegister = HashMultimap.create();
    protected final Multimap<ResourceKey<? extends Registry<?>>, Runnable> afterRegister = HashMultimap.create();


    private final Table<Pair<String, ResourceKey<? extends Registry<?>>>, TypewriterProvider<?>, Consumer<? extends IProvideData>> datagensByEntry = HashBasedTable.create();
    private final ListMultimap<TypewriterProvider<?>, Consumer<? extends IProvideData>> datagens = ArrayListMultimap.create();

    protected ResourceKey<CreativeModeTab> defaultCreativeTab = CreativeModeTabs.SEARCH;
    private final Multimap<ResourceKey<CreativeModeTab>, Consumer<BuildCreativeModeTabContentsEvent>> creativeModeTabModifiers = ArrayListMultimap.create();
    protected final Map<Specification<?, ?>, ResourceKey<CreativeModeTab>> creativeModeTabLookup = Collections.synchronizedMap(new IdentityHashMap<>());

    public TypewriterBase(String modId) {
        this.modId = modId;
    }

    // getters
    public IEventBus getModEventBus() { return modEventBus; }
    public String getModId() { return modId; }
    public String getCurrentName() { return currentName; }
    public DataProviderInitializer getDataProviderInitializer() { return initializer; }

    public SELF registerEventListeners(IEventBus bus) {
        if (this.modEventBus == null) {
            this.modEventBus = bus;
        }

        Consumer<RegisterEvent> onRegister = this::onRegister;
        Consumer<RegisterEvent> onRegisterLate = this::onRegisterLate;
        bus.addListener(onRegister);
        bus.addListener(EventPriority.LOWEST, onRegisterLate);
        bus.addListener(this::onBuildCreativeModeTabContents);

        EventReceiver.updateWaitingModBusListeners(this);

        EventReceiver.addModBusListener(this, FMLCommonSetupEvent.class, $ -> {
            EventReceiver.unregister(this, onRegister, RegisterEvent.class);
            EventReceiver.unregister(this, onRegisterLate, RegisterEvent.class);
        });

        if (doDatagen.get()) {
            EventReceiver.addModBusListener(this, GatherDataEvent.class, this::onGatherData);
        }

        return self();
    }

    protected void onRegister(RegisterEvent event) {
        ResourceKey<? extends Registry<?>> type = event.getRegistryKey();
        if (!onRegister.isEmpty()) {
            onRegister.asMap().forEach((k, v) -> log.warn("Found {} unused register callback(s) for specification {} [{}]. Was the specification ever registered?", v.size(), k.getLeft(), k.getRight().location()));
            onRegister.clear();
        }
        Map<String, SpecRegister<?, ?>> registrationsForType = specRegisters.row(type);
        if (!registrationsForType.isEmpty()) {
            log.trace("({}) Registering {} known objects of type {}", getModId(), registrationsForType.size(), type.location());
            for (Map.Entry<String, SpecRegister<?, ?>> e : registrationsForType.entrySet()) {
                try {
                    e.getValue().register(event);
                    log.trace("Registered {} to registry {}", e.getValue().getName(), event.getRegistryKey().location());
                } catch (Exception ex) {
                    throw new RuntimeException("Unexpected error while registering specification " + e.getValue().getName() + " to registry " + event.getRegistryKey().location());
                }
            }
        }
    }

    protected void onRegisterLate(RegisterEvent event)  {
        ResourceKey<? extends Registry<?>> type = event.getRegistryKey();
        Collection<Runnable> callbacks = afterRegister.get(type);
        callbacks.forEach(Runnable::run);
        callbacks.clear();
        completedSpecRegisters.add(type);
    }

    protected void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        creativeModeTabModifiers.forEach((key, value) -> {
            if(event.getTabKey().equals(key)) value.accept(event);
        });
    }

    protected void onGatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(true, provider = new TypewriterDataProvider(this, modId, event));
    }

    public <R, T extends R> Specification<R, T> get(String name, ResourceKey<? extends Registry<R>> type) {
        return this.<R, T>getSpec(name, type).get();
    }

    public <R, T extends R> Specification<R, T> get(ResourceKey<? extends Registry<R>> type) {
        return this.get(getCurrentName(), type);
    }

    public <R, T extends R> Optional<Specification<R, T>> getOptional(String name, ResourceKey<? extends Registry<R>> type) {
        SpecRegister<R, T> spec = this.getSpec(name, type);
        return spec == null ? Optional.empty() : Optional.of(spec.get());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected <R, T extends R> SpecRegister<R, T> getSpec(String name, ResourceKey<? extends Registry<R>> type) {
        return (SpecRegister<R, T>) specRegisters.get(type, name);
    }

    protected <R, T extends R> SpecRegister<R, T> getSafeSpec(String name, ResourceKey<? extends Registry<R>> type) {
        SpecRegister<R, T> spec = this.getSpec(name, type);
        if (spec != null) {
            return spec;
        }
        throw new IllegalArgumentException("Unknown registration " + name + " for type " + type.location());
    }

    @SuppressWarnings("unchecked")
    public <R, T extends R> Collection<Specification<R, T>> getAll(ResourceKey<? extends Registry<R>> type) {
        return specRegisters.row(type).values().stream().map(r -> (Specification<R, T>) r.get()).collect(Collectors.toList());
    }

    public <R, A extends R> SELF addRegisterCallback(String name, ResourceKey<? extends Registry<R>> registryType, Consumer<? super A> callback) {
        SpecRegister<R, A> spec = this.getSpec(name, registryType);
        if (spec == null) {
            onRegister.put(Pair.of(name, registryType), callback);
        } else {
            spec.addRegisterCallback(callback);
        }
        return self();
    }

    public <R> SELF addAfterRegisterCallback(ResourceKey<? extends Registry<R>> registryType, Runnable callback) {
        afterRegister.put(registryType, callback);
        return self();
    }

    public <R> boolean isRegistered(ResourceKey<? extends Registry<R>> registryKey) {
        return completedSpecRegisters.contains(registryKey);
    }

    public boolean isInCreativeModeTab(Specification<?, ?> spec, ResourceKey<CreativeModeTab> tabKey) {
        return creativeModeTabLookup.get(spec) == tabKey;
    }

    public <P extends IProvideData> Optional<P> getDataProvider(TypewriterProvider<P> type) {
        TypewriterDataProvider provider = this.provider;
        if (provider != null) {
            return provider.getSubProvider(type);
        }
        throw new IllegalStateException("Cannot get data provider before datagen is started");
    }

    public <P extends IProvideData, R> SELF setDataGenerator(Builder<R, ?, ?, ?> builder, TypewriterProvider<? extends P> type, Consumer<? extends P> cons) {
        return this.setDataGenerator(builder.getName(), builder.getRegistryKey(), type, cons);
    }

    public <P extends IProvideData, R> SELF setDataGenerator(String entry, ResourceKey<? extends Registry<R>> registryType, TypewriterProvider<? extends P> type, Consumer<? extends P> cons) {
        if (!doDatagen.get()) return self();
        @SuppressWarnings("null")
        Consumer<? extends IProvideData> existing = datagensByEntry.put(Pair.of(entry, registryType), type, cons);
        if (existing != null) {
            datagens.remove(type, existing);
        }
        return addDataGenerator(type, cons);
    }

    public <T extends IProvideData> SELF addDataGenerator(TypewriterProvider<? extends T> type, Consumer<? extends T> cons) {
        if (doDatagen.get()) {
            if (provider != null) throw new IllegalStateException("Cannot add data generator after construction of root generator");
            datagens.put(type, cons);
        }
        return self();
    }


    private final LazySupplier<List<Pair<String, String>>> extraLang = LazySupplier.of(() -> {
        final List<Pair<String, String>> ret = new ArrayList<>();
        addDataGenerator(TypewriterProvider.LANG, prov -> ret.forEach(p -> prov.add(p.getKey(), p.getValue())));
        return ret;
    });

    public MutableComponent addLang(String type, ResourceLocation id, String localizedName) {
        return addRawLang(Util.makeDescriptionId(type, id), localizedName);
    }

    public MutableComponent addLang(String type, ResourceLocation id, String suffix, String localizedName) {
        return addRawLang(Util.makeDescriptionId(type, id) + "." + suffix, localizedName);
    }

    public MutableComponent addRawLang(String key, String value) {
        if (doDatagen.get()) {
            extraLang.get().add(Pair.of(key, value));
        }
        return Component.translatable(key);
    }

    @SuppressWarnings("null")
    private Optional<Pair<String, ResourceKey<? extends Registry<?>>>> getEntryForGenerator(TypewriterProvider<?> type, Consumer<? extends IProvideData> generator) {
        for (Map.Entry<Pair<String, ResourceKey<? extends Registry<?>>>, Consumer<? extends IProvideData>> e : datagensByEntry.column(type).entrySet()) {
            if (e.getValue() == generator) {
                return Optional.of(e.getKey());
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public <T extends IProvideData> void genData(TypewriterProvider<? extends T> type, T gen) {
        if (!doDatagen.get()) return;

        datagens.get(type).forEach(cons -> {
            Optional<Pair<String, ResourceKey<? extends Registry<?>>>> entry = getEntryForGenerator(type, cons);
            if (entry.isPresent())
                log.debug("Generating data of type {} for entry {} [{}]", TypewriterDataProvider.getTypeName(type), entry.get().getLeft(), entry.get().getRight().location());
            else
                log.debug("Generating unassociated data of type {} ({})", TypewriterDataProvider.getTypeName(type), type);

            try {
                ((Consumer<T>) cons).accept(gen);
            } catch (Exception e) {
                Message err = entry.isPresent()
                        ? log.getMessageFactory().newMessage("Unexpected error while running data generator of type {} for entry {} [{}]", TypewriterDataProvider.getTypeName(type), entry.get().getLeft(), entry.get().getRight().location())
                        : log.getMessageFactory().newMessage("Unexpected error while running unassociated data generator of type {} ({})", TypewriterDataProvider.getTypeName(type), type);

                throw new RuntimeException(err.getFormattedMessage(), e);
            }
        });
    }

    public SELF spec(String name) {
        this.currentName = name;
        return self();
    }

    public SELF defaultCreativeTab(ResourceKey<CreativeModeTab> creativeModeTab) {
        defaultCreativeTab = creativeModeTab;
        return self();
    }

    public SELF modifyCreativeModeTab(ResourceKey<CreativeModeTab> creativeModeTab, Consumer<BuildCreativeModeTabContentsEvent> modifier) {
        creativeModeTabModifiers.put(creativeModeTab, modifier);
        return self();
    }

    public <R, T extends R, P extends IParent<R>, S2 extends Builder<R, T, P, S2>> S2 entry(BiFunction<String, Callback, S2> factory) {
        return entry(getCurrentName(), callback -> factory.apply(getCurrentName(), callback));
    }

    public <R, T extends R, P extends IParent<R>, S2 extends Builder<R, T, P, S2>> S2 entry(String name, Function<Callback, S2> factory) {
        return factory.apply(this::accept);
    }

    protected <R, T extends R> Specification<R, T> accept(String name, ResourceKey<? extends Registry<R>> type, Builder<R, T, ?, ?> builder, Supplier<? extends T> creator, Function<DeferredHolder<R, T>, ? extends Specification<R, T>> entryFactory) {
        SpecRegister<R, T> spec = new SpecRegister<>(ResourceLocation.fromNamespaceAndPath(modId, name), type, creator, entryFactory);
        log.trace("Captured registration for entry {}:{} of type {}", modId, name, type.location());
        onRegister.removeAll(Pair.of(name, type)).forEach(callback -> {
            @SuppressWarnings("unchecked")
            @Nonnull Consumer<? super T> unsafeCallback = (Consumer<? super T>) callback;
            spec.addRegisterCallback(unsafeCallback);
        });
        specRegisters.put(type, name, spec);
        return spec.get();
    }

    public <R> ResourceKey<Registry<R>> makeRegistry(String name, Function<ResourceKey<Registry<R>>, RegistryBuilder<R>> builder) {
        final ResourceKey<Registry<R>> registryId = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(modId, name));
        EventReceiver.addModBusListener(this, NewRegistryEvent.class, e -> e.register(builder.apply(registryId).create()));
        return registryId;
    }

    public <R> ResourceKey<Registry<R>> makeDatapackRegistry(String name, Codec<R> codec) {
        return makeDatapackRegistry(name, codec, null);
    }


    public <R> ResourceKey<Registry<R>> makeDatapackRegistry(String name, Codec<R> codec, @Nullable Codec<R> networkCodec) {
        final ResourceKey<Registry<R>> registryId = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(modId, name));
        EventReceiver.addModBusListener(this, DataPackRegistryEvent.NewRegistry.class, event -> event.dataPackRegistry(registryId, codec, networkCodec));
        return registryId;
    }

}

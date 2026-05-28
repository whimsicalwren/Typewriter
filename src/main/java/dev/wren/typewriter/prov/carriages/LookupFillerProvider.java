package dev.wren.typewriter.prov.carriages;

import dev.wren.typewriter.internal.waytoomanyinterfaces.IProvideData;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;


public interface LookupFillerProvider extends IProvideData {

    CompletableFuture<HolderLookup.Provider> getFilledProvider();

}
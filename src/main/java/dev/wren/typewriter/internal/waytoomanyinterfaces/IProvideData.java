package dev.wren.typewriter.internal.waytoomanyinterfaces;

import net.minecraft.data.DataProvider;
import net.neoforged.fml.LogicalSide;

public interface IProvideData extends DataProvider {
    LogicalSide getSide();
}

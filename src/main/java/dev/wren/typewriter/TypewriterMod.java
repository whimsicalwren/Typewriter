package dev.wren.typewriter;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TypewriterMod.MODID)
public class TypewriterMod {

    public static final String MODID = "typewriter";
    public static final String NAME = "Typewriter";

    public static final Logger LOGGER = LogManager.getLogger("typewriter");

    public TypewriterMod(IEventBus modEventBus, ModContainer container) {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();

        LOGGER.info("{} ({}) initialized!", NAME, MODID);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static String asResourceStr(String path) {
        return asResource(path).toString();
    }

}

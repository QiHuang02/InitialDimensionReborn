package cn.qihuang02.initial_dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.ConfigValue<String> INITIAL_DIMENSION;

    static {
        BUILDER.push("Spawn Settings");
        INITIAL_DIMENSION = BUILDER
                .comment("The initial dimension that players will spawn in")
                .define("InitialDimension", "minecraft:overworld");
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static @NotNull ResourceKey<Level> getDimension() {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(INITIAL_DIMENSION.get()));
    }
}

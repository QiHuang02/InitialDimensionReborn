package cn.qihuang02.initial_dimension;

import cn.qihuang02.initial_dimension.data.WorldDimensionData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.ConfigValue<String> INITIAL_DIMENSION;

    static {
        BUILDER.push("Spawn Settings");
        INITIAL_DIMENSION = BUILDER
                .comment("The default initial dimension that players will spawn in (can be overridden per-world)")
                .define("InitialDimension", "minecraft:overworld");
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    /**
     * 获取维度配置，优先使用世界特定设置，否则使用全局配置
     */
    public static @NotNull ResourceKey<Level> getDimension(@Nullable MinecraftServer server) {
        String dimensionKey;
        
        if (server != null) {
            // 尝试从世界数据中获取设置
            WorldDimensionData worldData = WorldDimensionData.getOrCreate(server);
            dimensionKey = worldData.getInitialDimension();
            InitialDimension.LOGGER.debug("使用世界特定维度设置: {}", dimensionKey);
        } else {
            // 回退到全局配置
            dimensionKey = INITIAL_DIMENSION.get();
            InitialDimension.LOGGER.debug("使用全局维度设置: {}", dimensionKey);
        }
        
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(dimensionKey));
    }

    /**
     * 获取维度配置（向后兼容方法）
     */
    public static @NotNull ResourceKey<Level> getDimension() {
        return getDimension(null);
    }
}

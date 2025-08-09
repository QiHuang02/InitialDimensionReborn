package cn.qihuang02.initial_dimension.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.registries.Registries;
import org.jetbrains.annotations.NotNull;

/**
 * 保存世界特定的初始维度设置
 */
public class WorldDimensionData extends SavedData {
    private static final String DATA_NAME = "initial_dimension_world_data";
    private String initialDimension = "minecraft:overworld";
    
    public WorldDimensionData() {
        super();
    }
    
    public WorldDimensionData(String initialDimension) {
        this();
        this.initialDimension = initialDimension;
        setDirty();
    }
    
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        tag.putString("initial_dimension", initialDimension);
        return tag;
    }
    
    public static WorldDimensionData load(CompoundTag tag, HolderLookup.Provider provider) {
        WorldDimensionData data = new WorldDimensionData();
        data.initialDimension = tag.getString("initial_dimension");
        return data;
    }
    
    public void setInitialDimension(String dimension) {
        this.initialDimension = dimension;
        setDirty();
    }
    
    public String getInitialDimension() {
        return initialDimension;
    }
    
    public ResourceKey<Level> getDimensionKey() {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(initialDimension));
    }
    
    public static WorldDimensionData getOrCreate(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            factory(),
            DATA_NAME
        );
    }
    
    private static Factory<WorldDimensionData> factory() {
        return new Factory<>(
            WorldDimensionData::new,
            WorldDimensionData::load,
            null
        );
    }
}
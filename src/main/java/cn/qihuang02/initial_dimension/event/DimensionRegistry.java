package cn.qihuang02.initial_dimension.event;

import cn.qihuang02.initial_dimension.InitialDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@EventBusSubscriber(modid = InitialDimension.MODID, value = Dist.CLIENT)
public class DimensionRegistry {
    
    private static final List<DimensionInfo> availableDimensions = new CopyOnWriteArrayList<>();
    
    private static final List<DimensionInfo> DEFAULT_DIMENSIONS = List.of(
        new DimensionInfo("minecraft:overworld", "主世界", "Overworld"),
        new DimensionInfo("minecraft:the_nether", "下界", "Nether"),
        new DimensionInfo("minecraft:the_end", "末地", "End")
    );
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        InitialDimension.LOGGER.info("Initializing dimension registry scanner");
        
        event.enqueueWork(() -> {
            scanAvailableDimensions();
        });
    }
    
    @SubscribeEvent
    public static void onPlayerJoinWorld(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft.getInstance().execute(() -> {
            scanAvailableDimensions();
        });
    }
    
    /**
     * Scan all available dimensions
     */
    public static void scanAvailableDimensions() {
        List<DimensionInfo> discoveredDimensions = new ArrayList<>();
        
        try {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.level != null && minecraft.level.registryAccess() != null) {
                scanFromRegistryAccess(minecraft.level.registryAccess(), discoveredDimensions);
            } else {
                scanFromBuiltinRegistry(discoveredDimensions);
            }
            
            if (discoveredDimensions.isEmpty()) {
                InitialDimension.LOGGER.warn("No dimensions found, using default dimension list");
                discoveredDimensions.addAll(DEFAULT_DIMENSIONS);
            }
            
            availableDimensions.clear();
            availableDimensions.addAll(discoveredDimensions);
            
            InitialDimension.LOGGER.info("Discovered {} available dimensions:", availableDimensions.size());
            for (DimensionInfo dim : availableDimensions) {
                InitialDimension.LOGGER.info("  - {} ({})", dim.getDisplayName(), dim.getKey());
            }
            
        } catch (Exception e) {
            InitialDimension.LOGGER.error("Error scanning dimensions, using default dimension list", e);
            availableDimensions.clear();
            availableDimensions.addAll(DEFAULT_DIMENSIONS);
        }
    }
    
    /**
     * Scan dimensions from registry access
     */
    private static void scanFromRegistryAccess(net.minecraft.core.RegistryAccess registryAccess, List<DimensionInfo> dimensions) {
        try {
            Registry<net.minecraft.world.level.dimension.LevelStem> dimensionRegistry = 
                registryAccess.registryOrThrow(Registries.LEVEL_STEM);
            
            for (var entry : dimensionRegistry.entrySet()) {
                ResourceKey<net.minecraft.world.level.dimension.LevelStem> key = entry.getKey();
                String dimensionKey = key.location().toString();
                
                // Convert to Level ResourceKey
                ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, key.location());
                
                String displayName = createDisplayName(key.location());
                String englishName = createEnglishName(key.location());
                
                dimensions.add(new DimensionInfo(dimensionKey, displayName, englishName));
            }
            
            InitialDimension.LOGGER.info("Scanned {} dimensions from registry access", dimensions.size());
            
        } catch (Exception e) {
            InitialDimension.LOGGER.warn("Failed to scan dimensions from registry access: {}", e.getMessage());
        }
    }
    
    /**
     * Scan dimensions from builtin registry (fallback method)
     */
    private static void scanFromBuiltinRegistry(List<DimensionInfo> dimensions) {
        try {
            // Use known common dimensions
            String[] commonDimensions = {
                "minecraft:overworld",
                "minecraft:the_nether", 
                "minecraft:the_end"
            };
            
            for (String dimKey : commonDimensions) {
                ResourceLocation location = ResourceLocation.tryParse(dimKey);
                if (location != null) {
                    String displayName = createDisplayName(location);
                    String englishName = createEnglishName(location);
                    dimensions.add(new DimensionInfo(dimKey, displayName, englishName));
                }
            }
            
            InitialDimension.LOGGER.info("Scanned {} dimensions from builtin list", dimensions.size());
            
        } catch (Exception e) {
            InitialDimension.LOGGER.warn("Failed to scan dimensions from builtin registry: {}", e.getMessage());
        }
    }
    
    /**
     * Create display name for dimension
     */
    private static String createDisplayName(ResourceLocation location) {
        String namespace = location.getNamespace();
        String path = location.getPath();
        String fullKey = location.toString();
        
        // Try to get dimension name from localization files
        String translationKey = "dimension." + namespace + "." + path;
        Component translatedName = Component.translatable(translationKey);
        
        // Check if we have a valid translation (different from the translation key itself)
        String translatedString = translatedName.getString();
        if (!translatedString.equals(translationKey)) {
            return translatedString;
        }
        
        // Handle vanilla dimension fallback
        if ("minecraft".equals(namespace)) {
            switch (path) {
                case "overworld" -> { return Component.translatable("dimension.minecraft.overworld").getString(); }
                case "the_nether" -> { return Component.translatable("dimension.minecraft.the_nether").getString(); }
                case "the_end" -> { return Component.translatable("dimension.minecraft.the_end").getString(); }
            }
        }
        
        // Handle modded dimensions  
        String modName = getLocalizedModName(namespace);
        String dimensionName = formatDimensionName(path);
        
        if (modName.equals(formatDimensionName(namespace))) {
            return dimensionName;
        } else {
            return String.format("%s (%s)", dimensionName, modName);
        }
    }
    
    /**
     * Create English name for dimension
     */
    private static String createEnglishName(ResourceLocation location) {
        String namespace = location.getNamespace();
        String path = location.getPath();
        
        // Handle vanilla dimensions
        if ("minecraft".equals(namespace)) {
            switch (path) {
                case "overworld" -> { return "Overworld"; }
                case "the_nether" -> { return "Nether"; }
                case "the_end" -> { return "End"; }
            }
        }
        
        // Handle modded dimensions
        String modName = getLocalizedModName(namespace);
        String dimensionName = formatDimensionName(path);
        
        if (modName.equals(formatDimensionName(namespace))) {
            return dimensionName;
        } else {
            return String.format("%s (%s)", dimensionName, modName);
        }
    }
    
    /**
     * Get localized mod name
     */
    private static String getLocalizedModName(String namespace) {
        // Try to get mod name from localization files
        String modNameKey = "modname." + namespace;
        Component translatedModName = Component.translatable(modNameKey);
        String translatedString = translatedModName.getString();
        
        // If we have a valid translation, use the translated name
        if (!translatedString.equals(modNameKey)) {
            return translatedString;
        }
        
        // Fallback to hardcoded friendly name mappings
        return switch (namespace) {
            case "minecraft" -> "Minecraft";
            case "twilightforest" -> "Twilight Forest";
            case "aether" -> "Aether";
            case "undergarden" -> "The Undergarden";
            case "betterendforge" -> "BetterEnd";
            case "betternether" -> "BetterNether";
            case "theabyss" -> "The Abyss";
            case "deeperdarker" -> "Deeper and Darker";
            default -> formatDimensionName(namespace);
        };
    }
    
    /**
     * Format dimension name
     */
    private static String formatDimensionName(String name) {
        // Remove common prefixes
        name = name.replaceFirst("^the_", "");
        
        // Replace underscores with spaces and capitalize first letter of each word
        String[] words = name.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Get all available dimensions
     */
    public static List<DimensionInfo> getAvailableDimensions() {
        if (availableDimensions.isEmpty()) {
            return new ArrayList<>(DEFAULT_DIMENSIONS);
        }
        return new ArrayList<>(availableDimensions);
    }
    
    /**
     * Force rescan dimensions
     */
    public static void forceRescan() {
        InitialDimension.LOGGER.info("Force rescanning dimension registry");
        scanAvailableDimensions();
    }
    
    /**
     * Dimension information class
     */
    public static class DimensionInfo {
        private final String key;
        private final String displayName;
        private final String englishName;
        
        public DimensionInfo(String key, String displayName, String englishName) {
            this.key = key;
            this.displayName = displayName;
            this.englishName = englishName;
        }
        
        public String getKey() {
            return key;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getEnglishName() {
            return englishName;
        }
        
        public ResourceKey<Level> toResourceKey() {
            ResourceLocation location = ResourceLocation.tryParse(key);
            return ResourceKey.create(Registries.DIMENSION, location);
        }
        
        public Component toComponent() {
            return Component.literal(displayName);
        }
        
        @Override
        public String toString() {
            return String.format("DimensionInfo{key='%s', displayName='%s'}", key, displayName);
        }
    }
}
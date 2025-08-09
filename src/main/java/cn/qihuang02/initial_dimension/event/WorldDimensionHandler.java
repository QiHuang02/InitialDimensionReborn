package cn.qihuang02.initial_dimension.event;

import cn.qihuang02.initial_dimension.InitialDimension;
import cn.qihuang02.initial_dimension.data.WorldDimensionData;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@EventBusSubscriber(modid = InitialDimension.MODID)
public class WorldDimensionHandler {
    
    private static final String TEMP_DIMENSION_FILE = "temp_initial_dimension_setting.txt";
    
    @SubscribeEvent
    public static void onServerStarting(@NotNull ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        
        // 检查是否有临时的维度设置文件（来自世界创建界面）
        loadTempDimensionSetting(server);
        
        InitialDimension.LOGGER.info("服务器启动中，检查世界维度设置...");
    }
    
    @SubscribeEvent
    public static void onServerStarted(@NotNull ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        
        // 加载或创建世界维度数据
        WorldDimensionData worldData = WorldDimensionData.getOrCreate(server);
        String currentDimension = worldData.getInitialDimension();
        
        InitialDimension.LOGGER.info("世界初始维度设置为: {}", currentDimension);
        
        // 清理临时文件
        cleanupTempFiles(server);
    }
    
    /**
     * 从临时文件加载维度设置（如果存在）
     */
    private static void loadTempDimensionSetting(MinecraftServer server) {
        try {
            // 检查游戏目录中的临时文件
            Path serverDir = server.getServerDirectory();
            Path gameDir = serverDir.getParent();
            if (gameDir == null) {
                gameDir = Path.of(".");
            }
            
            Path tempFile = gameDir.resolve(TEMP_DIMENSION_FILE);
            
            if (Files.exists(tempFile)) {
                String dimensionSetting = Files.readString(tempFile).trim();
                
                if (!dimensionSetting.isEmpty()) {
                    // 将临时设置保存到世界数据中
                    WorldDimensionData worldData = WorldDimensionData.getOrCreate(server);
                    worldData.setInitialDimension(dimensionSetting);
                    
                    InitialDimension.LOGGER.info("从临时文件加载维度设置: {}", dimensionSetting);
                }
            }
        } catch (IOException e) {
            InitialDimension.LOGGER.warn("无法读取临时维度设置文件", e);
        }
    }
    
    /**
     * 清理临时文件
     */
    private static void cleanupTempFiles(MinecraftServer server) {
        try {
            Path serverDir = server.getServerDirectory();
            Path gameDir = serverDir.getParent();
            if (gameDir == null) {
                gameDir = Path.of(".");
            }
            
            Path tempFile = gameDir.resolve(TEMP_DIMENSION_FILE);
            
            if (Files.exists(tempFile)) {
                if (Files.deleteIfExists(tempFile)) {
                    InitialDimension.LOGGER.info("已清理临时维度设置文件");
                } else {
                    InitialDimension.LOGGER.warn("无法删除临时维度设置文件");
                }
            }
        } catch (Exception e) {
            InitialDimension.LOGGER.warn("清理临时文件时出错", e);
        }
    }
    
    /**
     * 获取当前世界的维度设置
     */
    public static String getCurrentWorldDimension(MinecraftServer server) {
        if (server == null) {
            return "minecraft:overworld";
        }
        
        WorldDimensionData worldData = WorldDimensionData.getOrCreate(server);
        return worldData.getInitialDimension();
    }
    
    /**
     * 设置当前世界的维度
     */
    public static void setCurrentWorldDimension(MinecraftServer server, String dimension) {
        if (server == null) {
            return;
        }
        
        WorldDimensionData worldData = WorldDimensionData.getOrCreate(server);
        worldData.setInitialDimension(dimension);
        
        InitialDimension.LOGGER.info("已更新世界维度设置为: {}", dimension);
    }
}
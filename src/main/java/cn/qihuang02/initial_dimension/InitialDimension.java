package cn.qihuang02.initial_dimension;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(InitialDimension.MODID)
public class InitialDimension {
    public static final String MODID = "initial_dimension";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Contract(pure = true)
    public InitialDimension(@NotNull ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}

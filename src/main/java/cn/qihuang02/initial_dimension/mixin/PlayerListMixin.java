package cn.qihuang02.initial_dimension.mixin;

import cn.qihuang02.initial_dimension.Config;
import cn.qihuang02.initial_dimension.InitialDimension;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Shadow @Final private MinecraftServer server;

    @ModifyVariable(
            method = "placeNewPlayer",
            at = @At(value = "STORE"),
            ordinal = 0
    )
    private ResourceKey<Level> onPlaceNewPlayer$modifyDimension(
            ResourceKey<Level> originalKey,
            Connection connection,
            ServerPlayer player,
            @Local(name = "optional1") @NotNull Optional<CompoundTag> optional1
    ) {
        if (optional1.isEmpty()) {
            ResourceKey<Level> initialDimensionKey = Config.getDimension();
            InitialDimension.LOGGER.info("New player {} detected. Setting initial dimension to {}", player.getName().getString(), initialDimensionKey.location());

            return initialDimensionKey;
        }
        return originalKey;
    }

    @ModifyVariable(method = "respawn", at = @At(value = "STORE"), ordinal = 0)
    private ServerLevel onRespawn$modifyDimension(
            ServerLevel originalLevel
    ) {
        ResourceKey<Level> dimensionKey = Config.getDimension();
        ServerLevel targetLevel = this.server.getLevel(dimensionKey);

        if (targetLevel == null) {
            InitialDimension.LOGGER.warn("Dimension {} does not exist. Using overworld instead.", dimensionKey.location());
            return originalLevel;
        }

        return targetLevel;
    }
}

package cn.qihuang02.initial_dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@EventBusSubscriber
public class SafeTeleportHandler {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.@NotNull PlayerLoggedInEvent event) {
        Player entity = event.getEntity();
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.server.execute(() -> {
                teleportToSafeLocation(serverPlayer.level(), serverPlayer, 100);
            });
        }
    }

    private static void teleportToSafeLocation(Level level, Player player, int radius) {
        if (!isLocationSafe(player).isEmpty()) {
            BlockPos safePos = findSafeLocation(level, player, radius);
            if (safePos != null) {
                player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                InitialDimension.LOGGER.info("Player {} was teleported to a safe location at {}", player.getName().getString(), safePos);
            } else {
                InitialDimension.LOGGER.warn("Could not find a safe location for {}. Trying random teleport.", player.getName().getString());
                randomTeleportTo(level, player);
                safePos = findSafeLocation(level, player, radius);
                if (safePos != null) {
                    player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                    InitialDimension.LOGGER.info("Player {} was randomly teleported to a new safe location at {}", player.getName().getString(), safePos);
                } else {
                    InitialDimension.LOGGER.error("Still could not find a safe location for {}. Creating a platform.", player.getName().getString());
                    createPlatformAtPlayer(level, player);
                }
            }
        }
    }

    private static void createPlatformAtPlayer(Level level, @NotNull Player player) {
        BlockPos pos = player.blockPosition();
        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                BlockPos platformPos = pos.offset(x, -1, z);
                level.setBlockAndUpdate(platformPos.above(1), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(platformPos.above(2), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(platformPos, Blocks.STONE.defaultBlockState());
            }
        }
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    private static void randomTeleportTo(@NotNull Level level, @NotNull Player entity) {
        Random random = new Random();
        int x = entity.getBlockX() + random.nextInt(-250, 251);
        int z = entity.getBlockZ() + random.nextInt(-250, 251);
        int y = random.nextInt(level.getMinBuildHeight(), level.getMaxBuildHeight());
        entity.teleportTo(x + 0.5, y, z + 0.5);
    }

    @SuppressWarnings("resource")
    private static @NotNull List<Direction> isLocationSafe(@NotNull Player player) {
        BlockPos pos = player.blockPosition();
        Level level = player.level();

        List<Direction> directions = new ArrayList<>();

        if (!level.noCollision(player)) {
            directions.add(Direction.NORTH);
        }


        if (level.getBlockState(pos.below()).isAir()) {
            directions.add(Direction.DOWN);
        }

        if (!player.isInLava() || !player.isInWater()) {
            directions.add(Direction.UP);
        }

        return directions;
    }

    private static @Nullable BlockPos findSafeLocation(Level level, @NotNull Player player, int radius) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.offer(player.blockPosition());
        visited.add(player.blockPosition());

        int processedNodes = 0;
        int maxProcessedNodes = radius * radius * radius;

        while (!queue.isEmpty() && processedNodes < maxProcessedNodes) {
            BlockPos current = queue.poll();
            processedNodes++;

            if (isAreaSafe(level, current)) {
                return current.above();
            }

            if (player.blockPosition().distManhattan(current) > radius) {
                continue;
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = current.relative(direction);
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
            BlockPos up = current.above();
            if (!visited.contains(up)) {
                visited.add(up);
                queue.offer(up);
            }
            BlockPos down = current.below();
            if (!visited.contains(down)) {
                visited.add(down);
                queue.offer(down);
            }
        }

        return null;
    }

    private static boolean isAreaSafe(@NotNull Level level, @NotNull BlockPos pos) {
        if (level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP) && !level.getBlockState(pos).is(Blocks.LAVA) && !level.getBlockState(pos).is(Blocks.MAGMA_BLOCK)) {
            for (int i = 1; i <= 2; ++i) {
                if (!level.getBlockState(pos.above(i)).isAir() || !level.getFluidState(pos.above(i)).isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}

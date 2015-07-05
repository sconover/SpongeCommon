/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.WorldProvider;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.slf4j.impl.SLF4JLogger;
import org.spongepowered.api.Platform;
import org.spongepowered.api.service.ProviderExistsException;
import org.spongepowered.api.service.command.CommandService;
import org.spongepowered.api.service.command.SimpleCommandService;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.persistence.SerializationService;
import org.spongepowered.api.service.rcon.RconService;
import org.spongepowered.api.service.scheduler.SchedulerService;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.common.command.CommandSponge;
import org.spongepowered.common.command.SpongeCommandDisambiguator;
import org.spongepowered.common.configuration.SpongeConfig;
import org.spongepowered.common.registry.SpongeGameRegistry;
import org.spongepowered.common.service.pagination.SpongePaginationService;
import org.spongepowered.common.service.persistence.SpongeSerializationService;
import org.spongepowered.common.service.rcon.MinecraftRconService;
import org.spongepowered.common.service.scheduler.SpongeScheduler;
import org.spongepowered.common.service.sql.SqlServiceImpl;
import org.spongepowered.common.util.SpongeHooks;
import org.spongepowered.common.world.DimensionManager;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;

/**
 * Used to setup the ecosystem
 */
@NonnullByDefault
public final class SpongeBootstrap {
    private static final org.slf4j.Logger slf4jLogger = new SLF4JLogger((AbstractLogger) Sponge.getLogger(), Sponge.getLogger().getName());

    public static void initializeServices() {
        try {
            SimpleCommandService commandService = new SimpleCommandService(Sponge.getGame(), slf4jLogger, new SpongeCommandDisambiguator(Sponge.getGame()));
            Sponge.getGame().getServiceManager().setProvider(Sponge.getPlugin(), CommandService.class, commandService);
            commandService.register(Sponge.getPlugin(), CommandSponge.getCommand(), "sponge", "sp");
        } catch (ProviderExistsException e) {
            Sponge.getLogger().warn("Non-Sponge CommandService already registered: " + e.getLocalizedMessage());
        }

        try {
            Sponge.getGame().getServiceManager().setProvider(Sponge.getPlugin(), SqlService.class, new SqlServiceImpl());
        } catch (ProviderExistsException e) {
            Sponge.getLogger().warn("Non-Sponge SqlService already registered: " + e.getLocalizedMessage());
        }

        try {
            Sponge.getGame().getServiceManager().setProvider(Sponge.getPlugin(), SchedulerService.class, SpongeScheduler.getInstance());
        } catch (ProviderExistsException e) {
            Sponge.getLogger().error("Non-Sponge scheduler has been registered. Cannot continue!");
            throw new ExceptionInInitializerError(e);
        }

        try {
            SerializationService serializationService = new SpongeSerializationService();
            Sponge.getGame().getServiceManager().setProvider(Sponge.getPlugin(), SerializationService.class, serializationService);
        } catch (ProviderExistsException e2) {
            Sponge.getLogger().warn("Non-Sponge SerializationService already registered: " + e2.getLocalizedMessage());
        }

        try {
            PaginationService paginationService = new SpongePaginationService();
            Sponge.getGame().getServiceManager().setProvider(Sponge.getPlugin(), PaginationService.class, paginationService);
        } catch (ProviderExistsException e) {
            Sponge.getLogger().warn("Non-Sponge PaginationService already registered: " + e.getLocalizedMessage());

        }

        if (Sponge.getGame().getPlatform().getType() == Platform.Type.SERVER) {
            try {
                Sponge.getGame().getServiceManager().setProvider(Sponge.getPlugin(), RconService.class, new MinecraftRconService((DedicatedServer)
                        MinecraftServer.getServer()));
            } catch (ProviderExistsException e) {
                Sponge.getLogger().warn("Non-Sponge Rcon service already registered: " + e.getLocalizedMessage());
            }
        }
    }

    public static void preInitializeRegistry() {
        ((SpongeGameRegistry) Sponge.getGame().getRegistry()).preInit();
    }

    public static void initializeRegistry() {
        ((SpongeGameRegistry) Sponge.getGame().getRegistry()).init();
    }

    public static void postInitializeRegistry() {
        ((SpongeGameRegistry) Sponge.getGame().getRegistry()).postInit();
    }

    public static void registerWorlds() {
        final File[] directoryListing = DimensionManager.getCurrentSaveRootDirectory().listFiles();
        if (directoryListing == null) {
            return;
        }

        for (File child : directoryListing) {
            File levelData = new File(child, "level_sponge.dat");
            if (!child.isDirectory() || !levelData.exists()) {
                continue;
            }

            try {
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(levelData));
                if (nbt.hasKey(Sponge.ECOSYSTEM_NAME)) {
                    NBTTagCompound spongeData = nbt.getCompoundTag(Sponge.ECOSYSTEM_NAME);
                    String dimType = spongeData.getString("dimensionType");
                    String worldFolder = spongeData.getString("LevelName");
                    SpongeConfig<?> activeConfig = SpongeHooks.getActiveConfig(dimType, worldFolder);
                    if (!activeConfig.getConfig().getWorld().isWorldEnabled()) {
                            Sponge.getLogger().info("World {} is currently disabled. Skipping world load...", child.getName());
                            continue;
                    }
                    if (!activeConfig.getConfig().getWorld().getLoadOnStartup()) {
                        Sponge.getLogger().info("World {} 'loadOnStartup' is disabled.. Skipping world load...", child.getName());
                        continue;
                    }

                    if (spongeData.hasKey("uuid_most") && spongeData.hasKey("uuid_least")) {
                        UUID uuid = new UUID(spongeData.getLong("uuid_most"), spongeData.getLong("uuid_least"));
                        Sponge.getSpongeRegistry().registerWorldUniqueId(uuid, child.getName());
                    }
                    if (spongeData.hasKey("dimensionId") && spongeData.getBoolean("enabled")) {
                        int dimension = spongeData.getInteger("dimensionId");
                        for (DimensionType type : Sponge.getSpongeRegistry().getDimensionTypes()) {
                            if (type.getId().equalsIgnoreCase(spongeData.getString("dimensionType")) || type.getClass().getCanonicalName().equalsIgnoreCase(spongeData.getString("dimensionType"))) {
                                Sponge.getSpongeRegistry().registerWorldDimensionId(dimension, child.getName());
                                if (!DimensionManager.isDimensionRegistered(dimension)) {
                                    DimensionManager.registerDimension(dimension, ((WorldProvider) type).getDimensionId());
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                Sponge.getLogger().error("Error during world registration.", t);
            }
        }
    }
}

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
package org.spongepowered.common.util;

import com.google.common.collect.Maps;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.world.WorldUnloadEvent;
import org.spongepowered.api.world.World;
import org.spongepowered.common.Sponge;

import java.util.Map;

/**
 * The simulated player idea is based on Forge's FakePlayer.
 */
public class PlayerSimulatorFactory {

    public static class PlayerSimulator {

        private final EntityPlayerMP player;

        private PlayerSimulator(EntityPlayerMP player) {
            this.player = player;
        }

        /**
         * Reset the simulator.
         *
         * @return This simulator
         */
        private PlayerSimulator reset() {
            this.player.inventory.clear();
            this.player.onGround = true;
            return this.in(null).at(0, 0, 0);
        }

        /**
         * Places the simulated player in the given world.
         *
         * @param world The world
         * @return This simulator
         */
        public PlayerSimulator in(WorldServer world) {
            this.player.setWorld(world);
            this.player.theItemInWorldManager.setWorld(world);
            return this;
        }

        /**
         * Positions the simulated player at the given location.
         *
         * @param x Position X
         * @param y Position Y
         * @param z Position Z
         * @return This simulator
         */
        public PlayerSimulator at(double x, double y, double z) {
            this.player.posX = x;
            this.player.posY = y;
            this.player.posZ = z;
            return this;
        }

        /**
         * Hold the given item stack in the simulated player's hand.
         *
         * @param itemStack The item stack
         * @return This simulator
         */
        public PlayerSimulator holdStack(ItemStack itemStack) {
            this.player.inventory.setItemStack(itemStack);
            this.player.inventory.mainInventory[this.player.inventory.currentItem] = itemStack;
            return this;
        }

        /**
         * Gets the player that's being simulated.
         *
         * @return The player
         */
        public EntityPlayerMP getPlayer() {
            return this.player;
        }
    }

    public static PlayerSimulatorFactory instance = new PlayerSimulatorFactory();

    private final Map<WorldServer, PlayerSimulator> players = Maps.newHashMap();

    public PlayerSimulatorFactory() {
        if (instance != null) {
            Sponge.getGame().getEventManager().unregister(instance);
        }
        Sponge.getGame().getEventManager().register(Sponge.getPlugin(), this);
    }

    public final PlayerSimulator getSimulator(WorldServer world) {
        PlayerSimulator instance = this.players.get(world);
        if (instance == null) {
            instance = new PlayerSimulator(this.createPlayer(world));
            this.players.put(world, instance);
        }
        return instance.reset().in(world);
    }

    protected EntityPlayerMP createPlayer(WorldServer world) {
        return new SimulatedPlayer(world);
    }

    @Subscribe
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        if (this.players.containsKey(world)) {
            this.players.remove(world).reset();
        }
    }
}

// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet.client.listener;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleGroupS2C;
import com.reiasu.reiparticlesapi.particles.control.ControlType;
import com.reiasu.reiparticlesapi.particles.control.group.ClientParticleGroupManager;
import com.reiasu.reiparticlesapi.particles.control.group.ControllableParticleGroup;
import com.reiasu.reiparticlesapi.particles.control.group.ControllableParticleGroupProvider;
import com.reiasu.reiparticlesapi.particles.control.group.SequencedParticleGroup;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClientParticleGroupPacketHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientParticleGroupPacketHandler() {
    }

    public static void receive(PacketParticleGroupS2C packet) {
        UUID uuid = packet.uuid();
        ControlType type = packet.type();
        switch (type) {
            case CREATE -> handleCreate(uuid, packet.args());
            case CHANGE -> handleChange(uuid, packet.args());
            case REMOVE -> handleRemove(uuid);
        }
    }

    @SuppressWarnings("unchecked")
    private static void handleCreate(UUID groupUUID, Map<String, ParticleControllerDataBuffer<?>> args) {
        Object posObj = readValue(args, PacketParticleGroupS2C.PacketArgsType.POS.getOfArgs());
        if (!(posObj instanceof Vec3 pos)) return;

        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) return;

        Object typeObj = readValue(args, PacketParticleGroupS2C.PacketArgsType.GROUP_TYPE.getOfArgs());
        if (!(typeObj instanceof String typeName)) return;

        try {
            Class<?> rawClass = Class.forName(typeName);
            if (!ControllableParticleGroup.class.isAssignableFrom(rawClass)) return;
            Class<? extends ControllableParticleGroup> groupClass = (Class<? extends ControllableParticleGroup>) rawClass;

            ControllableParticleGroupProvider provider = ClientParticleGroupManager.INSTANCE.getBuilder(groupClass);
            if (provider == null) return;

            ControllableParticleGroup group = provider.createGroup(groupUUID, args);
            if (group == null) return;

            group.display(pos, world);
            ClientParticleGroupManager.INSTANCE.addVisibleGroup(group);
        } catch (Throwable t) {
            LOGGER.debug("Failed to display particle group {}: {}", typeName, t.getMessage());
        }
    }

    private static void handleChange(UUID groupUUID, Map<String, ParticleControllerDataBuffer<?>> args) {
        ControllableParticleGroup targetGroup = ClientParticleGroupManager.INSTANCE.getControlGroup(groupUUID);
        if (targetGroup == null) return;

        Set<String> argKeys = args.keySet();

        if (argKeys.contains(PacketParticleGroupS2C.PacketArgsType.POS.getOfArgs())) {
            Object val = readValue(args, PacketParticleGroupS2C.PacketArgsType.POS.getOfArgs());
            if (val instanceof Vec3 pos) targetGroup.teleportTo(pos);
        }

        if (argKeys.contains(PacketParticleGroupS2C.PacketArgsType.ROTATE_TO.getOfArgs())) {
            Object val = readValue(args, PacketParticleGroupS2C.PacketArgsType.ROTATE_TO.getOfArgs());
            if (val instanceof Vec3 v) targetGroup.rotateToPoint(RelativeLocation.of(v));
        }

        if (argKeys.contains(PacketParticleGroupS2C.PacketArgsType.ROTATE_AXIS.getOfArgs())) {
            Object val = readValue(args, PacketParticleGroupS2C.PacketArgsType.ROTATE_AXIS.getOfArgs());
            if (val instanceof Number n) targetGroup.rotateAsAxis(n.doubleValue());
        }

        if (argKeys.contains(PacketParticleGroupS2C.PacketArgsType.AXIS.getOfArgs())) {
            Object val = readValue(args, PacketParticleGroupS2C.PacketArgsType.AXIS.getOfArgs());
            if (val instanceof Vec3 v) targetGroup.setAxis(RelativeLocation.of(v));
        }

        if (argKeys.contains(PacketParticleGroupS2C.PacketArgsType.CURRENT_TICK.getOfArgs())) {
            Object val = readValue(args, PacketParticleGroupS2C.PacketArgsType.CURRENT_TICK.getOfArgs());
            if (val instanceof Number n) targetGroup.setTick(n.intValue());
        }

        if (argKeys.contains(PacketParticleGroupS2C.PacketArgsType.MAX_TICK.getOfArgs())) {
            Object val = readValue(args, PacketParticleGroupS2C.PacketArgsType.MAX_TICK.getOfArgs());
            if (val instanceof Number n) targetGroup.setMaxTick(n.intValue());
        }

        if (argKeys.contains(PacketParticleGroupS2C.PacketArgsType.INVOKE.getOfArgs())) {
            Object val = readValue(args, PacketParticleGroupS2C.PacketArgsType.INVOKE.getOfArgs());
            if (val instanceof String methodName) {
                try {
                    Method method = targetGroup.getClass().getDeclaredMethod(methodName);
                    method.setAccessible(true);
                    method.invoke(targetGroup);
                } catch (Throwable t) {
                    LOGGER.debug("Failed to invoke '{}' on group {}: {}", methodName, groupUUID, t.getMessage());
                }
            }
        }

        if (argKeys.contains(PacketParticleGroupS2C.PacketArgsType.SCALE.getOfArgs())) {
            Object val = readValue(args, PacketParticleGroupS2C.PacketArgsType.SCALE.getOfArgs());
            if (val instanceof Number n) targetGroup.scale(n.doubleValue());
        }

        if (targetGroup instanceof SequencedParticleGroup seq) {
            handleSequencedGroupArgs(seq, args);
        }

        ControllableParticleGroupProvider builder = ClientParticleGroupManager.INSTANCE.getBuilder(targetGroup.getClass());
        if (builder != null) {
            builder.changeGroup(targetGroup, args);
        }
    }

    private static void handleRemove(UUID groupUUID) {
        ClientParticleGroupManager.INSTANCE.removeVisible(groupUUID);
    }

    private static void handleSequencedGroupArgs(SequencedParticleGroup group, Map<String, ParticleControllerDataBuffer<?>> args) {
        Object toggleVal = readValue(args, "toggle");
        if (toggleVal instanceof Number n) {
            group.toggle(n.intValue());
        }

        Object addVal = readValue(args, "addCount");
        if (addVal instanceof Number n) {
            group.addMultiple(n.intValue());
        }

        Object removeVal = readValue(args, "removeCount");
        if (removeVal instanceof Number n) {
            group.removeMultiple(n.intValue());
        }

        Object statusVal = readValue(args, "toggle_status");
        if (statusVal instanceof long[] statusArray) {
            group.toggleStatus(statusArray);
        }
    }

    private static Object readValue(Map<String, ParticleControllerDataBuffer<?>> args, String key) {
        ParticleControllerDataBuffer<?> buf = args.get(key);
        return buf == null ? null : buf.getLoadedValue();
    }
}


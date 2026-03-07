// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BarrageItemEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(BarrageItemEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final String TAG_ITEM = "Item";
    private static final String TAG_ROLL = "Roll";
    private static final String TAG_SCALE = "Scale";
    private static final String TAG_BLOCK = "Block";

    private float roll;
    private float scale = 1.0F;
    private boolean block;

    public BarrageItemEntity(EntityType<? extends BarrageItemEntity> type, Level level) {
        super(type, level);
    }

    public BarrageItemEntity(Level level, Vec3 pos, ItemStack item) {
        this(SkillEntityTypes.BARRAGE_ITEM.get(), level);
        setPos(pos.x, pos.y, pos.z);
        setItem(item);
    }

    public ItemStack getItem() {
        return getEntityData().get(DATA_ITEM);
    }

    public void setItem(ItemStack stack) {
        getEntityData().set(DATA_ITEM, stack == null ? ItemStack.EMPTY : stack.copy());
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public boolean isBlock() {
        return block;
    }

    public void setBlock(boolean block) {
        this.block = block;
    }

    @Override
    protected void defineSynchedData() {
        getEntityData().define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(TAG_ITEM)) {
            setItem(ItemStack.of(tag.getCompound(TAG_ITEM)));
        }
        roll = tag.getFloat(TAG_ROLL);
        scale = tag.getFloat(TAG_SCALE);
        block = tag.getBoolean(TAG_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put(TAG_ITEM, getItem().save(new CompoundTag()));
        tag.putFloat(TAG_ROLL, roll);
        tag.putFloat(TAG_SCALE, scale);
        tag.putBoolean(TAG_BLOCK, block);
    }

    @Override
    public void tick() {
        super.tick();
        move(MoverType.SELF, getDeltaMovement());
        if (tickCount > 200) {
            discard();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        try {
            Class<?> hooks = Class.forName("net.minecraftforge.network.NetworkHooks");
            Method method = hooks.getMethod("getEntitySpawningPacket", Entity.class);
            @SuppressWarnings("unchecked")
            Packet<ClientGamePacketListener> packet = (Packet<ClientGamePacketListener>) method.invoke(null, this);
            return packet;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Forge NetworkHooks is unavailable for BarrageItemEntity", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to create spawn packet for BarrageItemEntity", e.getCause());
        }
    }
}

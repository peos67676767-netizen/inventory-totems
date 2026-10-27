package com.totemanywhere;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;

import java.util.ArrayList;
import java.util.List;

public final class TotemFinder {
    private static final String TOTEM_ID = "minecraft:totem_of_undying";

    private TotemFinder() {}

    public static boolean consumeTotem(ServerPlayerEntity player, DamageSource source) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;

        PlayerInventory inv = player.getInventory();
        List<List<ItemStack>> lists = List.of(inv.main, inv.armor, inv.offHand);

        for (List<ItemStack> list : lists)
            for (ItemStack s : list)
                if (s.isOf(Items.TOTEM_OF_UNDYING)) { s.decrement(1); return true; }

        for (List<ItemStack> list : lists)
            for (ItemStack s : list) {
                if (s.isEmpty()) continue;
                NbtCompound nbt = s.getNbt();
                if (nbt != null && consume(nbt)) return true;
            }
        return false;
    }

    public static void applyTotemEffects(ServerPlayerEntity player) {
        player.setHealth(1.0F);
        player.clearStatusEffects();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
        player.getWorld().sendEntityStatus(player, (byte) 35);
        player.incrementStat(Stats.USED.getOrCreateStat(Items.TOTEM_OF_UNDYING));
        Criteria.USED_TOTEM.trigger(player, new ItemStack(Items.TOTEM_OF_UNDYING));
        player.playerScreenHandler.sendContentUpdates();
        player.currentScreenHandler.sendContentUpdates();
    }

    /** Busca recursivamente {id:"minecraft:totem_of_undying", Count:N} en shulkers, bundles, mochilas, etc. */
    private static boolean consume(NbtElement tag) {
        if (tag instanceof NbtCompound compound) {
            for (String key : new ArrayList<>(compound.getKeys())) {
                NbtElement child = compound.get(key);
                if (child != null && consume(child)) return true;
            }
        } else if (tag instanceof NbtList list) {
            for (int i = 0; i < list.size(); i++) {
                NbtElement el = list.get(i);
                if (el instanceof NbtCompound ec) {
                    if (TOTEM_ID.equals(ec.getString("id")) && ec.getInt("Count") > 0) {
                        int count = ec.getInt("Count") - 1;
                        if (count <= 0) list.remove(i);
                        else if (ec.contains("Count", NbtElement.BYTE_TYPE)) ec.putByte("Count", (byte) count);
                        else ec.putInt("Count", count);
                        return true;
                    }
                }
                if (consume(el)) return true;
            }
        }
        return false;
    }
}

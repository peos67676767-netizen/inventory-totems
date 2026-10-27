package com.totemanywhere;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

@Mod("totemanywhere")
public class TotemAnywhere {
    private static final String TOTEM_ID = "minecraft:totem_of_undying";

    public TotemAnywhere() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Vanilla ya revisó ambas manos antes de llegar aquí (checkTotemDeathProtection se llama en hurt()).
     * Si llegamos a la muerte, buscamos un tótem en el resto de sitios.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Igual que el tótem vanilla: no funciona con /kill ni el vacío
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        if (!consumeTotem(player)) return;

        event.setCanceled(true);
        player.setHealth(1.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        player.level().broadcastEntityEvent(player, (byte) 35);
        player.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
        CriteriaTriggers.USED_TOTEM.trigger(player, new ItemStack(Items.TOTEM_OF_UNDYING));

        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    // ---------------------------------------------------------------- búsqueda

    private static boolean consumeTotem(ServerPlayer player) {
        Inventory inv = player.getInventory();
        List<List<ItemStack>> lists = List.of(inv.items, inv.armor, inv.offhand);

        // 1) Tótems sueltos en inventario / armadura / offhand
        for (List<ItemStack> list : lists) {
            for (ItemStack s : list) {
                if (s.is(Items.TOTEM_OF_UNDYING)) { s.shrink(1); return true; }
            }
        }

        if (Config.SEARCH_CONTAINERS.get()) {
            // 2) Items con inventario propio (capability) o con NBT (shulker, bundle, mochilas...)
            for (List<ItemStack> list : lists) {
                for (ItemStack s : list) {
                    if (s.isEmpty()) continue;
                    if (fromHandler(s)) return true;
                    CompoundTag tag = s.getTag();
                    if (tag != null && consumeFromTag(tag)) return true;
                }
            }
        }

        // 3) Ender chest (opcional)
        if (Config.SEARCH_ENDER_CHEST.get()) {
            PlayerEnderChestContainer ec = player.getEnderChestInventory();
            for (int i = 0; i < ec.getContainerSize(); i++) {
                ItemStack s = ec.getItem(i);
                if (s.isEmpty()) continue;
                if (s.is(Items.TOTEM_OF_UNDYING)) { s.shrink(1); ec.setChanged(); return true; }
                if (Config.SEARCH_CONTAINERS.get()) {
                    if (fromHandler(s)) { ec.setChanged(); return true; }
                    CompoundTag tag = s.getTag();
                    if (tag != null && consumeFromTag(tag)) { ec.setChanged(); return true; }
                }
            }
        }
        return false;
    }

    /** Mochilas que exponen IItemHandler en el ItemStack (Sophisticated Backpacks, etc.). */
    private static boolean fromHandler(ItemStack stack) {
        IItemHandler handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
        if (handler == null) return false;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).is(Items.TOTEM_OF_UNDYING)) {
                if (!handler.extractItem(i, 1, false).isEmpty()) return true;
            }
        }
        return false;
    }

    /**
     * Recorre el NBT recursivamente buscando {id:"minecraft:totem_of_undying", Count:N}
     * dentro de cualquier lista. Cubre shulker boxes (BlockEntityTag.Items), bundles (Items)
     * y la mayoría de mochilas que guardan items en NBT, incluso anidadas (shulker dentro de mochila).
     */
    private static boolean consumeFromTag(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            for (String key : new ArrayList<>(compound.getAllKeys())) {
                Tag child = compound.get(key);
                if (child != null && consumeFromTag(child)) return true;
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                Tag el = list.get(i);
                if (el instanceof CompoundTag ec) {
                    if (TOTEM_ID.equals(ec.getString("id")) && ec.getInt("Count") > 0) {
                        int count = ec.getInt("Count") - 1;
                        if (count <= 0) list.remove(i);
                        else if (ec.contains("Count", Tag.TAG_BYTE)) ec.putByte("Count", (byte) count);
                        else ec.putInt("Count", count);
                        return true;
                    }
                }
                if (consumeFromTag(el)) return true;
            }
        }
        return false;
    }
}

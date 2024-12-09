package com.person98.prismPack.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Utility class for serializing and deserializing Bukkit ItemStacks to/from Base64 strings.
 * This allows for easy storage and retrieval of inventory contents in a string format.
 */
public class ItemSerializationUtil {
    /**
     * Converts an array of ItemStacks into a Base64 encoded string.
     * 
     * @param items The array of ItemStacks to serialize
     * @return A Base64 encoded string representing the items, or null if serialization fails
     */
    public static String serializeInventory(ItemStack[] items) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitOutputStream = new BukkitObjectOutputStream(byteArrayOutputStream);

            bukkitOutputStream.writeInt(items.length);
            for (ItemStack item : items) {
                bukkitOutputStream.writeObject(item);
            }
            bukkitOutputStream.close();

            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            PLogger.severe("Failed to serialize inventory: " + e);
            return null;
        }
    }

    /**
     * Converts a Base64 encoded string back into an array of ItemStacks.
     * 
     * @param inventoryString The Base64 encoded string to deserialize
     * @return An array of ItemStacks, or an empty array if deserialization fails
     */
    public static ItemStack[] deserializeInventory(String inventoryString) {
        try {
            byte[] data = Base64.getDecoder().decode(inventoryString);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(byteArrayInputStream);

            int length = bukkitInputStream.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) bukkitInputStream.readObject();
            }
            bukkitInputStream.close();

            return items;
        } catch (Exception e) {
            PLogger.severe("Failed to deserialize inventory: " + e);
            return new ItemStack[0];
        }
    }
}

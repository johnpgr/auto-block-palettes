package com.autoblockpalette.data;

import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a block palette containing a named collection of blocks with
 * weights.
 *
 * @param id        Unique identifier for this palette
 * @param name      The human-readable name of the palette (not required to be
 *                  unique)
 * @param iconBlock The block ID used to visually represent this palette in
 *                  GUI/HUD
 * @param blocks    Map of block IDs to their selection weights
 */
public record Palette(@NotNull UUID id, @NotNull String name, @NotNull String iconBlock,
                      @NotNull Map<String, Integer> blocks) {

    /**
     * Creates a new Palette with a generated UUID and validation.
     *
     * @param name      The palette name (required)
     * @param iconBlock The icon block ID (required)
     * @param blocks    The blocks map (must contain at least 1 entry)
     * @return A new validated Palette instance with a generated UUID
     * @throws IllegalArgumentException if validation fails
     */
    public static Palette create(@NotNull String name, @NotNull String iconBlock, @NotNull Map<String, Integer> blocks) {
        return create(UUID.randomUUID(), name, iconBlock, blocks);
    }

    /**
     * Creates a new Palette with a specific UUID and validation.
     *
     * @param id        The UUID for this palette
     * @param name      The palette name (required)
     * @param iconBlock The icon block ID (required)
     * @param blocks    The blocks map (must contain at least 1 entry)
     * @return A new validated Palette instance
     * @throws IllegalArgumentException if validation fails
     */
    public static Palette create(@NotNull UUID id, @NotNull String name, @NotNull String iconBlock, @NotNull Map<String, Integer> blocks) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Palette name cannot be blank");
        }
        if (iconBlock.isBlank()) {
            throw new IllegalArgumentException("Icon block cannot be blank");
        }
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("Palette must contain at least one block");
        }
        return new Palette(id, name, iconBlock, Map.copyOf(blocks));
    }

    /**
     * Gets the weight for a specific block.
     *
     * @param blockId The block ID to look up
     * @return The weight, or 0 if the block is not in this palette
     */
    public int getWeight(String blockId) {
        return blocks.getOrDefault(blockId, 0);
    }

    /**
     * Checks if a block is in this palette and enabled (weight > 0).
     *
     * @param blockId The block ID to check
     * @return true if the block is enabled in this palette
     */
    public boolean hasBlock(String blockId) {
        return blocks.getOrDefault(blockId, 0) > 0;
    }

    /**
     * Gets the total weight of all enabled blocks.
     *
     * @return The sum of all positive weights
     */
    public int getTotalWeight() {
        return blocks.values().stream().filter(w -> w > 0).mapToInt(Integer::intValue).sum();
    }

    /**
     * Gets the count of enabled blocks (weight > 0).
     *
     * @return The number of enabled blocks
     */
    public int getEnabledBlockCount() {
        return (int) blocks.values().stream().filter(w -> w > 0).count();
    }
}

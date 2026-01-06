package com.autoblockpalette.data;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a single block entry with its selection weight.
 *
 * @param blockId The block's resource location as a string (e.g., "minecraft:stone_bricks")
 * @param weight  The relative weight for selection (0 = disabled, positive = relative frequency)
 */
public record BlockEntry(@NotNull String blockId, int weight) {

    /**
     * Compact constructor that validates the weight is non-negative.
     */
    public BlockEntry {
        if (blockId == null || blockId.isBlank()) {
            throw new IllegalArgumentException("Block ID cannot be null or blank");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("Weight cannot be negative: " + weight);
        }
    }

    /**
     * Checks if this block is enabled (weight > 0).
     *
     * @return true if this block participates in selection
     */
    public boolean isEnabled() {
        return weight > 0;
    }
}

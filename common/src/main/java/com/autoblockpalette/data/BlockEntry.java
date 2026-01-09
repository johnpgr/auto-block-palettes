package com.autoblockpalette.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * Represents a single block entry with its selection weight.
 *
 * @param blockId The block's resource location as a string (e.g., "minecraft:stone_bricks")
 * @param weight  The relative weight for selection (0 = disabled, positive = relative frequency)
 */
public record BlockEntry(
    @NotNull String blockId,
    @Range(from = 0, to = Integer.MAX_VALUE) int weight
) {
    /**
     * Factory method to create a BlockEntry with validation.
     *
     * @param blockId non-blank block identifier
     * @param weight  non-negative weight value
     * @return a new BlockEntry
     * @throws IllegalArgumentException if validation fails
     */
    public static BlockEntry of(@NotNull String blockId, @Range(from = 0, to = Integer.MAX_VALUE) int weight) {
        if (blockId.isBlank()) {
            throw new IllegalArgumentException("Block ID cannot be null or blank");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("Weight cannot be negative: " + weight);
        }
        return new BlockEntry(blockId, weight);
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

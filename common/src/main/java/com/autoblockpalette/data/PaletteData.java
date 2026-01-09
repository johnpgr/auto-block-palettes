package com.autoblockpalette.data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Top-level data structure for palette storage.
 * Represents the complete JSON file structure for persistence.
 * <p>
 * Internally uses a {@link SequencedMap} for O(1) lookups while preserving
 * insertion order for iteration.
 */
public final class PaletteData {

    private final SequencedMap<UUID, Palette> palettes;
    private final @Nullable UUID activePaletteId;

    /**
     * Creates a new PaletteData from a list of palettes.
     *
     * @param palettes        List of all palettes (order is preserved)
     * @param activePaletteId UUID of the currently active palette (null if none)
     */
    public PaletteData(@NotNull List<Palette> palettes, @Nullable UUID activePaletteId) {
        this.palettes = new LinkedHashMap<>();
        for (Palette palette : palettes) {
            this.palettes.put(palette.id(), palette);
        }
        this.activePaletteId = activePaletteId;
    }

    /**
     * Creates a new PaletteData from a sequenced map of palettes.
     *
     * @param palettes        Sequenced map of palettes (order is preserved)
     * @param activePaletteId UUID of the currently active palette (null if none)
     */
    public PaletteData(@NotNull SequencedMap<UUID, Palette> palettes, @Nullable UUID activePaletteId) {
        this.palettes = new LinkedHashMap<>(palettes);
        this.activePaletteId = activePaletteId;
    }

    /**
     * Creates an empty PaletteData with no palettes and no active palette.
     *
     * @return A new empty PaletteData instance
     */
    public static PaletteData empty() {
        return new PaletteData(List.of(), null);
    }

    /**
     * Returns an unmodifiable view of all palettes in order.
     *
     * @return Collection of all palettes in insertion order
     */
    public @NotNull Collection<Palette> palettes() {
        return List.copyOf(palettes.values());
    }

    /**
     * Returns the active palette ID.
     *
     * @return The active palette UUID, or null if none
     */
    public @Nullable UUID activePaletteId() {
        return activePaletteId;
    }

    /**
     * Returns an unmodifiable sequenced map of palettes for efficient operations.
     *
     * @return Sequenced map of UUID to Palette
     */
    public @NotNull SequencedMap<UUID, Palette> palettesMap() {
        return new LinkedHashMap<>(palettes);
    }

    /**
     * Finds a palette by its UUID. O(1) operation.
     *
     * @param id The palette UUID to find
     * @return Optional containing the palette if found
     */
    public Optional<Palette> findById(@Nullable UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(palettes.get(id));
    }

    /**
     * Finds a palette by name. Returns the first match if multiple palettes
     * share the same name.
     *
     * @param name The palette name to find
     * @return Optional containing the palette if found
     */
    public Optional<Palette> findByName(@Nullable String name) {
        if (name == null) {
            return Optional.empty();
        }
        return palettes.values().stream()
                .filter(p -> name.equals(p.name()))
                .findFirst();
    }

    /**
     * Gets the currently active palette, if any.
     *
     * @return Optional containing the active palette if one is set and exists
     */
    public Optional<Palette> getActivePalette() {
        return findById(activePaletteId);
    }

    /**
     * Checks if a palette with the given UUID exists. O(1) operation.
     *
     * @param id The palette UUID to check
     * @return true if a palette with this UUID exists
     */
    public boolean hasPalette(@Nullable UUID id) {
        if (id == null) {
            return false;
        }
        return palettes.containsKey(id);
    }

    /**
     * Checks if any palette is currently active.
     *
     * @return true if there is an active palette
     */
    public boolean hasActivePalette() {
        return activePaletteId != null && hasPalette(activePaletteId);
    }

    /**
     * Returns the number of palettes.
     *
     * @return The palette count
     */
    public int size() {
        return palettes.size();
    }
}

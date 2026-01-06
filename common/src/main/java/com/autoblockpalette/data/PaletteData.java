package com.autoblockpalette.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Top-level data structure for palette storage.
 * Represents the complete JSON file structure for persistence.
 *
 * @param palettes        List of all palettes
 * @param activePaletteId UUID of the currently active palette (null if none)
 */
public record PaletteData(
        @NotNull List<Palette> palettes,
        @Nullable UUID activePaletteId) {

    /**
     * Creates an empty PaletteData with no palettes and no active palette.
     *
     * @return A new empty PaletteData instance
     */
    public static PaletteData empty() {
        return new PaletteData(List.of(), null);
    }

    /**
     * Finds a palette by its UUID.
     *
     * @param id The palette UUID to find
     * @return Optional containing the palette if found
     */
    public Optional<Palette> findById(@Nullable UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return palettes.stream()
                .filter(p -> id.equals(p.id()))
                .findFirst();
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
        return palettes.stream()
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
     * Checks if a palette with the given UUID exists.
     *
     * @param id The palette UUID to check
     * @return true if a palette with this UUID exists
     */
    public boolean hasPalette(@Nullable UUID id) {
        return findById(id).isPresent();
    }

    /**
     * Checks if any palette is currently active.
     *
     * @return true if there is an active palette
     */
    public boolean hasActivePalette() {
        return activePaletteId != null && hasPalette(activePaletteId);
    }
}

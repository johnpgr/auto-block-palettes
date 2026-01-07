package com.autoblockpalette.platform.services;

import com.autoblockpalette.data.Palette;
import com.autoblockpalette.data.PaletteData;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing palette CRUD operations, active palette state,
 * and change notifications.
 * 
 * <p>
 * This service provides centralized palette management for the entire
 * application,
 * following the MultiLoader ServiceLoader architecture pattern. All operations
 * return new immutable {@link PaletteData} instances, maintaining
 * functional-style
 * immutability throughout.
 * 
 * <p>
 * Access this service via {@code Services.PALETTE_MANAGER}.
 * 
 * @see PaletteChangeListener
 * @see PaletteData
 * @see Palette
 */
public interface IPaletteManager {

    // ==================== State Access ====================

    /**
     * Gets the current palette data state.
     *
     * @return The current immutable PaletteData instance
     */
    PaletteData getData();

    /**
     * Gets the currently active palette, if any.
     *
     * @return Optional containing the active palette, or empty if none is active
     */
    Optional<Palette> getActivePalette();

    /**
     * Finds a palette by its UUID.
     *
     * @param id The UUID of the palette to find
     * @return Optional containing the palette if found, or empty if not found
     */
    Optional<Palette> getPalette(UUID id);

    /**
     * Finds a palette by its name.
     * 
     * <p>
     * If multiple palettes share the same name, returns the first match.
     *
     * @param name The name of the palette to find
     * @return Optional containing the palette if found, or empty if not found
     */
    Optional<Palette> getPaletteByName(String name);

    // ==================== CRUD Operations ====================

    /**
     * Adds an existing palette to the end of the list.
     * 
     * <p>
     * Use {@link Palette#create} to create the palette instance before calling
     * this method. This separation ensures the service focuses solely on
     * managing the palette collection.
     *
     * @param palette The palette to add (must not be null)
     * @return The new PaletteData with the palette added
     */
    PaletteData addPalette(Palette palette);

    /**
     * Replaces an existing palette with a new palette instance.
     * 
     * <p>
     * The replacement palette must have the same UUID as the palette being
     * replaced. The palette is replaced in-place at the same index in the list.
     * Use {@link Palette#create(UUID, String, String, java.util.Map)} to create
     * the replacement palette with the same UUID.
     *
     * @param palette The replacement palette (must have a UUID matching an
     *                existing palette)
     * @return Optional containing the new PaletteData if the palette existed,
     *         or empty if no palette with the given UUID exists
     */
    Optional<PaletteData> replacePalette(Palette palette);

    /**
     * Deletes a palette by its UUID.
     * 
     * <p>
     * If the deleted palette was the active palette, the active state is
     * automatically cleared (set to null).
     *
     * @param id The UUID of the palette to delete
     * @return Optional containing the new PaletteData if the palette existed,
     *         or empty if no palette with the given ID exists
     */
    Optional<PaletteData> deletePalette(UUID id);

    /**
     * Creates a duplicate of an existing palette.
     * 
     * <p>
     * The duplicate has a new generated UUID and the name is suffixed with "
     * (Copy)".
     * The duplicate is added to the end of the palettes list.
     *
     * @param id The UUID of the palette to duplicate
     * @return Optional containing the new PaletteData if the source palette
     *         existed,
     *         or empty if no palette with the given ID exists
     */
    Optional<PaletteData> duplicatePalette(UUID id);

    // ==================== Active Palette Management ====================

    /**
     * Sets or toggles the active palette.
     * 
     * <p>
     * This method implements toggle behavior:
     * <ul>
     * <li>If the specified palette is not currently active, it becomes active</li>
     * <li>If the specified palette is already active, it is deactivated (active set
     * to null)</li>
     * <li>If the palette ID does not exist, the state is returned unchanged (no
     * error)</li>
     * </ul>
     *
     * @param id The UUID of the palette to activate or toggle
     * @return The new PaletteData with the updated active state
     */
    PaletteData setActivePalette(UUID id);

    /**
     * Clears the active palette, setting it to null.
     *
     * @return The new PaletteData with no active palette
     */
    PaletteData clearActivePalette();

    // ==================== Organization ====================

    /**
     * Reorders a palette to a new position in the list.
     * 
     * <p>
     * The new index is clamped to the valid range [0, palettes.size()-1].
     * The active palette state is preserved (same UUID remains active if it was
     * active).
     *
     * @param id       The UUID of the palette to move
     * @param newIndex The target index (will be clamped to valid range)
     * @return Optional containing the new PaletteData if the palette existed,
     *         or empty if no palette with the given ID exists
     */
    Optional<PaletteData> reorderPalette(UUID id, int newIndex);

    // ==================== Observer Pattern ====================

    /**
     * Registers a listener to receive palette data change notifications.
     * 
     * <p>
     * Listeners are notified after every state mutation. The listener collection
     * is thread-safe and supports concurrent modification during iteration.
     *
     * @param listener The listener to add
     */
    void addChangeListener(PaletteChangeListener listener);

    /**
     * Removes a previously registered change listener.
     *
     * @param listener The listener to remove
     */
    void removeChangeListener(PaletteChangeListener listener);

    // ==================== State Initialization ====================

    /**
     * Loads (replaces) the internal palette data state.
     * 
     * <p>
     * This method is intended for use by the persistence layer to initialize
     * or restore state from disk. A change notification is fired to inform
     * all listeners of the state change.
     *
     * @param data The palette data to load as the new state
     */
    void loadState(PaletteData data);
}

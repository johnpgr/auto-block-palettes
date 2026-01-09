package com.autoblockpalette.platform.services;

import com.autoblockpalette.data.Palette;
import com.autoblockpalette.data.PaletteData;
import com.autoblockpalette.util.Result;

import java.util.Optional;
import java.util.SequencedMap;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of {@link IPaletteManager}.
 * 
 * <p>
 * This implementation maintains an internal {@link PaletteData} state and
 * provides
 * thread-safe observer notifications via {@link CopyOnWriteArrayList}. All CRUD
 * operations return new immutable {@code PaletteData} instances.
 * 
 * <p>
 * The service is loaded via the ServiceLoader mechanism and accessed through
 * {@code Services.PALETTE_MANAGER}.
 */
public class DefaultPaletteManager implements IPaletteManager {

    private volatile PaletteData state = PaletteData.empty();
    private final CopyOnWriteArrayList<PaletteChangeListener> listeners = new CopyOnWriteArrayList<>();

    // ==================== State Access ====================

    @Override
    public PaletteData getData() {
        return state;
    }

    @Override
    public Optional<Palette> getActivePalette() {
        return state.getActivePalette();
    }

    @Override
    public Optional<Palette> getPalette(UUID id) {
        return state.findById(id);
    }

    @Override
    public Optional<Palette> getPaletteByName(String name) {
        return state.findByName(name);
    }

    // ==================== CRUD Operations ====================

    @Override
    public PaletteData addPalette(Palette palette) {
        PaletteData oldState = state;

        // Add palette to end of map
        SequencedMap<UUID, Palette> newPalettes = oldState.palettesMap();
        newPalettes.put(palette.id(), palette);

        PaletteData newState = new PaletteData(newPalettes, oldState.activePaletteId());
        setState(newState, oldState);

        return newState;
    }

    @Override
    public Optional<PaletteData> replacePalette(Palette palette) {
        PaletteData oldState = state;

        // Check if palette exists (O(1) lookup)
        if (!oldState.hasPalette(palette.id())) {
            return Optional.empty();
        }

        // Replace palette in map (preserves order)
        SequencedMap<UUID, Palette> newPalettes = oldState.palettesMap();
        newPalettes.put(palette.id(), palette);

        PaletteData newState = new PaletteData(newPalettes, oldState.activePaletteId());
        setState(newState, oldState);

        return Optional.of(newState);
    }

    @Override
    public Optional<PaletteData> deletePalette(UUID id) {
        PaletteData oldState = state;

        // Check if palette exists (O(1) lookup)
        if (!oldState.hasPalette(id)) {
            return Optional.empty();
        }

        // Remove palette from map
        SequencedMap<UUID, Palette> newPalettes = oldState.palettesMap();
        newPalettes.remove(id);

        // Clear active palette if the deleted one was active
        UUID newActivePaletteId = oldState.activePaletteId();
        if (id.equals(newActivePaletteId)) {
            newActivePaletteId = null;
        }

        PaletteData newState = new PaletteData(newPalettes, newActivePaletteId);
        setState(newState, oldState);

        return Optional.of(newState);
    }

    @Override
    public Result<PaletteData> duplicatePalette(UUID id) {
        PaletteData oldState = state;

        // Find source palette
        Optional<Palette> sourcePaletteOpt = oldState.findById(id);
        if (sourcePaletteOpt.isEmpty()) {
            return Result.error("Palette not found: " + id);
        }

        Palette sourcePalette = sourcePaletteOpt.get();

        // Create duplicate with new UUID and "(Copy)" suffix
        Result<Palette> result = Palette.create(
                UUID.randomUUID(),
                sourcePalette.name() + " (Copy)",
                sourcePalette.iconBlock(),
                sourcePalette.blocks());

        if (result instanceof Result.Error<Palette>(String message)) {
            return Result.error(message);
        }

        Palette duplicatedPalette = result.getOrThrow();

        // Add to end of map
        SequencedMap<UUID, Palette> newPalettes = oldState.palettesMap();
        newPalettes.put(duplicatedPalette.id(), duplicatedPalette);

        PaletteData newState = new PaletteData(newPalettes, oldState.activePaletteId());
        setState(newState, oldState);

        return Result.success(newState);
    }

    // ==================== Active Palette Management ====================

    @Override
    public PaletteData setActivePalette(UUID id) {
        PaletteData oldState = state;

        // If palette doesn't exist, return unchanged state (no notification)
        if (!oldState.hasPalette(id)) {
            return oldState;
        }

        // Toggle behavior: if already active, deactivate
        UUID newActivePaletteId;
        if (id.equals(oldState.activePaletteId())) {
            newActivePaletteId = null;
        } else {
            newActivePaletteId = id;
        }

        PaletteData newState = new PaletteData(oldState.palettesMap(), newActivePaletteId);
        setState(newState, oldState);

        return newState;
    }

    @Override
    public PaletteData clearActivePalette() {
        PaletteData oldState = state;

        // No change needed if already null
        if (oldState.activePaletteId() == null) {
            return oldState;
        }

        PaletteData newState = new PaletteData(oldState.palettesMap(), null);
        setState(newState, oldState);

        return newState;
    }


    // ==================== Observer Pattern ====================

    @Override
    public void addChangeListener(PaletteChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeChangeListener(PaletteChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    // ==================== State Initialization ====================

    @Override
    public void loadState(PaletteData data) {
        PaletteData oldState = state;
        PaletteData newState = data != null ? data : PaletteData.empty();
        setState(newState, oldState);
    }

    // ==================== Private Helpers ====================

    /**
     * Updates the internal state and notifies all listeners.
     *
     * @param newState The new state to set
     * @param oldState The previous state (for notification)
     */
    private void setState(PaletteData newState, PaletteData oldState) {
        state = newState;
        notifyListeners(oldState, newState);
    }

    /**
     * Notifies all registered listeners of a state change.
     *
     * @param oldData The previous state
     * @param newData The new state
     */
    private void notifyListeners(PaletteData oldData, PaletteData newData) {
        for (PaletteChangeListener listener : listeners) {
            try {
                listener.onPaletteDataChanged(oldData, newData);
            } catch (Exception e) {
                // Log but don't propagate listener exceptions
                // This prevents a misbehaving listener from breaking other notifications
            }
        }
    }
}

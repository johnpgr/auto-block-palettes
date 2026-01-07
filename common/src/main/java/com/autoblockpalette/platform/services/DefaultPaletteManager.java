package com.autoblockpalette.platform.services;

import com.autoblockpalette.data.Palette;
import com.autoblockpalette.data.PaletteData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        // Add palette to end of list
        List<Palette> newPalettes = new ArrayList<>(oldState.palettes());
        newPalettes.add(palette);

        PaletteData newState = new PaletteData(List.copyOf(newPalettes), oldState.activePaletteId());
        setState(newState, oldState);

        return newState;
    }

    @Override
    public Optional<PaletteData> replacePalette(Palette palette) {
        PaletteData oldState = state;

        // Find the palette index by UUID
        int index = findPaletteIndex(oldState, palette.id());
        if (index < 0) {
            return Optional.empty();
        }

        // Replace palette at same index
        List<Palette> newPalettes = new ArrayList<>(oldState.palettes());
        newPalettes.set(index, palette);

        PaletteData newState = new PaletteData(List.copyOf(newPalettes), oldState.activePaletteId());
        setState(newState, oldState);

        return Optional.of(newState);
    }

    @Override
    public Optional<PaletteData> deletePalette(UUID id) {
        PaletteData oldState = state;

        // Find the palette index
        int index = findPaletteIndex(oldState, id);
        if (index < 0) {
            return Optional.empty();
        }

        // Remove palette from list
        List<Palette> newPalettes = new ArrayList<>(oldState.palettes());
        newPalettes.remove(index);

        // Clear active palette if the deleted one was active
        UUID newActivePaletteId = oldState.activePaletteId();
        if (id.equals(newActivePaletteId)) {
            newActivePaletteId = null;
        }

        PaletteData newState = new PaletteData(List.copyOf(newPalettes), newActivePaletteId);
        setState(newState, oldState);

        return Optional.of(newState);
    }

    @Override
    public Optional<PaletteData> duplicatePalette(UUID id) {
        PaletteData oldState = state;

        // Find source palette
        Optional<Palette> sourcePaletteOpt = oldState.findById(id);
        if (sourcePaletteOpt.isEmpty()) {
            return Optional.empty();
        }

        Palette sourcePalette = sourcePaletteOpt.get();

        // Create duplicate with new UUID and "(Copy)" suffix
        Palette duplicatedPalette = Palette.create(
                UUID.randomUUID(),
                sourcePalette.name() + " (Copy)",
                sourcePalette.iconBlock(),
                sourcePalette.blocks());

        // Add to end of list
        List<Palette> newPalettes = new ArrayList<>(oldState.palettes());
        newPalettes.add(duplicatedPalette);

        PaletteData newState = new PaletteData(List.copyOf(newPalettes), oldState.activePaletteId());
        setState(newState, oldState);

        return Optional.of(newState);
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

        PaletteData newState = new PaletteData(oldState.palettes(), newActivePaletteId);
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

        PaletteData newState = new PaletteData(oldState.palettes(), null);
        setState(newState, oldState);

        return newState;
    }

    // ==================== Organization ====================

    @Override
    public Optional<PaletteData> reorderPalette(UUID id, int newIndex) {
        PaletteData oldState = state;

        // Find the palette index
        int currentIndex = findPaletteIndex(oldState, id);
        if (currentIndex < 0) {
            return Optional.empty();
        }

        List<Palette> palettes = oldState.palettes();

        // Clamp newIndex to valid range [0, palettes.size()-1]
        int clampedIndex = Math.max(0, Math.min(newIndex, palettes.size() - 1));

        // No change needed if already at target position
        if (currentIndex == clampedIndex) {
            return Optional.of(oldState);
        }

        // Remove from current position and insert at new position
        List<Palette> newPalettes = new ArrayList<>(palettes);
        Palette palette = newPalettes.remove(currentIndex);
        newPalettes.add(clampedIndex, palette);

        PaletteData newState = new PaletteData(List.copyOf(newPalettes), oldState.activePaletteId());
        setState(newState, oldState);

        return Optional.of(newState);
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

    /**
     * Finds the index of a palette by its UUID.
     *
     * @param data The palette data to search
     * @param id   The UUID to find
     * @return The index of the palette, or -1 if not found
     */
    private int findPaletteIndex(PaletteData data, UUID id) {
        List<Palette> palettes = data.palettes();
        for (int i = 0; i < palettes.size(); i++) {
            if (palettes.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}

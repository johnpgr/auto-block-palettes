package com.autoblockpalette.platform.services;

import com.autoblockpalette.data.PaletteData;

/**
 * Listener interface for receiving notifications when palette data changes.
 * 
 * <p>
 * Implementations receive both the old and new state, allowing them to diff
 * the changes themselves without requiring separate event types for each
 * operation.
 * 
 * <p>
 * This interface is designed to be used with the observer pattern in
 * {@link IPaletteManager} for decoupled state change notifications.
 */
@FunctionalInterface
public interface PaletteChangeListener {

    /**
     * Called when the palette data has changed.
     * 
     * <p>
     * This method is invoked after any state mutation in the palette manager,
     * including create, update, delete, reorder, and active palette changes.
     * 
     * <p>
     * The listener is responsible for determining what specifically changed
     * by comparing the old and new data.
     *
     * @param oldData The previous palette data state before the change
     * @param newData The new palette data state after the change
     */
    void onPaletteDataChanged(PaletteData oldData, PaletteData newData);
}

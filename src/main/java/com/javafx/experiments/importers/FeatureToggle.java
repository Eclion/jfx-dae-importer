package com.javafx.experiments.importers;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.function.Consumer;

/**
 * @author Eclion
 */
public final class FeatureToggle {

    private static final BooleanProperty DISPLAY_MESHS_FEATURE = new SimpleBooleanProperty(true);
    private static final BooleanProperty DISPLAY_SKELETONS_FEATURE = new SimpleBooleanProperty(false);

    public static void onDisplayMeshsChange(Consumer<Boolean> consumer) {
        DISPLAY_MESHS_FEATURE.addListener((observable, oldValue, newValue) -> consumer.accept(newValue));
        consumer.accept(DISPLAY_MESHS_FEATURE.get());
    }

    public static void onDisplaySkeletonsChange(Consumer<Boolean> consumer) {
        DISPLAY_SKELETONS_FEATURE.addListener((observable, oldValue, newValue) -> consumer.accept(newValue));
        consumer.accept(DISPLAY_SKELETONS_FEATURE.get());
    }
}

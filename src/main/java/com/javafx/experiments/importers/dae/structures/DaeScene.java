package com.javafx.experiments.importers.dae.structures;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eclion
 */
public final class DaeScene {
    private final String name;
    public final String id;
    public final Map<String, DaeNode> cameraNodes = new HashMap<>();
    public final Map<String, DaeNode> lightNodes = new HashMap<>();
    public final Map<String, DaeNode> meshNodes = new HashMap<>();
    public final Map<String, DaeSkeleton> skeletons = new HashMap<>();
    public final Map<String, DaeNode> controllerNodes = new HashMap<>();

    public DaeScene(final String id, final String name) {
        this.id = id;
        this.name = name;
    }
}

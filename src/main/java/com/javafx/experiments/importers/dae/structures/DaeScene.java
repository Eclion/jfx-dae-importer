package com.javafx.experiments.importers.dae.structures;

import javafx.scene.Group;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eclion
 */
public final class DaeScene extends Group {
    private final String name;
    public final Map<String, DaeSkeleton> skeletons = new HashMap<>();

    public DaeScene(final String id, final String name) {
        this.setId(id);
        this.name = name;
    }

    public void build(DaeBuildHelper buildHelper) {
        System.out.println(this.getId());
        getChildren().stream()
                .filter(child -> child instanceof DaeNode)
                .map(child -> (DaeNode) child)
                .forEach(child -> child.build(buildHelper));
    }
}

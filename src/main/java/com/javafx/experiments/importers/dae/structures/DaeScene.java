package com.javafx.experiments.importers.dae.structures;

import com.javafx.experiments.importers.dae.utils.ParserUtils;
import javafx.scene.Group;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eclion
 */
public final class DaeScene extends Group {
    public final Map<String, DaeSkeleton> skeletons = new HashMap<>();

    public DaeScene(final String id) {
        this.setId(id);
    }

    public void build(final DaeBuildHelper buildHelper) {
        ParserUtils.getDaeNodeChildStream(this).
                forEach(child -> child.build(buildHelper));
    }
}

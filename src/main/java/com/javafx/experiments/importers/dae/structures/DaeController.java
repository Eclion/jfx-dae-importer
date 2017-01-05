package com.javafx.experiments.importers.dae.structures;

import javafx.scene.transform.Affine;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eclion
 */
public final class DaeController {
    final String name;
    final String id;

    public String skinId;
    public Affine bindShapeMatrix;
    public String[] jointNames;
    public float[][] vertexWeights;
    public final List<Affine> bindPoses = new ArrayList<>();

    public DaeController(String id, String name)
    {
        this.id = id;
        this.name = name;
    }
}

package com.javafx.experiments.importers.dae.structures;

import javafx.scene.transform.Affine;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eclion
 */
public final class DaeController {
    private final String name;

    private String skinId;
    private Affine bindShapeMatrix;
    private String[] jointNames;
    private float[][] vertexWeights;
    public final List<Affine> bindPoses = new ArrayList<>();

    public DaeController(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getSkinId() {
        return skinId;
    }

    public void setSkinId(String skinId) {
        this.skinId = skinId;
    }

    Affine getBindShapeMatrix() {
        return bindShapeMatrix;
    }

    public void setBindShapeMatrix(Affine bindShapeMatrix) {
        this.bindShapeMatrix = bindShapeMatrix;
    }

    public String[] getJointNames() {
        return jointNames;
    }

    public void setJointNames(String[] jointNames) {
        this.jointNames = jointNames;
    }

    public float[][] getVertexWeights() {
        return vertexWeights;
    }

    public void setVertexWeights(float[][] vertexWeights) {
        this.vertexWeights = vertexWeights;
    }
}

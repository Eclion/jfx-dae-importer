package com.javafx.experiments.animation;

import com.javafx.experiments.shape3d.SkinningMesh;
import javafx.animation.AnimationTimer;

/**
 * @author Eclion
 */
public class SkinningMeshTimer extends AnimationTimer {
    private final SkinningMesh mesh;

    public SkinningMeshTimer(SkinningMesh mesh) {
        this.mesh = mesh;
    }

    @Override
    public void handle(long l) {
        mesh.update();
    }
}
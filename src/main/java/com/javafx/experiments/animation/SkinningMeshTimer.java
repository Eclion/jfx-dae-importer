package com.javafx.experiments.animation;

import com.javafx.experiments.shape3d.SkinningMesh;
import javafx.animation.AnimationTimer;

/**
 * @author Eclion
 */
public final class SkinningMeshTimer extends AnimationTimer {
    private final SkinningMesh mesh;

    public SkinningMeshTimer(final SkinningMesh mesh) {
        this.mesh = mesh;
    }

    @Override
    public void handle(final long l) {
        mesh.update();
    }
}
package com.javafx.experiments.importers.dae.structures;

import javafx.scene.Camera;
import javafx.scene.paint.Material;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Eclion.
 */
public final class DaeBuildHelper {
    private final Map<String, Material> materialMap = new HashMap<>();
    private final Map<String, List<TriangleMesh>> meshes = new HashMap<>();
    private final Map<String, List<String>> meshMaterialIds = new HashMap<>();
    private final Map<String, DaeController> controllers = new HashMap<>();
    private final Map<String, DaeSkeleton> skeletons = new HashMap<>();
    private final Map<String, Camera> cameras = new HashMap<>();

    public DaeBuildHelper withMeshes(final Map<String, List<TriangleMesh>> meshes) {
        this.meshes.putAll(meshes);
        return this;
    }

    public DaeBuildHelper withMeshMaterialIds(final Map<String, List<String>> meshMaterialIds) {
        this.meshMaterialIds.putAll(meshMaterialIds);
        return this;
    }

    public DaeBuildHelper withMaterialMap(final Map<String, Material> materialMap) {
        this.materialMap.putAll(materialMap);
        return this;
    }

    public DaeBuildHelper withControllers(final Map<String, DaeController> controllers) {
        this.controllers.putAll(controllers);
        return this;
    }

    public DaeBuildHelper withSkeletons(final Map<String, DaeSkeleton> skeletons) {
        this.skeletons.putAll(skeletons);
        return this;
    }

    List<TriangleMesh> getMeshes(final String geometryId) {
        return meshes.get(geometryId);
    }

    List<Material> getMaterials(final String geometryId) {
        return meshMaterialIds.getOrDefault(geometryId, new ArrayList<>()).
                stream().
                map(materialMap::get).
                collect(Collectors.toList());
    }

    DaeController getController(final String controllerId) {
        return controllers.get(controllerId);
    }

    DaeSkeleton getSkeleton(final String skeletonId) {
        return skeletons.get(skeletonId);
    }

    public DaeBuildHelper withCameras(final Map<String, Camera> cameras) {
        this.cameras.putAll(cameras);
        return this;
    }

    Camera getCamera(final String instanceCameraId) {
        return cameras.get(instanceCameraId);
    }
}

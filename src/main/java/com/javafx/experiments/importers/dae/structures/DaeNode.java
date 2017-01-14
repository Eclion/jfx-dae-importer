package com.javafx.experiments.importers.dae.structures;

import javafx.scene.transform.Transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Eclion
 *         make subclasses of this one
 */
public final class DaeNode {
    public final String id;
    public final String name;
    public final String type;
    public final List<Transform> transforms = new ArrayList<>();
    public String instanceCameraId;
    public String instanceGeometryId;
    public String instanceControllerId;
    public String instanceLightId;
    public String instanceMaterialId;
    public String skeletonId;
    public final Map<String, DaeNode> children = new HashMap<>();

    public DaeNode(final String id, final String name, final String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public boolean isCamera() {
        return instanceCameraId != null;
    }

    public boolean isLight() {
        return instanceLightId != null;
    }

    public boolean isMesh() {
        return instanceGeometryId != null;
    }

    public boolean isController() {
        return instanceControllerId != null;
    }

    public boolean hasJoints() {
        return children.values().stream().anyMatch(DaeNode::isJoint);
    }

    boolean isJoint() {
        return "JOINT".equalsIgnoreCase(type);
    }

    @Override
    public String toString() {
        return "DaeNode{"
                + "id='" + this.id + '\''
                + ", name='" + this.name + '\''
                + ", instance_camera=" + this.instanceCameraId
                + ", instance_geometry=" + this.instanceGeometryId
                + ", instance_controller=" + this.instanceControllerId
                + '}';
    }
}
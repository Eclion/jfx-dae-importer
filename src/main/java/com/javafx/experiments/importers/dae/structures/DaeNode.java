package com.javafx.experiments.importers.dae.structures;

import com.javafx.experiments.animation.SkinningMeshTimer;
import com.javafx.experiments.shape3d.SkinningMesh;
import javafx.scene.Group;
import javafx.scene.paint.Material;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;

import java.util.*;


/**
 * @author Eclion
 */
public final class DaeNode extends Group {
    public final String name;
    public final String type;
    private Category instanceCategory = Category.NONE;
    private String instanceId;
    private String instanceMaterialId;
    public String skeletonId;

    private enum Category {
        CAMERA,
        GEOMETRY,
        CONTROLLER,
        LIGHT,
        NONE
    }

    public DaeNode(final String id, final String name, final String type) {
        this.setId(id);
        this.name = name;
        this.type = type;
    }

    public void setInstanceCameraId(final String instanceCameraId) {
        instanceCategory = Category.CAMERA;
        instanceId = instanceCameraId;
    }

    public void setInstanceGeometryId(final String instanceGeometryId) {
        instanceId = instanceGeometryId;
        instanceCategory = Category.GEOMETRY;
    }

    public void setInstanceControllerId(final String instanceControllerId) {
        instanceId = instanceControllerId;
        instanceCategory = Category.CONTROLLER;
    }

    public void setInstanceLightId(final String instanceLightId) {
        instanceId = instanceLightId;
        instanceCategory = Category.LIGHT;
    }

    public void setInstanceMaterialId(final String instanceMaterialId) {
        this.instanceMaterialId = instanceMaterialId;
    }

    public boolean hasJoints() {
        return getChildren().stream().
                filter(node -> node instanceof DaeNode).
                map(node -> (DaeNode) node).
                anyMatch(DaeNode::isJoint);
    }

    boolean isJoint() {
        return "JOINT".equalsIgnoreCase(type);
    }

    @Override
    public String toString() {
        return "DaeNode{"
                + "id='" + this.getId() + '\''
                + ", name='" + this.name + '\''
                + ", instance=" + this.instanceId
                + ", instance_category=" + this.instanceCategory.toString().toLowerCase()
                + '}';
    }

    void build(final DaeBuildHelper buildHelper) {
        switch (instanceCategory) {
            case CAMERA:
                buildCamera(buildHelper);
                break;
            case CONTROLLER:
                buildController(buildHelper);
                break;
            case GEOMETRY:
                buildGeometry(buildHelper);
                break;
            case LIGHT:
                break;
            case NONE:
            default:
                break;
        }

        daeNodeTreeBuild(this, buildHelper);
    }

    static void daeNodeTreeBuild(final Group node, final DaeBuildHelper buildHelper) {
        node.getChildren().stream().
                filter(child -> child instanceof DaeNode).
                map(child -> (DaeNode) child).
                forEach(child -> child.build(buildHelper));
    }

    private void buildCamera(final DaeBuildHelper buildHelper) {
        getChildren().add(buildHelper.getCamera(instanceId));
    }

    private void buildController(final DaeBuildHelper buildHelper) {
        final DaeController controller = buildHelper.getController(instanceId);

        final DaeSkeleton skeleton = buildHelper.getSkeleton(controller.getName());

        final String[] bones = skeleton.joints.keySet().toArray(new String[]{});
        final Affine[] bindTransforms = new Affine[bones.length];
        final Joint[] joints = new Joint[bones.length];

        for (int i = 0; i < bones.length; i++) {
            bindTransforms[i] = controller.bindPoses.get(i);
            joints[i] = skeleton.joints.get(bones[i]);
        }

        final List<TriangleMesh> meshes = buildHelper.getMeshes(controller.skinId);

        final List<Material> materials = buildHelper.getMaterials(controller.skinId);

        for (int i = 0; i < meshes.size(); i++) {
            final SkinningMesh skinningMesh = new SkinningMesh(
                    meshes.get(i), controller.vertexWeights, bindTransforms,
                    controller.bindShapeMatrix, Arrays.asList(joints), Arrays.asList(skeleton));

            final MeshView meshView = new MeshView(skinningMesh);

            final SkinningMeshTimer skinningMeshTimer = new SkinningMeshTimer(skinningMesh);
            if (meshView.getScene() != null) {
                skinningMeshTimer.start();
            }
            meshView.sceneProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null) {
                    skinningMeshTimer.stop();
                } else {
                    skinningMeshTimer.start();
                }
            });

            if (i < materials.size()) meshView.setMaterial(materials.get(i));
            getChildren().add(meshView);
        }
    }

    private void buildGeometry(final DaeBuildHelper buildHelper) {
        final List<TriangleMesh> meshes = buildHelper.getMeshes(instanceId);

        final List<Material> materials = buildHelper.getMaterials(instanceId);

        for (int i = 0; i < meshes.size(); i++) {
            final MeshView meshView = new MeshView(meshes.get(i));
            if (i < materials.size()) meshView.setMaterial(materials.get(i));
            getChildren().add(meshView);
        }

    }
}
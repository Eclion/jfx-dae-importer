package com.javafx.experiments.importers.dae.structures;

import com.javafx.experiments.animation.SkinningMeshTimer;
import com.javafx.experiments.importers.dae.utils.ParserUtils;
import com.javafx.experiments.shape3d.SkinningMesh;
import javafx.scene.Group;
import javafx.scene.paint.Material;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;

import java.util.*;
import java.util.stream.Stream;


/**
 * @author Eclion
 */
public final class DaeNode extends Group {
    public final String name;
    public final String type;
    private Category instanceCategory = Category.NONE;
    private String instanceId;

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

    public boolean hasJoints() {
        return getDaeNodeChildStream().anyMatch(DaeNode::isJoint);
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
            case NONE:
            default:
                break;
        }

        getDaeNodeChildStream().forEach(child -> child.build(buildHelper));
    }

    public Stream<DaeNode> getDaeNodeChildStream() {
        return ParserUtils.getDaeNodeChildStream(this);
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

        final List<TriangleMesh> meshes = buildHelper.getMeshes(controller.getSkinId());

        final List<Material> materials = buildHelper.getMaterials(controller.getSkinId());

        for (int i = 0; i < meshes.size(); i++) {
            final SkinningMesh skinningMesh = new SkinningMesh(
                    meshes.get(i), controller.getVertexWeights(), bindTransforms,
                    controller.getBindShapeMatrix(), Arrays.asList(joints), Arrays.asList(skeleton));

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

            if (i < materials.size()) {
                meshView.setMaterial(materials.get(i));
            }
            getChildren().add(meshView);
        }
    }

    private void buildGeometry(final DaeBuildHelper buildHelper) {
        final List<TriangleMesh> meshes = buildHelper.getMeshes(instanceId);

        final List<Material> materials = buildHelper.getMaterials(instanceId);

        for (int i = 0; i < meshes.size(); i++) {
            final MeshView meshView = new MeshView(meshes.get(i));
            if (i < materials.size()) {
                meshView.setMaterial(materials.get(i));
            }
            getChildren().add(meshView);
        }

    }
}
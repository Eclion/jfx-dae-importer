package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.structures.DaeNode;
import com.javafx.experiments.importers.dae.structures.DaeScene;
import com.javafx.experiments.importers.dae.structures.DaeSkeleton;
import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import org.xml.sax.Attributes;

import java.util.LinkedList;

/**
 * @author Eclion
 */
final class LibraryVisualSceneParser extends AbstractParser {
    private static final String INSTANCE_CAMERA_TAG = "instance_camera";
    private static final String INSTANCE_CONTROLLER_TAG = "instance_controller";
    private static final String INSTANCE_GEOMETRY_TAG = "instance_geometry";
    private static final String INSTANCE_LIGHT_TAG = "instance_light";
    private static final String INSTANCE_MATERIAL_TAG = "instance_material";
    private static final String NODE_TAG = "node";
    private static final String MATRIX_TAG = "matrix";
    private static final String ROTATE_TAG = "rotate";
    private static final String SCALE_TAG = "scale";
    private static final String SKELETON_TAG = "skeleton";
    private static final String TRANSLATE_TAG = "translate";
    private static final String VISUAL_SCENE_TAG = "visual_scene";

    private static final String URL_STR = "url";
    private static final String ID_STR = "id";
    private static final String NAME_STR = "name";

    private static final String WHITESPACES_REGEX = "\\s+";

    final LinkedList<DaeScene> scenes = new LinkedList<>();
    private final LinkedList<DaeNode> nodes = new LinkedList<>();

    LibraryVisualSceneParser() {
        addStartElementBiConsumer(INSTANCE_CAMERA_TAG, (qName, attributes) -> nodes.peek().setInstanceCameraId(extractUrl(attributes)));
        addStartElementBiConsumer(INSTANCE_CONTROLLER_TAG, (qName, attributes) -> nodes.peek().setInstanceControllerId(extractUrl(attributes)));
        addStartElementBiConsumer(INSTANCE_GEOMETRY_TAG, (qName, attributes) -> nodes.peek().setInstanceGeometryId(extractUrl(attributes)));
        addStartElementBiConsumer(INSTANCE_LIGHT_TAG, (qName, attributes) -> nodes.peek().setInstanceLightId(extractUrl(attributes)));
        addStartElementBiConsumer(INSTANCE_MATERIAL_TAG, (qName, attributes) -> nodes.peek().setInstanceMaterialId(attributes.getValue("target").substring(1)));
        addStartElementBiConsumer(NODE_TAG, (qName, attributes) -> createDaeNode(attributes));
        addStartElementBiConsumer(VISUAL_SCENE_TAG, (qName, attributes) -> createVisualScene(attributes));

        addEndElementBiConsumer(NODE_TAG, (qName, content) -> setDaeNode());
        addEndElementBiConsumer(MATRIX_TAG, (qName, content) -> addMatrixTransformation(content));
        addEndElementBiConsumer(ROTATE_TAG, (qName, content) -> addRotation(content));
        addEndElementBiConsumer(SCALE_TAG, (qName, content) -> addScaling(content));
        addEndElementBiConsumer(TRANSLATE_TAG, (qName, content) -> addTranslation(content));
        addEndElementBiConsumer(SKELETON_TAG, (qName, content) -> nodes.peek().skeletonId = content.substring(1));
    }

    private String extractUrl(final Attributes attributes) {
        return attributes.getValue(URL_STR).substring(1);
    }

    private void addTranslation(final String content) {
        final String[] tv = content.split(WHITESPACES_REGEX);
        nodes.peek().getTransforms().add(new Translate(
                Double.parseDouble(tv[0].trim()),
                Double.parseDouble(tv[1].trim()),
                Double.parseDouble(tv[2].trim())
        ));
    }

    private void addRotation(final String content) {
        final String[] rv = content.split(WHITESPACES_REGEX);
        nodes.peek().getTransforms().add(new Rotate(
                Double.parseDouble(rv[3].trim()),
                0, 0, 0,
                new Point3D(
                        Double.parseDouble(rv[0].trim()),
                        Double.parseDouble(rv[1].trim()),
                        Double.parseDouble(rv[2].trim())
                )
        ));
    }

    private void addScaling(final String content) {
        final String[] sv = content.split(WHITESPACES_REGEX);
        nodes.peek().getTransforms().add(new Scale(
                Double.parseDouble(sv[0].trim()),
                Double.parseDouble(sv[1].trim()),
                Double.parseDouble(sv[2].trim()),
                0, 0, 0
        ));
    }

    private void addMatrixTransformation(final String content) {
        final String[] mv = content.split(WHITESPACES_REGEX);
        nodes.peek().getTransforms().add(new Affine(
                Double.parseDouble(mv[0].trim()), // mxx
                Double.parseDouble(mv[1].trim()), // mxy
                Double.parseDouble(mv[2].trim()), // mxz
                Double.parseDouble(mv[3].trim()), // tx
                Double.parseDouble(mv[4].trim()), // myx
                Double.parseDouble(mv[5].trim()), // myy
                Double.parseDouble(mv[6].trim()), // myz
                Double.parseDouble(mv[7].trim()), // ty
                Double.parseDouble(mv[8].trim()), // mzx
                Double.parseDouble(mv[9].trim()), // mzy
                Double.parseDouble(mv[10].trim()), // mzz
                Double.parseDouble(mv[11].trim()) // tz
        ));
    }

    private void createVisualScene(final Attributes attributes) {
        scenes.push(new DaeScene(attributes.getValue(ID_STR), attributes.getValue(NAME_STR)));
    }

    private void createDaeNode(final Attributes attributes) {
        nodes.push(new DaeNode(
                attributes.getValue(ID_STR),
                attributes.getValue(NAME_STR),
                attributes.getValue("type")
        ));
    }

    private void setDaeNode() {
        final DaeNode thisNode = nodes.pop();
        if (nodes.isEmpty()) {
            scenes.peek().getChildren().add(thisNode);
            buildSkeletonIfChildrenHaveJoints(thisNode);
        } else {
            nodes.peek().getChildren().add(thisNode);
        }
    }

    private void buildSkeletonIfChildrenHaveJoints(DaeNode node) {
        if (node.hasJoints()) {
            scenes.peek().skeletons.put(node.getId(), DaeSkeleton.fromDaeNode(node));
        } else {
            node.getDaeNodeChildStream().forEach(this::buildSkeletonIfChildrenHaveJoints);
        }
    }
}
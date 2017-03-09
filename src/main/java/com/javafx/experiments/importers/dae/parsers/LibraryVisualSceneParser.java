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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.BiConsumer;

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

    final LinkedList<DaeScene> scenes = new LinkedList<>();
    final LinkedList<DaeNode> nodes = new LinkedList<>();

    LibraryVisualSceneParser() {
        final HashMap<String, BiConsumer<String, Attributes>> startElementConsumer = new HashMap<>();

        startElementConsumer.put(INSTANCE_CAMERA_TAG, (qName, attributes) -> nodes.peek().instanceCameraId = attributes.getValue("url").substring(1));
        startElementConsumer.put(INSTANCE_CONTROLLER_TAG, (qName, attributes) -> nodes.peek().instanceControllerId = attributes.getValue("url").substring(1));
        startElementConsumer.put(INSTANCE_GEOMETRY_TAG, (qName, attributes) -> nodes.peek().instanceGeometryId = attributes.getValue("url").substring(1));
        startElementConsumer.put(INSTANCE_LIGHT_TAG, (qName, attributes) -> nodes.peek().instanceLightId = attributes.getValue("url").substring(1));
        startElementConsumer.put(INSTANCE_MATERIAL_TAG, (qName, attributes) -> nodes.peek().instanceMaterialId = attributes.getValue("target").substring(1));
        startElementConsumer.put(NODE_TAG, this::createDaeNode);
        startElementConsumer.put(VISUAL_SCENE_TAG, this::createVisualScene);

        final HashMap<String, BiConsumer<String, String>> endElementConsumer = new HashMap<>();

        endElementConsumer.put(NODE_TAG, (qName, content) -> setDaeNode());
        endElementConsumer.put(MATRIX_TAG, this::addMatrixTransformation);
        endElementConsumer.put(ROTATE_TAG, this::addRotation);
        endElementConsumer.put(SCALE_TAG, this::addScaling);
        endElementConsumer.put(TRANSLATE_TAG, this::addTranslation);
        endElementConsumer.put(SKELETON_TAG, (qName, content) -> nodes.peek().skeletonId = content.substring(1));

        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }

    private void addTranslation(final String qName, final String content) {
        String[] tv = content.split("\\s+");
        nodes.peek().transforms.add(new Translate(
                Double.parseDouble(tv[0].trim()),
                Double.parseDouble(tv[1].trim()),
                Double.parseDouble(tv[2].trim())
        ));
    }

    private void addRotation(final String qName, final String content) {
        String[] rv = content.split("\\s+");
        nodes.peek().transforms.add(new Rotate(
                Double.parseDouble(rv[3].trim()),
                0, 0, 0,
                new Point3D(
                        Double.parseDouble(rv[0].trim()),
                        Double.parseDouble(rv[1].trim()),
                        Double.parseDouble(rv[2].trim())
                )
        ));
    }

    private void addScaling(final String qName, final String content) {
        String[] sv = content.split("\\s+");
        nodes.peek().transforms.add(new Scale(
                Double.parseDouble(sv[0].trim()),
                Double.parseDouble(sv[1].trim()),
                Double.parseDouble(sv[2].trim()),
                0, 0, 0
        ));
    }

    private void addMatrixTransformation(final String qName, final String content) {
        String[] mv = content.split("\\s+");
        nodes.peek().transforms.add(new Affine(
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

    private void createVisualScene(final String qName, final Attributes attributes)

    {
        scenes.push(new DaeScene(attributes.getValue("id"), attributes.getValue("name")));
    }

    private void createDaeNode(final String qName, final Attributes attributes)

    {
        nodes.push(new DaeNode(
                attributes.getValue("id"),
                attributes.getValue("name"),
                attributes.getValue("type")
        ));
    }

    private void setDaeNode() {
        DaeNode thisNode = nodes.pop();
        if (nodes.isEmpty()) {
            if (thisNode.isCamera()) {
                scenes.peek().cameraNodes.put(thisNode.id, thisNode);
            } else if (thisNode.isLight()) {
                scenes.peek().lightNodes.put(thisNode.id, thisNode);
            } else if (thisNode.hasJoints()) {
                scenes.peek().skeletons.put(thisNode.id, DaeSkeleton.fromDaeNode(thisNode));
            } else if (thisNode.isMesh()) {
                scenes.peek().meshNodes.put(thisNode.id, thisNode);
            } else if (thisNode.isController()) {
                scenes.peek().controllerNodes.put(thisNode.id, thisNode);
            }
        } else {
            nodes.peek().children.put(thisNode.id, thisNode);
        }
    }
}
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
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Eclion
 */
final class LibraryVisualSceneParser extends DefaultHandler {
    private final static Logger LOGGER = Logger.getLogger(LibraryVisualSceneParser.class.getSimpleName());
    private StringBuilder charBuf = new StringBuilder();
    private final Map<String, String> currentId = new HashMap<>();
    final LinkedList<DaeScene> scenes = new LinkedList<>();
    final LinkedList<DaeNode> nodes = new LinkedList<>();

    private enum State {
        UNKNOWN,
        instance_camera,
        instance_controller,
        instance_geometry,
        instance_light,
        instance_material,
        node,
        matrix,
        rotate,
        skeleton,
        translate,
        scale,
        visual_scene,

        // ignored, unsupported states:
        bind_material,
        connect,
        extra,
        layer,
        technique,
        technique_common,
        tip_x,
        tip_y,
        tip_z
    }

    private static State state(final String name) {
        try {
            return State.valueOf(name);
        } catch (Exception e) {
            return State.UNKNOWN;
        }
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
        currentId.put(qName, attributes.getValue("id"));
        charBuf = new StringBuilder();
        switch (state(qName)) {
            case UNKNOWN:
                LOGGER.log(Level.WARNING, "Unknown element: " + qName);
                break;
            case instance_camera:
                nodes.peek().instanceCameraId = attributes.getValue("url").substring(1);
                break;
            case instance_controller:
                nodes.peek().instanceControllerId = attributes.getValue("url").substring(1);
                break;
            case instance_geometry:
                nodes.peek().instanceGeometryId = attributes.getValue("url").substring(1);
                break;
            case instance_light:
                nodes.peek().instanceLightId = attributes.getValue("url").substring(1);
                break;
            case instance_material:
                nodes.peek().instanceMaterialId = attributes.getValue("target").substring(1);
                break;
            case node:
                createDaeNode(attributes);
                break;
            case visual_scene:
                createVisualScene(attributes);
                break;
            default:
                break;
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        switch (state(qName)) {
            case UNKNOWN:
                break;
            case node:
                setDaeNode();
                break;
            case matrix:
                addMatrixTransformation();
                break;
            case rotate:
                addRotation();
                break;
            case scale:
                addScaling();
                break;
            case translate:
                addTranslation();
                break;
            case skeleton:
                nodes.peek().skeletonId = charBuf.toString().trim().substring(1);
                break;
            default:
                break;
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        charBuf.append(ch, start, length);
    }

    private void addTranslation() {
        String[] tv = charBuf.toString().trim().split("\\s+");
        nodes.peek().transforms.add(new Translate(
                Double.parseDouble(tv[0].trim()),
                Double.parseDouble(tv[1].trim()),
                Double.parseDouble(tv[2].trim())
        ));
    }

    private void addRotation() {
        String[] rv = charBuf.toString().trim().split("\\s+");
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

    private void addScaling() {
        String[] sv = charBuf.toString().trim().split("\\s+");
        nodes.peek().transforms.add(new Scale(
                Double.parseDouble(sv[0].trim()),
                Double.parseDouble(sv[1].trim()),
                Double.parseDouble(sv[2].trim()),
                0, 0, 0
        ));
    }

    private void addMatrixTransformation() {
        String[] mv = charBuf.toString().trim().split("\\s+");
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

    private void createVisualScene(final Attributes attributes) {
        scenes.push(new DaeScene(attributes.getValue("id"), attributes.getValue("name")));
    }

    private void createDaeNode(final Attributes attributes) {
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
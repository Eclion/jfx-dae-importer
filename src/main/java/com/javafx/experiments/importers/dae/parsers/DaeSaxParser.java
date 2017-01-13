package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.animation.SkinningMeshTimer;
import com.javafx.experiments.importers.dae.structures.DaeController;
import com.javafx.experiments.importers.dae.structures.DaeNode;
import com.javafx.experiments.importers.dae.structures.DaeScene;
import com.javafx.experiments.importers.dae.structures.DaeSkeleton;
import com.javafx.experiments.importers.dae.structures.Joint;
import com.javafx.experiments.shape3d.SkinningMesh;
import javafx.animation.KeyFrame;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Eclion
 */
final public class DaeSaxParser extends DefaultHandler {

    private DefaultHandler subHandler;

    private Map<State, DefaultHandler> parsers = new HashMap<>();

    /*private AssetParser assetParser;
    private SceneParser sceneParser;
    private LibraryAnimationsParser libraryAnimationsParser;
    private LibraryCamerasParser libraryCamerasParser;
    private LibraryControllerParser libraryControllerParser;
    private LibraryEffectsParser libraryEffects;
    private LibraryGeometriesParser libraryGeometriesParser;
    private LibraryImagesParser libraryImagesParser;
    private LibraryLightsParser libraryLightsParser;
    private LibraryMaterialsParser libraryMaterialsParser;
    private LibraryVisualSceneParser libraryVisualSceneParser;*/

    private Camera firstCamera;

    private double firstCameraAspectRatio = 4.0f / 3.0f;

    private enum State {
        UNKNOWN,
        asset,
        scene,
        library_animations,
        library_cameras,
        library_controllers,
        library_effects,
        library_geometries,
        library_images,
        library_lights,
        library_materials,
        library_visual_scenes
    }

    private static State state(String name) {
        try {
            return State.valueOf(name);
        } catch (Exception e) {
            return State.UNKNOWN;
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        final State currentState = state(qName);
        switch (currentState) {
            case asset:
                setParser(currentState, new AssetParser());
                break;
            case scene:
                setParser(currentState, new SceneParser());
                break;
            case library_animations:
                setParser(currentState, new LibraryAnimationsParser());
                break;
            case library_cameras:
                setParser(currentState, new LibraryCamerasParser());
                break;
            case library_controllers:
                setParser(currentState, new LibraryControllerParser());
                break;
            case library_effects:
                setParser(currentState, new LibraryEffectsParser());
                break;
            case library_geometries:
                setParser(currentState, new LibraryGeometriesParser());
                break;
            case library_images:
                setParser(currentState, new LibraryImagesParser());
                break;
            case library_lights:
                setParser(currentState, new LibraryLightsParser());
                break;
            case library_materials:
                setParser(currentState, new LibraryMaterialsParser());
                break;
            case library_visual_scenes:
                setParser(currentState, new LibraryVisualSceneParser());
                break;
            default:
                if (subHandler != null) subHandler.startElement(uri, localName, qName, attributes);
                break;
        }
    }

    private void setParser(State state, DefaultHandler parser) {
        parsers.put(state, parser);
        subHandler = parsers.get(state);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (subHandler != null) subHandler.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (subHandler != null) subHandler.characters(ch, start, length);
    }

    public Camera getFirstCamera() {
        return (parsers.containsKey(State.library_cameras))
                ? ((LibraryCamerasParser) parsers.get(State.library_cameras)).firstCamera
                : null;
    }

    public double getFirstCameraAspectRatio() {
        return parsers.containsKey(State.library_cameras)
                ? ((LibraryCamerasParser) parsers.get(State.library_cameras)).firstCameraAspectRatio
                : 4.0 / 3.0;
    }

    public void buildScene(Group rootNode) {
        if (!parsers.containsKey(State.library_visual_scenes)) return;
        DaeScene scene = ((LibraryVisualSceneParser) parsers.get(State.library_visual_scenes)).scenes.peek();
        String upAxis = parsers.containsKey(State.asset)
                ? ((AssetParser) parsers.get(State.asset)).upAxis
                : "Z_UP";
        if ("Z_UP".equals(upAxis)) {
            rootNode.getTransforms().add(new Rotate(90, 0, 0, 0, Rotate.X_AXIS));
        } else if ("Y_UP".equals(upAxis)) {
            rootNode.getTransforms().add(new Rotate(180, 0, 0, 0, Rotate.X_AXIS));
        }
        rootNode.setId(scene.id);
        rootNode.getChildren().addAll(scene.meshNodes.values().stream().map(this::getMesh).collect(Collectors.toList()));
        rootNode.getChildren().addAll(scene.controllerNodes.values().stream().map(this::getController).collect(Collectors.toList()));
    }

    private Camera getCamera(DaeNode node) {
        if (!parsers.containsKey(State.library_cameras)) return null;
        Camera camera = ((LibraryCamerasParser) parsers.get(State.library_cameras)).cameras.get(node.instance_camera_id);
        camera.setId(node.name);
        camera.getTransforms().addAll(node.transforms);
        return camera;
    }

    private MeshView getMesh(DaeNode node) {
        if (!parsers.containsKey(State.library_geometries)) return new MeshView();
        TriangleMesh mesh =
                ((LibraryGeometriesParser) parsers.get(State.library_geometries))
                        .meshes.get(node.instance_geometry_id);
        MeshView meshView = new MeshView(mesh);

        meshView.setId(node.name);
        meshView.getTransforms().addAll(node.transforms);
        return meshView;
    }

    private MeshView getController(DaeNode node) {
        if (!parsers.containsKey(State.library_controllers)
                || !parsers.containsKey(State.library_visual_scenes)
                || !parsers.containsKey(State.library_geometries)) return new MeshView();

        final DaeController controller =
                ((LibraryControllerParser) parsers.get(State.library_controllers))
                        .controllers.get(node.instance_controller_id);

        //TODO get all skeletons
        final DaeSkeleton skeleton = (DaeSkeleton)
                ((LibraryVisualSceneParser) parsers.get(State.library_visual_scenes))
                        .scenes.get(0).skeletons.values().toArray()[0];

        final TriangleMesh mesh =
                ((LibraryGeometriesParser) parsers.get(State.library_geometries))
                        .meshes.get(controller.skinId);

        final String[] bones = skeleton.joints.keySet().toArray(new String[]{});
        final Affine[] bindTransforms = new Affine[bones.length];
        final Joint[] joints = new Joint[bones.length];

        for (int i = 0; i < bones.length; i++) {
            bindTransforms[i] = controller.bindPoses.get(i);
            joints[i] = skeleton.joints.get(bones[i]);
        }

        final SkinningMesh skinningMesh = new SkinningMesh(
                mesh, controller.vertexWeights, bindTransforms,
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

        meshView.setId(node.name);
        return meshView;
    }

    public List<KeyFrame> getAllKeyFrames() {
        if (!parsers.containsKey(State.library_animations)
                || !parsers.containsKey(State.library_visual_scenes)) return new ArrayList<>();

        final LibraryAnimationsParser animationsParser = (LibraryAnimationsParser) parsers.get(State.library_animations);

        final List<KeyFrame> frames = new ArrayList<>();
        ((LibraryVisualSceneParser) parsers.get(State.library_visual_scenes))
                .scenes.peek().skeletons.values()
                .forEach(skeleton -> animationsParser.animations.values()
                        .forEach(animation -> frames.addAll(animation.calculateAnimation(skeleton))));
        return frames;
    }

}

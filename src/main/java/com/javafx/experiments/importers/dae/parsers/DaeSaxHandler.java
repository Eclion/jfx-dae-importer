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
import javafx.scene.paint.Material;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Eclion
 */
public final class DaeSaxHandler extends DefaultHandler {

    private DefaultHandler subHandler;

    private Map<State, DefaultHandler> parsers = new HashMap<>();

    private Camera firstCamera;

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

    private static State state(final String name) {
        try {
            return State.valueOf(name);
        } catch (Exception e) {
            return State.UNKNOWN;
        }
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
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
                if (subHandler != null)
                    subHandler.startElement(uri, localName, qName, attributes);
                break;
        }
    }

    private void setParser(final State state, final DefaultHandler parser) {
        parsers.put(state, parser);
        subHandler = parsers.get(state);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        if (subHandler != null) subHandler.endElement(uri, localName, qName);
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
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

    public void buildScene(final Group rootNode) {
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
        scene.meshNodes.values().stream().map(this::getMeshes).forEach(rootNode.getChildren()::addAll);
        scene.controllerNodes.values().stream().map(this::getControllers).forEach(rootNode.getChildren()::addAll);
    }

    private Camera getCamera(final DaeNode node) {
        if (!parsers.containsKey(State.library_cameras)) return null;
        Camera camera = ((LibraryCamerasParser) parsers.get(State.library_cameras)).cameras.get(node.instanceCameraId);
        camera.setId(node.name);
        camera.getTransforms().addAll(node.transforms);
        return camera;
    }

    private List<MeshView> getMeshes(final DaeNode node) {
        final LibraryGeometriesParser geometriesParser = (LibraryGeometriesParser) parsers.get(State.library_geometries);
        if (geometriesParser == null) return new ArrayList<>();

        final List<TriangleMesh> meshes = geometriesParser.getMeshes(node.instanceGeometryId);
        final List<Material> materials = getMaterials(node.instanceGeometryId);

        final List<MeshView> views = new ArrayList<>();

        for (int i = 0; i < meshes.size(); i++) {
            final MeshView meshView = new MeshView(meshes.get(i));
            meshView.setId(node.name);
            if (i < materials.size()) meshView.setMaterial(materials.get(i));
            views.add(meshView);
        }

        return views;

    }

    private List<MeshView> getControllers(final DaeNode node) {
        final LibraryGeometriesParser geometriesParser = (LibraryGeometriesParser) parsers.get(State.library_geometries);
        final LibraryControllerParser controllerParser = (LibraryControllerParser) parsers.get(State.library_controllers);
        final LibraryVisualSceneParser visualSceneParser = (LibraryVisualSceneParser) parsers.get(State.library_visual_scenes);

        if (controllerParser == null || visualSceneParser == null
                || geometriesParser == null) return new ArrayList<>();

        final DaeController controller = controllerParser.
                controllers.get(node.instanceControllerId);

        final DaeSkeleton skeleton =
                ((LibraryVisualSceneParser) parsers.get(State.library_visual_scenes))
                        .scenes.get(0).skeletons.get(controller.getName());

        final String[] bones = skeleton.joints.keySet().toArray(new String[]{});
        final Affine[] bindTransforms = new Affine[bones.length];
        final Joint[] joints = new Joint[bones.length];

        for (int i = 0; i < bones.length; i++) {
            bindTransforms[i] = controller.bindPoses.get(i);
            joints[i] = skeleton.joints.get(bones[i]);
        }

        final List<TriangleMesh> meshes = geometriesParser.getMeshes(controller.skinId);

        final List<Material> materials = getMaterials(controller.skinId);

        final List<MeshView> views = new ArrayList<>();

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

            meshView.setId(node.name);
            if (i < materials.size()) meshView.setMaterial(materials.get(i));
            views.add(meshView);
        }

        return views;
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

    private List<Material> getMaterials(String meshId) {
        final LibraryGeometriesParser geometriesParser = (LibraryGeometriesParser) parsers.get(State.library_geometries);
        final LibraryMaterialsParser materialsParser = (LibraryMaterialsParser) parsers.get(State.library_materials);
        final LibraryEffectsParser effectsParser = (LibraryEffectsParser) parsers.get(State.library_effects);

        final List<Material> materials;
        if (materialsParser != null && effectsParser != null) {
            materials = geometriesParser.
                    getMaterialIds(meshId).
                    stream().
                    map(materialsParser::getEffectId).
                    map(effectsParser::getEffectMaterial)
                    .collect(Collectors.toList());
        } else {
            materials = new ArrayList<>();
        }
        return materials;
    }
}

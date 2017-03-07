package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.animation.SkinningMeshTimer;
import com.javafx.experiments.importers.dae.structures.*;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Eclion
 */
public final class DaeSaxHandlerV2 extends DefaultHandler {

    private DefaultHandler subHandler;

    private HashMap<State, AbstractParser> parsers = new HashMap<>();
    private HashMap<State, Consumer<State>> stateConsumers = new HashMap<>();

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

    public DaeSaxHandlerV2(final String fileUrl) {
        stateConsumers.put(State.asset, state -> setParser(state, new AssetParserV2()));
        stateConsumers.put(State.scene, state -> setParser(state, new SceneParserV2()));
        stateConsumers.put(State.library_animations, state -> setParser(state, new LibraryAnimationsParserV2()));
        stateConsumers.put(State.library_cameras, state -> setParser(state, new LibraryCamerasParserV2()));
        stateConsumers.put(State.library_controllers, state -> setParser(state, new LibraryControllerParserV2()));
        stateConsumers.put(State.library_effects, state -> setParser(state, new LibraryEffectsParserV2()));
        stateConsumers.put(State.library_geometries, state -> setParser(state, new LibraryGeometriesParserV2()));
        stateConsumers.put(State.library_images, state -> setParser(state, new LibraryImagesParserV2(fileUrl)));
        stateConsumers.put(State.library_lights, state -> setParser(state, new LibraryLightsParserV2()));
        stateConsumers.put(State.library_materials, state -> setParser(state, new LibraryMaterialsParserV2()));
        stateConsumers.put(State.library_visual_scenes, state -> setParser(state, new LibraryVisualSceneParserV2()));
    }

    private static State state(final String name) {
        try {
            return State.valueOf(name);
        } catch (Exception e) {
            return State.UNKNOWN;
        }
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
        final State currentState = state(qName);
        stateConsumers.getOrDefault(currentState, state -> {
            if (subHandler != null)
                try {
                    subHandler.startElement(uri, localName, qName, attributes);
                } catch (SAXException e) {
                    e.printStackTrace();
                }
        }).accept(currentState);
    }

    private void setParser(final State state, final AbstractParser parser) {
        parsers.put(state, parser);
        subHandler = parsers.get(state).handler;
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
                ? ((LibraryCamerasParserV2) parsers.get(State.library_cameras)).firstCamera
                : null;
    }

    public double getFirstCameraAspectRatio() {
        return parsers.containsKey(State.library_cameras)
                ? ((LibraryCamerasParserV2) parsers.get(State.library_cameras)).firstCameraAspectRatio
                : 4.0 / 3.0;
    }

    public void buildScene(final Group rootNode) {
        final LibraryVisualSceneParserV2 visualSceneParser = (LibraryVisualSceneParserV2) parsers.get(State.library_visual_scenes);
        if (visualSceneParser == null) return;
        final DaeScene scene = visualSceneParser.scenes.peek();
        final String upAxis = parsers.containsKey(State.asset)
                ? ((AssetParserV2) parsers.get(State.asset)).upAxis
                : "Z_UP";
        if ("Z_UP".equals(upAxis)) {
            rootNode.getTransforms().add(new Rotate(90, 0, 0, 0, Rotate.X_AXIS));
        } else if ("Y_UP".equals(upAxis)) {
            rootNode.getTransforms().add(new Rotate(180, 0, 0, 0, Rotate.X_AXIS));
        }
        rootNode.setId(scene.id);

        final LibraryImagesParserV2 imagesParser = (LibraryImagesParserV2) parsers.get(State.library_images);
        final LibraryEffectsParserV2 effectsParser = (LibraryEffectsParserV2) parsers.get(State.library_effects);
        if (imagesParser != null && effectsParser != null) {
            effectsParser.buildEffects(imagesParser);
        }

        scene.meshNodes.values().stream().map(this::getMeshes).forEach(rootNode.getChildren()::addAll);
        scene.controllerNodes.values().stream().map(this::getControllers).forEach(rootNode.getChildren()::addAll);
    }

    private Camera getCamera(final DaeNode node) {
        final LibraryCamerasParserV2 camerasParser = ((LibraryCamerasParserV2) parsers.get(State.library_cameras));
        if (camerasParser == null) return null;
        final Camera camera = camerasParser.cameras.get(node.instanceCameraId);
        camera.setId(node.name);
        camera.getTransforms().addAll(node.transforms);
        return camera;
    }

    private List<MeshView> getMeshes(final DaeNode node) {
        final LibraryGeometriesParserV2 geometriesParser = (LibraryGeometriesParserV2) parsers.get(State.library_geometries);
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
        final LibraryGeometriesParserV2 geometriesParser = (LibraryGeometriesParserV2) parsers.get(State.library_geometries);
        final LibraryControllerParserV2 controllerParser = (LibraryControllerParserV2) parsers.get(State.library_controllers);
        final LibraryVisualSceneParserV2 visualSceneParser = (LibraryVisualSceneParserV2) parsers.get(State.library_visual_scenes);

        if (controllerParser == null || visualSceneParser == null
                || geometriesParser == null) return new ArrayList<>();

        final DaeController controller = controllerParser.
                controllers.get(node.instanceControllerId);

        final DaeSkeleton skeleton =
                ((LibraryVisualSceneParserV2) parsers.get(State.library_visual_scenes))
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

    public HashMap<String, List<KeyFrame>> getKeyFramesMap() {
        final LibraryAnimationsParserV2 animationsParser = (LibraryAnimationsParserV2) parsers.get(State.library_animations);
        final LibraryVisualSceneParserV2 visualSceneParser = (LibraryVisualSceneParserV2) parsers.get(State.library_visual_scenes);
        if (animationsParser == null || visualSceneParser == null) return new HashMap<>();

        final HashMap<String, List<KeyFrame>> frames = new HashMap<>();
        visualSceneParser.scenes.peek().skeletons.values()
                .forEach(skeleton -> animationsParser.animations.values()
                        .forEach(animation -> frames.put(animation.id, animation.calculateAnimation(skeleton)))
                );
        return frames;
    }

    private List<Material> getMaterials(String meshId) {
        final LibraryGeometriesParserV2 geometriesParser = (LibraryGeometriesParserV2) parsers.get(State.library_geometries);
        final LibraryMaterialsParserV2 materialsParser = (LibraryMaterialsParserV2) parsers.get(State.library_materials);
        final LibraryEffectsParserV2 effectsParser = (LibraryEffectsParserV2) parsers.get(State.library_effects);

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

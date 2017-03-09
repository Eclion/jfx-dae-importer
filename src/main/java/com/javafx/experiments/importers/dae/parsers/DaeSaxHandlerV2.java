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

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Eclion
 */
public final class DaeSaxHandlerV2 extends AbstractParser {

    private final static String ASSET_TAG = "asset";
    private final static String SCENE_TAG = "scene";
    private final static String LIBRARY_ANIMATIONS_TAG = "library_animations";
    private final static String LIBRARY_CAMERAS_TAG = "library_cameras";
    private final static String LIBRARY_CONTROLLERS_TAG = "library_controllers";
    private final static String LIBRARY_EFFECTS_TAG = "library_effects";
    private final static String LIBRARY_GEOMETRIES_TAG = "library_geometries";
    private final static String LIBRARY_IMAGES_TAG = "library_images";
    private final static String LIBRARY_LIGHTS_TAG = "library_lights";
    private final static String LIBRARY_MATERIALS_TAG = "library_materials";
    private final static String LIBRARY_VISUAL_SCENES_TAG = "library_visual_scenes";

    private AbstractParser subHandler;

    private HashMap<String, AbstractParser> parsers = new HashMap<>();

    private Camera firstCamera;

    public DaeSaxHandlerV2(final String fileUrl) {
        final HashMap<String, Consumer<StartElement>> startElementConsumer = new HashMap<>();
        startElementConsumer.put("*", startElement -> {
            if (subHandler != null) {
                subHandler.getLibraryHandler().startElement(startElement);
            }
        });

        startElementConsumer.put(ASSET_TAG, startElement -> setParser(startElement.qName, new AssetParserV2()));
        startElementConsumer.put(SCENE_TAG, startElement -> setParser(startElement.qName, new SceneParserV2()));
        startElementConsumer.put(LIBRARY_ANIMATIONS_TAG, startElement -> setParser(startElement.qName, new LibraryAnimationsParserV2()));
        startElementConsumer.put(LIBRARY_CAMERAS_TAG, startElement -> setParser(startElement.qName, new LibraryCamerasParserV2()));
        startElementConsumer.put(LIBRARY_CONTROLLERS_TAG, startElement -> setParser(startElement.qName, new LibraryControllerParserV2()));
        startElementConsumer.put(LIBRARY_EFFECTS_TAG, startElement -> setParser(startElement.qName, new LibraryEffectsParserV2()));
        startElementConsumer.put(LIBRARY_GEOMETRIES_TAG, startElement -> setParser(startElement.qName, new LibraryGeometriesParserV2()));
        startElementConsumer.put(LIBRARY_IMAGES_TAG, startElement -> setParser(startElement.qName, new LibraryImagesParserV2(fileUrl)));
        startElementConsumer.put(LIBRARY_LIGHTS_TAG, startElement -> setParser(startElement.qName, new LibraryLightsParserV2()));
        startElementConsumer.put(LIBRARY_MATERIALS_TAG, startElement -> setParser(startElement.qName, new LibraryMaterialsParserV2()));
        startElementConsumer.put(LIBRARY_VISUAL_SCENES_TAG, startElement -> setParser(startElement.qName, new LibraryVisualSceneParserV2()));

        final HashMap<String, Consumer<LibraryHandler.EndElement>> endElementConsumer = new HashMap<>();

        endElementConsumer.put("*", endElement -> subHandler.getLibraryHandler().endElement(endElement));

        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }

    private void setParser(final String tag, final AbstractParser parser) {
        parsers.put(tag, parser);
        subHandler = parsers.get(tag);
    }

    public Camera getFirstCamera() {
        return (parsers.containsKey(LIBRARY_CAMERAS_TAG))
                ? ((LibraryCamerasParserV2) parsers.get(LIBRARY_CAMERAS_TAG)).firstCamera
                : null;
    }

    public double getFirstCameraAspectRatio() {
        return parsers.containsKey(LIBRARY_CAMERAS_TAG)
                ? ((LibraryCamerasParserV2) parsers.get(LIBRARY_CAMERAS_TAG)).firstCameraAspectRatio
                : 4.0 / 3.0;
    }

    public void buildScene(final Group rootNode) {
        final LibraryVisualSceneParserV2 visualSceneParser = (LibraryVisualSceneParserV2) parsers.get(LIBRARY_VISUAL_SCENES_TAG);
        if (visualSceneParser == null) return;
        final DaeScene scene = visualSceneParser.scenes.peek();
        final String upAxis = parsers.containsKey(ASSET_TAG)
                ? ((AssetParserV2) parsers.get(ASSET_TAG)).upAxis
                : "Z_UP";
        if ("Z_UP".equals(upAxis)) {
            rootNode.getTransforms().add(new Rotate(90, 0, 0, 0, Rotate.X_AXIS));
        } else if ("Y_UP".equals(upAxis)) {
            rootNode.getTransforms().add(new Rotate(180, 0, 0, 0, Rotate.X_AXIS));
        }
        rootNode.setId(scene.id);

        final LibraryImagesParserV2 imagesParser = (LibraryImagesParserV2) parsers.get(LIBRARY_IMAGES_TAG);
        final LibraryEffectsParserV2 effectsParser = (LibraryEffectsParserV2) parsers.get(LIBRARY_EFFECTS_TAG);
        if (imagesParser != null && effectsParser != null) {
            effectsParser.buildEffects(imagesParser);
        }

        scene.meshNodes.values().stream().map(this::getMeshes).forEach(rootNode.getChildren()::addAll);
        scene.controllerNodes.values().stream().map(this::getControllers).forEach(rootNode.getChildren()::addAll);
    }

    private Camera getCamera(final DaeNode node) {
        final LibraryCamerasParserV2 camerasParser = ((LibraryCamerasParserV2) parsers.get(LIBRARY_CAMERAS_TAG));
        if (camerasParser == null) return null;
        final Camera camera = camerasParser.cameras.get(node.instanceCameraId);
        camera.setId(node.name);
        camera.getTransforms().addAll(node.transforms);
        return camera;
    }

    private List<MeshView> getMeshes(final DaeNode node) {
        final LibraryGeometriesParserV2 geometriesParser = (LibraryGeometriesParserV2) parsers.get(LIBRARY_GEOMETRIES_TAG);
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
        final LibraryGeometriesParserV2 geometriesParser = (LibraryGeometriesParserV2) parsers.get(LIBRARY_GEOMETRIES_TAG);
        final LibraryControllerParserV2 controllerParser = (LibraryControllerParserV2) parsers.get(LIBRARY_CONTROLLERS_TAG);
        final LibraryVisualSceneParserV2 visualSceneParser = (LibraryVisualSceneParserV2) parsers.get(LIBRARY_VISUAL_SCENES_TAG);

        if (controllerParser == null || visualSceneParser == null
                || geometriesParser == null) return new ArrayList<>();

        final DaeController controller = controllerParser.
                controllers.get(node.instanceControllerId);

        final DaeSkeleton skeleton = visualSceneParser.scenes.get(0).skeletons.get(controller.getName());

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
        final LibraryAnimationsParserV2 animationsParser = (LibraryAnimationsParserV2) parsers.get(LIBRARY_ANIMATIONS_TAG);
        final LibraryVisualSceneParserV2 visualSceneParser = (LibraryVisualSceneParserV2) parsers.get(LIBRARY_VISUAL_SCENES_TAG);
        if (animationsParser == null || visualSceneParser == null) return new HashMap<>();

        final HashMap<String, List<KeyFrame>> frames = new HashMap<>();
        visualSceneParser.scenes.peek().skeletons.values()
                .forEach(skeleton -> animationsParser.animations.values()
                        .forEach(animation -> frames.put(animation.id, animation.calculateAnimation(skeleton)))
                );
        return frames;
    }

    private List<Material> getMaterials(String meshId) {
        final LibraryGeometriesParserV2 geometriesParser = (LibraryGeometriesParserV2) parsers.get(LIBRARY_GEOMETRIES_TAG);
        final LibraryMaterialsParserV2 materialsParser = (LibraryMaterialsParserV2) parsers.get(LIBRARY_MATERIALS_TAG);
        final LibraryEffectsParserV2 effectsParser = (LibraryEffectsParserV2) parsers.get(LIBRARY_EFFECTS_TAG);

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

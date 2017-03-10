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

/**
 * @author Eclion
 */
public final class DaeSaxHandler extends AbstractParser {

    private static final String ASSET_TAG = "asset";
    private static final String SCENE_TAG = "scene";
    private static final String LIBRARY_ANIMATIONS_TAG = "library_animations";
    private static final String LIBRARY_CAMERAS_TAG = "library_cameras";
    private static final String LIBRARY_CONTROLLERS_TAG = "library_controllers";
    private static final String LIBRARY_EFFECTS_TAG = "library_effects";
    private static final String LIBRARY_GEOMETRIES_TAG = "library_geometries";
    private static final String LIBRARY_IMAGES_TAG = "library_images";
    private static final String LIBRARY_LIGHTS_TAG = "library_lights";
    private static final String LIBRARY_MATERIALS_TAG = "library_materials";
    private static final String LIBRARY_VISUAL_SCENES_TAG = "library_visual_scenes";

    private AbstractParser subHandler;

    private final Map<String, AbstractParser> parsers = new HashMap<>();

    private Camera firstCamera;

    public DaeSaxHandler(final String fileUrl) {
        addStartElementBiConsumer("*", (qName, attributes) -> {
            if (subHandler != null) {
                subHandler.getLibraryHandler().startElement(qName, attributes);
            }
        });
        addStartElementBiConsumer(ASSET_TAG, (qName, attributes) -> setParser(qName, new AssetParser()));
        addStartElementBiConsumer(SCENE_TAG, (qName, attributes) -> setParser(qName, new SceneParser()));
        addStartElementBiConsumer(LIBRARY_ANIMATIONS_TAG, (qName, attributes) -> setParser(qName, new LibraryAnimationsParser()));
        addStartElementBiConsumer(LIBRARY_CAMERAS_TAG, (qName, attributes) -> setParser(qName, new LibraryCamerasParser()));
        addStartElementBiConsumer(LIBRARY_CONTROLLERS_TAG, (qName, attributes) -> setParser(qName, new LibraryControllerParser()));
        addStartElementBiConsumer(LIBRARY_EFFECTS_TAG, (qName, attributes) -> setParser(qName, new LibraryEffectsParser()));
        addStartElementBiConsumer(LIBRARY_GEOMETRIES_TAG, (qName, attributes) -> setParser(qName, new LibraryGeometriesParser()));
        addStartElementBiConsumer(LIBRARY_IMAGES_TAG, (qName, attributes) -> setParser(qName, new LibraryImagesParser(fileUrl)));
        addStartElementBiConsumer(LIBRARY_LIGHTS_TAG, (qName, attributes) -> setParser(qName, new LibraryLightsParser()));
        addStartElementBiConsumer(LIBRARY_MATERIALS_TAG, (qName, attributes) -> setParser(qName, new LibraryMaterialsParser()));
        addStartElementBiConsumer(LIBRARY_VISUAL_SCENES_TAG, (qName, attributes) -> setParser(qName, new LibraryVisualSceneParser()));

        addEndElementBiConsumer("*", (qName, content) -> subHandler.getLibraryHandler().endElement(qName, content));
    }

    private void setParser(final String tag, final AbstractParser parser) {
        parsers.put(tag, parser);
        subHandler = parsers.get(tag);
    }

    public Camera getFirstCamera() {
        return (parsers.containsKey(LIBRARY_CAMERAS_TAG))
                ? ((LibraryCamerasParser) parsers.get(LIBRARY_CAMERAS_TAG)).firstCamera
                : null;
    }

    public double getFirstCameraAspectRatio() {
        return parsers.containsKey(LIBRARY_CAMERAS_TAG)
                ? ((LibraryCamerasParser) parsers.get(LIBRARY_CAMERAS_TAG)).firstCameraAspectRatio
                : 4.0 / 3.0;
    }

    public void buildScene(final Group rootNode) {
        final LibraryVisualSceneParser visualSceneParser = (LibraryVisualSceneParser) parsers.get(LIBRARY_VISUAL_SCENES_TAG);
        if (visualSceneParser == null) return;
        final DaeScene scene = visualSceneParser.scenes.peek();
        final String upAxis = parsers.containsKey(ASSET_TAG)
                ? ((AssetParser) parsers.get(ASSET_TAG)).upAxis
                : "Z_UP";
        if ("Z_UP".equals(upAxis)) {
            rootNode.getTransforms().add(new Rotate(90, 0, 0, 0, Rotate.X_AXIS));
        } else if ("Y_UP".equals(upAxis)) {
            rootNode.getTransforms().add(new Rotate(180, 0, 0, 0, Rotate.X_AXIS));
        }
        rootNode.setId(scene.id);

        final LibraryImagesParser imagesParser = (LibraryImagesParser) parsers.get(LIBRARY_IMAGES_TAG);
        final LibraryEffectsParser effectsParser = (LibraryEffectsParser) parsers.get(LIBRARY_EFFECTS_TAG);
        if (imagesParser != null && effectsParser != null) {
            effectsParser.buildEffects(imagesParser);
        }

        scene.meshNodes.values().stream().map(this::getMeshes).forEach(rootNode.getChildren()::addAll);
        scene.controllerNodes.values().stream().map(this::getControllers).forEach(rootNode.getChildren()::addAll);
    }

    private Camera getCamera(final DaeNode node) {
        final LibraryCamerasParser camerasParser = (LibraryCamerasParser) parsers.get(LIBRARY_CAMERAS_TAG);
        if (camerasParser == null) return null;
        final Camera camera = camerasParser.cameras.get(node.instanceCameraId);
        camera.setId(node.name);
        camera.getTransforms().addAll(node.transforms);
        return camera;
    }

    private List<MeshView> getMeshes(final DaeNode node) {
        final LibraryGeometriesParser geometriesParser = (LibraryGeometriesParser) parsers.get(LIBRARY_GEOMETRIES_TAG);
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
        final LibraryGeometriesParser geometriesParser = (LibraryGeometriesParser) parsers.get(LIBRARY_GEOMETRIES_TAG);
        final LibraryControllerParser controllerParser = (LibraryControllerParser) parsers.get(LIBRARY_CONTROLLERS_TAG);
        final LibraryVisualSceneParser visualSceneParser = (LibraryVisualSceneParser) parsers.get(LIBRARY_VISUAL_SCENES_TAG);

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

    public Map<String, List<KeyFrame>> getKeyFramesMap() {
        final LibraryAnimationsParser animationsParser = (LibraryAnimationsParser) parsers.get(LIBRARY_ANIMATIONS_TAG);
        final LibraryVisualSceneParser visualSceneParser = (LibraryVisualSceneParser) parsers.get(LIBRARY_VISUAL_SCENES_TAG);
        if (animationsParser == null || visualSceneParser == null) return new HashMap<>();

        final Map<String, List<KeyFrame>> frames = new HashMap<>();
        visualSceneParser.scenes.peek().skeletons.values().
                forEach(skeleton -> animationsParser.animations.values().
                        forEach(animation -> frames.put(animation.id, animation.calculateAnimation(skeleton)))
                );
        return frames;
    }

    private List<Material> getMaterials(final String meshId) {
        final LibraryGeometriesParser geometriesParser = (LibraryGeometriesParser) parsers.get(LIBRARY_GEOMETRIES_TAG);
        final LibraryMaterialsParser materialsParser = (LibraryMaterialsParser) parsers.get(LIBRARY_MATERIALS_TAG);
        final LibraryEffectsParser effectsParser = (LibraryEffectsParser) parsers.get(LIBRARY_EFFECTS_TAG);

        final List<Material> materials = new ArrayList<>();
        if (materialsParser != null && effectsParser != null) {
            geometriesParser.
                    getMaterialIds(meshId).
                    stream().
                    map(materialsParser::getEffectId).
                    map(effectsParser::getEffectMaterial).
                    forEach(materials::add);
        }
        return materials;
    }
}

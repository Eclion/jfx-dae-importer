package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.structures.*;
import javafx.animation.KeyFrame;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import org.xml.sax.Attributes;

import java.util.*;

/**
 * @author Eclion
 */
public final class DaeSaxHandler extends AbstractParser {

    private static final String ALL = "*";
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

    public DaeSaxHandler(final String fileUrl) {
        addStartElementBiConsumer(ALL, this::delegateElement);
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

        addEndElementBiConsumer(ALL, (qName, content) -> subHandler.getLibraryHandler().endElement(qName, content));
    }

    private void delegateElement(final String qName, final Attributes attributes) {
        Optional.ofNullable(subHandler).
                ifPresent(handler -> handler.getLibraryHandler().startElement(qName, attributes));
    }

    private void setParser(final String tag, final AbstractParser parser) {
        parsers.put(tag, parser);
        subHandler = parsers.get(tag);
    }

    public Camera getFirstCamera() {
        return parsers.containsKey(LIBRARY_CAMERAS_TAG)
                ? ((LibraryCamerasParser) parsers.get(LIBRARY_CAMERAS_TAG)).firstCamera
                : null;
    }

    public double getFirstCameraAspectRatio() {
        return parsers.containsKey(LIBRARY_CAMERAS_TAG)
                ? ((LibraryCamerasParser) parsers.get(LIBRARY_CAMERAS_TAG)).firstCameraAspectRatio
                : 4.0 / 3.0;
    }

    public Group buildScene() {
        final LibraryVisualSceneParser visualSceneParser = (LibraryVisualSceneParser) parsers.get(LIBRARY_VISUAL_SCENES_TAG);

        if (visualSceneParser == null || visualSceneParser.scenes.isEmpty()) {
            return new Group();
        }

        final DaeScene rootNode = visualSceneParser.scenes.peek();

        final String upAxis = parsers.containsKey(ASSET_TAG)
                ? ((AssetParser) parsers.get(ASSET_TAG)).getUpAxis()
                : "Z_UP";

        if ("Z_UP".equals(upAxis)) {
            rootNode.getTransforms().add(new Rotate(90, 0, 0, 0, Rotate.X_AXIS));
        } else if ("Y_UP".equals(upAxis)) {
            rootNode.getTransforms().add(new Rotate(180, 0, 0, 0, Rotate.X_AXIS));
        }

        final DaeBuildHelper buildHelper = new DaeBuildHelper();

        addCamerasToBuildHelper(buildHelper);
        addControllersToBuildHelper(buildHelper);
        addGeometriesToBuildHelper(buildHelper);
        addMaterialsToBuildHelper(buildHelper);

        buildHelper.withSkeletons(rootNode.skeletons);

        rootNode.build(buildHelper);

        return rootNode;
    }

    private void addCamerasToBuildHelper(final DaeBuildHelper buildHelper) {
        Optional.ofNullable((LibraryCamerasParser) parsers.get(LIBRARY_CAMERAS_TAG)).
                ifPresent(camerasParser -> buildHelper.withCameras(camerasParser.cameras));
    }

    private void addControllersToBuildHelper(final DaeBuildHelper buildHelper) {
        Optional.ofNullable((LibraryControllerParser) parsers.get(LIBRARY_CONTROLLERS_TAG)).
                ifPresent(controllerParser -> buildHelper.withControllers(controllerParser.controllers));
    }

    private void addGeometriesToBuildHelper(final DaeBuildHelper buildHelper) {
        Optional.ofNullable((LibraryGeometriesParser) parsers.get(LIBRARY_GEOMETRIES_TAG)).
                ifPresent(geometriesParser ->
                        buildHelper.withMeshes(geometriesParser.meshes).
                                withMeshMaterialIds(geometriesParser.materials));
    }

    private void addMaterialsToBuildHelper(final DaeBuildHelper buildHelper) {
        final LibraryEffectsParser effectsParser = (LibraryEffectsParser) parsers.get(LIBRARY_EFFECTS_TAG);
        final LibraryMaterialsParser materialsParser = (LibraryMaterialsParser) parsers.get(LIBRARY_MATERIALS_TAG);
        final LibraryImagesParser imagesParser = (LibraryImagesParser) parsers.get(LIBRARY_IMAGES_TAG);

        if (imagesParser != null && effectsParser != null) {
            effectsParser.buildEffects(imagesParser.images);
        }

        if (materialsParser != null && effectsParser != null) {
            buildHelper.withMaterialMap(
                    mergeMaps(
                            materialsParser.materialIdToEffectIdMap,
                            effectsParser.effectIdToMaterialMap));
        }

    }

    public Map<String, List<KeyFrame>> getKeyFramesMap() {
        final LibraryAnimationsParser animationsParser = (LibraryAnimationsParser) parsers.get(LIBRARY_ANIMATIONS_TAG);
        final LibraryVisualSceneParser visualSceneParser = (LibraryVisualSceneParser) parsers.get(LIBRARY_VISUAL_SCENES_TAG);
        if (animationsParser == null || visualSceneParser == null) {
            return new HashMap<>();
        }

        final Map<String, List<KeyFrame>> frames = new HashMap<>();
        visualSceneParser.scenes.peek().skeletons.values().
                forEach(skeleton -> animationsParser.animations.values().
                        forEach(animation -> frames.put(animation.id, animation.calculateAnimation(skeleton)))
                );
        return frames;
    }

    private <A, B, C> Map<A, C> mergeMaps(final Map<A, B> abMap, final Map<B, C> bcMap) {
        final Map<A, C> acMap = new HashMap<>();
        abMap.forEach((key, value) -> {
            if (bcMap.containsKey(value)) acMap.put(key, bcMap.get(value));
        });
        return acMap;
    }
}

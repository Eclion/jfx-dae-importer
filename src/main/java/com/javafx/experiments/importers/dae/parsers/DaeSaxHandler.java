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

    public Group buildScene() {
        final LibraryVisualSceneParser visualSceneParser = (LibraryVisualSceneParser) parsers.get(LIBRARY_VISUAL_SCENES_TAG);
        final LibraryImagesParser imagesParser = (LibraryImagesParser) parsers.get(LIBRARY_IMAGES_TAG);
        final LibraryEffectsParser effectsParser = (LibraryEffectsParser) parsers.get(LIBRARY_EFFECTS_TAG);
        final LibraryGeometriesParser geometriesParser = (LibraryGeometriesParser) parsers.get(LIBRARY_GEOMETRIES_TAG);
        final LibraryControllerParser controllerParser = (LibraryControllerParser) parsers.get(LIBRARY_CONTROLLERS_TAG);
        final LibraryMaterialsParser materialsParser = (LibraryMaterialsParser) parsers.get(LIBRARY_MATERIALS_TAG);
        final LibraryCamerasParser camerasParser = (LibraryCamerasParser) parsers.get(LIBRARY_CAMERAS_TAG);

        if (visualSceneParser == null) return new Group();

        final DaeScene rootNode = visualSceneParser.scenes.peek();

        final String upAxis = parsers.containsKey(ASSET_TAG)
                ? ((AssetParser) parsers.get(ASSET_TAG)).upAxis
                : "Z_UP";

        if ("Z_UP".equals(upAxis)) {
            rootNode.getTransforms().add(new Rotate(90, 0, 0, 0, Rotate.X_AXIS));
        } else if ("Y_UP".equals(upAxis)) {
            rootNode.getTransforms().add(new Rotate(180, 0, 0, 0, Rotate.X_AXIS));
        }

        //TODO
        if (imagesParser != null && effectsParser != null) {
            effectsParser.buildEffects(imagesParser.images);
        }

        DaeBuildHelper buildHelper = new DaeBuildHelper();

        if (materialsParser != null && effectsParser != null) {
            buildHelper = buildHelper.withMaterialMap(
                    mergeMaps(
                            materialsParser.materialIdToEffectIdMap,
                            effectsParser.effectIdToMaterialMap));
        }

        if (geometriesParser != null) {
            buildHelper = buildHelper.withMeshes(geometriesParser.meshes)
                    .withMeshMaterialIds(geometriesParser.materials);
        }

        if (controllerParser != null) {
            buildHelper = buildHelper.withControllers(controllerParser.controllers);
        }

        if (camerasParser != null) {
            buildHelper = buildHelper.withCameras(camerasParser.cameras);
        }

        buildHelper = buildHelper.withSkeletons(rootNode.skeletons);

        rootNode.build(buildHelper);

        return rootNode;
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

    private <A, B, C> Map<A, C> mergeMaps(Map<A, B> abMap, Map<B, C> bcMap) {
        Map<A, C> acMap = new HashMap<>();
        abMap.forEach((key, value) -> {
            if (bcMap.containsKey(value)) acMap.put(key, bcMap.get(value));
        });
        return acMap;
    }
}

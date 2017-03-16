package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.structures.DaeAnimation;
import com.javafx.experiments.importers.dae.structures.DaeController;
import com.javafx.experiments.importers.dae.structures.DaeScene;
import javafx.scene.PerspectiveCamera;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;


import static org.junit.Assert.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Eclion
 *         TODO: improve tests, implement a system test
 */
public final class ParserTests {

    private void executeParsing(final String testResource, final DefaultHandler parser) throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
        final File resource = new File(this.getClass().getResource(testResource).toURI());
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        final SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(resource, parser);
    }

    @Test
    public void parseLibraryEffects() throws Exception {
        final LibraryEffectsParser effectsParser = new LibraryEffectsParser();
        final LibraryHandler parser = effectsParser.getLibraryHandler();
        executeParsing("effects_1.xml", parser);

        effectsParser.buildEffects(new HashMap<>());

        final PhongMaterial actualMaterial = (PhongMaterial) effectsParser.getEffectMaterial("shine-fx");

        final Color expectedSpecularColor = new Color(0.49586, 0.49586, 0.49586, 1);

        assertEquals(expectedSpecularColor, actualMaterial.getSpecularColor());
    }

    @Test
    public void parseAssets() throws Exception {
        final AssetParser assetParser = new AssetParser();
        final LibraryHandler parser = assetParser.getLibraryHandler();
        executeParsing("asset_1.xml", parser);

        assertEquals("Z_UP", assetParser.upAxis);
        assertEquals("meter", assetParser.unit);
        assertEquals(1.0f, assetParser.scale, 0.0f);
        assertEquals("Blender User", assetParser.author);
        assertEquals("Blender 2.78.0", assetParser.authoringTool);
    }

    @Test
    public void parseAnimations() throws Exception {
        final LibraryAnimationsParser animationsParser = new LibraryAnimationsParser();
        final LibraryHandler parser = animationsParser.getLibraryHandler();
        executeParsing("animations_1.xml", parser);

        final DaeAnimation actualArmatureBoneAnimation = animationsParser.animations.get("Armature_Bone_pose_matrix");

        assertEquals(2, animationsParser.animations.size());
        assertNotNull(actualArmatureBoneAnimation);
        assertEquals("Armature_Bone_pose_matrix", actualArmatureBoneAnimation.id);
        assertArrayEquals(new float[]{0.0f, 0.04166662f, 0.4166666f, 0.8333333f}, actualArmatureBoneAnimation.input, 0.0f);
        assertEquals("Bone/transform", actualArmatureBoneAnimation.target);
        assertEquals(64, actualArmatureBoneAnimation.output.length);
    }

    @Test
    public void parseCameras() throws Exception {
        final LibraryCamerasParser camerasParser = new LibraryCamerasParser();
        final LibraryHandler parser = camerasParser.getLibraryHandler();
        executeParsing("cameras_1.xml", parser);

        PerspectiveCamera actualCamera = (PerspectiveCamera) camerasParser.cameras.get("Camera-camera");

        assertEquals(49.13434, actualCamera.getFieldOfView(), 0.0);
        assertEquals(0.1, actualCamera.getNearClip(), 0.0);
        assertEquals(100, actualCamera.getFarClip(), 0.0);
        assertEquals(false, actualCamera.isVerticalFieldOfView());
        assertEquals(true, actualCamera.isFixedEyeAtCameraZero());
    }

    @Test
    public void parseControllers() throws Exception {
        final LibraryControllerParser controllerParser = new LibraryControllerParser();
        final LibraryHandler parser = controllerParser.getLibraryHandler();
        executeParsing("controllers_1.xml", parser);

        DaeController actualController = controllerParser.controllers.get("Armature_Cube-skin");

        assertEquals("Armature", actualController.getName());
        assertEquals("Cube-mesh", actualController.skinId);
        //assertEquals(new Affine(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0), actualController.bindShapeMatrix);
        assertArrayEquals(new String[]{"Bone", "Bone_001"}, actualController.jointNames);
        assertEquals(2, actualController.vertexWeights.length);
        /*assertArrayEquals(new Affine[]{
                new Affine(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0),
                new Affine(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -0.996825098991394, 0.0, -1.0, 0.0, 0.0)
        }, actualController.bindPoses.toArray());*/
    }

    @Test
    public void parseGeometries() throws Exception {
        final LibraryGeometriesParser geometriesParser = new LibraryGeometriesParser();
        final LibraryHandler parser = geometriesParser.getLibraryHandler();
        executeParsing("geometries_1.xml", parser);

        TriangleMesh actualMesh = geometriesParser.getMeshes("Cube-mesh").get(0);
        assertEquals(24, actualMesh.getPoints().size());
        assertEquals(36, actualMesh.getNormals().size());
        assertEquals(2, actualMesh.getTexCoords().size());
        assertEquals(108, actualMesh.getFaces().size());
        assertEquals(VertexFormat.POINT_NORMAL_TEXCOORD, actualMesh.getVertexFormat());

        assertEquals("Material-material", geometriesParser.getMaterialIds("Cube-mesh").get(0));
    }

    @Test
    public void parseLights() throws Exception {
        final LibraryLightsParser lightsParser = new LibraryLightsParser();
        final LibraryHandler parser = lightsParser.getLibraryHandler();
        executeParsing("lights_1.xml", parser);

        fail("Nothing implemented yet for the lights");
    }

    @Test
    public void parseMaterials() throws Exception {
        final LibraryMaterialsParser materialsParser = new LibraryMaterialsParser();
        final LibraryHandler parser = materialsParser.getLibraryHandler();
        executeParsing("materials_1.xml", parser);

        assertEquals("Material-effect", materialsParser.getEffectId("Material-material"));
    }

    @Test
    public void parseVisualScenes() throws Exception {
        final LibraryVisualSceneParser visualSceneParser = new LibraryVisualSceneParser();
        final LibraryHandler parser = visualSceneParser.getLibraryHandler();
        executeParsing("visual_scenes_1.xml", parser);

        DaeScene actualScene = visualSceneParser.scenes.get(0);

        assertEquals("Scene", actualScene.getId());
        assertEquals(1, actualScene.skeletons.size());
    }
}

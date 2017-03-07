package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.structures.DaeAnimation;
import com.javafx.experiments.importers.dae.structures.DaeController;
import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.transform.Affine;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Eclion
 */
public final class ParserTests {


    private void executeParsing(String testResource, DefaultHandler parser) throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
        final File resource = new File(this.getClass().getResource(testResource).toURI());
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        final SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(resource, parser);
    }

    @Test
    public void parseLibraryEffects() throws Exception {

        final LibraryEffectsParser effectsParser = new LibraryEffectsParser();
        executeParsing("effects_1.xml", effectsParser);

        final LibraryImagesParser mockImageParser = mock(LibraryImagesParser.class);
        when(mockImageParser.getImage(any())).thenReturn(null);

        effectsParser.buildEffects(mockImageParser);

        final PhongMaterial actualMaterial = (PhongMaterial) effectsParser.getEffectMaterial("shine-fx");

        final Color expectedSpecularColor = new Color(0.49586, 0.49586, 0.49586, 1);

        assertEquals(expectedSpecularColor, actualMaterial.getSpecularColor());
    }

    @Test
    public void parseAssets() throws Exception {
        //final AssetParserV2 assetParser = new AssetParserV2();
        //final LibraryHandler parser = assetParser.getLibraryHandler();
        //executeParsing(resource, parser);

        final AssetParser assetParser = new AssetParser();
        executeParsing("asset_1.xml", assetParser);

        assertEquals("Z_UP", assetParser.upAxis);
        assertEquals("meter", assetParser.unit);
        assertEquals(1.0f, assetParser.scale, 0.0f);
        assertEquals("Blender User", assetParser.author);
        assertEquals("Blender 2.78.0", assetParser.authoringTool);
    }

    @Test
    public void parseAnimations() throws Exception {
        final LibraryAnimationsParser animationsParser = new LibraryAnimationsParser();
        executeParsing("animations_1.xml", animationsParser);

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
        executeParsing("cameras_1.xml", camerasParser);

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
        executeParsing("controllers_1.xml", controllerParser);

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

    public void parseGeometries() throws Exception {
        final LibraryGeometriesParser geometriesParser = new LibraryGeometriesParser();
        executeParsing("geometries_1.xml", geometriesParser);

        System.out.println();
    }

    public void parseLights() throws Exception {
        final LibraryLightsParser lightsParser = new LibraryLightsParser();
        executeParsing("lights_1.xml", lightsParser);

        System.out.println();
    }

    public void parseMaterials() throws Exception {
        final LibraryMaterialsParser materialsParser = new LibraryMaterialsParser();
        executeParsing("materials_1.xml", materialsParser);

        System.out.println();
    }

    public void parseVisualScenes() throws Exception {
        final LibraryVisualSceneParser visualSceneParser = new LibraryVisualSceneParser();
        executeParsing("visual_scenes_1.xml", visualSceneParser);

        System.out.println();
    }
}

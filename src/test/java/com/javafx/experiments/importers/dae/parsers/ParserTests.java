package com.javafx.experiments.importers.dae.parsers;

import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Eclion
 */
public final class ParserTests {


    private static void executeParsing(File testResource, DefaultHandler parser) throws ParserConfigurationException, SAXException, IOException {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        final SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(testResource, parser);
    }

    @Test
    public void parseLibraryEffects() throws Exception {
        final LibraryEffectsParser effectsParser = new LibraryEffectsParser();
        final File resource = new File(this.getClass().getResource("effects1.xml").toURI());
        executeParsing(resource, effectsParser);

        final LibraryImagesParser mockImageParser = mock(LibraryImagesParser.class);
        when(mockImageParser.getImage(any())).thenReturn(null);

        effectsParser.buildEffects(mockImageParser);

        final PhongMaterial actualMaterial = (PhongMaterial) effectsParser.getEffectMaterial("shine-fx");

        final Color expectedSpecularColor = new Color(0.49586, 0.49586, 0.49586, 1);

        assertEquals(expectedSpecularColor, actualMaterial.getSpecularColor());
    }

    @Test
    public void parseAssets() throws Exception {
        final AssetParserV2 assetParser = new AssetParserV2();
        final LibraryHandler parser = assetParser.libraryHandler;

        //final AssetParser assetParser = new AssetParser();
        final File resource = new File(this.getClass().getResource("asset1.xml").toURI());
        //executeParsing(resource, assetParser);
        executeParsing(resource, parser);

        assertEquals("Z_UP", assetParser.upAxis);
        assertEquals("meter", assetParser.unit);
        assertEquals(1.0f, assetParser.scale, 0.0f);
        assertEquals("Blender User", assetParser.author);
        assertEquals("Blender 2.78.0", assetParser.authoringTool);
    }
}

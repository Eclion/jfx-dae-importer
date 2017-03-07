package com.javafx.experiments.importers.dae.parsers;

import com.javafx.experiments.importers.dae.utils.ParserUtils;
import com.javafx.experiments.utils.TestUtils;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;

/**
 * @author Eclion
 */
public final class ParserTests {

    public static void main(String... args) throws Exception {
        parseLibraryEffects();
        System.out.println(TestUtils.ALL_TEST_PASSED);
    }

    private static void executeParsing(File testResource, DefaultHandler parser) throws ParserConfigurationException, SAXException, IOException {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        final SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(testResource, parser);
    }

    private static void parseLibraryEffects() throws Exception {
        LibraryEffectsParser parser = new LibraryEffectsParser();
        File resource = new File(parser.getClass().getResource("effects1.xml").toURI());
        executeParsing(resource, parser);
        Material actualMaterial = parser.getEffectMaterial("shine-fx");
        PhongMaterial expectedMaterial = new PhongMaterial();
        TestUtils.assertEquals(expectedMaterial, actualMaterial);
    }
}

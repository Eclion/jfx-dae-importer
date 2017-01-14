package com.javafx.experiments.importers.dae.parsers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Eclion
 */
final class AssetParser extends DefaultHandler {
    private static final Logger LOGGER = Logger.getLogger(AssetParser.class.getName());
    private StringBuilder charBuf = new StringBuilder();
    private final Map<String, String> currentId = new HashMap<>();
    private String author;
    private String authoringTool;
    private String unit;
    private float scale;
    String upAxis;

    private enum State {
        UNKNOWN,
        author,
        authoring_tool,
        contributor, //ignored
        created, //ignored
        modified, //ignored
        unit,
        up_axis

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
        currentId.put(qName, attributes.getValue("id"));
        charBuf = new StringBuilder();
        switch (state(qName)) {
            case UNKNOWN:
                LOGGER.log(Level.WARNING, "Unknown element: " + qName);
                break;
            case unit:
                unit = attributes.getValue("name");
                scale = Float.parseFloat(attributes.getValue("meter"));
                break;
            default:
                break;
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        switch (state(qName)) {
            case author:
                author = charBuf.toString().trim();
                break;
            case authoring_tool:
                authoringTool = charBuf.toString().trim();
                break;
            case up_axis:
                upAxis = charBuf.toString().trim();
                break;
            default:
                break;
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        charBuf.append(ch, start, length);
    }
}

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
final class LibraryMaterialsParser extends DefaultHandler {
    private static final Logger LOGGER = Logger.getLogger(LibraryMaterialsParser.class.getSimpleName());
    private StringBuilder charBuf = new StringBuilder();
    private final Map<String, String> currentId = new HashMap<>();

    private final Map<String, String> materialEffectMap = new HashMap<>();

    private enum State {
        UNKNOWN,
        instance_effect,
        material //ignored
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
        this.currentId.put(qName, attributes.getValue("id"));
        this.charBuf = new StringBuilder();
        switch (state(qName)) {
            case UNKNOWN:
                LOGGER.log(Level.WARNING, "Unknown element: " + qName);
                break;
            case instance_effect:
                materialEffectMap.put(currentId.get("material"), attributes.getValue("url"));
                break;
            default:
                break;
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        switch (state(qName)) {
            default:
                break;
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        this.charBuf.append(ch, start, length);
    }
}
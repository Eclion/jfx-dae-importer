package com.javafx.experiments.importers.dae.parsers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Eclion
 */
final class LibraryLightsParser extends AbstractParser {
    //private final Map<String, String> currentId = new HashMap<>();

    LibraryLightsParser() {
        final Map<String, Consumer<StartElement>> startElementConsumer = new HashMap<>();
        //startElementConsumer.put("*", startElement -> currentId.put(startElement.qName, startElement.getAttributeValue("id")));
        final Map<String, Consumer<EndElement>> endElementConsumer = new HashMap<>();
        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }
}
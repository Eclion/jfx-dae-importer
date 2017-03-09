package com.javafx.experiments.importers.dae.parsers;

import org.xml.sax.Attributes;

import java.util.HashMap;
import java.util.function.BiConsumer;

/**
 * @author Eclion
 */
final class SceneParser extends AbstractParser {
    //private final Map<String, String> currentId = new HashMap<>();

    SceneParser(){
        final HashMap<String, BiConsumer<String, Attributes>> startElementConsumer = new HashMap<>();
        //startElementConsumer.put("*", (qName, attributes) -> currentId.put(qName, attributes.getValue("id")));
        final HashMap<String, BiConsumer<String, String>> endElementConsumer = new HashMap<>();
        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }
}

package com.javafx.experiments.importers.dae.parsers;

import org.xml.sax.Attributes;

import java.util.HashMap;
import java.util.function.BiConsumer;

/**
 * @author Eclion
 */
final class LibraryLightsParser extends AbstractParser {

    LibraryLightsParser() {
        final HashMap<String, BiConsumer<String, Attributes>> startElementConsumer = new HashMap<>();
        final HashMap<String, BiConsumer<String, String>> endElementConsumer = new HashMap<>();
        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }
}
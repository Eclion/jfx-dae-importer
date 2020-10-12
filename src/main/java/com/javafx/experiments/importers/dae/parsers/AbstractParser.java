package com.javafx.experiments.importers.dae.parsers;

import org.xml.sax.Attributes;

import java.util.function.BiConsumer;

/**
 * @author Eclion
 */
public class AbstractParser {

    private final LibraryHandler handler = new LibraryHandler();

    public final LibraryHandler getLibraryHandler() {
        return handler;
    }


    final void addStartElementBiConsumer(final String tag, final BiConsumer<String, Attributes> startElementBiConsumer) {
        handler.addStartElementBiConsumer(tag, startElementBiConsumer);
    }

    final void addEndElementBiConsumer(final String tag, final BiConsumer<String, String> endElementBiConsumer) {
        handler.addEndElementBiConsumer(tag, endElementBiConsumer);
    }
}

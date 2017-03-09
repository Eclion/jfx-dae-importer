package com.javafx.experiments.importers.dae.parsers;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Eclion
 */
public class LibraryHandler extends DefaultHandler {
    private StringBuilder charBuf = new StringBuilder();

    private final Map<String, BiConsumer<String, Attributes>> startElementBiConsumers;
    private final Map<String, BiConsumer<String, String>> endElementBiConsumers;

    LibraryHandler(Map<String, BiConsumer<String, Attributes>> startElementBiConsumers, Map<String, BiConsumer<String, String>> endElementBiConsumers) {
        this.startElementBiConsumers = startElementBiConsumers;
        this.endElementBiConsumers = endElementBiConsumers;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
        charBuf = new StringBuilder();
        startElement(qName, attributes);
    }

    public void startElement(final String qName, final Attributes attributes) {
        if (startElementBiConsumers.containsKey("*")) {
            startElementBiConsumers.get("*").accept(qName, attributes);
        }
        if (startElementBiConsumers.containsKey(qName)) {
            startElementBiConsumers.get(qName).accept(qName, attributes);
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) {
        endElement(qName, charBuf.toString().trim());
    }

    public void endElement(final String qName, final String content) {
        if (endElementBiConsumers.containsKey("*")) {
            endElementBiConsumers.get("*").accept(qName, content);
        }
        if (endElementBiConsumers.containsKey(qName)) {
            endElementBiConsumers.get(qName).accept(qName, content);
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) {
        charBuf.append(ch, start, length);
    }
}

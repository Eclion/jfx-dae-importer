package com.javafx.experiments.importers.dae.parsers;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Eclion
 */
public class LibraryHandler extends DefaultHandler {
    private StringBuilder charBuf = new StringBuilder();

    private final Map<String, Consumer<StartElement>> startElementConsumers;
    private final Map<String, Consumer<EndElement>> endElementConsumers;

    LibraryHandler(Map<String, Consumer<StartElement>> startElementConsumers, Map<String, Consumer<EndElement>> endElementConsumers) {
        this.startElementConsumers = startElementConsumers;
        this.endElementConsumers = endElementConsumers;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
        charBuf = new StringBuilder();
        startElement(new StartElement(qName, attributes));
    }

    public void startElement(StartElement startElement) {
        if (startElementConsumers.containsKey("*")) {
            startElementConsumers.get("*").accept(startElement);
        }
        if (startElementConsumers.containsKey(startElement.qName)) {
            startElementConsumers.get(startElement.qName).accept(startElement);
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) {
        endElement(new EndElement(qName, charBuf.toString().trim()));
    }

    public void endElement(EndElement endElement) {
        if (endElementConsumers.containsKey("*")) {
            endElementConsumers.get("*").accept(endElement);
        }
        if (endElementConsumers.containsKey(endElement.qName)) {
            endElementConsumers.get(endElement.qName).accept(endElement);
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) {
        charBuf.append(ch, start, length);
    }
}

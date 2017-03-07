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
public class LibraryHandler extends DefaultHandler {
    private static final Logger LOGGER = Logger.getLogger(LibraryHandler.class.getName());
    private StringBuilder charBuf = new StringBuilder();

    private Map<String, Consumer<StartElement>> startElementConsumers = new HashMap<>();
    private Map<String, Consumer<EndElement>> endElementConsumers = new HashMap<>();

    LibraryHandler(Map<String, Consumer<StartElement>> startElementConsumers, Map<String, Consumer<EndElement>> endElementConsumers) {
        this.startElementConsumers = startElementConsumers;
        this.endElementConsumers = endElementConsumers;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
        charBuf = new StringBuilder();
        if (startElementConsumers.containsKey(qName)) {
            startElementConsumers.get(qName).accept(new StartElement(uri, localName, qName, attributes));
        } else {
            LOGGER.log(Level.WARNING, "Unknown element: " + qName);
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        if (endElementConsumers.containsKey(qName)) {
            endElementConsumers.get(qName).accept(new EndElement(uri, localName, qName, charBuf.toString().trim()));
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        charBuf.append(ch, start, length);
    }

    class StartElement {
        final String uri;
        final String localName;
        final String qName;
        final Attributes attributes;

        StartElement(final String uri, final String localName, final String qName, final Attributes attributes) {
            this.uri = uri;
            this.localName = localName;
            this.qName = qName;
            this.attributes = attributes;
        }
    }

    class EndElement {
        final String uri;
        final String localName;
        final String qName;
        final String content;

        EndElement(final String uri, final String localName, final String qName, final String content) {
            this.uri = uri;
            this.localName = localName;
            this.qName = qName;
            this.content = content;
        }
    }
}

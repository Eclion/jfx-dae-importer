package com.javafx.experiments.importers.dae.parsers;

import org.xml.sax.Attributes;

/**
 * @author Eclion
 */
public class StartElement{
    final String uri;
    final String localName;
    final String qName;
    private final Attributes attributes;

    StartElement(final String uri, final String localName, final String qName, final Attributes attributes) {
        this.uri = uri;
        this.localName = localName;
        this.qName = qName;
        this.attributes = attributes;
    }

    public String getAttributeValue(String id) {
        if (attributes == null) {
            return null;
        }
        return attributes.getValue(id);
    }
}

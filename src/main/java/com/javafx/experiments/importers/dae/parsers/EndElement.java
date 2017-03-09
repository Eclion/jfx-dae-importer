package com.javafx.experiments.importers.dae.parsers;

/**
 * Created by Eclion on 09/03/17.
 */
public class EndElement {
    final String qName;
    final String content;

    EndElement(final String qName, final String content) {
        this.qName = qName;
        this.content = content;
    }
}

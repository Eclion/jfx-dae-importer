package com.javafx.experiments.importers.dae.parsers;

import javafx.scene.image.Image;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Eclion
 */
final class LibraryImagesParser extends DefaultHandler {
    private static final Logger LOGGER = Logger.getLogger(LibraryImagesParser.class.getSimpleName());
    private StringBuilder charBuf = new StringBuilder();
    private final HashMap<String, String> currentId = new HashMap<>();
    private final HashMap<String, Image> images = new HashMap<>();
    private final String rootUrl;

    private enum State {
        UNKNOWN,
        init_from,

        // ignored, unsupported states:
        image
    }

    LibraryImagesParser(final String fileUrl) {
        this.rootUrl = fileUrl;
    }

    Image getImage(String imageId) {
        return images.get(imageId);
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
            default:
                break;
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        switch (state(qName)) {
            case UNKNOWN:
                break;
            case init_from:
                final String filePath = charBuf.toString();
                final File folder = new File(rootUrl);
                final Path imagePath = folder.toPath().resolve(filePath);
                final Image image = new Image("file:" + imagePath.toString());
                images.put(currentId.get("image"), image);
                break;
            default:
                break;
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        this.charBuf.append(ch, start, length);
    }
}
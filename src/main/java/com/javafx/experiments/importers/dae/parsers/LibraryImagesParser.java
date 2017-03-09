package com.javafx.experiments.importers.dae.parsers;

import javafx.scene.image.Image;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Eclion
 */
final class LibraryImagesParser extends AbstractParser {
    private static final String INIT_FROM_TAG = "init_from";

    private final HashMap<String, String> currentId = new HashMap<>();
    private final HashMap<String, Image> images = new HashMap<>();
    private final String rootUrl;

    LibraryImagesParser(final String fileUrl) {
        this.rootUrl = fileUrl;
        final Map<String, Consumer<StartElement>> startElementConsumer = new HashMap<>();

        startElementConsumer.put("*", startElement -> currentId.put(startElement.qName, startElement.getAttributeValue("id")));

        final Map<String, Consumer<LibraryHandler.EndElement>> endElementConsumer = new HashMap<>();

        endElementConsumer.put(INIT_FROM_TAG, endElement -> {
            final String filePath = endElement.content;
            final File folder = new File(rootUrl);
            final Path imagePath = folder.toPath().resolve(filePath);
            final Image image = new Image("file:" + imagePath.toString());
            images.put(currentId.get("image"), image);
        });

        handler = new LibraryHandler(startElementConsumer, endElementConsumer);
    }

    Image getImage(String imageId) {
        return images.get(imageId);
    }
}
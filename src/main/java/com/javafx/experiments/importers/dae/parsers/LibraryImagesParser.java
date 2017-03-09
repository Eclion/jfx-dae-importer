package com.javafx.experiments.importers.dae.parsers;

import javafx.scene.image.Image;
import org.xml.sax.Attributes;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.BiConsumer;

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
        final HashMap<String, BiConsumer<String, Attributes>> startElementConsumer = new HashMap<>();

        startElementConsumer.put("*", (qName, attributes) -> currentId.put(qName, attributes.getValue("id")));

        final HashMap<String, BiConsumer<String, String>> endElementConsumer = new HashMap<>();

        endElementConsumer.put(INIT_FROM_TAG, (qName, content) -> {
            final String filePath = content;
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
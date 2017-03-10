package com.javafx.experiments.importers.dae.parsers;

import javafx.scene.image.Image;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eclion
 */
final class LibraryImagesParser extends AbstractParser {
    private static final String INIT_FROM_TAG = "init_from";

    private final Map<String, String> currentId = new HashMap<>();
    private final Map<String, Image> images = new HashMap<>();
    private final String rootUrl;

    LibraryImagesParser(final String fileUrl) {
        this.rootUrl = fileUrl;

        addStartElementBiConsumer("*", (qName, attributes) -> currentId.put(qName, attributes.getValue("id")));

        addEndElementBiConsumer(INIT_FROM_TAG, (qName, content) -> {
            final File folder = new File(rootUrl);
            final Path imagePath = folder.toPath().resolve(content);
            final Image image = new Image("file:" + imagePath.toString());
            images.put(currentId.get("image"), image);
        });
    }

    Image getImage(final String imageId) {
        return images.get(imageId);
    }
}
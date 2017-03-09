/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.importers.dae;

import com.javafx.experiments.importers.Importer;
import com.javafx.experiments.importers.dae.parsers.DaeSaxHandler;
import javafx.animation.Timeline;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Loader for ".dae" 3D files
 * <p>
 * Notes:
 * <p>
 * - Assume Y is up for now
 * - Assume 1 Unit = 1 FX Unit
 */
@SuppressWarnings("UnusedDeclaration")
public final class DaeImporter extends Importer {
    private static final Logger LOGGER = Logger.getLogger(DaeImporter.class.getSimpleName());
    private final Group rootNode = new Group();
    private Camera firstCamera;
    private double firstCameraAspectRatio;
    private HashMap<String, Timeline> timelines = new HashMap<>();

    public Scene createScene(final int width) {
        Scene scene = new Scene(rootNode, width, (int) (width / firstCameraAspectRatio), true);
        if (firstCamera != null) scene.setCamera(firstCamera);
        scene.setFill(Color.BEIGE);
        return scene;
    }

    public Camera getFirstCamera() {
        return firstCamera;
    }

    @Override
    public Group getRoot() {
        return rootNode;
    }

    @Override
    public void load(final String url) throws IOException {
        final int dot = url.lastIndexOf('.');
        final String extension = url.substring(dot + 1, url.length());
        if (".dae".equalsIgnoreCase(extension)) {
            throw new IOException("unsupported 3D format");
        }

        final long start = System.currentTimeMillis();
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            final SAXParser saxParser = factory.newSAXParser();

            final DaeSaxHandler handler = new DaeSaxHandler(extractRootPath(url));
            final HandlerAdapter handlerAdapter = new HandlerAdapter(handler);
            saxParser.parse(url, handlerAdapter);

            buildTimelines(handler);

            handler.buildScene(rootNode);
            firstCamera = handler.getFirstCamera();
            firstCameraAspectRatio = handler.getFirstCameraAspectRatio();

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        final long end = System.currentTimeMillis();
        LOGGER.log(Level.INFO, "IMPORTED [" + url + "] in  " + ((end - start)) + "ms");
    }

    private void buildTimelines(final DaeSaxHandler parser) {
        parser.getKeyFramesMap().entrySet()
                .forEach(entry -> {
                            if (!timelines.containsKey(entry.getKey())) {
                                timelines.put(entry.getKey(), new Timeline());
                            }
                            timelines.get(entry.getKey()).getKeyFrames().addAll(entry.getValue());
                        }
                );
    }

    @Override
    public boolean isSupported(final String extension) {
        return extension != null && "dae".equals(extension);
    }

    @Override
    public Map<String, Timeline> getTimelines() {
        return timelines;
    }

    private String extractRootPath(final String relativeUrl) throws IOException {
        final File file = new File(relativeUrl);

        return file.getCanonicalFile().getParent();
    }

    class HandlerAdapter extends DefaultHandler {

        final DaeSaxHandler daeSaxHandler;

        HandlerAdapter(DaeSaxHandler daeSaxHandler) {
            this.daeSaxHandler = daeSaxHandler;
        }

        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
            daeSaxHandler.getLibraryHandler().startElement(uri, localName, qName, attributes);
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            daeSaxHandler.getLibraryHandler().endElement(uri, localName, qName);
        }

        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            daeSaxHandler.getLibraryHandler().characters(ch, start, length);
        }

    }
}

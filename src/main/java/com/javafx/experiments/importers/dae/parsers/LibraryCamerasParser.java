package com.javafx.experiments.importers.dae.parsers;

import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Eclion
 */
final class LibraryCamerasParser extends DefaultHandler {
    private static final Logger LOGGER = Logger.getLogger(LibraryCamerasParser.class.getSimpleName());
    private static final double DEFAULT_ASPECT_RATIO = 4d / 3d;

    private StringBuilder charBuf = new StringBuilder();
    private final Map<String, String> currentId = new HashMap<>();
    private Double aspectRatio, xfov, yfov, znear, zfar;
    Camera firstCamera = null;
    final Map<String, Camera> cameras = new HashMap<>();
    double firstCameraAspectRatio = DEFAULT_ASPECT_RATIO;

    private enum State {
        UNKNOWN,
        aspect_ratio,
        camera,
        extra, //ignored
        perspective, //ignored
        optics, //ignored
        shiftx, //ignored
        shifty, //ignored
        technique, //ignored
        technique_common, //ignored
        xfov,
        yfov,
        YF_dofdist, //ignored
        zfar,
        znear
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
        currentId.put(qName, attributes.getValue("id"));
        charBuf = new StringBuilder();
        switch (state(qName)) {
            case UNKNOWN:
                LOGGER.log(Level.WARNING, "Unknown element: " + qName);
                break;
            case camera:
                aspectRatio = xfov = yfov = znear = zfar = null;
                break;
            default:
                break;
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        switch (state(qName)) {
            case aspect_ratio:
                aspectRatio = Double.parseDouble(charBuf.toString().trim());
                break;
            case camera:
                saveCamera();
                break;
            case xfov:
                xfov = Double.parseDouble(charBuf.toString().trim());
                break;
            case yfov:
                yfov = Double.parseDouble(charBuf.toString().trim());
                break;
            case zfar:
                zfar = Double.parseDouble(charBuf.toString().trim());
                break;
            case znear:
                znear = Double.parseDouble(charBuf.toString().trim());
                break;
            default:
                break;
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        charBuf.append(ch, start, length);
    }

    private void saveCamera() {
        PerspectiveCamera camera = new PerspectiveCamera(true);
        if (yfov != null) {
            camera.setVerticalFieldOfView(true);
            camera.setFieldOfView(yfov);
        } else if (xfov != null) {
            camera.setVerticalFieldOfView(false);
            camera.setFieldOfView(xfov);
        }
        if (znear != null) camera.setNearClip(znear);
        if (zfar != null) camera.setFarClip(zfar);
        cameras.put(currentId.get("camera"), camera);
        if (firstCamera == null) {
            firstCamera = camera;
            if (aspectRatio != null) firstCameraAspectRatio = aspectRatio;
        }
    }
}

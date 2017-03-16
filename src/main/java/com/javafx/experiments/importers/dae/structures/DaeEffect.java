package com.javafx.experiments.importers.dae.structures;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Eclion on 16/03/17.
 */
public final class DaeEffect {

    private static final String PHONG_TAG = "phong";
    private static final String AMBIENT_TAG = "ambient";
    private static final String DIFFUSE_TAG = "diffuse";
    private static final String EMISSION_TAG = "emission";
    private static final String SPECULAR_TAG = "specular";


    public final String id;

    public final Map<String, String> surfaces = new HashMap<>();
    public final Map<String, String> samplers = new HashMap<>();
    public final Map<String, Color> colors = new HashMap<>();
    public final Map<String, String> textureIds = new HashMap<>();
    public float shininess;
    public float refractionIndex;

    public String type;

    public DaeEffect(final String id) {
        this.id = id;
    }

    public Material build(final Map<String, Image> images) {
        Material material = null;
        switch (this.type) {
            case PHONG_TAG:
                material = buildPhongMaterial(images);
                break;
            default:
                break;
        }
        return material;
    }

    PhongMaterial buildPhongMaterial(final Map<String, Image> images) {
        final PhongMaterial material = new PhongMaterial();

        buildPhongMaterialColors(material);

        buildPhongMaterialTextures(material, images);
        return material;
    }

    private void buildPhongMaterialColors(final PhongMaterial material) {
        colors.entrySet().forEach(entry -> {
            switch (entry.getKey()) {
                case AMBIENT_TAG:
                    break;
                case DIFFUSE_TAG:
                    material.setDiffuseColor(entry.getValue());
                    break;
                case EMISSION_TAG:
                    break;
                case SPECULAR_TAG:
                    material.setSpecularColor(entry.getValue());
                    break;
                default:
                    break;
            }
        });
    }

    private void buildPhongMaterialTextures(final PhongMaterial material, final Map<String, Image> images) {
        textureIds.entrySet().stream().
                filter(entry -> samplers.containsKey(entry.getValue())).
                filter(entry -> surfaces.containsKey(samplers.get(entry.getValue()))).
                filter(entry -> images.get(surfaces.get(samplers.get(entry.getValue()))) != null).
                forEach(entry -> {
                    final Image image = images.get(surfaces.get(samplers.get(entry.getValue())));
                    switch (entry.getKey()) {
                        case AMBIENT_TAG:
                            break;
                        case DIFFUSE_TAG:
                            material.setDiffuseMap(image);
                            break;
                        case EMISSION_TAG:
                            break;
                        case SPECULAR_TAG:
                            material.setSpecularMap(image);
                            break;
                        default:
                            break;
                    }
                });
    }
}
package com.javafx.experiments.importers.dae.structures;

/**
 * @author Eclion
 */
public final class Input {
    public final int offset;
    public final String semantic;
    public final String source;

    public Input(int offset, String semantic, String source) {
        this.offset = offset;
        this.semantic = semantic;
        this.source = source;
    }

    @Override
    public String toString() {
        return "Input{" +
                "offset=" + offset +
                ", semantic='" + semantic + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}
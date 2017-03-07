package com.javafx.experiments.importers.dae.structures;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.beans.value.WritableValue;
import javafx.scene.transform.Affine;
import javafx.scene.transform.MatrixType;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eclion
 */
public final class DaeAnimation {

    //ratio set for the animation used for testing.
    private static final int TIMER_RATIO = 3000;

    public final String id;
    public float[] input;
    public double[] output;
    private String[] interpolations;
    public String target;
    private final List<DaeAnimation> childAnimations = new ArrayList<>();

    public DaeAnimation(final String id) {
        this.id = id;
    }

    public List<KeyFrame> calculateAnimation(final DaeSkeleton skeleton) {
        final List<KeyFrame> keyFrames = new ArrayList<>();
        final String targetJointName = this.target.split("/")[0];
        final Joint animatedJoint = skeleton.joints.get(targetJointName);
        if (animatedJoint == null) return new ArrayList<>();
        for (int i = 0; i < this.input.length; i++) {
            final Affine keyAffine = new Affine(this.output, MatrixType.MT_3D_4x4, i * 16);
            keyFrames.add(this.convertToKeyFrame(this.input[i] * TIMER_RATIO, animatedJoint.a, keyAffine));
        }
        keyFrames.addAll(this.childAnimations.stream().map(animation -> animation.calculateAnimation(skeleton)).
                reduce(new ArrayList<>(), (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                }));
        return keyFrames;
    }

    private KeyFrame convertToKeyFrame(final float t, final Affine jointAffine, final Affine keyAffine) {
        final Duration duration = new Duration(t);
        final List<KeyValue> kvs = convertToKeyValues(jointAffine, keyAffine);
        final KeyValue[] kvs2 = kvs.toArray(new KeyValue[kvs.size()]);
        return new KeyFrame(duration, kvs2);
    }

    private KeyValue convertToKeyValue(final WritableValue<Number> target, final Number endValue) {
        return new KeyValue(target, endValue);
    }

    private List<KeyValue> convertToKeyValues(final Affine jointAffine, final Affine keyAffine) {
        final List<KeyValue> keyValues = new ArrayList<>();
        keyValues.add(convertToKeyValue(jointAffine.mxxProperty(), keyAffine.getMxx()));
        keyValues.add(convertToKeyValue(jointAffine.mxyProperty(), keyAffine.getMxy()));
        keyValues.add(convertToKeyValue(jointAffine.mxzProperty(), keyAffine.getMxz()));
        keyValues.add(convertToKeyValue(jointAffine.myxProperty(), keyAffine.getMyx()));
        keyValues.add(convertToKeyValue(jointAffine.myyProperty(), keyAffine.getMyy()));
        keyValues.add(convertToKeyValue(jointAffine.myzProperty(), keyAffine.getMyz()));
        keyValues.add(convertToKeyValue(jointAffine.mzxProperty(), keyAffine.getMzx()));
        keyValues.add(convertToKeyValue(jointAffine.mzyProperty(), keyAffine.getMzy()));
        keyValues.add(convertToKeyValue(jointAffine.mzzProperty(), keyAffine.getMzz()));
        keyValues.add(convertToKeyValue(jointAffine.txProperty(), keyAffine.getTx()));
        keyValues.add(convertToKeyValue(jointAffine.tyProperty(), keyAffine.getTy()));
        keyValues.add(convertToKeyValue(jointAffine.tzProperty(), keyAffine.getTz()));
        return keyValues;
    }

    public void setInterpolations(final String[] interpolations) {
        this.interpolations = interpolations;
    }

    public void addChild(final DaeAnimation animation) {
        childAnimations.add(animation);
    }
}
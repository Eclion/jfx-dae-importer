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

    private final String id;
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
        for (int i = 0; i < this.input.length; i++) {
            final Affine keyAffine = new Affine(this.output, MatrixType.MT_3D_4x4, i * 16);
            keyFrames.addAll(this.convertToKeyFrames(this.input[i] * TIMER_RATIO, animatedJoint.a, keyAffine));
        }
        keyFrames.addAll(this.childAnimations.stream().map(animation -> animation.calculateAnimation(skeleton)).
                reduce(new ArrayList<>(), (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                }));
        return keyFrames;
    }

    private List<KeyFrame> convertToKeyFrames(final float t, final Affine jointAffine, final Affine keyAffine) {
        final Duration duration = new Duration(t);

        final List<KeyFrame> keyFrames = new ArrayList<>();

        keyFrames.add(convertToKeyFrame(duration, jointAffine.mxxProperty(), keyAffine.getMxx()));
        keyFrames.add(convertToKeyFrame(duration, jointAffine.mxyProperty(), keyAffine.getMxy()));
        keyFrames.add(convertToKeyFrame(duration, jointAffine.mxzProperty(), keyAffine.getMxz()));
        keyFrames.add(convertToKeyFrame(duration, jointAffine.myxProperty(), keyAffine.getMyx()));
        keyFrames.add(convertToKeyFrame(duration, jointAffine.myyProperty(), keyAffine.getMyy()));
        keyFrames.add(convertToKeyFrame(duration, jointAffine.myzProperty(), keyAffine.getMyz()));
        keyFrames.add(convertToKeyFrame(duration, jointAffine.mzxProperty(), keyAffine.getMzx()));
        keyFrames.add(convertToKeyFrame(duration, jointAffine.mzyProperty(), keyAffine.getMzy()));
        keyFrames.add(convertToKeyFrame(duration, jointAffine.mzzProperty(), keyAffine.getMzz()));
        keyFrames.add(convertToKeyFrame(duration, jointAffine.txProperty(), keyAffine.getTx()));
        keyFrames.add(convertToKeyFrame(duration, jointAffine.tyProperty(), keyAffine.getTy()));
        keyFrames.add(convertToKeyFrame(duration, jointAffine.tzProperty(), keyAffine.getTz()));

        return keyFrames;
    }

    private KeyFrame convertToKeyFrame(final Duration d, final WritableValue<Number> target, final Number endValue) {
        return new KeyFrame(d, new KeyValue(target, endValue));
    }

    public void setInterpolations(final String[] interpolations) {
        this.interpolations = interpolations;
    }

    public void addChild(final DaeAnimation animation) {
        childAnimations.add(animation);
    }
}
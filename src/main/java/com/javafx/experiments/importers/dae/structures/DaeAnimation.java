package com.javafx.experiments.importers.dae.structures;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.scene.transform.Affine;
import javafx.scene.transform.MatrixType;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eclion
 */
public final class DaeAnimation {

    final String id;
    public float[] input;
    public double[] output;
    public String[] interpolations;
    public String target;
    final List<DaeAnimation> childAnimations = new ArrayList<>();

    public DaeAnimation(String id) {
        this.id = id;
    }

    public List<KeyFrame> calculateAnimation(DaeSkeleton skeleton) {
        final List<KeyFrame> keyFrames = new ArrayList<>();
        final String targetJointName = target.split("/")[0];
        final Joint animatedJoint = skeleton.joints.get(targetJointName);
        for (int i = 0; i < input.length; i++) {
            final Affine keyAffine = new Affine(output, MatrixType.MT_3D_4x4, i * 16);
            keyFrames.addAll(convertToKeyFrames(input[i] * 3000, animatedJoint.a, keyAffine));
        }
        keyFrames.addAll(childAnimations.stream().map(animation -> animation.calculateAnimation(skeleton))
                .reduce(new ArrayList<>(), (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                }));
        return keyFrames;
    }

    private List<KeyFrame> convertToKeyFrames(final float t, final Affine jointAffine, final Affine keyAffine) {
        final Duration duration = new Duration(t);

        final List<KeyFrame> keyFrames = new ArrayList<>();

        keyFrames.add(new KeyFrame(duration, new KeyValue(jointAffine.mxxProperty(), keyAffine.getMxx())));
        keyFrames.add(new KeyFrame(duration, new KeyValue(jointAffine.mxyProperty(), keyAffine.getMxy())));
        keyFrames.add(new KeyFrame(duration, new KeyValue(jointAffine.mxzProperty(), keyAffine.getMxz())));
        keyFrames.add(new KeyFrame(duration, new KeyValue(jointAffine.myxProperty(), keyAffine.getMyx())));
        keyFrames.add(new KeyFrame(duration, new KeyValue(jointAffine.myyProperty(), keyAffine.getMyy())));
        keyFrames.add(new KeyFrame(duration, new KeyValue(jointAffine.myzProperty(), keyAffine.getMyz())));
        keyFrames.add(new KeyFrame(duration, new KeyValue(jointAffine.mzxProperty(), keyAffine.getMzx())));
        keyFrames.add(new KeyFrame(duration, new KeyValue(jointAffine.mzyProperty(), keyAffine.getMzy())));
        keyFrames.add(new KeyFrame(duration, new KeyValue(jointAffine.mzzProperty(), keyAffine.getMzz())));
        keyFrames.add(new KeyFrame(duration, new KeyValue(jointAffine.txProperty(), keyAffine.getTx())));
        keyFrames.add(new KeyFrame(duration, new KeyValue(jointAffine.tyProperty(), keyAffine.getTy())));
        keyFrames.add(new KeyFrame(duration, new KeyValue(jointAffine.tzProperty(), keyAffine.getTz())));

        return keyFrames;
    }

    public void setInterpolations(String[] interpolations) {
        this.interpolations = interpolations;
    }

    public void addChild(DaeAnimation animation) {
        childAnimations.add(animation);
    }
}
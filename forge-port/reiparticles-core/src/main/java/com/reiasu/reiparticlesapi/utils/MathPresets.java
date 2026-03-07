// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils;

import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Preset particle shapes for Roman numeral display (I through XII).
 * Each method returns a list of RelativeLocations forming the numeral shape.
 */
public final class MathPresets {
    private MathPresets() {
    }

    public static List<RelativeLocation> romaI(double scale) {
        requireMinScale(scale);
        PointsBuilder builder = new PointsBuilder();
        int preLineCount = Math.max((int) Math.round(5.0 * scale), 1);
        double height = 0.25 * scale;
        double width = 0.125 * scale;
        builder.addLine(new RelativeLocation(-width, height, 0), new RelativeLocation(width, height, 0), preLineCount)
                .addLine(new RelativeLocation(-width, -height, 0), new RelativeLocation(width, -height, 0), preLineCount)
                .addLine(new RelativeLocation(0, height, 0), new RelativeLocation(0, -height, 0), preLineCount);
        return builder.create();
    }

    public static List<RelativeLocation> romaII(double scale) {
        requireMinScale(scale);
        ArrayList<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2.0;
        List<RelativeLocation> left = romaI(scale);
        for (RelativeLocation it : left) it.setX(it.getX() - offset);
        res.addAll(left);
        List<RelativeLocation> right = romaI(scale);
        for (RelativeLocation it : right) it.setX(it.getX() + offset);
        res.addAll(right);
        return res;
    }

    public static List<RelativeLocation> romaIII(double scale) {
        requireMinScale(scale);
        ArrayList<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale);
        res.addAll(romaI(scale));
        List<RelativeLocation> left = romaI(scale);
        for (RelativeLocation it : left) it.setX(it.getX() - offset);
        res.addAll(left);
        List<RelativeLocation> right = romaI(scale);
        for (RelativeLocation it : right) it.setX(it.getX() + offset);
        res.addAll(right);
        return res;
    }

    public static List<RelativeLocation> romaIV(double scale) {
        requireMinScale(scale);
        ArrayList<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2.0;
        List<RelativeLocation> v = romaV(scale);
        for (RelativeLocation it : v) it.setX(it.getX() + offset);
        res.addAll(v);
        List<RelativeLocation> i = romaI(scale);
        for (RelativeLocation it : i) it.setX(it.getX() - offset);
        res.addAll(i);
        return res;
    }

    public static List<RelativeLocation> romaV(double scale) {
        requireMinScale(scale);
        PointsBuilder builder = new PointsBuilder();
        int preLineCount = Math.max((int) Math.round(5.0 * scale), 1);
        double height = 0.25 * scale;
        double width = 0.125 * scale;
        builder.addLine(new RelativeLocation(-width, height, 0), new RelativeLocation(0, -height, 0), preLineCount)
                .addLine(new RelativeLocation(width, height, 0), new RelativeLocation(0, -height, 0), preLineCount);
        return builder.create();
    }

    public static List<RelativeLocation> romaVI(double scale) {
        requireMinScale(scale);
        ArrayList<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2.0;
        List<RelativeLocation> v = romaV(scale);
        for (RelativeLocation it : v) it.setX(it.getX() - offset);
        res.addAll(v);
        List<RelativeLocation> i = romaI(scale);
        for (RelativeLocation it : i) it.setX(it.getX() + offset);
        res.addAll(i);
        return res;
    }

    public static List<RelativeLocation> romaVII(double scale) {
        requireMinScale(scale);
        ArrayList<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale);
        List<RelativeLocation> v = romaV(scale);
        for (RelativeLocation it : v) it.setX(it.getX() - offset);
        res.addAll(v);
        res.addAll(romaI(scale));
        List<RelativeLocation> right = romaI(scale);
        for (RelativeLocation it : right) it.setX(it.getX() + offset);
        res.addAll(right);
        return res;
    }

    public static List<RelativeLocation> romaVIII(double scale) {
        requireMinScale(scale);
        ArrayList<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2.0;
        List<RelativeLocation> vii = romaVII(scale);
        for (RelativeLocation it : vii) it.setX(it.getX() - offset);
        res.addAll(vii);
        List<RelativeLocation> i = romaI(scale);
        for (RelativeLocation it : i) it.setX(it.getX() + offset * 3.0);
        res.addAll(i);
        return res;
    }

    public static List<RelativeLocation> romaIX(double scale) {
        requireMinScale(scale);
        ArrayList<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2.0;
        List<RelativeLocation> x = romaX(scale);
        for (RelativeLocation it : x) it.setX(it.getX() + offset);
        res.addAll(x);
        List<RelativeLocation> i = romaI(scale);
        for (RelativeLocation it : i) it.setX(it.getX() - offset);
        res.addAll(i);
        return res;
    }

    public static List<RelativeLocation> romaX(double scale) {
        requireMinScale(scale);
        PointsBuilder builder = new PointsBuilder();
        int preLineCount = Math.max((int) Math.round(5.0 * scale), 1);
        double height = 0.25 * scale;
        double width = 0.125 * scale;
        builder.addLine(new RelativeLocation(-width, height, 0), new RelativeLocation(width, -height, 0), preLineCount)
                .addLine(new RelativeLocation(-width, -height, 0), new RelativeLocation(width, height, 0), preLineCount);
        return builder.create();
    }

    public static List<RelativeLocation> romaXI(double scale) {
        requireMinScale(scale);
        ArrayList<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2.0;
        List<RelativeLocation> x = romaX(scale);
        for (RelativeLocation it : x) it.setX(it.getX() - offset);
        res.addAll(x);
        List<RelativeLocation> i = romaI(scale);
        for (RelativeLocation it : i) it.setX(it.getX() + offset);
        res.addAll(i);
        return res;
    }

    public static List<RelativeLocation> romaXII(double scale) {
        requireMinScale(scale);
        ArrayList<RelativeLocation> res = new ArrayList<>();
        double offset = getRomaOffsetX(scale) / 2.0;
        List<RelativeLocation> xi = romaXI(scale);
        for (RelativeLocation it : xi) it.setX(it.getX() - offset * 2.0);
        res.addAll(xi);
        List<RelativeLocation> i = romaI(scale);
        for (RelativeLocation it : i) it.setX(it.getX() + offset);
        res.addAll(i);
        return res;
    }

    public static double getRomaOffsetX(double scale) {
        return 0.125 * scale * 2.0;
    }

    public static double getRomaOffsetY(double scale) {
        return 0.25 * scale * 2.0;
    }

    public static List<RelativeLocation> withRomaNumber(int i, double scale) {
        if (i < 1 || i > 12) {
            throw new IllegalArgumentException("Only supports Roman numerals 1-12");
        }
        switch (i) {
            case 1: return romaI(scale);
            case 2: return romaII(scale);
            case 3: return romaIII(scale);
            case 4: return romaIV(scale);
            case 5: return romaV(scale);
            case 6: return romaVI(scale);
            case 7: return romaVII(scale);
            case 8: return romaVIII(scale);
            case 9: return romaIX(scale);
            case 10: return romaX(scale);
            case 11: return romaXI(scale);
            case 12: return romaXII(scale);
            default: return new ArrayList<>();
        }
    }

    private static void requireMinScale(double scale) {
        if (scale < 0.01) {
            throw new IllegalArgumentException("Minimum scale is 0.01");
        }
    }
}

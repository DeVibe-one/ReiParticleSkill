// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;

import java.util.ArrayList;
import java.util.List;

public abstract class BezierValueScaleHelper extends ScaleHelper {
    private RelativeLocation controlPoint1;
    private RelativeLocation controlPoint2;
    private final double deltaScale;
    private List<RelativeLocation> bezierPoints;

    protected BezierValueScaleHelper(
            int scaleTick,
            double minScale,
            double maxScale,
            RelativeLocation controlPoint1,
            RelativeLocation controlPoint2
    ) {
        super(minScale, maxScale, scaleTick);
        this.controlPoint1 = controlPoint1.copy();
        this.controlPoint2 = controlPoint2.copy();
        this.deltaScale = maxScale - minScale;
        this.bezierPoints = buildBezierCurve();
    }

    public RelativeLocation getControlPoint1() {
        return controlPoint1.copy();
    }

    public void setControlPoint1(RelativeLocation controlPoint1) {
        this.controlPoint1 = controlPoint1.copy();
    }

    public RelativeLocation getControlPoint2() {
        return controlPoint2.copy();
    }

    public void setControlPoint2(RelativeLocation controlPoint2) {
        this.controlPoint2 = controlPoint2.copy();
    }

    public List<RelativeLocation> getBezierPoints() {
        return new ArrayList<>(bezierPoints);
    }

    public void setBezierPoints(List<RelativeLocation> bezierPoints) {
        this.bezierPoints = new ArrayList<>(bezierPoints);
    }

    @Override
    public BezierValueScaleHelper recalculateStep() {
        super.recalculateStep();
        bezierPoints = buildBezierCurve();
        return this;
    }

    @Override
    public void toggleScale(double scale) {
        int nearest = 0;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < bezierPoints.size(); i++) {
            double delta = Math.abs(bezierPoints.get(i).getY() - (scale - getMinScale()));
            if (delta < best) {
                best = delta;
                nearest = i;
            }
        }
        setCurrent(nearest);
        scale(getMinScale() + bezierPoints.get(nearest).getY());
    }

    @Override
    public void doScale() {
        if (getLoadedGroup() == null || over()) {
            return;
        }
        int index = Math.min(getCurrent(), Math.max(0, bezierPoints.size() - 1));
        setCurrent(getCurrent() + 1);
        scale(getMinScale() + bezierPoints.get(index).getY());
    }

    @Override
    public void doScaleTo(int current) {
        int clamped = Math.max(0, current);
        setCurrent(clamped);
        if (current >= getScaleTick()) {
            resetScaleMax();
            return;
        }
        if (current <= 0) {
            resetScaleMin();
            return;
        }
        int index = Math.min(current, Math.max(0, bezierPoints.size() - 1));
        scale(getMinScale() + bezierPoints.get(index).getY());
    }

    @Override
    public void doScaleReversed() {
        if (getLoadedGroup() == null || isZero()) {
            return;
        }
        setCurrent(getCurrent() - 1);
        int index = Math.max(0, Math.min(getCurrent(), bezierPoints.size() - 1));
        scale(getMinScale() + bezierPoints.get(index).getY());
    }

    private List<RelativeLocation> buildBezierCurve() {
        List<RelativeLocation> points = new ArrayList<>();
        int tick = Math.max(1, getScaleTick());
        RelativeLocation p0 = new RelativeLocation(0.0, 0.0, 0.0);
        RelativeLocation p1 = controlPoint1.copy();
        RelativeLocation end = new RelativeLocation(tick, deltaScale, 0.0);
        // Keep the Fabric convention where c2 is an offset from end.
        RelativeLocation p2 = end.copy().add(controlPoint2);
        RelativeLocation p3 = end;
        for (int i = 0; i < tick; i++) {
            double t = (double) i / (double) tick;
            double omt = 1.0 - t;
            double x = omt * omt * omt * p0.getX()
                    + 3.0 * omt * omt * t * p1.getX()
                    + 3.0 * omt * t * t * p2.getX()
                    + t * t * t * p3.getX();
            double y = omt * omt * omt * p0.getY()
                    + 3.0 * omt * omt * t * p1.getY()
                    + 3.0 * omt * t * t * p2.getY()
                    + t * t * t * p3.getY();
            points.add(new RelativeLocation(x, y, 0.0));
        }
        if (points.isEmpty()) {
            points.add(new RelativeLocation(0.0, 0.0, 0.0));
        }
        return points;
    }
}

// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils;

import com.reiasu.reiparticlesapi.utils.builder.FourierSeriesBuilder;
import org.joml.Vector2d;
import org.joml.Vector2i;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts raster images into Fourier series representations for particle rendering.
 * <p>
 * The pipeline:
 * <ol>
 *   <li>Build a binary solid/transparent mask from the image</li>
 *   <li>Find connected components via flood-fill BFS</li>
 *   <li>Extract ordered contours via Moore boundary tracing</li>
 *   <li>Resample the contour to a fixed sample count</li>
 *   <li>Compute DFT coefficients</li>
 *   <li>Convert to {@link FourierSeriesBuilder.Fourier} entries</li>
 * </ol>
 */
public final class FourierPhotoUtil {

    public static final FourierPhotoUtil INSTANCE = new FourierPhotoUtil();

    private FourierPhotoUtil() {
    }

    // ---- Public API ----

    /**
     * Converts an image into a single {@link FourierSeriesBuilder}.
     * If the image has multiple connected components, only the first (largest) is used.
     */
    public FourierSeriesBuilder toFourierBuilder(BufferedImage image,
                                                  int alphaThreshold,
                                                  int sampleCount,
                                                  int harmonics,
                                                  double step,
                                                  boolean centerMode,
                                                  boolean sortByAmplitude) {
        List<FourierSeriesBuilder> builders = toFourierBuilders(image, alphaThreshold,
                sampleCount, harmonics, step, centerMode, sortByAmplitude, 16, 20);
        if (builders.isEmpty()) {
            throw new IllegalArgumentException(
                    "No contours found: check image alpha channel / alphaThreshold=" + alphaThreshold);
        }
        return builders.get(0);
    }

    /**
     * Default-parameter overload.
     */
    public FourierSeriesBuilder toFourierBuilder(BufferedImage image) {
        return toFourierBuilder(image, 1, 1024, 150, 1.0, true, true);
    }

    /**
     * Converts an image into multiple {@link FourierSeriesBuilder}s, one per connected component.
     * Components are sorted by size (largest first) and limited to {@code maxComponents}.
     */
    public List<FourierSeriesBuilder> toFourierBuilders(BufferedImage image,
                                                         int alphaThreshold,
                                                         int sampleCount,
                                                         int harmonics,
                                                         double step,
                                                         boolean centerMode,
                                                         boolean sortByAmplitude,
                                                         int maxComponents,
                                                         int minComponentPixels) {
        boolean[] mask = buildSolidMask(image, alphaThreshold);
        List<int[]> allComponents = findConnectedComponents(mask, image.getWidth(), image.getHeight(), minComponentPixels);

        // Sort by size descending, take top N
        List<int[]> components = allComponents.stream()
                .sorted((a, b) -> Integer.compare(b.length, a.length))
                .limit(maxComponents)
                .collect(Collectors.toList());

        List<FourierSeriesBuilder> builders = new ArrayList<>(components.size());
        for (int[] comp : components) {
            List<Vector2i> contour = extractOrderedContourFromComponent(image.getWidth(), image.getHeight(), comp);
            if (contour.size() < 8) continue;

            List<Vector2d> resampled = resampleClosedPath(contour, sampleCount);
            List<Vector2d> samples = mapToXZ(resampled, image.getWidth(), image.getHeight(), step, centerMode);
            Vector2d[] coeffs = dft(samples);
            List<FourierSeriesBuilder.Fourier> series = coeffsToFouriers(coeffs, harmonics, sortByAmplitude);

            FourierSeriesBuilder builder = new FourierSeriesBuilder().count(sampleCount).scale(1.0);
            for (FourierSeriesBuilder.Fourier f : series) {
                builder.addFourier(f.r(), f.w(), f.startAngleDeg());
            }
            builders.add(builder);
        }
        return builders;
    }

    /**
     * Default-parameter overload.
     */
    public List<FourierSeriesBuilder> toFourierBuilders(BufferedImage image) {
        return toFourierBuilders(image, 1, 1024, 150, 1.0, true, true, 16, 20);
    }

    /**
     * Like {@link #toFourierBuilders} but returns a map of offset -> builder,
     * where each component's offset is relative to the image center.
     */
    public Map<RelativeLocation, FourierSeriesBuilder> toFourierBuildersWithOffset(
            BufferedImage image,
            int alphaThreshold,
            int sampleCount,
            int harmonics,
            double step,
            boolean sortByAmplitude,
            int maxComponents,
            int minComponentPixels) {

        boolean[] mask = buildSolidMask(image, alphaThreshold);
        List<int[]> allComponents = findConnectedComponents(mask, image.getWidth(), image.getHeight(), minComponentPixels);

        List<int[]> components = allComponents.stream()
                .sorted((a, b) -> Integer.compare(b.length, a.length))
                .limit(maxComponents)
                .collect(Collectors.toList());

        Map<RelativeLocation, FourierSeriesBuilder> result = new LinkedHashMap<>(components.size());
        double globalCx = image.getWidth() / 2.0;
        double globalCy = image.getHeight() / 2.0;

        for (int[] comp : components) {
            List<Vector2i> contour = extractOrderedContourFromComponent(image.getWidth(), image.getHeight(), comp);
            if (contour.size() < 8) continue;

            double[] center = computeComponentCenterPx(comp, image.getWidth());
            double compCx = center[0];
            double compCy = center[1];
            double offX = (compCx - globalCx) * step;
            double offZ = (globalCy - compCy) * step;
            RelativeLocation offset = new RelativeLocation(offX, 0.0, offZ);

            List<Vector2d> resampled = resampleClosedPath(contour, sampleCount);
            // Map relative to component center (not image center)
            List<Vector2d> localSamples = new ArrayList<>(resampled.size());
            for (Vector2d p : resampled) {
                localSamples.add(new Vector2d((p.x - compCx) * step, (compCy - p.y) * step));
            }

            Vector2d[] coeffs = dft(localSamples);
            List<FourierSeriesBuilder.Fourier> series = coeffsToFouriers(coeffs, harmonics, sortByAmplitude);

            FourierSeriesBuilder builder = new FourierSeriesBuilder().count(sampleCount).scale(1.0);
            for (FourierSeriesBuilder.Fourier f : series) {
                builder.addFourier(f.r(), f.w(), f.startAngleDeg());
            }
            result.put(offset, builder);
        }
        return result;
    }

    /**
     * Default-parameter overload.
     */
    public Map<RelativeLocation, FourierSeriesBuilder> toFourierBuildersWithOffset(BufferedImage image) {
        return toFourierBuildersWithOffset(image, 1, 1024, 150, 1.0, true, 16, 20);
    }

    /**
     * Converts each connected component into a list of {@link RelativeLocation} points
     * by building then evaluating a Fourier series.
     */
    public List<List<RelativeLocation>> toFourierPointsMulti(BufferedImage image,
                                                              int alphaThreshold,
                                                              int sampleCount,
                                                              int harmonics,
                                                              double step,
                                                              boolean centerMode,
                                                              boolean sortByAmplitude,
                                                              int maxComponents,
                                                              int minComponentPixels) {
        List<FourierSeriesBuilder> builders = toFourierBuilders(image, alphaThreshold,
                sampleCount, harmonics, step, centerMode, sortByAmplitude, maxComponents, minComponentPixels);
        return builders.stream()
                .map(FourierSeriesBuilder::build)
                .collect(Collectors.toList());
    }

    /**
     * Default-parameter overload.
     */
    public List<List<RelativeLocation>> toFourierPointsMulti(BufferedImage image) {
        return toFourierPointsMulti(image, 1, 1024, 150, 1.0, true, true, 16, 20);
    }

    // ---- Internal: mask & components ----

    private boolean[] buildSolidMask(BufferedImage image, int alphaThreshold) {
        int w = image.getWidth();
        int h = image.getHeight();
        boolean[] out = new boolean[w * h];
        boolean hasAlpha = image.getColorModel().hasAlpha();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!hasAlpha) {
                    out[y * w + x] = true;
                } else {
                    int pixel = image.getRGB(x, y);
                    int a = (pixel >>> 24) & 0xFF;
                    out[y * w + x] = a > alphaThreshold;
                }
            }
        }
        return out;
    }

    private List<int[]> findConnectedComponents(boolean[] mask, int w, int h, int minPixels) {
        boolean[] visited = new boolean[mask.length];
        List<int[]> comps = new ArrayList<>();
        // 8-connectivity neighbor offsets: dx, dy pairs
        int[] neighbors = {-1, 0, 1, 0, 0, -1, 0, 1, -1, -1, 1, -1, -1, 1, 1, 1};
        int[] queue = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int start = y * w + x;
                if (!mask[start] || visited[start]) continue;

                int qs = 0, qe = 0;
                queue[qe++] = start;
                visited[start] = true;
                List<Integer> pixels = new ArrayList<>(256);

                while (qs < qe) {
                    int cur = queue[qs++];
                    pixels.add(cur);
                    int cx = cur % w;
                    int cy = cur / w;

                    for (int n = 0; n < neighbors.length; n += 2) {
                        int nx = cx + neighbors[n];
                        int ny = cy + neighbors[n + 1];
                        if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                        int ni = ny * w + nx;
                        if (visited[ni] || !mask[ni]) continue;
                        visited[ni] = true;
                        queue[qe++] = ni;
                    }
                }

                if (pixels.size() >= minPixels) {
                    int[] arr = new int[pixels.size()];
                    for (int i = 0; i < pixels.size(); i++) {
                        arr[i] = pixels.get(i);
                    }
                    comps.add(arr);
                }
            }
        }
        return comps;
    }

    private double[] computeComponentCenterPx(int[] compPixels, int w) {
        double sx = 0.0, sy = 0.0;
        for (int p : compPixels) {
            sx += p % w;
            sy += p / w;
        }
        int count = Math.max(compPixels.length, 1);
        return new double[]{sx / count, sy / count};
    }

    // ---- Internal: contour extraction ----

    private List<Vector2i> extractOrderedContourFromComponent(int width, int height, int[] componentPixels) {
        boolean[] compMask = new boolean[width * height];
        for (int p : componentPixels) {
            compMask[p] = true;
        }

        // Find a starting edge pixel
        Vector2i start = null;
        for (int p : componentPixels) {
            int x = p % width;
            int y = p / width;
            if (isEdgePixel(x, y, width, height, compMask)) {
                start = new Vector2i(x, y);
                break;
            }
        }

        if (start == null) {
            return Collections.emptyList();
        }

        return traceMoore(start, width, height, compMask);
    }

    private boolean isEdgePixel(int x, int y, int w, int h, boolean[] mask) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                if (!isSolid(x + dx, y + dy, w, h, mask)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSolid(int x, int y, int w, int h, boolean[] mask) {
        if (x < 0 || x >= w || y < 0 || y >= h) return false;
        return mask[y * w + x];
    }

    /**
     * Moore neighborhood boundary tracing.
     */
    private List<Vector2i> traceMoore(Vector2i start, int w, int h, boolean[] mask) {
        Vector2i[] dir = {
                new Vector2i(1, 0), new Vector2i(1, 1), new Vector2i(0, 1), new Vector2i(-1, 1),
                new Vector2i(-1, 0), new Vector2i(-1, -1), new Vector2i(0, -1), new Vector2i(1, -1)
        };

        Vector2i current = new Vector2i(start);
        int backDirIndex = 4;
        List<Vector2i> contour = new ArrayList<>(4096);
        contour.add(new Vector2i(current));

        int maxSteps = w * h * 4;
        int steps = 0;

        while (steps++ < maxSteps) {
            Vector2i foundNext = null;
            int foundDirIndex = -1;

            for (int i = 1; i <= 8; i++) {
                int idx = (backDirIndex + i) & 7;
                int nx = current.x + dir[idx].x;
                int ny = current.y + dir[idx].y;
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                if (!mask[ny * w + nx]) continue;
                foundNext = new Vector2i(nx, ny);
                foundDirIndex = idx;
                break;
            }

            if (foundNext == null) break;

            if (foundNext.x == start.x && foundNext.y == start.y && contour.size() > 10) break;

            contour.add(new Vector2i(foundNext));
            backDirIndex = (foundDirIndex + 4) & 7;
            current.set(foundNext);
        }

        return contour;
    }

    // ---- Internal: resampling & coordinate mapping ----

    private List<Vector2d> resampleClosedPath(List<Vector2i> path, int targetCount) {
        List<Vector2d> pts = new ArrayList<>(path.size());
        for (Vector2i v : path) {
            pts.add(new Vector2d(v.x, v.y));
        }
        if (pts.size() <= targetCount) {
            return pts;
        }

        int n = pts.size();
        double[] dist = new double[n + 1];
        dist[0] = 0.0;
        for (int i = 1; i <= n; i++) {
            Vector2d a = pts.get(i - 1);
            Vector2d b = pts.get(i % n);
            dist[i] = dist[i - 1] + Math.hypot(b.x - a.x, b.y - a.y);
        }

        double total = dist[n];
        double step = total / targetCount;
        List<Vector2d> out = new ArrayList<>(targetCount);
        int seg = 0;

        for (int k = 0; k < targetCount; k++) {
            double d = k * step;
            while (seg + 1 <= n && dist[seg + 1] < d) {
                seg++;
            }
            Vector2d a = pts.get(seg % n);
            Vector2d b = pts.get((seg + 1) % n);
            double db = dist[seg + 1];
            double da = dist[seg];
            double t = (db - da <= 1.0E-9) ? 0.0 : (d - da) / (db - da);
            out.add(new Vector2d(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t));
        }
        return out;
    }

    private List<Vector2d> mapToXZ(List<Vector2d> pts, int width, int height, double step, boolean centerMode) {
        double cx, cy;
        if (centerMode) {
            double sx = 0.0, sy = 0.0;
            for (Vector2d p : pts) {
                sx += p.x;
                sy += p.y;
            }
            cx = sx / pts.size();
            cy = sy / pts.size();
        } else {
            cx = width / 2.0;
            cy = height / 2.0;
        }

        List<Vector2d> result = new ArrayList<>(pts.size());
        for (Vector2d p : pts) {
            result.add(new Vector2d((p.x - cx) * step, (cy - p.y) * step));
        }
        return result;
    }

    // ---- Internal: DFT ----

    private Vector2d[] dft(List<? extends Vector2d> samples) {
        int n = samples.size();
        Vector2d[] out = new Vector2d[n];
        for (int i = 0; i < n; i++) {
            out[i] = new Vector2d(0.0, 0.0);
        }

        for (int k = 0; k < n; k++) {
            double re = 0.0, im = 0.0;
            double coef = -2.0 * Math.PI * k / n;
            for (int i = 0; i < n; i++) {
                double angle = coef * i;
                double c = Math.cos(angle);
                double s = Math.sin(angle);
                Vector2d x = samples.get(i);
                re += x.x * c - x.y * s;
                im += x.x * s + x.y * c;
            }
            out[k].set(re / n, im / n);
        }
        return out;
    }

    // ---- Internal: coefficients to Fourier list ----

    private List<FourierSeriesBuilder.Fourier> coeffsToFouriers(Vector2d[] coeffs, int harmonics, boolean sortByAmplitude) {
        int n = coeffs.length;
        List<FourierSeriesBuilder.Fourier> all = new ArrayList<>(n);

        for (int k = 0; k < n; k++) {
            double w = kToW(n, k);
            Vector2d c = coeffs[k];
            double r = Math.hypot(c.x, c.y);
            double angRad = Math.atan2(c.y, c.x);
            double startAngleDeg = Math.toDegrees(angRad);
            all.add(new FourierSeriesBuilder.Fourier(w, r, startAngleDeg));
        }

        // Find the DC component (w == 0)
        FourierSeriesBuilder.Fourier zero = all.stream()
                .filter(f -> f.w() == 0.0)
                .findFirst()
                .orElse(new FourierSeriesBuilder.Fourier(0.0, 0.0, 0.0));

        // Non-DC components
        List<FourierSeriesBuilder.Fourier> others = all.stream()
                .filter(f -> f.w() != 0.0)
                .collect(Collectors.toList());

        List<FourierSeriesBuilder.Fourier> selected;
        if (sortByAmplitude) {
            // Take top harmonics by amplitude
            selected = others.stream()
                    .sorted((a, b) -> Double.compare(b.r(), a.r()))
                    .limit(harmonics)
                    .collect(Collectors.toList());
        } else {
            // Take harmonics by frequency: -h..-1, 1..h
            int h = Math.max(Math.min(harmonics, n / 2 - 1), 1);
            List<FourierSeriesBuilder.Fourier> freqList = new ArrayList<>(2 * h);
            for (int w = -h; w < 0; w++) {
                final int ww = w;
                freqList.add(all.stream().filter(f -> f.w() == ww).findFirst()
                        .orElseThrow(() -> new NoSuchElementException("No element with w=" + ww)));
            }
            for (int w = 1; w <= h; w++) {
                final int ww = w;
                freqList.add(all.stream().filter(f -> f.w() == ww).findFirst()
                        .orElseThrow(() -> new NoSuchElementException("No element with w=" + ww)));
            }
            selected = freqList;
        }

        List<FourierSeriesBuilder.Fourier> result = new ArrayList<>(1 + selected.size());
        result.add(zero);
        result.addAll(selected);
        return result;
    }

    private static int kToW(int n, int k) {
        return k <= n / 2 ? k : k - n;
    }
}

package com.mprlab.portal;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

/** Worker-owned visual tracking of the selected head and upper body in the full camera frame. */
final class PhotoMotion {
    private final Mat previous = new Mat();
    private final MatOfPoint2f points = new MatOfPoint2f();
    private long selection = -1;
    private int frames;
    private PhotoFaces.Face target;
    boolean needsDetection(long chosen) { return chosen != selection || frames % 4 == 0 || points.empty(); }
    void reset() { selection = -1; target = null; frames = 0; previous.release(); points.release(); }

    List<PhotoFaces.Face> follow(Bitmap source, long chosen, PhotoFaces.Face selected, List<PhotoFaces.Face> faces) {
        Mat gray = gray(source);
        try {
            if (selection != chosen) { reset(); selection = chosen; target = selected; }
            Flow flow = previous.empty() ? null : flow(gray);
            PhotoFaces.Face predicted = flow == null ? target : flow.face;
            List<PhotoFaces.Face> anchors = near(predicted, faces);
            if (anchors.size() > 1) { gray.copyTo(previous); points.release(); return anchors; }
            if (anchors.size() == 1) {
                target = anchors.get(0); seed(gray, target);
            } else if (flow != null) {
                target = flow.face; points.fromList(flow.points);
            } else {
                gray.copyTo(previous); points.release(); frames++;
                return Collections.emptyList();
            }
            gray.copyTo(previous); frames++;
            return Collections.singletonList(target);
        } finally { gray.release(); }
    }
    boolean confirms(Bitmap captured, PhotoFaces.Face selected) {
        List<PhotoFaces.Face> anchors = near(selected, PhotoFaces.find(captured));
        if (anchors.size() > 1) return false;
        if (anchors.size() == 1) return true;
        Mat gray = gray(captured);
        try { Flow flow = previous.empty() ? null : flow(gray); return flow != null && near(selected, Collections.singletonList(flow.face)).size() == 1; }
        finally { gray.release(); }
    }
    private static List<PhotoFaces.Face> near(PhotoFaces.Face selected, List<PhotoFaces.Face> faces) {
        List<PhotoFaces.Face> result = new ArrayList<>();
        for (PhotoFaces.Face face : faces) {
            float ratio = face.span / selected.span;
            if (ratio >= .5f && ratio <= 2 && Math.hypot(face.x - selected.x, face.y - selected.y) <= Math.min(.2f, selected.span * .8f)) result.add(face);
        }
        return result;
    }
    private static Mat gray(Bitmap source) {
        float scale = 320f / Math.max(source.getWidth(), source.getHeight());
        int width = Math.max(2, Math.round(source.getWidth() * scale)), height = Math.max(2, Math.round(source.getHeight() * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(source, width, height, true);
        Mat rgba = new Mat(), gray = new Mat();
        try { Utils.bitmapToMat(scaled, rgba); Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY); return gray; }
        finally { rgba.release(); if (scaled != source) scaled.recycle(); }
    }
    private void seed(Mat gray, PhotoFaces.Face face) {
        Mat mask = Mat.zeros(gray.rows(), gray.cols(), CvType.CV_8UC1);
        MatOfPoint corners = new MatOfPoint();
        try {
            // Include head boundaries and upper-body texture so a turned or briefly covered face remains trackable.
            Point left = new Point(Math.max(0, (face.x - face.span * .85) * gray.cols()), Math.max(0, (face.y - face.height * .65) * gray.rows()));
            Point right = new Point(Math.min(gray.cols() - 1, (face.x + face.span * .85) * gray.cols()), Math.min(gray.rows() - 1, (face.y + face.height * 1.7) * gray.rows()));
            Imgproc.rectangle(mask, left, right, new Scalar(255), -1);
            Imgproc.goodFeaturesToTrack(gray, corners, 80, .01, 4, mask, 3, false, .04);
            points.fromArray(corners.toArray());
        } finally { mask.release(); corners.release(); }
    }
    private static final class Flow {
        final PhotoFaces.Face face;
        final List<Point> points;
        Flow(PhotoFaces.Face face, List<Point> points) { this.face = face; this.points = points; }
    }
    private Flow flow(Mat current) {
        if (points.rows() < 6 || previous.cols() != current.cols() || previous.rows() != current.rows()) return null;
        MatOfPoint2f next = new MatOfPoint2f(), back = new MatOfPoint2f();
        MatOfByte valid = new MatOfByte(), reverseValid = new MatOfByte();
        MatOfFloat errors = new MatOfFloat(), reverseErrors = new MatOfFloat();
        try {
            TermCriteria criteria = new TermCriteria(TermCriteria.COUNT | TermCriteria.EPS, 20, .03);
            Video.calcOpticalFlowPyrLK(previous, current, points, next, valid, errors, new Size(21, 21), 3, criteria);
            Video.calcOpticalFlowPyrLK(current, previous, next, back, reverseValid, reverseErrors, new Size(21, 21), 3, criteria);
            Point[] old = points.toArray(), moved = next.toArray(), reversed = back.toArray();
            byte[] status = valid.toArray(), reverseStatus = reverseValid.toArray(); float[] error = errors.toArray();
            List<Integer> reliable = new ArrayList<>();
            for (int i = 0; i < old.length; i++) {
                if (status[i] == 0 || reverseStatus[i] == 0 || error[i] > 25
                        || Math.hypot(old[i].x - reversed[i].x, old[i].y - reversed[i].y) > 1.5
                        || moved[i].x < 0 || moved[i].x >= current.cols() || moved[i].y < 0 || moved[i].y >= current.rows()) continue;
                reliable.add(i);
            }
            if (reliable.size() < 6) return null;
            double[] dx = new double[reliable.size()], dy = new double[reliable.size()];
            for (int i = 0; i < reliable.size(); i++) { int p = reliable.get(i); dx[i] = moved[p].x - old[p].x; dy[i] = moved[p].y - old[p].y; }
            Arrays.sort(dx); Arrays.sort(dy); double mx = dx[dx.length / 2], my = dy[dy.length / 2];
            List<Point> inliers = new ArrayList<>();
            for (int p : reliable) if (Math.hypot(moved[p].x - old[p].x - mx, moved[p].y - old[p].y - my) < 3) inliers.add(moved[p]);
            if (inliers.size() < 6 || inliers.size() * 2 < reliable.size() || Math.hypot(mx, my) > current.cols() * .15) return null;
            float x = target.x + (float) mx / current.cols(), y = target.y + (float) my / current.rows();
            if (x < 0 || x > 1 || y < 0 || y > 1) return null;
            return new Flow(target.translated((float) mx / current.cols(), (float) my / current.rows()), inliers);
        } finally { next.release(); back.release(); valid.release(); reverseValid.release(); errors.release(); reverseErrors.release(); }
    }
}

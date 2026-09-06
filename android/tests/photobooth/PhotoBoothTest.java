package com.mprlab.portal.photoboothtest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

public final class PhotoBoothTest extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            getTargetContext().getSharedPreferences("children_portal", Context.MODE_PRIVATE).edit()
                    .putString("profiles_json", "[{\"id\":\"photo-test\",\"name\":\"Photo Test\"}]")
                    .putString("active_profile_id", "photo-test").commit();
            Activity home = startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal.MainActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            ActivityMonitor monitor = addMonitor("com.mprlab.portal.PhotoBoothActivity", null, false);
            View[] tile = new View[1];
            runOnMainSync(() -> tile[0] = find(home.getWindow().getDecorView(), "Photo Booth. Take a picture"));
            if (tile[0] == null) throw new AssertionError("Missing control: Photo Booth. Take a picture");
            runOnMainSync(() -> tile[0].performClick());
            Activity booth = waitForMonitorWithTimeout(monitor, 4000);
            removeMonitor(monitor);
            if (booth == null) throw new AssertionError("Photo Booth did not open");
            result.putString("stream", "PhotoBooth passed: Home opens Photo Booth.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "PhotoBooth failed: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private View find(View view, String label) {
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), label);
                if (found != null) return found;
            }
        }
        return null;
    }
}

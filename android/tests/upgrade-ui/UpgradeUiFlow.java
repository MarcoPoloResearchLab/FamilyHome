package com.mprlab.portal.upgradeuitest;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.util.Xml;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlSerializer;

public final class UpgradeUiFlow extends Instrumentation {
    private static final String APPLICATION_PACKAGE = "com.mprlab.portal";
    private static final long WINDOW_TIMEOUT_MS = 3000;
    private static final String HOME_MARKER = "content-desc=\"Games. Choose and play\"";
    private UiAutomation automation;
    private String stage = "connect accessibility";
    private String lastHierarchy = "";

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            automation = getUiAutomation();
            AccessibilityServiceInfo service = automation.getServiceInfo();
            service.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            automation.setServiceInfo(service);
            exerciseNavigation();
            result.putString("result", "Upgrade UI navigation passed");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Upgrade UI failed at " + stage + ": " + Log.getStackTraceString(error));
            result.putString("ui", lastHierarchy);
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void exerciseNavigation() throws Exception {
        stage = "reject another application";
        shell("input keyevent KEYCODE_HOME");
        rejectForeignWindow();
        shell("am start -W -n " + APPLICATION_PACKAGE + "/.MainActivity");
        waitForActivity("MainActivity");
        String home = capture(HOME_MARKER);
        if (home.indexOf(HOME_MARKER) != home.lastIndexOf(HOME_MARKER)
                || home.contains("text=\"Race\"") || home.contains("content-desc=\"Race.")
                || home.contains("Kart Adventure")) {
            throw new AssertionError("Home must contain exactly one Games entry");
        }
        for (int round = 0; round < 5; round++) {
            clickLabel("Open settings");
            waitForActivity("SettingsActivity");
            capture("text=\"Settings\"");
            backTo("MainActivity");
            capture(HOME_MARKER);
        }
        clickLabel("Draw. Make a picture");
        waitForActivity("DrawingActivity");
        backTo("MainActivity");
        clickLabel("Music. Choose an instrument");
        waitForActivity("MusicActivity");
        clickLabel("Piano");
        waitForActivity("PianoActivity");
        shell("input tap 65 400");
        waitForActivity("PianoActivity");
        backTo("MusicActivity");
        backTo("MainActivity");
        clickLabel("Games. Choose and play");
        waitForActivity("GameLibraryActivity");
        String library = capture("content-desc=\"Match. ");
        for (String game : new String[]{"Kart", "Blocks", "Tiles", "Match"}) {
            if (!library.contains("content-desc=\"" + game + ". ")) {
                throw new AssertionError("Game library is missing " + game);
            }
        }
        backTo("MainActivity");
        capture(HOME_MARKER);
    }

    private void rejectForeignWindow() {
        long deadline = SystemClock.uptimeMillis() + WINDOW_TIMEOUT_MS;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = focusedRoot();
            if (root != null) {
                boolean foreign = !APPLICATION_PACKAGE.contentEquals(root.getPackageName() == null ? "" : root.getPackageName());
                root.recycle();
                if (foreign) {
                    AccessibilityNodeInfo accepted = focusedApplicationRoot();
                    if (accepted != null) {
                        accepted.recycle();
                        throw new AssertionError("Capture accepted FamilyHome while the launcher has focus");
                    }
                    return;
                }
            }
            SystemClock.sleep(100);
        }
        throw new AssertionError("Android launcher did not receive input focus");
    }

    private void backTo(String activity) throws Exception {
        shell("input keyevent KEYCODE_BACK");
        waitForActivity(activity);
    }

    private void waitForActivity(String activity) throws Exception {
        stage = "wait for " + activity;
        String resumed = "";
        String focused = "";
        for (int attempt = 0; attempt < 20; attempt++) {
            boolean activityResumed = false;
            for (String line : shell("dumpsys activity activities").split("\n")) {
                if (!line.contains("mResumedActivity") && !line.contains("topResumedActivity")) continue;
                resumed = line;
                if (line.contains(APPLICATION_PACKAGE + "/." + activity + " ")
                        || line.contains(APPLICATION_PACKAGE + "/" + APPLICATION_PACKAGE + "." + activity + " ")) activityResumed = true;
            }
            if (activityResumed) {
                for (String line : shell("dumpsys window").split("\n")) {
                    if (!line.contains("mCurrentFocus=")) continue;
                    focused = line;
                    if (line.contains(APPLICATION_PACKAGE + "/." + activity + "}")
                            || line.contains(APPLICATION_PACKAGE + "/" + APPLICATION_PACKAGE + "." + activity + "}")) return;
                }
            }
            SystemClock.sleep(250);
        }
        throw new AssertionError("Activity did not open: " + activity + "; resumed: " + resumed + "; focused: " + focused);
    }

    private String capture(String marker) throws Exception {
        stage = "capture " + marker;
        long deadline = SystemClock.uptimeMillis() + WINDOW_TIMEOUT_MS;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = focusedApplicationRoot();
            if (root != null) {
                StringWriter output = new StringWriter();
                XmlSerializer xml = Xml.newSerializer();
                xml.setOutput(output);
                xml.startTag(null, "hierarchy");
                writeNode(xml, root);
                xml.endTag(null, "hierarchy");
                xml.flush();
                lastHierarchy = output.toString();
                if (lastHierarchy.contains(marker)) return lastHierarchy;
            }
            SystemClock.sleep(100);
        }
        throw new AssertionError("FamilyHome window does not contain " + marker);
    }

    private void clickLabel(String label) {
        stage = "click " + label;
        long deadline = SystemClock.uptimeMillis() + WINDOW_TIMEOUT_MS;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = focusedApplicationRoot();
            List<AccessibilityNodeInfo> matches = new ArrayList<>();
            try {
                if (root != null) collectLabel(root, label, matches);
                if (matches.size() > 1) throw new AssertionError("Duplicate control: " + label);
                if (matches.size() == 1) {
                    if (!matches.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        throw new AssertionError("Control rejected click: " + label);
                    }
                    return;
                }
            } finally {
                if (root != null) root.recycle();
                for (AccessibilityNodeInfo match : matches) match.recycle();
            }
            SystemClock.sleep(100);
        }
        throw new AssertionError("Control is unavailable: " + label);
    }

    private void collectLabel(AccessibilityNodeInfo node, String label, List<AccessibilityNodeInfo> matches) {
        if (label.contentEquals(node.getContentDescription() == null ? "" : node.getContentDescription())) {
            matches.add(AccessibilityNodeInfo.obtain(node));
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            AccessibilityNodeInfo child = node.getChild(index);
            if (child == null) continue;
            try { collectLabel(child, label, matches); }
            finally { child.recycle(); }
        }
    }

    private AccessibilityNodeInfo focusedApplicationRoot() {
        AccessibilityNodeInfo root = focusedRoot();
        if (root == null) return null;
        if (APPLICATION_PACKAGE.contentEquals(root.getPackageName() == null ? "" : root.getPackageName())) return root;
        root.recycle();
        return null;
    }

    private AccessibilityNodeInfo focusedRoot() {
        List<AccessibilityWindowInfo> windows = automation.getWindows();
        try {
            for (AccessibilityWindowInfo window : windows) {
                if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION || !window.isFocused()) continue;
                AccessibilityNodeInfo root = window.getRoot();
                if (root != null) return root;
            }
            return null;
        } finally {
            for (AccessibilityWindowInfo window : windows) window.recycle();
        }
    }

    private String shell(String command) throws Exception {
        try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command));
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void writeNode(XmlSerializer xml, AccessibilityNodeInfo node) throws Exception {
        try {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            xml.startTag(null, "node");
            xml.attribute(null, "text", node.getText() == null ? "" : node.getText().toString());
            xml.attribute(null, "content-desc", node.getContentDescription() == null ? "" : node.getContentDescription().toString());
            xml.attribute(null, "bounds", bounds.toShortString());
            for (int index = 0; index < node.getChildCount(); index++) {
                AccessibilityNodeInfo child = node.getChild(index);
                if (child != null) writeNode(xml, child);
            }
            xml.endTag(null, "node");
        } finally {
            node.recycle();
        }
    }
}

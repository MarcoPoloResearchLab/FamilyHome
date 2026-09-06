package com.mprlab.portal.upgradeuitest;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Xml;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.io.StringWriter;
import java.util.List;
import org.xmlpull.v1.XmlSerializer;

public final class UpgradeUiDump extends Instrumentation {
    private static final String APPLICATION_PACKAGE = "com.mprlab.portal";

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            UiAutomation automation = getUiAutomation();
            AccessibilityServiceInfo service = automation.getServiceInfo();
            service.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            automation.setServiceInfo(service);
            AccessibilityNodeInfo root = null;
            long deadline = SystemClock.uptimeMillis() + 3000;
            while (root == null && SystemClock.uptimeMillis() < deadline) {
                root = focusedApplicationRoot(automation);
                if (root == null) SystemClock.sleep(100);
            }
            if (root == null) throw new AssertionError("Focused application window is unavailable: " + APPLICATION_PACKAGE);
            StringWriter output = new StringWriter();
            XmlSerializer xml = Xml.newSerializer();
            xml.setOutput(output);
            xml.startTag(null, "hierarchy");
            writeNode(xml, root);
            xml.endTag(null, "hierarchy");
            xml.flush();
            result.putString("ui", output.toString());
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Upgrade UI capture failed: " + error);
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private AccessibilityNodeInfo focusedApplicationRoot(UiAutomation automation) {
        List<AccessibilityWindowInfo> windows = automation.getWindows();
        try {
            for (AccessibilityWindowInfo window : windows) {
                if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION || !window.isFocused()) continue;
                AccessibilityNodeInfo root = window.getRoot();
                if (root == null) continue;
                if (APPLICATION_PACKAGE.contentEquals(root.getPackageName() == null ? "" : root.getPackageName())) return root;
                root.recycle();
            }
            return null;
        } finally {
            for (AccessibilityWindowInfo window : windows) window.recycle();
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

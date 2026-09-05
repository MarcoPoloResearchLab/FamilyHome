package com.mprlab.portal.upgradeuitest;

import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Xml;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.StringWriter;
import org.xmlpull.v1.XmlSerializer;

public final class UpgradeUiDump extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            AccessibilityNodeInfo root = null;
            long deadline = SystemClock.uptimeMillis() + 3000;
            while (root == null && SystemClock.uptimeMillis() < deadline) {
                root = getUiAutomation().getRootInActiveWindow();
                if (root == null) SystemClock.sleep(100);
            }
            if (root == null) throw new AssertionError("Active accessibility window is unavailable");
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

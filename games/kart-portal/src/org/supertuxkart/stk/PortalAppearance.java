package org.supertuxkart.stk;

import android.app.Activity;
import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Applies the FamilyHome skin before the native game reads its configuration. */
public final class PortalAppearance {
    public static void install(Activity activity) {
        try {
            File game = new File(activity.getExternalFilesDir(null), "supertuxkart");
            File file = new File(game, "home/supertuxkart/config-0.10/config.xml");
            if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) {
                throw new IllegalStateException("Cannot create Kart configuration directory");
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);
            javax.xml.parsers.DocumentBuilder parser = factory.newDocumentBuilder();
            parser.setEntityResolver((publicId, systemId) -> { throw new org.xml.sax.SAXException("External configuration entities are not allowed"); });
            Document doc;
            if (file.isFile()) doc = parser.parse(file);
            else {
                doc = parser.newDocument();
                Element root = doc.createElement("stkconfig");
                root.setAttribute("version", "8");
                doc.appendChild(root);
            }
            element(doc, "skin_name").setAttribute("value", "classic");
            float scale = activity.getResources().getConfiguration().fontScale;
            element(doc, "Video").setAttribute("font_size", Float.toString(Math.min(6, 4 + (scale - 1) * 7)));
            File temporary = new File(file.getPath() + ".tmp");
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(temporary));
            if (!temporary.renameTo(file)) throw new IllegalStateException("Cannot save Kart appearance configuration");
            // Invalidate extracted interface assets when this adaptation changes. The engine preserves home/save data.
            String identity;
            try (java.io.InputStream input = activity.getAssets().open("portal-interface-id")) {
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[128]; int count;
                while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
                identity = bytes.toString("UTF-8");
            }
            File stamp = new File(game, "home/portal-interface-id");
            String previous = stamp.isFile() ? new String(java.nio.file.Files.readAllBytes(stamp.toPath()), java.nio.charset.StandardCharsets.UTF_8) : "";
            if (!identity.equals(previous)) {
                File marker = new File(game, ".extracted");
                if (marker.isFile() && !marker.delete()) throw new IllegalStateException("Cannot refresh Kart interface assets");
                java.nio.file.Files.write(stamp.toPath(), identity.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception error) {
            throw new IllegalStateException("Cannot prepare the FamilyHome Kart interface", error);
        }
    }

    private static Element element(Document doc, String name) {
        org.w3c.dom.NodeList nodes = doc.getDocumentElement().getElementsByTagName(name);
        if (nodes.getLength() != 0) return (Element) nodes.item(0);
        Element node = doc.createElement(name);
        doc.getDocumentElement().appendChild(node);
        return node;
    }
}

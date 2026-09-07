package com.mprlab.portal.styletest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

public final class StyleTest extends Instrumentation {
    private static final String[] ACTIONS = {"Draw. Make a picture", "Ask. Learn something",
        "Music. Choose an instrument", "Games. Choose and play", "Photo Booth. Take a picture"};
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            getTargetContext().getSharedPreferences("children_portal", Context.MODE_PRIVATE).edit()
                .putString("profiles_json", "[{\"id\":\"style-test\",\"name\":\"Sam\"}]")
                .putString("active_profile_id", "style-test")
                .putString("drawings_style-test", sampleDrawing())
                .putString("active_drawing_style-test", "sample-house")
                .putString("weather_location", "Demo town")
                .putString("weather_cache_location", "Demo town")
                .putLong("weather_cache_updated_at", System.currentTimeMillis())
                .putString("weather_cache_json", "{\"location\":\"Demo town\",\"temperature_f\":68,\"feels_like_f\":66,\"high_f\":72,\"low_f\":58,\"precipitation_probability\":10,\"condition\":\"Partly cloudy\",\"icon\":\"partly_cloudy\"}")
                .commit();
            setInTouchMode(false);
            Activity home = open("MainActivity");
            ready();
            final Activity initialHome = home;
            capture("home");
            onUi(() -> {
                View decor = initialHome.getWindow().getDecorView();
                Rect previous = null;
                for (String label : ACTIONS) {
                    View tile = required(decor, label);
                    Rect bounds = bounds(tile);
                    float density = initialHome.getResources().getDisplayMetrics().density;
                    if (bounds.height() / density < 180) throw new AssertionError("Activity illustration needs a tall tile: " + label + " " + bounds);
                    if (previous != null && (bounds.left <= previous.right || bounds.top != previous.top)) {
                        throw new AssertionError("Activity tiles must share one separated row");
                    }
                    previous = bounds;
                    assertOutline(tile);
                    assertTextFits(tile);
                }
                assertTextFits(decor);
            });
            capture("home");
            for (String[] entry : new String[][]{{ACTIONS[0], "DrawingActivity", "drawing"},
                    {ACTIONS[1], "AskActivity", "ask"}, {ACTIONS[2], "MusicActivity", "music"}, {ACTIONS[3], "GameLibraryActivity", "games"}}) {
                Activity screen = clickOpen(home, entry[0], entry[1]);
                ready();
                onUi(() -> {
                    View root = screen.getWindow().getDecorView();
                    assertOutline(required(root, "Home"));
                    assertOutline(required(root, "Back"));
                    assertTextFits(root);
                    assertButtons(root);
                    if (entry[1].equals("DrawingActivity")) {
                        View blue = required(root, "Blue brush");
                        blue.performClick();
                        if (!blue.isSelected()) throw new AssertionError("Selected brush must expose its selection");
                        View eraser = required(root, "Eraser");
                        eraser.performClick();
                        if (!eraser.isSelected() || blue.isSelected()) throw new AssertionError("Eraser selection must replace color selection");
                        blue.performClick();
                        assertOutline(required(root, "New picture"));
                    }
                });
                if (entry[1].equals("DrawingActivity")) {
                    drawGround(screen);
                    ready();
                }
                capture(entry[2]);
                if (entry[1].equals("GameLibraryActivity")) {
                    assertGameIllustrations(screen);
                    capture("games-scrolled");
                }
                home = clickOpen(screen, "Home", "MainActivity");
                ready();
                if (entry[1].equals("DrawingActivity")) {
                    JSONArray documents = new JSONArray(getTargetContext().getSharedPreferences("children_portal", Context.MODE_PRIVATE)
                        .getString("drawings_style-test", "[]"));
                    if (documents.getJSONObject(0).getJSONArray("strokes").length() != 10) {
                        throw new AssertionError("Home did not save the new drawing stroke");
                    }
                }
            }
            Activity music = clickOpen(home, ACTIONS[2], "MusicActivity"); ready();
            for (String instrument : new String[]{"Piano", "Guitar"}) {
                Activity screen = clickOpen(music, instrument, instrument + "Activity"); ready();
                onUi(() -> { assertTextFits(screen.getWindow().getDecorView()); assertButtons(screen.getWindow().getDecorView()); });
                if (instrument.equals("Piano")) onUi(() -> {
                    TextView readout = findText(screen.getWindow().getDecorView(), "Tap a key");
                    if (readout == null) throw new AssertionError("Missing Piano instruction");
                    bounds(readout);
                    if (readout.getCurrentTextColor() != Color.BLACK)
                        throw new AssertionError("Piano readout must use black text on its colored surface");
                });
                capture(instrument.toLowerCase());
                home = clickOpen(screen, "Home", "MainActivity"); ready();
                music = clickOpen(home, ACTIONS[2], "MusicActivity"); ready();
            }
            home = clickOpen(music, "Home", "MainActivity"); ready();
            Activity booth = clickOpen(home, ACTIONS[4], "PhotoBoothActivity");
            for (int attempt = 0; attempt < 100; attempt++) {
                final boolean[] found = {false};
                onUi(() -> found[0] = find(booth.getWindow().getDecorView(), "Take picture") != null);
                if (found[0]) break;
                SystemClock.sleep(100);
            }
            ready();
            onUi(() -> {
                View root = booth.getWindow().getDecorView();
                TextView shutter = (TextView) required(root, "Take picture");
                assertButtons(root);
                bounds(shutter);
                if (shutter.getTextSize() + .1f < android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 24, shutter.getResources().getDisplayMetrics()))
                    throw new AssertionError("Shutter needs primary action typography");
                assertTextFits(root);
            });
            capture("photobooth");
            Rect[] shutterBounds = {null};
            onUi(() -> shutterBounds[0] = bounds(required(booth.getWindow().getDecorView(), "Take picture")));
            long scrollDown = SystemClock.uptimeMillis();
            for (int index = 0; index <= 10; index++) {
                int action = index == 0 ? MotionEvent.ACTION_DOWN : index == 10 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_MOVE;
                MotionEvent event = MotionEvent.obtain(scrollDown, SystemClock.uptimeMillis(), action, 1100, 650 - index * 40, 0);
                sendPointerSync(event); event.recycle(); SystemClock.sleep(16);
            }
            ready();
            onUi(() -> {
                if (!shutterBounds[0].equals(bounds(required(booth.getWindow().getDecorView(), "Take picture"))))
                    throw new AssertionError("Photo Booth shutter moved with the options");
            });
            capture("photobooth-scrolled");
            onUi(() -> required(booth.getWindow().getDecorView(), "Filters").performClick());
            ready(); capture("photobooth-filters");
            home = clickOpen(booth, "Home", "MainActivity"); ready();
            Activity settings = clickOpen(home, "Open settings", "SettingsActivity");
            ready(); onUi(() -> { assertTextFits(settings.getWindow().getDecorView()); assertButtons(settings.getWindow().getDecorView()); }); capture("settings");
            home = clickOpen(settings, "Home", "MainActivity"); ready();
            final Activity timerHome = home;
            onUi(() -> required(timerHome.getWindow().getDecorView(), "Reading timer, 20 min").performClick());
            ready();
            if (!getTargetContext().getSharedPreferences("children_portal", Context.MODE_PRIVATE)
                    .getString("profiles_json", "").contains("\"timer_running\":true")) throw new AssertionError("Timer action did not persist");
            if (largestTimerText(getUiAutomation().getRootInActiveWindow()) < 64 * getTargetContext().getResources().getDisplayMetrics().density)
                throw new AssertionError("Timer dialog lost its large countdown text");
            capture("timer");
            result.putString("stream", "Style passed: bold controls, readable text, fixed Photo Booth shutter, public navigation, drawing persistence, and timer action.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Style failed: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private void assertGameIllustrations(Activity screen) throws Exception {
        java.util.HashSet<Integer> drawings = new java.util.HashSet<>();
        for (String game : new String[]{"Freedoom", "Kart", "Blocks", "Tiles", "Match"}) {
            ViewGroup[] card = {null};
            onUi(() -> {
                TextView title = findText(screen.getWindow().getDecorView(), game);
                if (title == null) throw new AssertionError("Missing game: " + game);
                card[0] = (ViewGroup) title.getParent();
                card[0].requestRectangleOnScreen(new Rect(0, 0, card[0].getWidth(), card[0].getHeight()), true);
            });
            ready();
            onUi(() -> {
                View illustration = card[0].getChildAt(0);
                float density = illustration.getResources().getDisplayMetrics().density;
                if (illustration instanceof TextView || Math.min(illustration.getWidth(), illustration.getHeight()) < 144 * density)
                    throw new AssertionError(game + " needs a large illustration instead of a text symbol");
                bounds(illustration);
                assertOutline(card[0]);
                assertTextFits(card[0]);
                if (!card[0].isClickable() || !card[0].isFocusable() || !card[0].getContentDescription().toString().startsWith(game + "."))
                    throw new AssertionError(game + " must keep its labeled launch control");
                Bitmap pixels = Bitmap.createBitmap(illustration.getWidth(), illustration.getHeight(), Bitmap.Config.ARGB_8888);
                illustration.draw(new android.graphics.Canvas(pixels));
                int occupied = 0, black = 0, colorful = 0, signature = 1;
                for (int y = 0; y < pixels.getHeight(); y++) for (int x = 0; x < pixels.getWidth(); x++) {
                    int color = pixels.getPixel(x, y);
                    signature = 31 * signature + color;
                    if (Color.alpha(color) < 240) continue;
                    occupied++;
                    int min = Math.min(Color.red(color), Math.min(Color.green(color), Color.blue(color)));
                    int max = Math.max(Color.red(color), Math.max(Color.green(color), Color.blue(color)));
                    if (max < 30) black++;
                    if (max - min > 70) colorful++;
                }
                int area = pixels.getWidth() * pixels.getHeight();
                pixels.recycle();
                if (occupied < area * .25f || black < occupied * .03f || colorful < occupied * .12f)
                    throw new AssertionError(game + " needs bold black outlines and a large colorful drawing: occupied="
                        + occupied + ", black=" + black + ", colorful=" + colorful);
                if (!drawings.add(signature)) throw new AssertionError(game + " needs its own illustration");
            });
        }
    }
    private int largestTimerText(android.view.accessibility.AccessibilityNodeInfo node) {
        if (node == null) return 0;
        int height = 0;
        if (node.getText() != null && node.getText().toString().matches("[0-9]{2}:[0-9]{2}")) {
            Rect rect = new Rect(); node.getBoundsInScreen(rect); height = rect.height();
        }
        for (int i = 0; i < node.getChildCount(); i++) height = Math.max(height, largestTimerText(node.getChild(i)));
        return height;
    }
    private String sampleDrawing() throws Exception {
        JSONArray strokes = new JSONArray();
        stroke(strokes, 0xffe84142, 10, 180, 300, 310, 180, 450, 300);
        stroke(strokes, 0xff0866ff, 10, 220, 470, 220, 300, 420, 300, 420, 470);
        stroke(strokes, 0xff8b5cf6, 10, 295, 470, 295, 385, 350, 385, 350, 470);
        stroke(strokes, 0xffff8a00, 8, 245, 350, 245, 325, 270, 325, 270, 350, 245, 350);
        for (int band = 0; band < 5; band++) {
            JSONArray points = new JSONArray();
            float radius = 165 - band * 16;
            for (int angle = 180; angle <= 360; angle += 5) {
                double radians = Math.toRadians(angle);
                points.put(850 + Math.cos(radians) * radius).put(350 + Math.sin(radians) * radius);
            }
            int[] colors = {0xffe84142, 0xffff8a00, 0xffffd65c, 0xff2fa84f, 0xff8b5cf6};
            strokes.put(new JSONObject().put("color", colors[band]).put("width", 10).put("eraser", false).put("points", points));
        }
        return new JSONArray().put(new JSONObject().put("id", "sample-house").put("title", "Our house").put("strokes", strokes)).toString();
    }
    private void stroke(JSONArray strokes, int color, int width, int... coordinates) throws Exception {
        JSONArray points = new JSONArray();
        for (int coordinate : coordinates) points.put(coordinate);
        strokes.put(new JSONObject().put("color", color).put("width", width).put("eraser", false).put("points", points));
    }
    private void drawGround(Activity activity) {
        onUi(() -> required(activity.getWindow().getDecorView(), "Green brush").performClick());
        long down = SystemClock.uptimeMillis();
        for (int index = 0; index < 3; index++) {
            int action = index == 0 ? MotionEvent.ACTION_DOWN : index == 2 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_MOVE;
            MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, 180 + index * 460, 570, 0);
            sendPointerSync(event); event.recycle();
        }
        onUi(() -> required(activity.getWindow().getDecorView(), "Blue brush").performClick());
    }
    private Activity open(String name) {
        return startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal." + name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private Activity clickOpen(Activity from, String label, String name) {
        ActivityMonitor monitor = addMonitor("com.mprlab.portal." + name, null, false);
        onUi(() -> required(from.getWindow().getDecorView(), label).performClick());
        Activity screen = waitForMonitorWithTimeout(monitor, 5000);
        removeMonitor(monitor);
        if (screen == null) throw new AssertionError(label + " did not open " + name);
        return screen;
    }
    private void ready() throws Exception { waitForIdleSync(); getUiAutomation().waitForIdle(300, 5000); }
    private void onUi(Runnable action) {
        Throwable[] failure = new Throwable[1];
        runOnMainSync(() -> { try { action.run(); } catch (Throwable error) { failure[0] = error; } });
        if (failure[0] != null) throw new AssertionError(failure[0]);
    }
    private void capture(String name) throws Exception {
        try (FileOutputStream out = new FileOutputStream(new File(getTargetContext().getFilesDir(), "style-" + name + ".png"))) {
            getUiAutomation().takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
    private View required(View root, String label) {
        View found = find(root, label);
        if (found == null) throw new AssertionError("Missing control: " + label);
        return found;
    }
    private View find(View view, String label) {
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View result = find(group.getChildAt(i), label);
                if (result != null) return result;
            }
        }
        return null;
    }
    private TextView findText(View view, String value) {
        if (view instanceof TextView && value.contentEquals(((TextView) view).getText())) return (TextView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findText(group.getChildAt(i), value);
                if (found != null) return found;
            }
        }
        return null;
    }
    private Rect bounds(View view) {
        Rect rect = new Rect();
        if (!view.getGlobalVisibleRect(rect) || rect.width() < view.getWidth() || rect.height() < view.getHeight()) {
            throw new AssertionError("Clipped control: " + view.getContentDescription());
        }
        return rect;
    }
    private void assertOutline(View view) {
        Rect visible = bounds(view);
        Bitmap pixels = getUiAutomation().takeScreenshot();
        if (pixels == null) throw new AssertionError("Cannot capture the visible control");
        int longest = 0;
        for (int y = visible.top; y < Math.min(visible.top + 9, pixels.getHeight()); y++) {
            int count = 0;
            for (int x = visible.left; x < visible.right; x++) {
                int color = pixels.getPixel(x, y);
                if (Color.alpha(color) > 240 && Color.red(color) < 30 && Color.green(color) < 30 && Color.blue(color) < 30) count++;
            }
            longest = Math.max(longest, count);
        }
        pixels.recycle();
        if (longest < view.getWidth() * .35f) throw new AssertionError("Missing solid black outline: " + view.getContext().getClass().getSimpleName() + " " + view.getContentDescription() + " pixels=" + longest + "/" + view.getWidth() + " pressed=" + view.isPressed() + " focused=" + view.isFocused() + " rootFocus=" + view.getRootView().findFocus());
    }
    private void assertButtons(View view) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view instanceof Button || view instanceof android.widget.ImageButton) {
            float density = view.getResources().getDisplayMetrics().density;
            if (view.getHeight() + 1 < 60 * density) throw new AssertionError("Control is shorter than 60dp: " + view.getContentDescription());
        }
        if (view instanceof Button) {
            TextView text = (TextView) view;
            float sp = text.getTextSize() / text.getResources().getDisplayMetrics().scaledDensity;
            if (text.getTextSize() + .1f < android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 22, text.getResources().getDisplayMetrics()) || text.getTypeface().getWeight() < 700)
                throw new AssertionError("Control must be at least 22sp and bold: " + text.getText() + " " + sp);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) assertButtons(group.getChildAt(i));
        }
    }
    private void assertTextFits(View view) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getLayout() != null && text.getText().length() > 0) {
                float minimum = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 20, text.getResources().getDisplayMetrics());
                if (text.getTextSize() + .1f < minimum || text.getTypeface().getWeight() < 500)
                    throw new AssertionError("Text must use a readable shared role: " + text.getText());
                int available = text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom();
                if (text.getLayout().getHeight() > available + 2) throw new AssertionError("Vertically clipped text: " + text.getText());
                for (int line = 0; line < text.getLayout().getLineCount(); line++) {
                    if (text.getLayout().getEllipsisCount(line) > 0) throw new AssertionError("Truncated text: " + text.getText());
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) assertTextFits(group.getChildAt(i));
        }
    }
}

package net.nullsum.freedoom;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class PortalGameActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        TextView preparing = new TextView(this);
        preparing.setText("Preparing Freedoom…");
        PortalStyle.text(preparing, PortalStyle.TextRole.TITLE);
        preparing.setTextColor(PortalStyle.INK);
        preparing.setGravity(Gravity.CENTER);
        preparing.setBackgroundColor(PortalStyle.PAPER);
        setContentView(preparing);
        new Thread(this::prepareAndLaunch, "Freedoom preparation").start();
    }

    private void prepareAndLaunch() {
        try {
            String profileID = getIntent().getStringExtra("portal_profile_id");
            if (profileID == null || !profileID.matches("[a-zA-Z0-9_-]+")) {
                throw new IllegalArgumentException("A valid FamilyHome profile is required");
            }
            File base = new File(getFilesDir(), "portal-game");
            directory(base);
            for (String name : new String[]{"freedoom1.wad", "freedoom2.wad", "gzdoom.pk3", "gzdoom.sf2"}) {
                File destination = new File(base, name);
                if (!destination.isFile()) copyAsset(name, destination);
            }
            String scale = getResources().getConfiguration().fontScale >= 1.3f ? "1.3" : "1.0";
            copyAsset("portal-ui-" + scale + ".pk3", new File(base, "portal-ui.pk3"));
            File saves = new File(base, "portal-saves/" + profileID);
            directory(saves);
            Intent game = new Intent(Intent.ACTION_MAIN);
            game.setClassName(getPackageName(), "net.nullsum.freedoom.Game");
            game.putExtra("res_div", 1);
            game.putExtra("game_path", base.getAbsolutePath());
            game.putExtra("game", getPackageName());
            game.putExtra("args", "-iwad freedoom1.wad -file portal-ui.pk3 -savedir " + saves.getAbsolutePath()
                    + " +set fluid_patchset gzdoom.sf2 +set midi_dmxgus 0 +set dimcolor \"ff fb ef\" +set dimamount 1 +set screenblocks 10");
            runOnUiThread(() -> { startActivity(game); finish(); });
        } catch (Exception error) {
            Log.e("FamilyHome", "Cannot prepare Freedoom", error);
            runOnUiThread(() -> {
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Freedoom could not start")
                        .setMessage("Return to Games and try again.").setPositiveButton("Back to Games", (d, which) -> finish()).create();
                dialog.show();
                PortalStyle.dialog(dialog);
            });
        }
    }

    private static void directory(File path) {
        if (!path.isDirectory() && !path.mkdirs()) throw new IllegalStateException("Cannot create " + path.getName());
    }

    private void copyAsset(String name, File destination) throws Exception {
        File temporary = new File(destination.getParentFile(), destination.getName() + ".tmp");
        try (InputStream input = getAssets().open(name); FileOutputStream output = new FileOutputStream(temporary)) {
            byte[] buffer = new byte[32768];
            int count;
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            output.getFD().sync();
        }
        if (!temporary.renameTo(destination)) throw new IllegalStateException("Cannot install " + name);
    }
}

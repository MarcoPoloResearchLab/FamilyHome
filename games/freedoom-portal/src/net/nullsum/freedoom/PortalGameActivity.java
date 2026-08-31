package net.nullsum.freedoom;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

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
        preparing.setTextColor(Color.WHITE);
        preparing.setTextSize(30);
        preparing.setGravity(Gravity.CENTER);
        preparing.setBackgroundColor(Color.rgb(26, 26, 26));
        setContentView(preparing);
        new Thread(() -> prepareAndLaunch()).start();
    }

    private void prepareAndLaunch() {
        try {
            File base = new File(getFilesDir(), "portal-game");
            if (!base.isDirectory() && !base.mkdirs()) throw new IllegalStateException("game directory");
            copyAsset("freedoom1.wad", new File(base, "freedoom1.wad"));
            copyAsset("freedoom2.wad", new File(base, "freedoom2.wad"));
            copyAsset("gzdoom.pk3", new File(base, "gzdoom.pk3"));
            copyAsset("gzdoom.sf2", new File(base, "gzdoom.sf2"));
            String profileID = getIntent().getStringExtra("portal_profile_id");
            if (profileID == null) profileID = "child";
            profileID = profileID.replaceAll("[^a-zA-Z0-9_-]+", "-");
            File saveDirectory = new File(base, "portal-saves/" + profileID);
            if (!saveDirectory.isDirectory() && !saveDirectory.mkdirs()) throw new IllegalStateException("save directory");
            Intent game = new Intent(Intent.ACTION_MAIN);
            game.setClassName(getPackageName(), "net.nullsum.freedoom.Game");
            game.putExtra("res_div", 1);
            game.putExtra("game_path", base.getAbsolutePath());
            game.putExtra("game", getPackageName());
            game.putExtra("args", "-iwad freedoom1.wad -savedir " + saveDirectory.getAbsolutePath()
                    + " +set fluid_patchset gzdoom.sf2 +set midi_dmxgus 0 ");
            runOnUiThread(() -> {
                startActivity(game);
                finish();
            });
        } catch (Exception error) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Freedoom could not be prepared.", Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }

    private void copyAsset(String name, File destination) throws Exception {
        if (destination.isFile() && destination.length() > 0) return;
        File temporary = new File(destination.getParentFile(), destination.getName() + ".tmp");
        try (InputStream input = getAssets().open(name); FileOutputStream output = new FileOutputStream(temporary)) {
            byte[] buffer = new byte[32768];
            int count;
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            output.getFD().sync();
        }
        if (!temporary.renameTo(destination)) throw new IllegalStateException("install " + name);
    }
}

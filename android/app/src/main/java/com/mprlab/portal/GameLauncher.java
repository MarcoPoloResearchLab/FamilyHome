package com.mprlab.portal;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;

final class GameLauncher {
    static boolean isInstalled(Activity activity, GameCatalog.Game game) {
        if (game == null) return false;
        try {
            activity.getPackageManager().getApplicationInfo(game.packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException error) {
            return false;
        }
    }

    static void open(Activity activity, GameCatalog.Game game) {
        if (game == null) return;
        if (!isInstalled(activity, game)) {
            Toast.makeText(activity, game.name + " is not installed on this Portal yet.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent launch = new Intent(Intent.ACTION_MAIN);
        launch.setClassName(game.packageName, game.activityName);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            activity.startActivity(launch);
        } catch (RuntimeException error) {
            Toast.makeText(activity, game.name + " could not open.", Toast.LENGTH_LONG).show();
        }
    }

    private GameLauncher() {
    }
}

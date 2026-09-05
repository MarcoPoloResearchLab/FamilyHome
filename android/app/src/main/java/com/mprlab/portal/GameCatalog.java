package com.mprlab.portal;

import android.content.Intent;
import android.graphics.Color;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class GameCatalog {
    enum LaunchMode {
        PROFILE_ENTRY(0),
        RESUME_TASK(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        final int flags;

        LaunchMode(int flags) {
            this.flags = flags;
        }
    }

    static final class Game {
        final String name;
        final String description;
        final String icon;
        final String packageName;
        final String activityName;
        final LaunchMode launchMode;
        final int color;

        Game(String name, String description, String icon, String packageName,
             String activityName, LaunchMode launchMode, int color) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.packageName = packageName;
            this.activityName = activityName;
            this.launchMode = launchMode;
            this.color = color;
        }
    }

    private static final List<Game> GAMES = Collections.unmodifiableList(Arrays.asList(
            new Game("Freedoom", "Explore maze levels", "F",
                    "net.nullsum.freedoom", "net.nullsum.freedoom.PortalGameActivity",
                    LaunchMode.PROFILE_ENTRY, Color.rgb(0, 166, 153)),
            new Game("Kart", "Race together", "K",
                    "org.supertuxkart.stk", "org.supertuxkart.stk.SuperTuxKartActivity",
                    LaunchMode.RESUME_TASK, Color.rgb(232, 84, 145)),
            new Game("Blocks", "Fit falling shapes", "▦",
                    "com.blockdrop.game", "com.blockdrop.game.MainActivity",
                    LaunchMode.RESUME_TASK, Color.rgb(75, 99, 214)),
            new Game("Tiles", "Make bright patterns", "◆",
                    "net.vantulder.tessel", "net.vantulder.tessel.MainActivity",
                    LaunchMode.RESUME_TASK, Color.rgb(242, 139, 50)),
            new Game("Match", "Find matching pairs", "●●",
                    "org.secuso.privacyfriendlymemory",
                    "org.secuso.privacyfriendlymemory.ui.SplashActivity",
                    LaunchMode.RESUME_TASK, Color.rgb(112, 74, 179))
    ));

    static List<Game> all() {
        return GAMES;
    }

    private GameCatalog() {
    }
}

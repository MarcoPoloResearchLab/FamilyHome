package com.mprlab.portal;

import android.graphics.Color;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class GameCatalog {
    static final String ADVENTURE = "adventure";
    static final String KART = "kart";
    static final String BLOCKS = "blocks";
    static final String TILES = "tiles";
    static final String MATCH = "match";

    static final class Game {
        final String id;
        final String name;
        final String description;
        final String icon;
        final String packageName;
        final String activityName;
        final int color;

        Game(String id, String name, String description, String icon, String packageName,
             String activityName, int color) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.packageName = packageName;
            this.activityName = activityName;
            this.color = color;
        }
    }

    private static final List<Game> GAMES = Collections.unmodifiableList(Arrays.asList(
            new Game(ADVENTURE, "Adventure", "Explore mazes", "A",
                    "net.nullsum.freedoom", "net.nullsum.freedoom.PortalGameActivity",
                    Color.rgb(0, 166, 153)),
            new Game(KART, "Kart", "Race together", "K",
                    "org.supertuxkart.stk", "org.supertuxkart.stk.SuperTuxKartActivity",
                    Color.rgb(232, 84, 145)),
            new Game(BLOCKS, "Blocks", "Fit falling shapes", "▦",
                    "com.blockdrop.game", "com.blockdrop.game.MainActivity",
                    Color.rgb(75, 99, 214)),
            new Game(TILES, "Tiles", "Make bright patterns", "◆",
                    "net.vantulder.tessel", "net.vantulder.tessel.MainActivity",
                    Color.rgb(242, 139, 50)),
            new Game(MATCH, "Match", "Find matching pairs", "●●",
                    "org.secuso.privacyfriendlymemory",
                    "org.secuso.privacyfriendlymemory.ui.SplashActivity",
                    Color.rgb(112, 74, 179))
    ));

    static List<Game> all() {
        return GAMES;
    }

    static Game find(String id) {
        for (Game game : GAMES) {
            if (game.id.equals(id)) return game;
        }
        return null;
    }

    static int enabledCount(ProfileStore.Profile profile) {
        if (profile == null) return 0;
        int count = 0;
        for (Game game : GAMES) {
            if (profile.isGameEnabled(game.id)) count++;
        }
        return count;
    }

    private GameCatalog() {
    }
}

package com.mprlab.portal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class GameCatalog {
    static final class Game {
        final String name;
        final String description;
        final CharacterView.Kind illustration;
        final String packageName;
        final String activityName;
        final int color;

        Game(String name, String description, CharacterView.Kind illustration, String packageName,
             String activityName, int color) {
            this.name = name;
            this.description = description;
            this.illustration = illustration;
            this.packageName = packageName;
            this.activityName = activityName;
            this.color = color;
        }
    }

    private static final List<Game> GAMES = Collections.unmodifiableList(Arrays.asList(
            new Game("Kart", "Race together", CharacterView.Kind.KART,
                    "org.supertuxkart.stk", "org.supertuxkart.stk.SuperTuxKartActivity",
                    PortalStyle.CORAL),
            new Game("Blocks", "Fit falling shapes", CharacterView.Kind.BLOCKS,
                    "com.blockdrop.game", "com.blockdrop.game.MainActivity",
                    PortalStyle.BLUE),
            new Game("Tiles", "Make bright patterns", CharacterView.Kind.TILES,
                    "net.vantulder.tessel", "net.vantulder.tessel.MainActivity",
                    PortalStyle.YELLOW),
            new Game("Match", "Find matching pairs", CharacterView.Kind.MATCH,
                    "org.secuso.privacyfriendlymemory",
                    "org.secuso.privacyfriendlymemory.ui.SplashActivity",
                    PortalStyle.PURPLE)
    ));

    static List<Game> all() {
        return GAMES;
    }

    private GameCatalog() {
    }
}

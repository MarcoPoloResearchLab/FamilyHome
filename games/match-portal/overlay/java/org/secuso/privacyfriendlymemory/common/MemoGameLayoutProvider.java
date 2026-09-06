package org.secuso.privacyfriendlymemory.common;

import android.net.Uri;
import org.secuso.privacyfriendlymemory.model.MemoGame;

public final class MemoGameLayoutProvider {
    private final MemoGame memory;
    private final int columns;
    private final int cardSize;
    private final int boardSize;

    public MemoGameLayoutProvider(MemoGame memory, int width, int height, int spacing) {
        this.memory = memory;
        columns = (int) Math.sqrt(memory.getDifficulty().getDeckSize());
        int gaps = (columns - 1) * spacing;
        cardSize = (Math.min(width, height) - gaps) / columns;
        boardSize = columns * cardSize + gaps;
    }

    public int getColumnCount() {
        return columns;
    }

    public int getDeckSize() {
        return memory.getDifficulty().getDeckSize();
    }

    public int getImageResID(int position) {
        return memory.getImageResID(position);
    }

    public Uri getImageUri(int position) {
        return memory.getImageUri(position);
    }

    public int getCardSizePixel() {
        return cardSize;
    }

    public int getBoardSizePixel() {
        return boardSize;
    }

    public boolean isCustomDeck() {
        return memory.isCustomDesign();
    }
}

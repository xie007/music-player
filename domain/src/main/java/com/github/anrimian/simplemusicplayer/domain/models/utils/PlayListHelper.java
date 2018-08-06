package com.github.anrimian.simplemusicplayer.domain.models.utils;

import com.github.anrimian.simplemusicplayer.domain.models.composition.Composition;
import com.github.anrimian.simplemusicplayer.domain.models.playlist.PlayList;
import com.github.anrimian.simplemusicplayer.domain.utils.Objects;

import javax.annotation.Nonnull;

public class PlayListHelper {

    public static boolean hasChanges(@Nonnull PlayList first, @Nonnull PlayList second) {
        return !Objects.equals(first.getName(), second.getName())
                || !Objects.equals(first.getDateAdded(), second.getDateAdded())
                || !Objects.equals(first.getDateModified(), second.getDateModified())
                || first.getCompositionsCount() != second.getCompositionsCount()
                || first.getTotalDuration() != second.getTotalDuration();
    }
}

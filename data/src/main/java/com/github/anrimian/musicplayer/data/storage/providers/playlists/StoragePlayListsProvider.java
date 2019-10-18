package com.github.anrimian.musicplayer.data.storage.providers.playlists;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Audio.Playlists;

import com.github.anrimian.musicplayer.data.models.exceptions.CompositionNotDeletedException;
import com.github.anrimian.musicplayer.data.models.exceptions.CompositionNotMovedException;
import com.github.anrimian.musicplayer.data.models.exceptions.PlayListAlreadyDeletedException;
import com.github.anrimian.musicplayer.data.models.exceptions.PlayListNotCreatedException;
import com.github.anrimian.musicplayer.data.models.exceptions.PlayListNotDeletedException;
import com.github.anrimian.musicplayer.data.models.exceptions.PlayListNotModifiedException;
import com.github.anrimian.musicplayer.data.utils.IOUtils;
import com.github.anrimian.musicplayer.data.utils.db.CursorWrapper;
import com.github.anrimian.musicplayer.data.utils.rx.content_observer.RxContentObserver;
import com.github.anrimian.musicplayer.domain.models.composition.Composition;
import com.github.anrimian.musicplayer.domain.utils.rx.FastDebounceFilter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.reactivex.Observable;

import static android.provider.BaseColumns._ID;
import static android.provider.MediaStore.Audio.Playlists.Members.AUDIO_ID;
import static android.provider.MediaStore.Audio.Playlists.Members.getContentUri;
import static android.text.TextUtils.isEmpty;
import static com.github.anrimian.musicplayer.domain.utils.Objects.requireNonNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class StoragePlayListsProvider {

    private final ContentResolver contentResolver;

    public StoragePlayListsProvider(Context context) {
        contentResolver = context.getContentResolver();
    }

    public Observable<Map<Long, StoragePlayList>> getPlayListsObservable() {
        return RxContentObserver.getObservable(contentResolver, Playlists.EXTERNAL_CONTENT_URI)
                .map(o -> getPlayLists());
    }

    public Map<Long, StoragePlayList> getPlayLists() {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    Playlists.EXTERNAL_CONTENT_URI,
                    null,
                    null,
                    null,
                    null);
            if (cursor == null) {
                return emptyMap();
            }
            CursorWrapper cursorWrapper = new CursorWrapper(cursor);

            Map<Long, StoragePlayList> map = new HashMap<>();
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                StoragePlayList playList = getPlayListFromCursor(cursorWrapper);
                map.put(playList.getId(), playList);
            }
            return map;
        } finally {
            IOUtils.closeSilently(cursor);
        }
    }

    public StoragePlayList createPlayList(String name) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Playlists.NAME, name);
        contentValues.put(Playlists.DATE_MODIFIED, System.currentTimeMillis() / 1000L);
        Uri uri = contentResolver.insert(Playlists.EXTERNAL_CONTENT_URI, contentValues);
        if (uri == null || isEmpty(uri.getLastPathSegment())) {
            throw new PlayListNotCreatedException();
        }
        long id = Long.valueOf(uri.getLastPathSegment());
        StoragePlayList playList = findPlayList(id);
        if (playList == null) {
            throw new PlayListNotCreatedException();
        }
        return playList;
    }

    public void deletePlayList(long id) {
        int deletedRows = contentResolver.delete(Playlists.EXTERNAL_CONTENT_URI,
                Playlists._ID + " = ?",
                new String[] { String.valueOf(id) });

        if (deletedRows == 0) {
            StoragePlayList storagePlayList = findPlayList(id);
            if (storagePlayList == null) {
                throw new PlayListAlreadyDeletedException();
            }
            throw new PlayListNotDeletedException();
        }
    }

    public Observable<List<StoragePlayListItem>> getPlayListEntriesObservable(long playListId) {
        return RxContentObserver.getObservable(contentResolver, getContentUri("external", playListId))
                .debounce(new FastDebounceFilter<>())
                .map(o -> getPlayListItems(playListId));
    }

    public List<StoragePlayListItem> getPlayListItems(long playListId) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    getContentUri("external", playListId),
                    new String[] {AUDIO_ID, _ID},
                    null,
                    null,
                    Playlists.Members.PLAY_ORDER);
            if (cursor == null) {
                return emptyList();
            }
            CursorWrapper cursorWrapper = new CursorWrapper(cursor);
            List<StoragePlayListItem> items = new ArrayList<>(cursor.getCount());
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);

                StoragePlayListItem item = getPlayListItemFromCursor(cursorWrapper);
                items.add(item);
            }
            return items;
        } finally {
            IOUtils.closeSilently(cursor);
        }
    }

    public void addCompositionToPlayList(long compositionId, long playListId, int position) {
        ContentValues values = new ContentValues();
        values.put(Playlists.Members.PLAY_ORDER, position);
        values.put(Playlists.Members.AUDIO_ID, compositionId);
        values.put(Playlists.Members.PLAYLIST_ID, playListId);

        Uri uri = contentResolver.insert(getContentUri("external", playListId), values);
        if (uri == null) {
            throw new PlayListNotModifiedException();
        }
        updateModifyTime(playListId);
    }

    public void addCompositionsToPlayList(List<Composition> compositions, long playListId, int startPosition) {
        int position = startPosition;
        ContentValues[] valuesList = new ContentValues[compositions.size()];
        for (int i = 0; i < compositions.size(); i++) {
            Long compositionId = compositions.get(i).getStorageId();
            if (compositionId == null) {
                continue;
            }
            ContentValues values = new ContentValues();
            values.put(Playlists.Members.PLAY_ORDER, position);
            values.put(Playlists.Members.AUDIO_ID, compositionId);
            values.put(Playlists.Members.PLAYLIST_ID, playListId);
            valuesList[i] = values;
            position++;
        }

        try {
            int inserted = contentResolver.bulkInsert(
                    getContentUri("external", playListId),
                    valuesList
            );
            if (inserted == 0) {
                throw new PlayListNotModifiedException();
            }
            updateModifyTime(playListId);
        } catch (SecurityException ignored) {}
    }

    public void deleteItemFromPlayList(long itemId, long playListId) {
        int deletedRows = contentResolver.delete(
                getContentUri("external", playListId),
                Playlists.Members._ID + " = ?",
                new String[] { String.valueOf(itemId) }
        );

        if (deletedRows == 0) {
            throw new CompositionNotDeletedException();
        }
    }

    public void moveItemInPlayList(long playListId, int from, int to) {
        boolean moved = Playlists.Members.moveItem(contentResolver, playListId, from, to);
        if (!moved) {
            throw new CompositionNotMovedException();
        }
    }

    public void updatePlayListName(long playListId, String name) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Playlists.NAME, name);
        //update time or not?
//        contentValues.put(Playlists.DATE_MODIFIED, System.currentTimeMillis() / 1000L);
        contentResolver.update(Playlists.EXTERNAL_CONTENT_URI,
                contentValues,
                Playlists._ID + " = ?",
                new String[] { String.valueOf(playListId) });
    }

    private void updateModifyTime(long playListId) {
        ContentValues playListValues = new ContentValues();
        playListValues.put(Playlists.DATE_MODIFIED, System.currentTimeMillis() / 1000L);
        contentResolver.update(Playlists.EXTERNAL_CONTENT_URI,
                playListValues,
                Playlists._ID + " = ?",
                new String[] { String.valueOf(playListId) });
    }

    @Nullable
    private StoragePlayList findPlayList(long id) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    Playlists.EXTERNAL_CONTENT_URI,
                    null,
                    Playlists._ID + " = ?",
                    new String[] { String.valueOf(id) },
                    Playlists.DATE_ADDED + " DESC");
            if (cursor != null && cursor.moveToFirst()) {
                CursorWrapper cursorWrapper = new CursorWrapper(cursor);
                return getPlayListFromCursor(cursorWrapper);
            }
            return null;
        } finally {
            IOUtils.closeSilently(cursor);
        }
    }

    private StoragePlayListItem getPlayListItemFromCursor(CursorWrapper cursorWrapper) {
        long itemId = cursorWrapper.getLong(Playlists.Members._ID);
        long audioId = cursorWrapper.getLong(Playlists.Members.AUDIO_ID);
        return new StoragePlayListItem(itemId, audioId);
    }

    private StoragePlayList getPlayListFromCursor(CursorWrapper cursorWrapper) {
        long id = cursorWrapper.getLong(Playlists._ID);
        String name = cursorWrapper.getString(Playlists.NAME);
        long dateAdded = cursorWrapper.getLong(Playlists.DATE_ADDED);
        long dateModified = cursorWrapper.getLong(Playlists.DATE_MODIFIED);

        return new StoragePlayList(id,
                requireNonNull(name),
                new Date(dateAdded * 1000L),
                new Date(dateModified * 1000L));
    }
}

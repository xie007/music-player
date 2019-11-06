package com.github.anrimian.musicplayer.di.app;

import android.content.Context;

import com.github.anrimian.musicplayer.data.database.AppDatabase;
import com.github.anrimian.musicplayer.data.database.DatabaseManager;
import com.github.anrimian.musicplayer.data.database.dao.albums.AlbumsDao;
import com.github.anrimian.musicplayer.data.database.dao.albums.AlbumsDaoWrapper;
import com.github.anrimian.musicplayer.data.database.dao.artist.ArtistsDao;
import com.github.anrimian.musicplayer.data.database.dao.artist.ArtistsDaoWrapper;
import com.github.anrimian.musicplayer.data.database.dao.compositions.CompositionsDao;
import com.github.anrimian.musicplayer.data.database.dao.compositions.CompositionsDaoWrapper;
import com.github.anrimian.musicplayer.data.database.dao.play_list.PlayListDao;
import com.github.anrimian.musicplayer.data.database.dao.play_list.PlayListsDaoWrapper;
import com.github.anrimian.musicplayer.data.database.dao.play_queue.PlayQueueDao;
import com.github.anrimian.musicplayer.data.database.dao.play_queue.PlayQueueDaoWrapper;

import javax.annotation.Nonnull;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created on 20.11.2017.
 */

@Module
public class DbModule {

    @Provides
    @Nonnull
    @Singleton
    DatabaseManager provideDatabaseManager(Context context) {
        return new DatabaseManager(context);
    }

    @Provides
    @Nonnull
    @Singleton
    AppDatabase provideAppDatabase(DatabaseManager databaseManager) {
        return databaseManager.getAppDatabase();
    }

    @Provides
    @Nonnull
    @Singleton
    PlayQueueDao playQueueDao(AppDatabase appDatabase) {
        return appDatabase.playQueueDao();
    }

    @Provides
    @Nonnull
    @Singleton
    CompositionsDao compositionsDao(AppDatabase appDatabase) {
        return appDatabase.compositionsDao();
    }

    @Provides
    @Nonnull
    @Singleton
    PlayQueueDaoWrapper playQueueDaoWrapper(AppDatabase appDatabase, PlayQueueDao playQueueDao) {
        return new PlayQueueDaoWrapper(appDatabase, playQueueDao);
    }

    @Provides
    @Nonnull
    @Singleton
    ArtistsDao artistsDao(AppDatabase appDatabase) {
        return appDatabase.artistsDao();
    }

    @Provides
    @Nonnull
    @Singleton
    AlbumsDao albumssDao(AppDatabase appDatabase) {
        return appDatabase.albumsDao();
    }

    @Provides
    @Nonnull
    @Singleton
    AlbumsDaoWrapper albumsDaoWrapper(AlbumsDao albumsDao, ArtistsDao artistsDao) {
        return new AlbumsDaoWrapper(albumsDao, artistsDao);
    }

    @Provides
    @Nonnull
    @Singleton
    ArtistsDaoWrapper artistsDaoWrapper(ArtistsDao artistsDao) {
        return new ArtistsDaoWrapper(artistsDao);
    }

    @Provides
    @Nonnull
    @Singleton
    CompositionsDaoWrapper compositionsDaoWrapper(AppDatabase appDatabase,
                                                  ArtistsDao artistsDao,
                                                  CompositionsDao compositionsDao,
                                                  AlbumsDao albumsDao) {
        return new CompositionsDaoWrapper(appDatabase, artistsDao, compositionsDao, albumsDao);
    }

    @Provides
    @Nonnull
    @Singleton
    PlayListDao playListDao(AppDatabase appDatabase) {
        return appDatabase.playListDao();
    }

    @Provides
    @Nonnull
    @Singleton
    PlayListsDaoWrapper playListsDaoWrapper(PlayListDao playListDao, AppDatabase appDatabase) {
        return new PlayListsDaoWrapper(playListDao, appDatabase);
    }
}

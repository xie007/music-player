package com.github.anrimian.musicplayer.data.database.dao.genre;

import androidx.collection.LongSparseArray;
import androidx.sqlite.db.SimpleSQLiteQuery;

import com.github.anrimian.musicplayer.data.database.AppDatabase;
import com.github.anrimian.musicplayer.data.database.dao.compositions.CompositionsDao;
import com.github.anrimian.musicplayer.data.database.entities.IdPair;
import com.github.anrimian.musicplayer.data.database.entities.genres.GenreEntity;
import com.github.anrimian.musicplayer.data.database.entities.genres.GenreEntryEntity;
import com.github.anrimian.musicplayer.data.storage.providers.genres.StorageGenre;
import com.github.anrimian.musicplayer.data.storage.providers.genres.StorageGenreItem;
import com.github.anrimian.musicplayer.data.utils.collections.AndroidCollectionUtils;
import com.github.anrimian.musicplayer.domain.models.composition.Composition;
import com.github.anrimian.musicplayer.domain.models.composition.order.Order;
import com.github.anrimian.musicplayer.domain.models.genres.Genre;
import com.github.anrimian.musicplayer.domain.utils.ListUtils;

import java.util.List;
import java.util.Set;

import io.reactivex.Observable;

import static com.github.anrimian.musicplayer.domain.utils.ListUtils.mapList;
import static com.github.anrimian.musicplayer.domain.utils.TextUtils.isEmpty;

public class GenresDaoWrapper {

    private final AppDatabase appDatabase;
    private final GenreDao genreDao;
    private final CompositionsDao compositionsDao;

    public GenresDaoWrapper(AppDatabase appDatabase,
                            GenreDao genreDao,
                            CompositionsDao compositionsDao) {
        this.appDatabase = appDatabase;
        this.genreDao = genreDao;
        this.compositionsDao = compositionsDao;
    }

    public void applyChanges(List<StorageGenre> addedGenres) {
        genreDao.insertAll(mapList(addedGenres, this::toEntity));
    }

    public void applyChanges(List<StorageGenreItem> addedGenres, long genreId) {
        genreDao.insertGenreEntities(mapList(addedGenres, genre -> toEntity(genre, genreId)));
    }

    public Set<String> selectAllGenreNames() {
        return ListUtils.mapToSet(genreDao.selectAllGenreNames(), name -> name);
    }

    public LongSparseArray<StorageGenreItem> selectAllAsStorageGenreItems(long genreId) {
        return AndroidCollectionUtils.mapToSparseArray(
                genreDao.selectAllAsStorageGenreItems(genreId),
                StorageGenreItem::getId);
    }

    public Observable<List<Genre>> getAllObservable(Order order, String searchText) {
        String query = "SELECT id as id," +
                "name as name, " +
                "(SELECT count() FROM genre_entries WHERE genreId = genres.id) as compositionsCount, " +
                "(SELECT sum(duration) FROM compositions WHERE compositions.id IN (SELECT audioId FROM genre_entries WHERE genreId = genres.id)) as totalDuration " +
                "FROM genres";
        query += getSearchQuery(searchText);
        query += getOrderQuery(order);
        SimpleSQLiteQuery sqlQuery = new SimpleSQLiteQuery(query);
        return genreDao.getAllObservable(sqlQuery);
    }

    public Observable<Genre> getGenreObservable(long genreId) {
        return genreDao.getGenreObservable(genreId)
                .takeWhile(list -> !list.isEmpty())
                .map(list -> list.get(0));
    }

    public Observable<List<Composition>> getCompositionsInGenreObservable(long genreId) {
        return genreDao.getCompositionsInGenreObservable(genreId);
    }

    public List<Composition> getCompositionsInGenre(long genreId) {
        return genreDao.getCompositionsInGenre(genreId);
    }

    public List<IdPair> getGenresIds() {
        return genreDao.getGenresIds();
    }

    public void updateCompositionGenre(long compositionId, String genreName) {
        appDatabase.runInTransaction(() -> {

            // 1) find new genre by name
            Long genreId = genreDao.findGenre(genreName);

            // 2) if album not exists - create album
            if (genreId == null) {
                genreId = genreDao.insert(new GenreEntity(null, genreName));//hmm, storage?
            }

            // 3) add composition to new genre
            Long[] oldGenreIds = genreDao.getGenresByCompositionId(compositionId);
            genreDao.insertGenreEntry(new GenreEntryEntity(compositionId, genreId, null));

            if (oldGenreIds != null && oldGenreIds.length > 0) {
                genreDao.removeGenreEntry(compositionId, oldGenreIds);
                genreDao.deleteEmptyGenre(oldGenreIds);
            }
        });
    }

    public String[] getGenreNames() {
        return genreDao.getGenreNames();
    }

    public void updateGenreName(String name, long genreId) {
        genreDao.updateGenreName(name, genreId);
    }

    public void deleteGenre(long genreId) {
        genreDao.deleteGenre(genreId);
    }

    private GenreEntity toEntity(StorageGenre genre) {
        return new GenreEntity(genre.getId(), genre.getName());
    }

    private GenreEntryEntity toEntity(StorageGenreItem genre, long genreId) {
        long audioId = compositionsDao.selectIdByStorageId(genre.getAudioId());
        return new GenreEntryEntity(audioId, genreId, genre.getId());
    }

    private String getOrderQuery(Order order) {
        StringBuilder orderQuery = new StringBuilder(" ORDER BY ");
        switch (order.getOrderType()) {
            case ALPHABETICAL: {
                orderQuery.append("name");
                break;
            }
            case COMPOSITION_COUNT: {
                orderQuery.append("compositionsCount");
                break;
            }
            default: throw new IllegalStateException("unknown order type" + order);
        }
        orderQuery.append(" ");
        orderQuery.append(order.isReversed()? "DESC" : "ASC");
        return orderQuery.toString();
    }

    private String getSearchQuery(String searchText) {
        if (isEmpty(searchText)) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" WHERE ");
        sb.append("name LIKE '%");
        sb.append(searchText);
        sb.append("%'");

        return sb.toString();
    }
}

package com.github.anrimian.filesync

import com.github.anrimian.filesync.models.RemoteFileSource
import com.github.anrimian.filesync.models.SyncEnvCondition
import com.github.anrimian.filesync.models.repo.RemoteRepoFullInfo
import com.github.anrimian.filesync.models.repo.RemoteRepoInfo
import com.github.anrimian.filesync.models.repo.RepoSetupTemplate
import com.github.anrimian.filesync.models.state.SyncState
import com.github.anrimian.filesync.models.state.file.FileSyncState
import com.github.anrimian.filesync.models.task.FileTaskInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface SyncInteractor<K, T, I> {

    fun onAppStarted()
    fun requestFileSync()
    fun runFileTasks()
    fun addRemoteRepository(template: RepoSetupTemplate): Completable
    fun removeRemoteRepository(repo: RemoteRepoInfo): Completable
    fun setRemoteRepositoryEnabled(repo: RemoteRepoInfo): Completable
    fun onLocalFileAdded()
    fun onLocalFileDeleted(key: K)
    fun onLocalFilesDeleted(keys: List<K>)
    fun onLocalFileKeyChanged(key: Pair<K, K>)
    fun onLocalFilesKeyChanged(keys: List<Pair<K, K>>)
    fun notifyLocalFileChanged()
    fun isSyncEnabled(): Boolean
    fun setSyncEnabled(enabled: Boolean)
    fun onScheduledSyncCalled(): Completable
    fun getSyncConditions(): List<SyncEnvCondition>
    fun setSyncConditionEnabled(condition: SyncEnvCondition, enabled: Boolean)
    fun getAvailableRemoteRepositories(): List<Int>
    fun getSyncStateObservable(): Observable<SyncState>
    fun getRepositoriesObservable(): Observable<List<RemoteRepoFullInfo>>
    fun getTasksListObservable(): Observable<List<FileTaskInfo<K>>>
    fun getTasksCountObservable(): Observable<Int>
    fun getFileSyncStateObservable(fileId: I): Observable<FileSyncState>
    fun getFilesSyncStateObservable(): Observable<Map<I, FileSyncState>>
    fun getHasScheduledTasksObservable(): Observable<Boolean>
    fun requestFileSource(fileId: I): Single<RemoteFileSource>
}
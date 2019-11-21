package com.github.anrimian.musicplayer.ui.editor;

import com.github.anrimian.musicplayer.domain.business.editor.CompositionEditorInteractor;
import com.github.anrimian.musicplayer.domain.models.composition.Composition;
import com.github.anrimian.musicplayer.ui.common.error.ErrorCommand;
import com.github.anrimian.musicplayer.ui.common.error.parser.ErrorParser;

import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import moxy.InjectViewState;
import moxy.MvpPresenter;

import static com.github.anrimian.musicplayer.data.utils.rx.RxUtils.dispose;

@InjectViewState
public class CompositionEditorPresenter extends MvpPresenter<CompositionEditorView> {

    private final long compositionId;
    private final CompositionEditorInteractor editorInteractor;
    private final Scheduler uiScheduler;
    private final ErrorParser errorParser;

    private final CompositeDisposable presenterDisposable = new CompositeDisposable();
    private Disposable changeDisposable;

    private Composition composition;

    public CompositionEditorPresenter(long compositionId,
                                      CompositionEditorInteractor editorInteractor,
                                      Scheduler uiScheduler,
                                      ErrorParser errorParser) {
        this.compositionId = compositionId;
        this.editorInteractor = editorInteractor;
        this.uiScheduler = uiScheduler;
        this.errorParser = errorParser;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        loadComposition();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenterDisposable.dispose();
    }

    void onChangeAuthorClicked() {
        if (composition == null) {
            return;
        }
        editorInteractor.getAuthorNames()
                .observeOn(uiScheduler)
                .doOnSuccess(artists -> getViewState().showEnterAuthorDialog(composition, artists))
                .doOnError(throwable -> {
                    getViewState().showEnterAuthorDialog(composition, null);
                    onDefaultError(throwable);
                })
                .subscribe();
    }

    void onChangeTitleClicked() {
        if (composition == null) {
            return;
        }
        getViewState().showEnterTitleDialog(composition);
    }

    void onChangeFileNameClicked() {
        if (composition == null) {
            return;
        }
        getViewState().showEnterFileNameDialog(composition);
    }

    void onChangeAlbumClicked() {
        if (composition == null) {
            return;
        }
        editorInteractor.getAuthorNames()
                .observeOn(uiScheduler)
                .doOnSuccess(albums -> getViewState().showEnterAlbumDialog(composition, albums))
                .doOnError(throwable -> {
                    getViewState().showEnterAlbumDialog(composition, null);
                    onDefaultError(throwable);
                })
                .subscribe();    }

    void onNewAuthorEntered(String author) {
        if (composition == null) {
            return;
        }

        dispose(changeDisposable, presenterDisposable);
        changeDisposable = editorInteractor.editCompositionAuthor(composition, author)
                .observeOn(uiScheduler)
                .subscribe(() -> {}, this::onDefaultError);
        presenterDisposable.add(changeDisposable);
    }

    void onNewAlbumEntered(String album) {
        if (composition == null) {
            return;
        }

        dispose(changeDisposable, presenterDisposable);
        changeDisposable = editorInteractor.editCompositionAlbum(composition, album)
                .observeOn(uiScheduler)
                .subscribe(() -> {}, this::onDefaultError);
        presenterDisposable.add(changeDisposable);
    }

    void onNewTitleEntered(String title) {
        if (composition == null) {
            return;
        }

        dispose(changeDisposable, presenterDisposable);
        changeDisposable = editorInteractor.editCompositionTitle(composition, title)
                .observeOn(uiScheduler)
                .subscribe(() -> {}, this::onDefaultError);
        presenterDisposable.add(changeDisposable);
    }

    void onNewFileNameEntered(String fileName) {
        if (composition == null) {
            return;
        }

        dispose(changeDisposable, presenterDisposable);
        changeDisposable = editorInteractor.editCompositionFileName(composition, fileName)
                .observeOn(uiScheduler)
                .subscribe(() -> {}, this::onDefaultError);
        presenterDisposable.add(changeDisposable);
    }

    void onCopyFileNameClicked() {
        if (composition == null) {
            return;
        }
        getViewState().copyFileNameText(composition.getFilePath());
    }

    private void onDefaultError(Throwable throwable) {
        ErrorCommand errorCommand = errorParser.parseError(throwable);
        getViewState().showErrorMessage(errorCommand);
    }

    private void loadComposition() {
        presenterDisposable.add(editorInteractor.getCompositionObservable(compositionId)
                .observeOn(uiScheduler)
                .subscribe(this::onCompositionReceived,
                        this::onCompositionLoadingError,
                        getViewState()::closeScreen));
    }

    private void onCompositionReceived(Composition composition) {
        this.composition = composition;
        getViewState().showComposition(composition);
    }

    private void onCompositionLoadingError(Throwable throwable) {
        ErrorCommand errorCommand = errorParser.parseError(throwable);
        getViewState().showCompositionLoadingError(errorCommand);
    }
}

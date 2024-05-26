package io.github.oldmanpushcart.internal.dashscope4j.base.files;

import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.FilesOp;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiExecutor;

import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class FilesOpImpl implements FilesOp {

    private final ApiExecutor executor;

    public FilesOpImpl(ApiExecutor executor) {
        this.executor = executor;
    }

    @Override
    public CompletableFuture<FileMeta> upload(URI resource, String filename) {
        final var request = FileCreateRequest.newBuilder()
                .uri(resource)
                .name(filename)
                .purpose("file-extract")
                .build();
        return executor.async(request)
                .thenApply(response -> response.output().meta());
    }

    @Override
    public CompletableFuture<FileMeta> detail(String id) {
        final var request = FileDetailRequest.newBuilder()
                .id(id)
                .build();
        return executor.async(request)
                .thenApply(response -> response.output().meta());
    }

    @Override
    public CompletableFuture<Boolean> delete(String id) {
        return delete(id, false);
    }

    @Override
    public CompletableFuture<Boolean> delete(String id, boolean isForce) {
        final var request = FileDeleteRequest.newBuilder()
                .id(id)
                .build();
        return executor.async(request)
                .thenApply(response -> response.output().deleted())
                .exceptionallyCompose(ex -> isForce
                        ? CompletableFuture.completedFuture(false)
                        : CompletableFuture.failedFuture(ex)
                );
    }

    @Override
    public CompletableFuture<Flow.Publisher<FileMeta>> flow() {
        final var request = FileListRequest.newBuilder()
                .build();
        return executor.async(request)
                .thenApply(FileListResponse::output)
                .thenApply(output -> {
                    final var iterator = output.data().iterator();
                    final var isCancelled = new AtomicBoolean(false);
                    return subscriber -> subscriber.onSubscribe(new Flow.Subscription() {

                        @Override
                        public void request(long n) {
                            try {
                                for (long i = 0; i < n && iterator.hasNext() && !isCancelled.get(); i++) {
                                    subscriber.onNext(iterator.next());
                                }
                                if (!iterator.hasNext()) {
                                    subscriber.onComplete();
                                } else if (isCancelled.get()) {
                                    throw new CancellationException();
                                }
                            } catch (Throwable ex) {
                                subscriber.onError(ex);
                            }
                        }

                        @Override
                        public void cancel() {
                            isCancelled.set(true);
                        }

                    });
                });
    }

}

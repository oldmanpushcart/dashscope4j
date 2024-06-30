package io.github.oldmanpushcart.test.dashscope4j.base.files;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.test.dashscope4j.CommonAssertions;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class FilesOpTestCase implements LoadingEnv {

    @Test
    public void test$files$flow() {

        final var uri = URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg");
        final var name = "image-002.jpeg";
        final var res = client.base().files().upload(uri, name).join();

        final var existed = new AtomicBoolean(false);

        client.base().files().flow().join()
                .subscribe(new Flow.Subscriber<>() {

                    private volatile Flow.Subscription subscription;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(FileMeta item) {
                        if (item.id().equals(res.id())) {
                            existed.set(true);
                            subscription.cancel();
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        //
                    }

                    @Override
                    public void onComplete() {
                        //
                    }
                });

        client.base().files().delete(res.id()).join();

        Assertions.assertTrue(existed.get());

    }

    @Test
    public void test$files$delete$not_existed() {
        CommonAssertions.assertRootThrows(
                ApiException.class,
                () -> client.base().files().delete("not_existed").join(),
                ex -> Assertions.assertEquals(404, ex.status())
        );
    }

    @Test
    public void test$files$detail$not_existed() {
        CommonAssertions.assertRootThrows(
                ApiException.class,
                () -> client.base().files().detail("not_existed").join(),
                ex -> Assertions.assertEquals(404, ex.status())
        );
    }

    @Test
    public void test$files$detail$not_existed$force() {
        final var ret = client.base().files().delete("not_existed", true).join();
        Assertions.assertFalse(ret);
    }

    @Test
    public void test$file$upload$detail$delete() {
        final var uri = URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg");
        final var name = "image-002.jpeg";

        final var res = client.base().files().upload(uri, name).join();
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.id());
        Assertions.assertEquals(name, res.name());
        Assertions.assertTrue(res.size() > 0);
        Assertions.assertTrue(res.uploadedAt() > 0);

        final var detail = client.base().files().detail(res.id()).join();
        Assertions.assertNotNull(detail);
        Assertions.assertNotNull(detail.id());
        Assertions.assertEquals(name, detail.name());
        Assertions.assertEquals(res.size(), detail.size());
        Assertions.assertEquals(res.uploadedAt(), detail.uploadedAt());

        final var deleted = client.base().files().delete(res.id()).join();
        Assertions.assertTrue(deleted);
    }

    @Test
    public void test$files$upload_hit_cache() {

        final var uri = URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg");
        final var name = "image-002.jpeg";

        final var first = client.base().files().upload(uri, name).join();
        final var second = client.base().files().upload(uri, name).join();
        Assertions.assertEquals(first.id(), second.id());

    }

}

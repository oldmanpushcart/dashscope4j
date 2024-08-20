package io.github.oldmanpushcart.test.dashscope4j.base.files;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.test.dashscope4j.CommonAssertions;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class FilesOpTestCase implements LoadingEnv {

    @Test
    public void test$files$iterator$1() {

        final var uri = URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg");
        final var name = "image-002.jpeg";
        final var res = client.base().files().upload(uri, name)
                .toCompletableFuture()
                .join();

        final var existed = new AtomicBoolean(false);
        client.base().files().iterator().toCompletableFuture().join().forEachRemaining(it -> {
            if (it.id().equals(res.id())) {
                existed.set(true);
            }
        });

        client.base().files().delete(res.id()).toCompletableFuture().join();

        Assertions.assertTrue(existed.get());

    }

    @Test
    public void test$files$iterator$2() {

        final var filename = UUID.randomUUID() + ".tmp";
        final var resource = URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg");

        // 列表
        final var uploadMeta = client.base().files().upload(resource, filename).toCompletableFuture().join();
        Assertions.assertNotNull(uploadMeta);
        Assertions.assertNotNull(uploadMeta.id());
        Assertions.assertFalse(uploadMeta.id().isBlank());
        Assertions.assertNotNull(uploadMeta.name());
        Assertions.assertEquals(filename, uploadMeta.name());
        Assertions.assertTrue(uploadMeta.size() > 0);
        Assertions.assertTrue(uploadMeta.uploadedAt() > 0);
        Assertions.assertNotNull(uploadMeta.purpose());
        Assertions.assertFalse(uploadMeta.purpose().isBlank());
        Assertions.assertNotNull(uploadMeta.toURI());

        final var metas = new ArrayList<FileMeta>();
        client.base().files().iterator().toCompletableFuture().join().forEachRemaining(metas::add);

        // 列表查询
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.stream().anyMatch(it -> filename.equals(it.name())));
        metas.forEach(meta -> {
            if (meta.name().equals(filename)) {
                Assertions.assertNotNull(meta);
                Assertions.assertNotNull(meta.id());
                Assertions.assertFalse(meta.id().isBlank());
                Assertions.assertNotNull(meta.name());
                Assertions.assertEquals(filename, meta.name());
                Assertions.assertTrue(meta.size() > 0);
                Assertions.assertTrue(meta.uploadedAt() > 0);
                Assertions.assertNotNull(meta.purpose());
                Assertions.assertFalse(meta.purpose().isBlank());
                Assertions.assertNotNull(meta.toURI());
            }
        });

        // 详情
        final var detailMeta = client.base().files().detail(uploadMeta.id()).toCompletableFuture().join();
        Assertions.assertNotNull(detailMeta);
        Assertions.assertNotNull(detailMeta.id());
        Assertions.assertFalse(detailMeta.id().isBlank());
        Assertions.assertNotNull(detailMeta.name());
        Assertions.assertEquals(filename, detailMeta.name());
        Assertions.assertTrue(detailMeta.size() > 0);
        Assertions.assertTrue(detailMeta.uploadedAt() > 0);
        Assertions.assertNotNull(detailMeta.purpose());
        Assertions.assertFalse(detailMeta.purpose().isBlank());
        Assertions.assertNotNull(detailMeta.toURI());

        // 删除
        final var deleteResult = client.base().files().delete(uploadMeta.id()).toCompletableFuture().join();
        Assertions.assertTrue(deleteResult);

    }

    @Test
    public void test$files$delete$not_existed() {
        CommonAssertions.assertRootThrows(
                ApiException.class,
                () -> client.base().files().delete("not_existed").toCompletableFuture().join(),
                ex -> Assertions.assertEquals(404, ex.status())
        );
    }

    @Test
    public void test$files$detail$not_existed() {
        CommonAssertions.assertRootThrows(
                ApiException.class,
                () -> client.base().files().detail("not_existed").toCompletableFuture().join(),
                ex -> Assertions.assertEquals(404, ex.status())
        );
    }

    @Test
    public void test$files$detail$not_existed$force() {
        final var ret = client.base().files().delete("not_existed", true).toCompletableFuture().join();
        Assertions.assertFalse(ret);
    }

    @Test
    public void test$file$upload$detail$delete() {
        final var uri = URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg");
        final var name = "image-002.jpeg";

        final var res = client.base().files().upload(uri, name).toCompletableFuture().join();
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.id());
        Assertions.assertEquals(name, res.name());
        Assertions.assertTrue(res.size() > 0);
        Assertions.assertTrue(res.uploadedAt() > 0);

        final var detail = client.base().files().detail(res.id()).toCompletableFuture().join();
        Assertions.assertNotNull(detail);
        Assertions.assertNotNull(detail.id());
        Assertions.assertEquals(name, detail.name());
        Assertions.assertEquals(res.size(), detail.size());
        Assertions.assertEquals(res.uploadedAt(), detail.uploadedAt());

        final var deleted = client.base().files().delete(res.id()).toCompletableFuture().join();
        Assertions.assertTrue(deleted);
    }

    @Test
    public void test$files$upload_hit_cache() {

        final var uri = URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg");
        final var name = "image-002.jpeg";

        final var first = client.base().files().upload(uri, name).toCompletableFuture().join();
        final var second = client.base().files().upload(uri, name).toCompletableFuture().join();
        Assertions.assertEquals(first.id(), second.id());

    }

}

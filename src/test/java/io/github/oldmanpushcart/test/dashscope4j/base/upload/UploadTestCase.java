package io.github.oldmanpushcart.test.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Flow;

public class UploadTestCase implements LoadingEnv {

    @Test
    public void test$upload$image() {

        final var resource = URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg");
        final var model = ChatModel.QWEN_PLUS;

        final var request = UploadRequest.newBuilder()
                .resource(resource)
                .model(model)
                .build();

        final var response = client.base().upload(request).async()
                .join();

        Assertions.assertEquals(resource, response.output().resource());
        Assertions.assertEquals(model, response.output().model());
        Assertions.assertNotNull(response.output().uploaded());
        Assertions.assertTrue(response.ret().isSuccess());
        Assertions.assertNotNull(response.usage());

    }

    @Test
    public void test$upload$op() {
        final var uri = client.base().upload()
                .upload(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg"), ChatModel.QWEN_PLUS)
                .join();

        Assertions.assertNotNull(uri);
    }

    @Test
    public void test$upload$op$hit_cache() {
        final var uri = URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg");
        final var first = client.base().upload().upload(uri, ChatModel.QWEN_PLUS).join();
        final var second = client.base().upload().upload(uri, ChatModel.QWEN_PLUS).join();
        Assertions.assertEquals(first, second);
    }

    @Test
    public void test$upload$op$flow() {

        final var filename = UUID.randomUUID() + ".tmp";
        final var resource = URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg");

        // 列表
        final var uploadMeta = client.base().files().upload(resource, filename).join();
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
        client.base().files().flow().join()
                .subscribe(new Flow.Subscriber<>() {

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(FileMeta item) {
                        metas.add(item);
                    }

                    @Override
                    public void onError(Throwable ex) {
                        throw new RuntimeException(ex);
                    }

                    @Override
                    public void onComplete() {

                    }

                });

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
        final var detailMeta = client.base().files().detail(uploadMeta.id()).join();
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
        final var deleteResult = client.base().files().delete(uploadMeta.id()).join();
        Assertions.assertTrue(deleteResult);

    }

}

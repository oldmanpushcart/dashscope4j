package io.github.oldmanpushcart.dashscope4j.base.files;

import java.io.File;
import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

/**
 * 文件操作
 */
public interface FilesOp {

    /**
     * 上传资源
     *
     * @param resource 资源URI
     * @param filename 文件名
     * @return 创建操作
     */
    CompletableFuture<FileMeta> upload(URI resource, String filename);

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 创建操作
     */
    default CompletableFuture<FileMeta> upload(File file) {
        return upload(file.toURI(), file.getName());
    }

    /**
     * 上传资源
     *
     * @param resource 资源URI
     * @return 创建操作
     */
    default CompletableFuture<FileMeta> upload(URI resource) {
        return upload(resource, resource.getPath());
    }

    /**
     * 文件详情
     *
     * @param id 文件ID
     * @return 详情操作
     */
    CompletableFuture<FileMeta> detail(String id);

    /**
     * 删除文件
     *
     * @param id 文件ID
     * @return 删除操作
     */
    CompletableFuture<Boolean> delete(String id);

    /**
     * 删除文件
     *
     * @param id      文件ID
     * @param isForce 是否强制删除
     * @return 删除操作
     */
    CompletableFuture<Boolean> delete(String id, boolean isForce);

    /**
     * @return 文件迭代器
     */
    CompletableFuture<Iterator<FileMeta>> iterator();

}

package io.github.oldmanpushcart.dashscope4j.base.files;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 文件操作
 */
public interface FilesOp {

    /**
     * 上传资源
     *
     * @param resource 资源URI
     * @param filename 文件名
     * @param purpose  用途
     * @return 创建操作
     */
    CompletionStage<FileMeta> create(URI resource, String filename, Purpose purpose);

    /**
     * 上传文件
     *
     * @param file    文件
     * @param purpose 用途
     * @return 创建操作
     */
    default CompletionStage<FileMeta> create(File file, Purpose purpose) {
        return create(file.toURI(), file.getName(), purpose);
    }

    /**
     * 文件详情
     *
     * @param id 文件ID
     * @return 详情操作
     */
    CompletionStage<FileMeta> detail(String id);

    /**
     * 删除文件
     *
     * @param id 文件ID
     * @return 删除操作
     */
    CompletionStage<Boolean> delete(String id);

    /**
     * 删除文件
     *
     * @param id      文件ID
     * @param isForce 是否强制删除
     * @return 删除操作
     */
    CompletionStage<Boolean> delete(String id, boolean isForce);

    /**
     * @return 文件迭代器
     */
    CompletionStage<List<FileMeta>> list();

}

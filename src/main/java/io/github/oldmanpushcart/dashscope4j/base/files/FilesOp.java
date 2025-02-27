package io.github.oldmanpushcart.dashscope4j.base.files;

import io.github.oldmanpushcart.dashscope4j.util.ProgressListener;
import io.reactivex.rxjava3.core.Flowable;

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
     * 上传资源
     *
     * @param resource 资源URI
     * @param filename 文件名
     * @param purpose  用途
     * @param listener 进度监听器
     * @return 创建操作
     * @since 3.1.0
     */
    CompletionStage<FileMeta> create(URI resource, String filename, Purpose purpose, ProgressListener listener);

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
     * 上传文件
     *
     * @param file     文件
     * @param purpose  用途
     * @param listener 进度监听器
     * @return 创建操作
     * @since 3.1.0
     */
    default CompletionStage<FileMeta> create(File file, Purpose purpose, ProgressListener listener) {
        return create(file.toURI(), file.getName(), purpose, listener);
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
     * 文件列表
     * <p>按照文件创建顺序,从新到旧排序</p>
     *
     * @param after 从指定的文件ID之后开始(不含)
     *              <p>若为{@code null}则从排序最新的开始</p>
     * @param limit 列出的个数
     * @return 文件列表操作
     */
    CompletionStage<List<FileMeta>> list(String after, int limit);

    /**
     * 文件流
     * <p>按照文件创建顺序,从新到旧排序</p>
     *
     * @return 文件流
     */
    Flowable<FileMeta> flow();

}

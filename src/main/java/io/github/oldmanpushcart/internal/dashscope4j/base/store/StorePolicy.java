package io.github.oldmanpushcart.internal.dashscope4j.base.store;

import java.time.Duration;

/**
 * 存储凭证
 *
 * @param value     凭证值
 * @param signature 凭证签名
 * @param expire    过期时间
 * @param max       本次最大允许上传的文件大小(单位：字节)
 * @param capacity  同一个用户每天的上传容量限制(单位：字节)
 * @param oss       OSS配置
 */
public record StorePolicy(
        String value,
        String signature,
        Duration expire,
        long max,
        long capacity,
        Oss oss
) {

    /**
     * OSS配置
     *
     * @param host              主机
     * @param directory         目录
     * @param ak                AccessKey
     * @param acl               ACL
     * @param isForbidOverwrite 是否禁止覆盖
     */
    public record Oss(
            String host,
            String directory,
            String ak,
            String acl,
            boolean isForbidOverwrite
    ) {

    }

}

package io.github.oldmanpushcart.dashscope4j.client.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 向量计算工具类
 */
public class VectorUtils {

    /**
     * 相似度算法接口（支持外部自定义扩展）
     */
    @FunctionalInterface
    public interface Similarity {
        /**
         * 计算两个向量的相似度
         *
         * @param a 向量a
         * @param b 向量b
         * @return 相似度得分
         */
        float compute(float[] a, float[] b);
    }

    // ================= 内置的预置算法常量 =================

    /**
     * 余弦相似度
     * <p>
     * 只要方向一致，哪怕向量长度相差巨大，相似度依然很高。它不关心“绝对数值的大小”，只关心“特征的分布趋势”
     * </p>
     * <p>
     * 适合场景
     *     <ul>
     *         <li>文本与NLP（最经典场景）： 比如判断两篇文章或者两个句子意思是否相近。一篇长文章和一篇短文章如果核心词汇（特征方向）一致，余弦相似度依然会很高。如果在这里用欧氏距离，长文章的向量模长太大，会导致距离被严重拉远。</li>
     *         <li>用户兴趣偏好： 在推荐系统中，判断两个用户的兴趣是否相似。比如两个用户都喜欢同一类电影，但一个用户看过的电影总数是另一个的10倍（向量长度不同），用余弦相似度依然能判定他们是“同类人”。</li>
     *         <li>高维稀疏数据： 当数据维度极高且大部分是0（如文本的词袋模型）时，余弦相似度表现非常稳定。</li>
     *     </ul>
     * </p>
     */
    public static final Similarity COSINE = (a, b) -> {
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0f;
        return dotProduct / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
    };

    /**
     * 欧几里得相似度
     * <p>
     * 几何中最熟悉的“两点之间线段最短”。它非常看重向量在空间中的绝对位置和数值大小。
     * </p>
     * <p>
     * 适合场景
     *     <ul>
     *         <li>图像与视觉检索： 比如淘宝的“拍立淘”功能，提取图片的像素或特征向量。两张非常相似的图片，它们在特征空间中的绝对位置应该非常接近，欧氏距离能很好地反映这种像素级的差异。</li>
     *         <li>空间定位与地理信息： 比如计算地图上两个坐标点（经纬度）的实际物理距离，或者在GPS定位、机器人导航中计算两点间的直线距离。</li>
     *         <li>聚类与异常检测： 在 K-Means 聚类或 KNN（K近邻）算法中，如果特征的绝对数值大小有实际意义（比如用户的年龄、收入、消费金额），欧氏距离能更准确地衡量样本间的“实际差异”。</li>
     *     </ul>
     * </p>
     */
    public static final Similarity EUCLIDEAN = (a, b) -> {
        float sum = 0.0f;
        for (int i = 0; i < a.length; i++) {
            float diff = a[i] - b[i];
            sum += diff * diff;
        }
        return 1.0f / (1.0f + (float) Math.sqrt(sum));
    };

    /**
     * 曼哈顿相似度
     * <p>
     * 计算的是各个维度差值的绝对值之和（就像在城市街区里只能横平竖直地走，不能走对角线）。
     * </p>
     * <p>
     * 适用场景
     *     <ul>
     *         <li>高维稀疏数据与含噪声数据： 相比欧氏距离，曼哈顿距离对异常值（噪声）不敏感。因为欧氏距离会对差值进行平方，容易放大某个维度上的巨大偏差；而曼哈顿距离是线性累加，表现更稳健。如果你的数据中有很多不确定的噪声或异常点，选它更合适。</li>
     *         <li>计算性能要求极高的场景： 它的计算只需要加减法和绝对值，没有开平方等复杂运算，计算速度极快。在一些实时性要求极高、或者硬件资源受限的嵌入式设备路径规划中非常常用。</li>
     *     </ul>
     * </p>
     */
    public static final Similarity MANHATTAN = (a, b) -> {
        float sum = 0.0f;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return 1.0f / (1.0f + sum);
    };

    /**
     * 点积相似度
     * <p>
     * 它是余弦相似度的“未归一化”版本，同时受向量的方向和长度（模长）影响。
     * </p>
     * <p>
     * 适用场景
     *     <ul>
     *         <li>推荐系统： 在协同过滤中，点积能很好地反映“用户活跃度”和“物品热门度”。比如一个高频活跃用户（向量长）对一个热门商品（向量长）产生交互，点积的数值会非常大，这在实际业务中往往代表着更强的匹配信号。</li>
     *         <li>经过特定训练的 LLM 嵌入模型： 现在很多先进的语义检索模型（如专门针对搜索优化的 Sentence-BERT 变体），在训练时就使用了点积作为损失函数。这意味着模型已经将语义信息编码到了向量的模长中，此时直接暴力归一化用余弦相似度反而会丢失信息，直接使用点积效果最好。</li>
     *     </ul>
     * </p>
     */
    public static final Similarity DOT_PRODUCT = (a, b) -> {
        float sum = 0.0f;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    };

    // ================= 核心静态工具方法 =================

    /**
     * 计算两个向量的相似度
     *
     * @param vecA       向量A
     * @param vecB       向量B
     * @param similarity 相似度算法
     * @return 相似度得分
     */
    public static float computeSimilarity(float[] vecA, float[] vecB, Similarity similarity) {
        Objects.requireNonNull(vecA, "vecA can not be null");
        Objects.requireNonNull(vecB, "vecB can not be null");
        return similarity.compute(vecA, vecB);
    }

    /**
     * 搜索最相似的向量
     *
     * @param qVector    检索向量
     * @param vectorMap  向量数据库
     * @param similarity 相似度算法
     * @param topK       搜索结果数量
     * @return 匹配结果
     */
    public static List<Matched> search(float[] qVector, Map<String, float[]> vectorMap, Similarity similarity, int topK) {
        return vectorMap.entrySet().stream()
                .map(entry -> {
                    final var key = entry.getKey();
                    final var dVector = entry.getValue();
                    final var source = computeSimilarity(qVector, dVector, similarity);
                    return new Matched(key, source);
                })
                .sorted((o1, o2) -> Float.compare(o2.score(), o1.score()))
                .limit(topK)
                .toList();
    }

    /**
     * 匹配结果
     *
     * @param key   KEY
     * @param score 得分
     */
    public record Matched(String key, float score) {

    }

}

package io.github.ompc.dashscope4j.internal.algo;

import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.internal.api.ApiData;
import io.github.ompc.dashscope4j.internal.api.ApiResponse;
import io.github.ompc.dashscope4j.internal.util.Aggregatable;

public abstract class AlgoResponse<D extends ApiData, DR extends AlgoResponse<D, DR>> extends ApiResponse<D, DR> implements Aggregatable<DR> {

    public AlgoResponse(String uuid, String code, String message, Usage usage, D data) {
        super(uuid, code, message, usage, data);
    }

    @Override
    public DR aggregate(boolean increment, DR other) {
        throw new UnsupportedOperationException("Not implemented");
    }


}

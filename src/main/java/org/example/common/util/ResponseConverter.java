package org.example.common.util;

import org.example.common.Message.RpcResponse;
import org.example.common.pojo.Result;

/**
 * 响应类型转换工具类
 */
public class ResponseConverter {

    /**
     * 将Result转换为RpcResponse
     * @param result 服务端返回结果
     * @return 转换后的RpcResponse对象
     */
    public static RpcResponse convertToResponse(Result result) {
        if (result == null) {
            return RpcResponse.fail("结果为空");
        }

        if (Boolean.TRUE.equals(result.getSuccess())) {
            return RpcResponse.builder()
                    .code(200)
                    .data(result)
                    .dataType(result.getData() != null ? result.getClass() : null)
                    .build();
        } else {
            return RpcResponse.builder()
                    .code(500)
                    .message(result.getErrorMsg())
                    .build();
        }
    }

    /**
     * 快捷成功转换
     * @param data 业务数据
     * @return RpcResponse
     */
    public static RpcResponse success(Object data) {
        return convertToResponse(Result.ok(data));
    }

    /**
     * 快捷失败转换
     * @param errorMsg 错误信息
     * @return RpcResponse
     */
    public static RpcResponse fail(String errorMsg) {
        return convertToResponse(Result.fail(errorMsg));
    }

    public static Result convertToResult(RpcResponse response) {
        if (response == null) {
            return Result.fail("响应为空");
        }

        if (response.getCode() == 200) {
            return response.getData() != null ? Result.ok(response.getData()) : Result.ok();
        } else {
            return Result.fail(response.getMessage() != null ?
                    response.getMessage() : "RPC调用失败");
        }
    }

}
// ... 省略其他代码 ...

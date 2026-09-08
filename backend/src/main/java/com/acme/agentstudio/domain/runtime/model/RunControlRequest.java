package com.acme.agentstudio.domain.runtime.model;

/**
 * RunControl 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 RunControl 操作参数。
 */
/** 类型化运行控制请求。 */
public record RunControlRequest(RunControlAction action, String transferTarget, String comment) {
    public RunControlRequest {
        if (action == null) throw new IllegalArgumentException("运行控制动作不能为空");
    }
}

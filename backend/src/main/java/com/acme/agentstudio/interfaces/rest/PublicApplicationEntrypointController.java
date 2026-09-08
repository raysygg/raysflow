package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.runtime.EntrypointInvocationService;
import com.acme.agentstudio.application.runtime.ExternalEntrypointAuthenticationService;
import com.acme.agentstudio.application.realtime.RealtimeTicketService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.domain.application.ApplicationEntrypointContracts.InvocationRequest;
import com.acme.agentstudio.domain.application.RunTriggerSource;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeCursorRequest;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEntrypointEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公共开放 API 与 Webhook 外部调用端点 REST 控制器。
 * 负责提供第三方系统以 API Key 方式触发 Runtime 应用、以 HMAC 签名校验方式触发 Webhook 订阅，以及基于 RunID 申请外部只读 WebSocket Ticket 游标监听接口。
 */
@Tag(name = "公共开放端点", description = "第三方开放 API 触发、Webhook 响应与只读实时 Ticket")
@RestController
@RequestMapping("/api/public/v1")
public class PublicApplicationEntrypointController {

    /** 外部端点身份鉴权与 Webhook 签名验证服务 */
    private final ExternalEntrypointAuthenticationService authenticationService;

    /** 开放端点触发执行服务 */
    private final EntrypointInvocationService invocationService;

    /** 实时 WebSocket 鉴权 Ticket 服务 */
    private final RealtimeTicketService ticketService;

    /** JSON 序列化/反序列化映射器 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入开放端点依赖服务。
     */
    public PublicApplicationEntrypointController(ExternalEntrypointAuthenticationService authenticationService,
                                                 EntrypointInvocationService invocationService,
                                                 RealtimeTicketService ticketService,
                                                 ObjectMapper objectMapper) {
        this.authenticationService = authenticationService;
        this.invocationService = invocationService;
        this.ticketService = ticketService;
        this.objectMapper = objectMapper;
    }

    /**
     * 第三方应用通过 invokeCode 与 API Key / Bearer Token 鉴权异步/同步调用应用流程。
     *
     * @param invokeCode 开放端点编码
     * @param authorization 请求头 Authorization Bearer API Key 凭证
     * @param idempotencyKey 幂等校验 Key（可选）
     * @param input 业务输入 JSON 参数映射
     * @return 运行实例 Trigger 响应结果
     */
    @Operation(summary = "开放 API 触发应用", description = "第三方系统使用 ApiKey 认证触发指定开放端点关联的 Agent/工作流。")
    @PostMapping("/entrypoints/{invokeCode}/invoke")
    public ApiResponse<?> invoke(@PathVariable String invokeCode,
                                 @RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                 @RequestBody(required = false) Map<String, Object> input) {
        ApplicationEntrypointEntity entrypoint = authenticationService.authenticateApi(invokeCode, authorization);
        InvocationRequest request = new InvocationRequest(idempotencyKey, null, null, input);
        return ApiResponse.ok(invocationService.invokeExternal(entrypoint, request, RunTriggerSource.EXTERNAL_API));
    }

    /**
     * 接收并验证第三方 Webhook 事件回调（HMAC-SHA256 签名与防重放非重复随机数 Nonce 校验）。
     *
     * @param invokeCode Webhook 触发点编码
     * @param timestamp 时间戳标头
     * @param nonce 随机数标头
     * @param signature 校验签名
     * @param rawBody 原始文本 Body
     * @return 响应结果
     */
    @Operation(summary = "接收 Webhook 回调", description = "校验第三方系统推派的签名与时间戳，解析事件 Body 触发应用执行。")
    @PostMapping("/webhooks/{invokeCode}")
    public ApiResponse<?> webhook(@PathVariable String invokeCode,
                                  @RequestHeader(value = "X-Webhook-Timestamp", required = false) String timestamp,
                                  @RequestHeader(value = "X-Webhook-Nonce", required = false) String nonce,
                                  @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
                                  @RequestBody String rawBody) {
        ApplicationEntrypointEntity entrypoint = authenticationService.authenticateWebhook(
                invokeCode, timestamp, nonce, signature, rawBody);
        Map<String, Object> input = readObject(rawBody);
        InvocationRequest request = new InvocationRequest("webhook:" + entrypoint.getId() + ":" + nonce,
                null, null, input);
        return ApiResponse.ok(invocationService.invokeExternal(entrypoint, request, RunTriggerSource.WEBHOOK));
    }

    /**
     * 为外部未登录或公开调用的 RunID 申请短期有效的 WebSocket 实时游标 Ticket。
     *
     * @param runId 运行实例 ID
     * @param authorization 可选的 API 凭证
     * @param request 游标起始位置
     * @return 包含一次性凭证 Ticket 的响应
     */
    @Operation(summary = "申请公开 WebSocket Ticket", description = "为第三方调用的运行实例申请用于订阅实时执行日志事件的临时 Ticket。")
    @PostMapping("/runs/{runId}/realtime-ticket")
    public ApiResponse<?> realtimeTicket(@PathVariable String runId,
                                         @RequestHeader(value = "Authorization", required = false) String authorization,
                                         @RequestBody(required = false) RealtimeCursorRequest request) {
        long afterSequence = request == null ? 0L : request.afterSequence();
        return ApiResponse.ok(ticketService.createPublicRun(runId, authorization, afterSequence));
    }

    /**
     * 辅助解析：将 Webhook 的 Raw JSON Body 反序列化为 Map 对象。
     */
    private Map<String, Object> readObject(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, new TypeReference<>() { });
        } catch (Exception exception) {
            throw new IllegalArgumentException("Webhook 请求体必须是标准的 JSON 对象", exception);
        }
    }
}


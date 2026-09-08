package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 平台内置 Agent 策略（ReAct, Plan-Execute, Multi-Agent 等）的统一数据规范契约集中类（Runtime Strategy Contracts）。
 * 约定 EventKind 事件阶段、AdapterBoundary 框架适配边界、PlatformSemantic 平台协作语义、
 * 规范错误码 ErrorCode 及 Descriptor, SemanticMapping, AdapterDescriptor, StrategyTemplate, Request, Event, Error, Result 核心模型。
 */
public final class RuntimeStrategyContracts {

    /** 平台设计模式适配器协议版本，所有内置模式共用 */
    public static final String DEFAULT_ADAPTER_VERSION = "1.0";

    /** 私有构造函数，防止工具类被实例化 */
    private RuntimeStrategyContracts() {
    }

    /** 策略执行阶段事件枚举（Event Kind） */
    public enum EventKind {
        /** 策略启动 */
        STRATEGY_STARTED,

        /** 运行上下文已解析就绪 */
        CONTEXT_RESOLVED,

        /** 规划生成步骤计划 */
        PLAN_CREATED,

        /** 单步执行开始 */
        STEP_STARTED,

        /** 发起外部工具调用 */
        TOOL_CALLED,

        /** 观察工具/模型返回结果记录 */
        OBSERVATION_RECORDED,

        /** 任务委派下发至下游 Agent */
        AGENT_DELEGATED,

        /** 挂起等待人工确认审批 */
        HUMAN_APPROVAL_REQUIRED,

        /** 策略成功完成 */
        STRATEGY_COMPLETED,

        /** 策略异常失败 */
        STRATEGY_FAILED
    }

    /** Agent 设计语义与平台执行器的适配边界枚举 */
    public enum AdapterBoundary {
        /** 平台原生内置 */
        PLATFORM_NATIVE
    }

    /** 平台公开的协作语义模式枚举 */
    public enum PlatformSemantic {
        /** 多轮对话会话 */
        CHAT_SESSION,

        /** ReAct 思考观察循环 */
        REACT_LOOP,

        /** 拆解执行图 */
        PLAN_GRAPH,

        /** 可视化工作流拓扑图 */
        WORKFLOW_GRAPH,

        /** 多 Agent 团队拓扑 */
        MULTI_AGENT_TOPOLOGY
    }

    /** 对外标准的策略错误码枚举 */
    public enum ErrorCode {

        /** 策略请求参数无效 */
        INVALID_REQUEST("策略请求无效", false),

        /** 运行上下文配置无效 */
        CONTEXT_INVALID("运行上下文无效", false),

        /** 策略组件尚未准备就绪 */
        STRATEGY_NOT_READY("运行策略尚未就绪", false),

        /** 策略逻辑执行失败 */
        EXECUTION_FAILED("策略执行失败", true),

        /** 策略运行步骤/Token 预算耗尽 */
        BUDGET_EXCEEDED("策略运行预算已耗尽", false),

        /** 策略挂起，要求人工二次确认 */
        HUMAN_APPROVAL_REQUIRED("策略等待人工确认", false);

        private final String defaultMessage;
        private final boolean retryable;

        ErrorCode(String defaultMessage, boolean retryable) {
            this.defaultMessage = defaultMessage;
            this.retryable = retryable;
        }

        /**
         * 获取错误码对应的默认中文提示信息。
         *
         * @return 默认中文说明
         */
        public String defaultMessage() {
            return defaultMessage;
        }

        /**
         * 是否允许重试。
         *
         * @return true 表示允许重试
         */
        public boolean retryable() {
            return retryable;
        }
    }

    /** 策略能力描述符 Record */
    public record Descriptor(
            RuntimeMode mode,
            String displayName,
            String inputContract,
            String outputContract,
            String contextContract,
            List<EventKind> eventKinds,
            boolean ready
    ) {
        public Descriptor {
            if (mode == null) {
                throw new IllegalArgumentException("策略运行模式不能为空。");
            }
            requireText(displayName, "策略名称不能为空。");
            requireText(inputContract, "策略输入契约不能为空。");
            requireText(outputContract, "策略输出契约不能为空。");
            requireText(contextContract, "策略上下文契约不能为空。");
            eventKinds = (eventKinds == null) ? List.of() : List.copyOf(eventKinds);
        }
    }

    /** 单个框架模式到平台语义的显式映射 Record */
    public record SemanticMapping(
            RuntimeMode mode,
            PlatformSemantic semantic
    ) {
        public SemanticMapping {
            if (mode == null || semantic == null) {
                throw new IllegalArgumentException("框架模式和平台语义不能为空。");
            }
        }
    }

    /** 框架适配器描述符 Record */
    public record AdapterDescriptor(
            NativeFramework framework,
            String adapterVersion,
            AdapterBoundary boundary,
            List<SemanticMapping> mappings,
            String executionNote
    ) {
        public AdapterDescriptor {
            if (framework == null || boundary == null) {
                throw new IllegalArgumentException("框架和适配边界不能为空。");
            }
            requireText(adapterVersion, "适配器版本不能为空。");
            mappings = (mappings == null) ? List.of() : List.copyOf(mappings);
            requireText(executionNote, "适配器执行说明不能为空。");
        }
    }

    /** 模板参数字段描述 Record */
    public record TemplateField(
            String name,
            String type,
            boolean required,
            String description
    ) {
        public TemplateField {
            requireText(name, "模板字段名称不能为空。");
            requireText(type, "模板字段类型不能为空。");
            requireText(description, "模板字段说明不能为空。");
        }
    }

    /** Agent 策略模板与能力证据 Record */
    public record StrategyTemplate(
            RuntimeMode mode,
            String name,
            String description,
            List<TemplateField> inputFields,
            List<TemplateField> outputFields,
            List<TemplateField> configurationFields,
            List<String> evidence,
            boolean runnable
    ) {
        public StrategyTemplate {
            if (mode == null) {
                throw new IllegalArgumentException("模板运行模式不能为空。");
            }
            requireText(name, "模板名称不能为空。");
            requireText(description, "模板说明不能为空。");
            inputFields = (inputFields == null) ? List.of() : List.copyOf(inputFields);
            outputFields = (outputFields == null) ? List.of() : List.copyOf(outputFields);
            configurationFields = (configurationFields == null) ? List.of() : List.copyOf(configurationFields);
            evidence = (evidence == null) ? List.of() : List.copyOf(evidence);
            if (evidence.isEmpty()) {
                throw new IllegalArgumentException("策略模板必须提供能力证据。");
            }
        }
    }

    /**
     * 根据运行模式获取内置策略模板描述。
     *
     * @param mode 运行模式
     * @return 策略模板 StrategyTemplate 对象
     */
    public static StrategyTemplate templateFor(RuntimeMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("模板运行模式不能为空。");
        }
        TemplateField task = new TemplateField("task", "string", true, "用户希望 Agent 完成的业务目标");
        TemplateField answer = new TemplateField("answer", "string", true, "策略生成的最终结果");
        return switch (mode) {
            case REACT -> new StrategyTemplate(mode, "循环推理 Agent",
                    "通过思考、工具调用和观察循环完成任务。",
                    List.of(task), List.of(answer), List.of(
                    new TemplateField("maxSteps", "integer", false, "最大推理步数"),
                    new TemplateField("maxToolCalls", "integer", false, "最大工具调用次数")),
                    List.of("统一 RuntimeContext", "ReAct 决策与观察协议", "工具调用边界", "失败和重试事件"), false);
            case PLAN -> new StrategyTemplate(mode, "计划执行 Agent",
                    "先生成可追踪计划，再按步骤执行并汇总结果。",
                    List.of(task), List.of(answer), List.of(
                    new TemplateField("maxSteps", "integer", false, "最大计划步骤数"),
                    new TemplateField("requireApproval", "boolean", false, "执行前是否需要人工确认")),
                    List.of("统一 RuntimeContext", "计划步骤协议", "步骤状态事件", "人工确认协议"), false);
            case MULTI_AGENT -> new StrategyTemplate(mode, "多 Agent 协作",
                    "通过角色分工、共享上下文和有限拓扑协作完成任务。",
                    List.of(task), List.of(answer), List.of(
                    new TemplateField("topology", "enum", true, "顺序、Supervisor 或 Debate"),
                    new TemplateField("roles", "agent-role[]", true, "参与协作的角色列表"),
                    new TemplateField("maxRounds", "integer", false, "最大协作轮次")),
                    List.of("统一 RuntimeContext", "AgentRoleDefinition", "协作拓扑调度", "共享上下文边界", "协作事件协议"), false);
            default -> new StrategyTemplate(mode, "平台运行策略",
                    "平台运行策略模板。",
                    List.of(task), List.of(answer), List.of(),
                    List.of("统一 RuntimeContext", "统一策略事件协议", "统一错误协议"), false);
        };
    }

    /** 策略执行请求描述符 Record */
    public record Request(
            String requestId,
            RuntimeContext context,
            String strategyNodeId,
            Map<String, Object> parameters
    ) {
        public Request {
            requireText(requestId, "策略请求标识不能为空。");
            if (context == null) {
                throw new IllegalArgumentException("策略运行上下文不能为空。");
            }
            requireText(strategyNodeId, "策略节点标识不能为空。");
            parameters = (parameters == null) ? Map.of() : Map.copyOf(parameters);
        }
    }

    /** 统一策略运行事件实体 Record */
    public record Event(
            String requestId,
            String runId,
            long sequence,
            EventKind kind,
            RunStatus status,
            String nodeId,
            Map<String, Object> payload,
            Instant occurredAt
    ) {
        public Event {
            requireText(requestId, "事件请求标识不能为空。");
            requireText(runId, "事件 Run 标识不能为空。");
            if (sequence < 0 || kind == null || status == null) {
                throw new IllegalArgumentException("策略事件序号、类型和状态无效。");
            }
            requireText(nodeId, "策略事件节点标识不能为空。");
            payload = (payload == null) ? Map.of() : Map.copyOf(payload);
            occurredAt = (occurredAt == null) ? Instant.now() : occurredAt;
        }
    }

    /** 统一策略执行异常实体 Record */
    public record Error(
            ErrorCode code,
            String message,
            String requestId,
            List<String> details,
            boolean retryable
    ) {
        public Error {
            if (code == null) {
                throw new IllegalArgumentException("策略错误码不能为空。");
            }
            message = (message == null || message.isBlank()) ? code.defaultMessage() : message;
            requireText(requestId, "策略错误请求标识不能为空。");
            details = (details == null) ? List.of() : List.copyOf(details);
        }

        /**
         * 构建标准化策略错误对象。
         *
         * @param code 错误码
         * @param requestId 请求 ID
         * @param detail 错误详情
         * @return Error 实例
         */
        public static Error of(ErrorCode code, String requestId, String detail) {
            return new Error(code, code.defaultMessage(), requestId,
                    (detail == null || detail.isBlank()) ? List.of() : List.of(detail), code.retryable());
        }
    }

    /** 策略执行最终结果承载 Record */
    public record Result(
            RunStatus status,
            Map<String, Object> output,
            Map<String, Object> contextSummary,
            List<Event> events,
            Error error
    ) {
        public Result {
            if (status == null) {
                throw new IllegalArgumentException("策略结果状态不能为空。");
            }
            output = (output == null) ? Map.of() : Map.copyOf(output);
            contextSummary = (contextSummary == null) ? Map.of() : Map.copyOf(contextSummary);
            events = (events == null) ? List.of() : List.copyOf(events);
            if (status == RunStatus.FAILED && error == null) {
                throw new IllegalArgumentException("策略失败结果必须包含错误信息。");
            }
        }

        /**
         * 快速构造失败的策略执行结果。
         *
         * @param requestId 请求 ID
         * @param runId Run ID
         * @param nodeId 节点 ID
         * @param error 错误明细
         * @return Result 实例
         */
        public static Result failed(String requestId, String runId, String nodeId, Error error) {
            Event event = new Event(requestId, runId, 0,
                    EventKind.STRATEGY_FAILED, RunStatus.FAILED, nodeId,
                    Map.of("errorCode", error.code().name(), "message", error.message()), Instant.now());
            return new Result(RunStatus.FAILED, Map.of(), Map.of(), List.of(event), error);
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}


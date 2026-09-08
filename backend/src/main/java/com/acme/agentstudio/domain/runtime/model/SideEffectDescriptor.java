package com.acme.agentstudio.domain.runtime.model;

/**
 * 描述一次外部工具 API 调用或写操作动作的副作用（Side Effect）与幂等补偿描述符 Record（Side Effect Descriptor）。
 * 包含动作 ID actionId、幂等唯一 Key idempotencyKey、副作用分类 effectClass (SideEffectClass)、
 * 提交状态 commitStatus (CommitStatus) 与可逆补偿动作 handler 说明 compensationAction。
 *
 * @param actionId 动作唯一 ID
 * @param idempotencyKey 防重复调用的幂等 Key
 * @param effectClass 副作用分类（READ_ONLY, REVERSIBLE, IRREVERSIBLE）
 * @param commitStatus 提交持久化状态（COMMITTED, IN_FLIGHT 等）
 * @param compensationAction 失败或回滚时的逆向补偿动作描述
 */
public record SideEffectDescriptor(
        String actionId,
        String idempotencyKey,
        SideEffectClass effectClass,
        CommitStatus commitStatus,
        String compensationAction
) {
    /** 紧凑构造函数做输入副作用属性与幂等键强校验 */
    public SideEffectDescriptor {
        if (actionId == null || actionId.isBlank()) {
            throw new IllegalArgumentException("动作标识不能为空");
        }
        if (effectClass == null || commitStatus == null) {
            throw new IllegalArgumentException("副作用等级和提交状态不能为空");
        }
        if (effectClass != SideEffectClass.READ_ONLY && (idempotencyKey == null || idempotencyKey.isBlank())) {
            throw new IllegalArgumentException("有副作用的动作必须提供幂等键");
        }
        if (effectClass == SideEffectClass.REVERSIBLE && (compensationAction == null || compensationAction.isBlank())) {
            throw new IllegalArgumentException("可逆动作必须提供补偿动作");
        }
        compensationAction = (compensationAction == null) ? "" : compensationAction;
    }
}


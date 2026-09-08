/*
 Navicat Premium Dump SQL

 Source Server         : ali
 Source Server Type    : MySQL
 Source Server Version : 80036 (8.0.36)
 Source Host           : rm-bp16o8590l35s6g0dno.mysql.rds.aliyuncs.com:3306
 Source Schema         : enterprise_agent

 Target Server Type    : MySQL
 Target Server Version : 80036 (8.0.36)
 File Encoding         : 65001

 Date: 07/08/2026 10:53:51
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for agent_profile
-- ----------------------------
DROP TABLE IF EXISTS `agent_profile`;
CREATE TABLE `agent_profile`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `agent_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `agent_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `agent_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `owner_team` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `prompt_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `model_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `tool_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `workflow_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_tenant_code`(`tenant_id` ASC, `agent_code` ASC) USING BTREE,
  INDEX `idx_agent_tenant_status`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of agent_profile
-- ----------------------------

-- ----------------------------
-- Table structure for application_entrypoint
-- ----------------------------
DROP TABLE IF EXISTS `application_entrypoint`;
CREATE TABLE `application_entrypoint`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `invoke_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `entrypoint_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version_policy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `pinned_version_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `delivery_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `input_schema_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `cron_expression` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `timezone` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `next_fire_at` datetime NULL DEFAULT NULL,
  `schedule_lease_owner` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `schedule_lease_until` datetime NULL DEFAULT NULL,
  `api_credential_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `webhook_secret_ciphertext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `credential_mask` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_application_entrypoint_invoke_code`(`invoke_code` ASC) USING BTREE,
  INDEX `idx_application_entrypoint_app`(`tenant_id` ASC, `application_id` ASC, `entrypoint_type` ASC) USING BTREE,
  INDEX `idx_application_entrypoint_schedule`(`enabled` ASC, `entrypoint_type` ASC, `next_fire_at` ASC, `schedule_lease_until` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of application_entrypoint
-- ----------------------------
INSERT INTO `application_entrypoint` VALUES (1, 4, 1026, 'c0ed74f6794e435591164092111d1d3d', '对话入口', 'CONVERSATION', 'FOLLOW_PRODUCTION', NULL, 'REALTIME', '[{\"name\":\"request\",\"type\":\"string\",\"required\":true,\"description\":\"验证应用发布前的配置和质量检查流程\"}]', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-08-06 10:12:12', '2026-08-06 10:12:12');
INSERT INTO `application_entrypoint` VALUES (2, 4, 1026, '76cfb61327834448a59475d978425afa', '表单入口', 'FORM', 'FOLLOW_PRODUCTION', NULL, 'IMMEDIATE', '[{\"name\":\"request\",\"type\":\"string\",\"required\":true,\"description\":\"验证应用发布前的配置和质量检查流程\"}]', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, '2026-08-06 10:12:12', '2026-08-06 10:12:12');

-- ----------------------------
-- Table structure for application_evaluation_case
-- ----------------------------
DROP TABLE IF EXISTS `application_evaluation_case`;
CREATE TABLE `application_evaluation_case`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `suite_version_id` bigint NOT NULL,
  `case_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `input_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `expected_rule_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `metric_applicability_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_evaluation_case_code`(`tenant_id` ASC, `suite_version_id` ASC, `case_code` ASC) USING BTREE,
  INDEX `idx_evaluation_case_suite`(`tenant_id` ASC, `application_id` ASC, `suite_version_id` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of application_evaluation_case
-- ----------------------------

-- ----------------------------
-- Table structure for application_evaluation_result
-- ----------------------------
DROP TABLE IF EXISTS `application_evaluation_result`;
CREATE TABLE `application_evaluation_result`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `evaluation_run_id` bigint NOT NULL,
  `case_id` bigint NOT NULL,
  `result_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `evaluation_variant` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CANDIDATE',
  `input_digest` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `output_digest` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `evidence_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `task_success_score` decimal(10, 6) NULL DEFAULT NULL,
  `correctness_score` decimal(10, 6) NULL DEFAULT NULL,
  `groundedness_score` decimal(10, 6) NULL DEFAULT NULL,
  `citation_score` decimal(10, 6) NULL DEFAULT NULL,
  `latency_ms` bigint NULL DEFAULT NULL,
  `input_tokens` bigint NULL DEFAULT NULL,
  `output_tokens` bigint NULL DEFAULT NULL,
  `estimated_cost` decimal(18, 8) NULL DEFAULT NULL,
  `failure_category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `failure_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_evaluation_result_case_variant`(`tenant_id` ASC, `evaluation_run_id` ASC, `case_id` ASC, `evaluation_variant` ASC) USING BTREE,
  INDEX `idx_evaluation_result_run`(`tenant_id` ASC, `application_id` ASC, `evaluation_run_id` ASC, `result_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of application_evaluation_result
-- ----------------------------

-- ----------------------------
-- Table structure for application_evaluation_run
-- ----------------------------
DROP TABLE IF EXISTS `application_evaluation_run`;
CREATE TABLE `application_evaluation_run`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `candidate_id` bigint NOT NULL,
  `candidate_fingerprint` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `suite_version_id` bigint NOT NULL,
  `baseline_release_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `dependency_fingerprint` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `evaluation_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'QUEUED',
  `aggregate_report_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `failure_category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `failure_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `started_at` datetime NULL DEFAULT NULL,
  `completed_at` datetime NULL DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_evaluation_run_candidate`(`tenant_id` ASC, `application_id` ASC, `candidate_id` ASC, `evaluation_status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_evaluation_run_suite`(`tenant_id` ASC, `suite_version_id` ASC, `completed_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of application_evaluation_run
-- ----------------------------

-- ----------------------------
-- Table structure for application_evaluation_suite
-- ----------------------------
DROP TABLE IF EXISTS `application_evaluation_suite`;
CREATE TABLE `application_evaluation_suite`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `suite_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `suite_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `suite_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_evaluation_suite_code`(`tenant_id` ASC, `application_id` ASC, `suite_code` ASC) USING BTREE,
  INDEX `idx_evaluation_suite_app`(`tenant_id` ASC, `application_id` ASC, `suite_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of application_evaluation_suite
-- ----------------------------

-- ----------------------------
-- Table structure for application_evaluation_suite_version
-- ----------------------------
DROP TABLE IF EXISTS `application_evaluation_suite_version`;
CREATE TABLE `application_evaluation_suite_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `suite_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `scoring_policy_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_evaluation_suite_version`(`tenant_id` ASC, `suite_id` ASC, `version_no` ASC) USING BTREE,
  INDEX `idx_evaluation_suite_version_app`(`tenant_id` ASC, `application_id` ASC, `suite_id` ASC, `version_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of application_evaluation_suite_version
-- ----------------------------

-- ----------------------------
-- Table structure for application_release_audit
-- ----------------------------
DROP TABLE IF EXISTS `application_release_audit`;
CREATE TABLE `application_release_audit`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `candidate_id` bigint NULL DEFAULT NULL,
  `release_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `gate_report_id` bigint NULL DEFAULT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `operator_id` bigint NOT NULL,
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `event_detail_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_release_audit_app`(`tenant_id` ASC, `application_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_release_audit_release`(`tenant_id` ASC, `release_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of application_release_audit
-- ----------------------------

-- ----------------------------
-- Table structure for application_release_candidate
-- ----------------------------
DROP TABLE IF EXISTS `application_release_candidate`;
CREATE TABLE `application_release_candidate`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `draft_revision_id` bigint NOT NULL,
  `draft_revision_no` int NOT NULL,
  `snapshot_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `snapshot_fingerprint` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `base_release_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `change_summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `candidate_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CREATED',
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `lock_version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_release_candidate_fingerprint`(`tenant_id` ASC, `application_id` ASC, `snapshot_fingerprint` ASC) USING BTREE,
  INDEX `idx_release_candidate_app_status`(`tenant_id` ASC, `application_id` ASC, `candidate_status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_release_candidate_draft`(`tenant_id` ASC, `application_id` ASC, `draft_revision_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of application_release_candidate
-- ----------------------------
INSERT INTO `application_release_candidate` VALUES (1, 4, 1026, 1028, 2, '{\"schemaVersion\":\"application-release-candidate-v1\",\"applicationId\":1026,\"draftRevisionNo\":2,\"graph\":{\"graphType\":\"APPLICATION_WORKFLOW\",\"schemaVersion\":\"1.0\",\"inputSchema\":{\"type\":\"object\",\"required\":[\"request\"],\"properties\":{\"request\":{\"type\":\"string\"}},\"description\":\"验证应用发布前的配置和质量检查流程\"},\"outputSchema\":{\"type\":\"object\",\"properties\":{\"result\":{\"type\":\"string\",\"format\":\"text\"}}},\"nodes\":[{\"id\":\"start\",\"type\":\"START\",\"name\":\"开始\",\"x\":80.0,\"y\":190.0,\"config\":{},\"inputSchema\":{},\"outputSchema\":{}},{\"id\":\"user-input\",\"type\":\"USER_INPUT\",\"name\":\"接收用户消息\",\"x\":300.0,\"y\":190.0,\"config\":{},\"inputSchema\":{},\"outputSchema\":{}},{\"id\":\"reply\",\"type\":\"DIRECT_REPLY\",\"name\":\"返回答复\",\"x\":520.0,\"y\":190.0,\"config\":{\"messageTemplate\":\"已收到：{{variables.user_message}}\"},\"inputSchema\":{},\"outputSchema\":{}},{\"id\":\"end\",\"type\":\"END\",\"name\":\"结束\",\"x\":740.0,\"y\":190.0,\"config\":{},\"inputSchema\":{},\"outputSchema\":{}}],\"edges\":[{\"edgeId\":\"edge-1\",\"sourceNodeId\":\"start\",\"sourcePort\":\"default\",\"targetNodeId\":\"user-input\",\"targetPort\":\"default\"},{\"edgeId\":\"edge-2\",\"sourceNodeId\":\"user-input\",\"sourcePort\":\"default\",\"targetNodeId\":\"reply\",\"targetPort\":\"default\"},{\"edgeId\":\"edge-3\",\"sourceNodeId\":\"reply\",\"sourcePort\":\"default\",\"targetNodeId\":\"end\",\"targetPort\":\"default\"}],\"variables\":[{\"sourceNodeId\":\"start\",\"outputPath\":\"$.output\",\"dataType\":\"any\",\"sensitive\":false},{\"sourceNodeId\":\"user-input\",\"outputPath\":\"$.output\",\"dataType\":\"any\",\"sensitive\":false},{\"sourceNodeId\":\"reply\",\"outputPath\":\"$.output\",\"dataType\":\"any\",\"sensitive\":false}]},\"dependencyFingerprint\":\"16e226e3439c917f5221e5b36826cee410ed236fbdcb80b8a0122e353bdc8b1f\",\"entrypointReferences\":[],\"policyReferences\":[]}', 'd6b04e7cf92d7cedca63a072c7ff5ab53e138d069e007ac5e911b94df7e762c3', NULL, '从应用工作区创建候选版本', 'CREATED', 6, '2026-08-06 10:42:47', '2026-08-06 10:42:47', 0);

-- ----------------------------
-- Table structure for application_release_gate_finding
-- ----------------------------
DROP TABLE IF EXISTS `application_release_gate_finding`;
CREATE TABLE `application_release_gate_finding`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `gate_report_id` bigint NOT NULL,
  `finding_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `finding_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `evidence_summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `remediation_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `overridable` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_gate_finding_code`(`tenant_id` ASC, `gate_report_id` ASC, `finding_code` ASC) USING BTREE,
  INDEX `idx_gate_finding_level`(`tenant_id` ASC, `application_id` ASC, `finding_level` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of application_release_gate_finding
-- ----------------------------

-- ----------------------------
-- Table structure for application_release_gate_report
-- ----------------------------
DROP TABLE IF EXISTS `application_release_gate_report`;
CREATE TABLE `application_release_gate_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `candidate_id` bigint NOT NULL,
  `candidate_fingerprint` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `evaluation_run_id` bigint NULL DEFAULT NULL,
  `overall_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `override_granted` tinyint(1) NOT NULL DEFAULT 0,
  `override_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `override_scope_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `override_by` bigint NULL DEFAULT NULL,
  `override_expires_at` datetime NULL DEFAULT NULL,
  `evaluated_by` bigint NOT NULL,
  `evaluated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_gate_report_candidate`(`tenant_id` ASC, `application_id` ASC, `candidate_id` ASC, `evaluated_at` ASC) USING BTREE,
  INDEX `idx_gate_report_level`(`tenant_id` ASC, `overall_level` ASC, `evaluated_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of application_release_gate_report
-- ----------------------------

-- ----------------------------
-- Table structure for application_webhook_nonce
-- ----------------------------
DROP TABLE IF EXISTS `application_webhook_nonce`;
CREATE TABLE `application_webhook_nonce`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `entrypoint_id` bigint NOT NULL,
  `nonce_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_application_webhook_nonce`(`entrypoint_id` ASC, `nonce_hash` ASC) USING BTREE,
  INDEX `idx_application_webhook_nonce_expiry`(`expires_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of application_webhook_nonce
-- ----------------------------

-- ----------------------------
-- Table structure for approval_task
-- ----------------------------
DROP TABLE IF EXISTS `approval_task`;
CREATE TABLE `approval_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `owner_team` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `risk_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `approval_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `payload_json` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_approval_tenant_status`(`tenant_id` ASC, `approval_status` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of approval_task
-- ----------------------------

-- ----------------------------
-- Table structure for async_task
-- ----------------------------
DROP TABLE IF EXISTS `async_task`;
CREATE TABLE `async_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `task_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `payload_json` json NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `max_retries` int NOT NULL DEFAULT 3,
  `worker_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `available_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `heartbeat_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_at` datetime NULL DEFAULT NULL,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `run_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `idempotency_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_async_task_idempotency`(`tenant_id` ASC, `task_type` ASC, `idempotency_key` ASC) USING BTREE,
  INDEX `idx_async_task_poll`(`status` ASC, `available_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_async_task_heartbeat`(`status` ASC, `heartbeat_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 58 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of async_task
-- ----------------------------
INSERT INTO `async_task` VALUES (56, 4, 'KNOWLEDGE_REINDEX', '{\"modelId\": null, \"language\": \"OTHER\", \"modelKey\": \"local-all-minilm-l6-v2\", \"tenantId\": 4, \"documentId\": 13, \"modelSource\": \"LOCAL\"}', 'COMPLETED', 0, 3, 'dad7a75c-c9ca-4ce1-83cc-0dfe39f05f90', '2026-08-06 14:41:36', '2026-08-06 14:41:40', '2026-08-06 14:41:36', '2026-08-06 14:41:40', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `async_task` VALUES (57, 4, 'ORCHESTRATION_DRAFT_TEST', '{\"role\": \"ADMIN\", \"appId\": 1026, \"input\": {\"request\": \"cs\"}, \"userId\": 6, \"traceId\": \"a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb\", \"username\": \"raysy\", \"messageId\": null, \"requestId\": \"a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb\", \"versionId\": null, \"executionId\": \"f3199e3e-305f-485a-9bd0-1902cf8d828f\", \"executionType\": \"APPLICATION_WORKFLOW\", \"conversationId\": null, \"idempotencyKey\": \"studio-test-1786069991698\"}', 'COMPLETED', 0, 5, 'df81a26c-7871-4526-8f7f-f017e6402ae6', '2026-08-07 10:33:12', '2026-08-07 10:33:14', '2026-08-07 10:33:12', '2026-08-07 10:33:14', NULL, 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', 'f3199e3e-305f-485a-9bd0-1902cf8d828f', 'studio-test-1786069991698');

-- ----------------------------
-- Table structure for audit_log
-- ----------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `operator_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `action_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `target_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `target_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `risk_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `detail_json` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_audit_tenant_time`(`tenant_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_audit_risk`(`tenant_id` ASC, `risk_level` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of audit_log
-- ----------------------------
INSERT INTO `audit_log` VALUES (1, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1003', 'P3', '{\"revisionNo\": 1}', '2026-07-24 13:53:31');
INSERT INTO `audit_log` VALUES (2, 4, 'raysy', 'ORCHESTRATION_PUBLISH', 'WORKFLOW', '1003', 'P3', '{\"versionId\": \"v_58c470e6f45e48b6978e805eb32c7ab9\", \"versionNo\": 1}', '2026-07-24 13:54:39');
INSERT INTO `audit_log` VALUES (3, 2, 'tenant-admin', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1004', 'P3', '{\"revisionNo\": 1}', '2026-07-24 15:12:04');
INSERT INTO `audit_log` VALUES (7, 2, 'tenant-admin', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1004', 'P3', '{\"revisionNo\": 2}', '2026-07-24 15:13:25');
INSERT INTO `audit_log` VALUES (8, 2, 'tenant-admin', 'ORCHESTRATION_PUBLISH', 'WORKFLOW', '1004', 'P3', '{\"versionId\": \"v_2ca10252b6504f779fb227a5eeffd1cd\", \"versionNo\": 1}', '2026-07-24 15:13:26');
INSERT INTO `audit_log` VALUES (9, 4, 'raysy', 'GRANT_RESOURCE_PERMISSION', 'AGENT', '5', 'P2', NULL, '2026-07-24 17:35:44');
INSERT INTO `audit_log` VALUES (10, 4, 'raysy', 'GRANT_RESOURCE_PERMISSION', 'AGENT', '5', 'P2', NULL, '2026-07-24 17:35:46');
INSERT INTO `audit_log` VALUES (11, 4, 'raysy', 'GRANT_RESOURCE_PERMISSION', 'AGENT', '5', 'P2', NULL, '2026-07-24 17:35:48');
INSERT INTO `audit_log` VALUES (12, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1003', 'P3', '{\"revisionNo\": 2}', '2026-07-24 17:40:40');
INSERT INTO `audit_log` VALUES (13, 4, 'raysy', 'ORCHESTRATION_PUBLISH', 'WORKFLOW', '1003', 'P3', '{\"versionId\": \"v_7457927ec0714dbd935f803aaa0334f4\", \"versionNo\": 2}', '2026-07-24 17:40:49');
INSERT INTO `audit_log` VALUES (14, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1003', 'P3', '{\"revisionNo\": 3}', '2026-07-24 17:53:59');
INSERT INTO `audit_log` VALUES (15, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1003', 'P3', '{\"revisionNo\": 4}', '2026-07-24 17:55:06');
INSERT INTO `audit_log` VALUES (16, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1003', 'P3', '{\"revisionNo\": 5}', '2026-07-24 17:55:25');
INSERT INTO `audit_log` VALUES (17, 4, 'raysy', 'ORCHESTRATION_PUBLISH', 'WORKFLOW', '1003', 'P3', '{\"versionId\": \"v_9429c726b5104b2d9d2ea4dac15b4727\", \"versionNo\": 3}', '2026-07-24 17:55:30');
INSERT INTO `audit_log` VALUES (18, 4, 'raysy', 'ORCHESTRATION_PUBLISH', 'WORKFLOW', '1003', 'P3', '{\"versionId\": \"v_9fa7091ccad64b07aaca21cff15fab82\", \"versionNo\": 4}', '2026-07-24 17:55:41');
INSERT INTO `audit_log` VALUES (19, 4, 'raysy', 'ORCHESTRATION_PUBLISH', 'WORKFLOW', '1003', 'P3', '{\"versionId\": \"v_5500ac1352ec4cd38df0a819e462bae9\", \"versionNo\": 5}', '2026-07-24 17:55:54');
INSERT INTO `audit_log` VALUES (20, 4, 'raysy', 'INSTALL_TEMPLATE', 'MARKETPLACE', '企业财务智能对账工作流', 'P3', '{\"itemType\": \"Workflow\"}', '2026-07-27 17:00:58');
INSERT INTO `audit_log` VALUES (21, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1003', 'P3', '{\"revisionNo\": 6}', '2026-07-28 09:22:42');
INSERT INTO `audit_log` VALUES (22, 4, 'raysy', 'ORCHESTRATION_PUBLISH', 'WORKFLOW', '1003', 'P3', '{\"versionId\": \"v_2bd33d6d21d9444e87b5ab9d95f8c60a\", \"versionNo\": 6}', '2026-07-28 09:22:49');
INSERT INTO `audit_log` VALUES (23, 1, 'admin', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1005', 'P3', '{\"revisionNo\": 1}', '2026-07-28 09:49:05');
INSERT INTO `audit_log` VALUES (24, 1, 'admin', 'ORCHESTRATION_PUBLISH', 'WORKFLOW', '1005', 'P3', '{\"versionId\": \"v_9540044a48ec45178e115879180d92f7\", \"versionNo\": 1}', '2026-07-28 09:49:13');
INSERT INTO `audit_log` VALUES (25, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1011', 'P3', '{\"revisionNo\": 1}', '2026-07-28 16:35:40');
INSERT INTO `audit_log` VALUES (26, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1017', 'P3', '{\"revisionNo\": 1}', '2026-07-29 10:32:59');
INSERT INTO `audit_log` VALUES (31, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1018', 'P3', '{\"revisionNo\": 1}', '2026-07-29 10:45:47');
INSERT INTO `audit_log` VALUES (34, 4, 'raysy', 'INSTALL_TEMPLATE', 'MARKETPLACE', '售后智能客服 Agent 模版', 'P3', '{\"itemType\": \"Agent\"}', '2026-07-29 10:52:18');
INSERT INTO `audit_log` VALUES (35, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1019', 'P3', '{\"revisionNo\": 1}', '2026-07-29 11:16:43');
INSERT INTO `audit_log` VALUES (36, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1023', 'P3', '{\"revisionNo\": 1}', '2026-07-29 14:39:59');
INSERT INTO `audit_log` VALUES (37, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1024', 'P3', '{\"revisionNo\": 1}', '2026-07-29 16:40:15');
INSERT INTO `audit_log` VALUES (38, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1024', 'P3', '{\"revisionNo\": 2}', '2026-07-29 16:47:49');
INSERT INTO `audit_log` VALUES (39, 4, 'raysy', 'ORCHESTRATION_PUBLISH', 'WORKFLOW', '1024', 'P3', '{\"versionId\": \"v_e0b67ded883e4348afb1a7b3d895f546\", \"versionNo\": 1}', '2026-07-29 16:48:03');
INSERT INTO `audit_log` VALUES (40, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1024', 'P3', '{\"revisionNo\": 3}', '2026-07-29 16:49:29');
INSERT INTO `audit_log` VALUES (41, 4, 'raysy', 'ORCHESTRATION_PUBLISH', 'WORKFLOW', '1024', 'P3', '{\"versionId\": \"v_55d1acd4343a439da0dff9a424aba72e\", \"versionNo\": 2}', '2026-07-29 16:49:37');
INSERT INTO `audit_log` VALUES (42, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1026', 'P3', '{\"revisionNo\": 1}', '2026-08-06 10:12:12');
INSERT INTO `audit_log` VALUES (43, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1026', 'P3', '{\"revisionNo\": 2}', '2026-08-06 10:15:05');
INSERT INTO `audit_log` VALUES (44, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1026', 'P3', '{\"revisionNo\": 3}', '2026-08-07 10:30:46');
INSERT INTO `audit_log` VALUES (45, 4, 'raysy', 'ORCHESTRATION_DRAFT_SAVE', 'WORKFLOW', '1026', 'P3', '{\"revisionNo\": 4}', '2026-08-07 10:31:03');

-- ----------------------------
-- Table structure for business_application
-- ----------------------------
DROP TABLE IF EXISTS `business_application`;
CREATE TABLE `business_application`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `app_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `app_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `owner_id` bigint NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `current_release_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_business_application_code`(`tenant_id` ASC, `app_code` ASC) USING BTREE,
  INDEX `idx_business_application_owner`(`tenant_id` ASC, `owner_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_business_application_release`(`tenant_id` ASC, `current_release_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of business_application
-- ----------------------------

-- ----------------------------
-- Table structure for business_application_access_policy
-- ----------------------------
DROP TABLE IF EXISTS `business_application_access_policy`;
CREATE TABLE `business_application_access_policy`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `organization_id` bigint NULL DEFAULT NULL,
  `role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `action_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `effect` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ALLOW',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_business_application_policy_scope`(`tenant_id` ASC, `application_id` ASC, `organization_id` ASC, `role_code` ASC, `action_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of business_application_access_policy
-- ----------------------------

-- ----------------------------
-- Table structure for business_application_release
-- ----------------------------
DROP TABLE IF EXISTS `business_application_release`;
CREATE TABLE `business_application_release`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `release_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version_no` int NOT NULL,
  `environment_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PUBLISHED',
  `release_snapshot_json` json NOT NULL,
  `released_by` bigint NULL DEFAULT NULL,
  `released_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_business_application_release`(`application_id` ASC, `release_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_business_application_release_version`(`application_id` ASC, `version_no` ASC, `environment_code` ASC) USING BTREE,
  INDEX `idx_business_application_release_tenant`(`tenant_id` ASC, `application_id` ASC, `released_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of business_application_release
-- ----------------------------

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `message_role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `message_text` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `reference_json` json NULL,
  `tool_trace_json` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chat_message_session`(`session_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_message
-- ----------------------------
INSERT INTO `chat_message` VALUES (1, 1, 'USER', '1', NULL, NULL, '2026-07-24 17:35:55');
INSERT INTO `chat_message` VALUES (2, 2, 'USER', '招聘Java开发工程师', NULL, NULL, '2026-07-24 17:36:19');
INSERT INTO `chat_message` VALUES (3, 3, 'USER', '招聘Java开发工程师', NULL, NULL, '2026-07-24 17:37:11');
INSERT INTO `chat_message` VALUES (4, 4, 'USER', '招聘Java开发工程师', NULL, NULL, '2026-07-24 17:37:37');
INSERT INTO `chat_message` VALUES (5, 5, 'USER', '招聘Java开发工程师', NULL, NULL, '2026-07-24 17:37:59');
INSERT INTO `chat_message` VALUES (6, 6, 'USER', '招聘Java开发工程师', NULL, NULL, '2026-07-24 17:38:24');
INSERT INTO `chat_message` VALUES (7, 7, 'USER', '招聘Java开发工程师', NULL, NULL, '2026-07-24 17:39:23');
INSERT INTO `chat_message` VALUES (8, 8, 'USER', '招聘Java开发工程师', NULL, NULL, '2026-07-24 17:40:05');
INSERT INTO `chat_message` VALUES (9, 9, 'USER', '招聘Java开发工程师', NULL, NULL, '2026-07-24 17:41:01');
INSERT INTO `chat_message` VALUES (10, 10, 'USER', '招聘Java开发工程师', NULL, NULL, '2026-07-24 17:56:20');
INSERT INTO `chat_message` VALUES (11, 11, 'USER', '招聘Java开发工程师', NULL, NULL, '2026-07-27 10:10:22');
INSERT INTO `chat_message` VALUES (12, 12, 'USER', '招聘Java开发工程师', NULL, NULL, '2026-07-27 10:12:59');
INSERT INTO `chat_message` VALUES (13, 13, 'USER', '招聘Java开发工程师', NULL, NULL, '2026-07-27 14:21:59');
INSERT INTO `chat_message` VALUES (14, 14, 'USER', '招聘java开发', NULL, NULL, '2026-07-28 11:49:29');

-- ----------------------------
-- Table structure for chat_session
-- ----------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `agent_id` bigint NOT NULL,
  `session_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chat_session_tenant`(`tenant_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_chat_session_agent`(`agent_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_session
-- ----------------------------
INSERT INTO `chat_session` VALUES (1, 4, 5, '1', 'raysy', '2026-07-24 17:35:54');
INSERT INTO `chat_session` VALUES (2, 4, 5, '招聘Java开发工程师', 'raysy', '2026-07-24 17:36:19');
INSERT INTO `chat_session` VALUES (3, 4, 5, '招聘Java开发工程师', 'raysy', '2026-07-24 17:37:11');
INSERT INTO `chat_session` VALUES (4, 4, 5, '招聘Java开发工程师', 'raysy', '2026-07-24 17:37:37');
INSERT INTO `chat_session` VALUES (5, 4, 5, '招聘Java开发工程师', 'raysy', '2026-07-24 17:37:59');
INSERT INTO `chat_session` VALUES (6, 4, 5, '招聘Java开发工程师', 'raysy', '2026-07-24 17:38:24');
INSERT INTO `chat_session` VALUES (7, 4, 5, '招聘Java开发工程师', 'raysy', '2026-07-24 17:39:23');
INSERT INTO `chat_session` VALUES (8, 4, 5, '招聘Java开发工程师', 'raysy', '2026-07-24 17:40:05');
INSERT INTO `chat_session` VALUES (9, 4, 5, '招聘Java开发工程师', 'raysy', '2026-07-24 17:41:01');
INSERT INTO `chat_session` VALUES (10, 4, 5, '招聘Java开发工程师', 'raysy', '2026-07-24 17:56:19');
INSERT INTO `chat_session` VALUES (11, 4, 5, '招聘Java开发工程师', 'raysy', '2026-07-27 10:10:22');
INSERT INTO `chat_session` VALUES (12, 4, 5, '招聘Java开发工程师', 'raysy', '2026-07-27 10:12:59');
INSERT INTO `chat_session` VALUES (13, 4, 5, '招聘Java开发工程师', 'raysy', '2026-07-27 14:21:59');
INSERT INTO `chat_session` VALUES (14, 4, 5, '招聘java开发', 'raysy', '2026-07-28 11:49:29');

-- ----------------------------
-- Table structure for cost_alert_event
-- ----------------------------
DROP TABLE IF EXISTS `cost_alert_event`;
CREATE TABLE `cost_alert_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `alert_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `current_cost` decimal(18, 8) NOT NULL,
  `limit_cost` decimal(18, 8) NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OPEN',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_cost_alert_tenant_time`(`tenant_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cost_alert_event
-- ----------------------------

-- ----------------------------
-- Table structure for enterprise_identity_config
-- ----------------------------
DROP TABLE IF EXISTS `enterprise_identity_config`;
CREATE TABLE `enterprise_identity_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `protocol` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `issuer` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `entity_id` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `organization_claim` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `callback_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `claim_mapping_json` json NULL,
  `scim_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `mfa_policy_json` json NULL,
  `secret_ref` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `validated_at` datetime NULL DEFAULT NULL,
  `activated_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_identity_config_version`(`tenant_id` ASC, `version_no` ASC) USING BTREE,
  INDEX `idx_identity_config_active`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of enterprise_identity_config
-- ----------------------------

-- ----------------------------
-- Table structure for evaluation_result
-- ----------------------------
DROP TABLE IF EXISTS `evaluation_result`;
CREATE TABLE `evaluation_result`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `sample_id` bigint NOT NULL,
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `expected_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `actual_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `score` decimal(8, 3) NULL DEFAULT NULL,
  `latency_ms` bigint NULL DEFAULT NULL,
  `result_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_evaluation_result_task`(`task_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of evaluation_result
-- ----------------------------

-- ----------------------------
-- Table structure for evaluation_sample
-- ----------------------------
DROP TABLE IF EXISTS `evaluation_sample`;
CREATE TABLE `evaluation_sample`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `expected_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_evaluation_sample_tenant`(`tenant_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of evaluation_sample
-- ----------------------------

-- ----------------------------
-- Table structure for evaluation_task
-- ----------------------------
DROP TABLE IF EXISTS `evaluation_task`;
CREATE TABLE `evaluation_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `agent_id` bigint NULL DEFAULT NULL,
  `task_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sample_count` int NOT NULL DEFAULT 0,
  `scoring_rule` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'EXACT_MATCH',
  `score` decimal(8, 3) NULL DEFAULT NULL,
  `evaluation_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `report_json` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_evaluation_task_tenant`(`tenant_id` ASC, `evaluation_status` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of evaluation_task
-- ----------------------------

-- ----------------------------
-- Table structure for iam_permission
-- ----------------------------
DROP TABLE IF EXISTS `iam_permission`;
CREATE TABLE `iam_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `permission_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `permission_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `resource_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `action_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iam_permission_code`(`permission_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 66 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iam_permission
-- ----------------------------
INSERT INTO `iam_permission` VALUES (1, 'agent:read', '读取 Agent', 'AGENT', 'READ', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (2, 'agent:write', '编辑 Agent', 'AGENT', 'WRITE', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (3, 'agent:publish', '发布 Agent', 'AGENT', 'PUBLISH', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (4, 'workflow:read', '读取工作流', 'WORKFLOW', 'READ', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (5, 'workflow:write', '编辑工作流', 'WORKFLOW', 'WRITE', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (6, 'workflow:publish', '发布工作流', 'WORKFLOW', 'PUBLISH', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (7, 'workflow:rollback', '回滚工作流', 'WORKFLOW', 'ROLLBACK', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (8, 'workflow:execution:view', '查看执行详情', 'EXECUTION', 'READ', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (9, 'knowledge:read', '读取知识文档', 'KNOWLEDGE_DOCUMENT', 'READ', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (10, 'tool:invoke', '调用工具连接器', 'TOOL_CONNECTOR', 'INVOKE', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (11, 'approval:approve', '审批人工任务', 'APPROVAL_TASK', 'APPROVE', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (12, 'analytics:read', '读取统计分析', 'ANALYTICS', 'READ', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (13, 'audit:read', '读取审计日志', 'AUDIT', 'READ', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (14, 'model:read', '读取模型配置', 'MODEL', 'READ', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (15, 'model:write', '编辑模型配置', 'MODEL', 'WRITE', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (61, 'APPLICATION_CANDIDATE_CREATE', '创建应用候选版本', 'APPLICATION', 'CANDIDATE_CREATE', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (62, 'APPLICATION_EVALUATION_RUN', '执行应用版本评测', 'APPLICATION', 'EVALUATION_RUN', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (63, 'APPLICATION_PUBLISH', '发布应用生产版本', 'APPLICATION', 'PUBLISH', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (64, 'APPLICATION_RELEASE_OVERRIDE', '紧急豁免发布门禁', 'APPLICATION', 'RELEASE_OVERRIDE', 'ACTIVE');
INSERT INTO `iam_permission` VALUES (65, 'APPLICATION_ROLLBACK', '回滚应用生产版本', 'APPLICATION', 'ROLLBACK', 'ACTIVE');

-- ----------------------------
-- Table structure for iam_permission_deny_rule
-- ----------------------------
DROP TABLE IF EXISTS `iam_permission_deny_rule`;
CREATE TABLE `iam_permission_deny_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `subject_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `subject_id` bigint NOT NULL,
  `resource_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `resource_id` bigint NULL DEFAULT NULL,
  `action_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_iam_deny_lookup`(`tenant_id` ASC, `subject_type` ASC, `subject_id` ASC, `resource_type` ASC, `action_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iam_permission_deny_rule
-- ----------------------------

-- ----------------------------
-- Table structure for iam_permission_version
-- ----------------------------
DROP TABLE IF EXISTS `iam_permission_version`;
CREATE TABLE `iam_permission_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `version_no` bigint NOT NULL,
  `change_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `changed_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iam_permission_version`(`tenant_id` ASC, `version_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iam_permission_version
-- ----------------------------

-- ----------------------------
-- Table structure for iam_role_menu_template
-- ----------------------------
DROP TABLE IF EXISTS `iam_role_menu_template`;
CREATE TABLE `iam_role_menu_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `menu_id` bigint NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iam_role_menu_template`(`role_code` ASC, `menu_id` ASC) USING BTREE,
  INDEX `idx_iam_role_menu_template_role`(`role_code` ASC, `status` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iam_role_menu_template
-- ----------------------------
INSERT INTO `iam_role_menu_template` VALUES (1, 'ADMIN', 1, 10, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (2, 'ADMIN', 2, 20, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (3, 'ADMIN', 3, 30, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (4, 'ADMIN', 4, 40, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (5, 'ADMIN', 5, 50, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (6, 'ADMIN', 6, 60, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (7, 'ADMIN', 7, 70, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (8, 'ADMIN', 8, 80, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (9, 'ADMIN', 9, 90, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (10, 'ADMIN', 10, 100, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (11, 'ADMIN', 12, 120, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (12, 'ADMIN', 13, 130, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (13, 'ADMIN', 14, 140, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (14, 'ADMIN', 15, 150, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (15, 'ADMIN', 16, 160, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (16, 'ADMIN', 17, 170, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (17, 'ADMIN', 18, 180, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (18, 'ADMIN', 19, 190, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (19, 'OPERATOR', 1, 10, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (20, 'OPERATOR', 2, 20, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (21, 'OPERATOR', 3, 30, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (22, 'OPERATOR', 4, 40, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (23, 'OPERATOR', 5, 50, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (24, 'OPERATOR', 8, 80, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (25, 'OPERATOR', 9, 90, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (26, 'OPERATOR', 13, 130, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (27, 'OPERATOR', 16, 160, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (28, 'OPERATOR', 19, 190, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (29, 'STAFF', 1, 10, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (30, 'STAFF', 3, 30, 'ACTIVE', '2026-07-23 15:04:57');
INSERT INTO `iam_role_menu_template` VALUES (31, 'STAFF', 19, 190, 'ACTIVE', '2026-07-23 15:04:57');

-- ----------------------------
-- Table structure for iam_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `iam_role_permission`;
CREATE TABLE `iam_role_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  `data_scope` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SELF',
  `custom_scope_json` json NULL,
  `valid_until` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iam_role_permission`(`tenant_id` ASC, `role_id` ASC, `permission_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iam_role_permission
-- ----------------------------

-- ----------------------------
-- Table structure for iam_user_role_binding
-- ----------------------------
DROP TABLE IF EXISTS `iam_user_role_binding`;
CREATE TABLE `iam_user_role_binding`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `source_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'MIGRATED',
  `is_primary` tinyint NOT NULL DEFAULT 0,
  `valid_from` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `valid_until` datetime NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iam_user_role_binding`(`tenant_id` ASC, `user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `idx_iam_user_role_validity`(`tenant_id` ASC, `user_id` ASC, `status` ASC, `valid_until` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iam_user_role_binding
-- ----------------------------
INSERT INTO `iam_user_role_binding` VALUES (1, 1, 1, 1, 'SEED', 1, '2026-07-23 15:04:57', NULL, 'ACTIVE', NULL, '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `iam_user_role_binding` VALUES (2, 2, 2, 2, 'SEED', 1, '2026-07-23 15:04:57', NULL, 'ACTIVE', NULL, '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `iam_user_role_binding` VALUES (3, 2, 3, 3, 'SEED', 1, '2026-07-23 15:04:57', NULL, 'ACTIVE', NULL, '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `iam_user_role_binding` VALUES (4, 2, 4, 4, 'SEED', 1, '2026-07-23 15:04:57', NULL, 'ACTIVE', NULL, '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `iam_user_role_binding` VALUES (5, 3, 5, 5, 'SEED', 1, '2026-07-23 15:04:57', NULL, 'ACTIVE', NULL, '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `iam_user_role_binding` VALUES (11, 4, 6, 8, 'SYSTEM', 1, '2026-07-24 13:41:26', NULL, 'ACTIVE', 6, '2026-07-24 13:41:26', '2026-07-24 13:41:26');

-- ----------------------------
-- Table structure for knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `base_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `base_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `base_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_knowledge_base_tenant_code`(`tenant_id` ASC, `base_code` ASC) USING BTREE,
  INDEX `idx_knowledge_base_tenant_status`(`tenant_id` ASC, `base_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_base
-- ----------------------------
INSERT INTO `knowledge_base` VALUES (1, 1, 'default', '默认知识库', 'ACTIVE', NULL, '2026-08-06 12:28:31', '2026-08-06 12:28:31');

-- ----------------------------
-- Table structure for knowledge_chunk
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_chunk`;
CREATE TABLE `knowledge_chunk`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `document_id` bigint NOT NULL,
  `chunk_no` int NOT NULL,
  `chunk_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `vector_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `index_generation_id` bigint NULL DEFAULT NULL,
  `parent_chunk_id` bigint NULL DEFAULT NULL,
  `chunk_role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CHILD',
  `section_path` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `page_no` int NULL DEFAULT NULL,
  `token_count` int NOT NULL DEFAULT 0,
  `content_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `embedding_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `late_vector_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DISABLED',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_knowledge_chunk_vector`(`vector_key` ASC) USING BTREE,
  INDEX `idx_knowledge_chunk_document`(`document_id` ASC) USING BTREE,
  INDEX `idx_knowledge_chunk_generation`(`index_generation_id` ASC, `document_id` ASC) USING BTREE,
  INDEX `idx_knowledge_chunk_parent`(`parent_chunk_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2085254959200247825 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_chunk
-- ----------------------------
INSERT INTO `knowledge_chunk` VALUES (2085254958843731969, 13, 1, 'AI era large model intelligent agent training project\n\n目 录\n\nP R O J E C T  I N T R O D U C T I O N\n\n4\n\n人工智能\n\n人工智能（Artificial Intelligence，简称AI）是指由计算机系\n\n统所表现出的智能行为。它是一门研究、开发用于模拟、延伸和\n\n扩展人的智能的理论、方法、技术及应用系统的科学。AI的目标\n\n是创建能够执行通常需要人类智能的任务的软件或机器，这些任\n\n务包括但不限于学习、推理、解决问题、理解语言、识别图像等。\n\n具体来说，人工智能涵盖了多种技术和算法，如深度学习、机器\n\n学习、计算机视觉、自然语言处理等。这些技术使得机器能够处\n\n理和理解大量的数据，从而模拟人类的智能行为。\n\n例如，通过自然语言处理技术，\n计算机可以理解和生成人类语\n言例如，通过自然语言处理技\n术，计算机可以理解和生成人\n类语言。\n\n例如通过计算机视觉技术，\n机器可以识别图像中的物\n体、人脸 、文字等信息，\n应用于安防监控、自动驾驶\n等领域。\n\n5\n\n.\n\n什么是模型智能体训练\n大模型智能体训练是指使用大规模数据和\n\n强大的计算能力来训练具有海量参数的复\n\n杂人工智能模型和智能体。 这些模型和智\n\n能体通过深度神经网络架构进行学习与训\n\n练，具备处理多种复杂任务的能力，而不\n\n仅仅局限于单一任务。\n\n模型、智能体训练\n数据收集 -> 预训练 -> 微调\n\n数据来源\n系 统 的 日 志 收 集 / 互 联 网 数 据 收 集\n\n/app 移动端数据收集/数据服务机构\n\n进行合作 。\n\n质量标准\n\n对于人眼所见的图像而言 ，计算机所\n\n见的图像只是一堆枯燥的数字 。 图像\n\n标注就是根据需求将这一堆数字划分\n\n区 域 ，让计算机在划分出来的区域中\n\n找寻数字的规律 。多音字标注的质量\n\n标准就是标注一个字的全部读音 ，这\n\n就需要借助字典等专业性工具进行检\n\n验 。\n\n有多少人工就有多少智能\n数据处理的量级与质量直接关系到机\n\n器的智能程度\n\n6\n\nAI模型、智能体训练行业优势\n\n符合产业发展方向，\n\n紧贴国家政策。\n\n安全落地、无人脉风\n\n险。\n\n副业刚需、市场刚需、\n\n时间自由。\n\n不受大环境的影响。\n\n自主创业，解决就\n\n业人口问题。\n轻资产、投资小、\n\n风险低。\n\n无需跑市场，在家即\n\n可创业。\n\n按劳分配，多劳多得，\n\n少劳少得。\n\n业务稳定、无需维护\n\n社会关系。\n\n一站式服务，无需高\n\n学历高技术，包教包\n\n会。\n\n7\n\n1：图片处理(地图导航无人\n\n驾驶医学刷脸支付 )\n\n2：语音转写(智能机器人/语\n\n音指令/通讯/电影/会议 )\n\n3：文本转写(教育行业/零售\n\n/广告/金融 )\n\n4：视频处理(娱乐/家具/物\n\n联网/无人驾驶 )\n\n5：游戏处理(游戏对话 游戏\n\n人物 游戏地图 )\n\n6：影视处理(语音对白字幕\n\n标志物 )\n\n8\n\n语音转写，就是把听到的语音内容以文字的形式翻\n译出来就可以，语音数据的应用场景比如：天猫精\n灵小艾同学地图导航手机语音APP等，语音转写价\n格是150-260元左右，新手通过公司5-7天培训就\n可以上手做语音转写，操作很简单，熟能生巧的人\n产值在7000—12000左右。\n\n图片处理 ，根据项目的要求需要把图片中道路上的\n行人/车辆 /车道线/红绿灯等框出来 ，一般是4-8\n分一个框每张图片中的要求拉框目标内容都不同，\n图片处理主要是应用于无人驾驶让汽车的传感器和\n雷达来识别障碍物，还有地图、导航、人脸识别等。\n\n9\n\n智能交通 智能零售 智能医疗 智能安防\n通过AI大模型训练，可以\n确定商品类别、品牌、颜\n色等属性信息，实现自动\n化 识别和分类。此外，还\n可以对零售场 所的监控视\n频进行训练，识别和跟踪\n异常行为、窃盗行为和欺\n诈行为。\n\n医疗影像处理是对医疗影像\n进行区域及分类处理，多应\n用于辅助临床诊断，人工智\n能通过学习大量的医疗影像\nAI大模型训练数据集，将\n会更好的辅助医生诊断。\n\n智能安防是人工智能与信息\n技术结合的关键领域，AI大\n模型训练提供了一个关键的\n训练数据集，通过对图像、\n视频和音频等数据进行处理，\n标明其中的目标对象、动作\n行为等信息，以供系统算法\n学习和训练。', 'parent-ffb1addd-bc64-4629-a406-11d3b0234241', '2026-08-06 14:41:39', 10, NULL, 'PARENT', '正文', NULL, 600, '69d0a086de824551fef23e1fd07962b66c17580f38ebb5b099e097aa9cf742ac', 'DISABLED', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254958843731970, 13, 2, '无人驾驶正在慢慢地走进人\n们的生活，想要让汽车本身\n的算法处理更多、更复杂的\n场景，背后就需要有 海量的\n真实道路数据做支撑，而这 \n就需要依靠AI大模型训练。\n\nB R I E F  H I S T O R Y  O F  E N T E R P R I S E  D E V E L O P M E N T\n\n企业荣誉\n\n公 司 自 成 立 以 来 ，取 得 了 一 系 列 的 荣 誉 和 成 绩 ，得 到 了 广 \n大 合 作 伙 伴 以 及 政 府 机 关 和 行 业 的 认 可 和 认 证。\n\n部分合作伙伴展示\n\nP R O J E C T  O P E R A T I O N  M O D E\n\n大模型、智能体训练项目（语音转写）盈利参考分析\n\n转换时长 单价 一天收益（元） 一个月收益（元）\n\n1小时 200 200x1=200 200x30=6000\n\n1.5小时 200 200x1.5=300 300x30=9000\n\n2小时 200 200x2=400 400x30=12000\n\n语音转写：价格是150-260左右每小时，具体价格以实际发包为准。(备注：价格是和转写难\n易程度是成正比的)，正常一天工作8小时正常可以转换1-2小时左右数据。\n\n（注:以下表格按照语音转写均价200/小时，5人团标准计算具体以实际为准）\n\n场地租金 1500-2000/月 一般商住楼或者小区套二套三就行，如果在小区可以中午管饭，员工的留存率\n和积极性会更高。\n\n办公桌椅 500 简单办公卡位75-100一套。\n\n场地布置 500 网络/饮水机/垃圾桶等。\n\n电脑5台 5000 网上二手电脑或者网吧处理800-1200/台。\n\n提供协助合作伙伴的团队招募成立、培训、培养专业AI大模型训练师，集一体的一站式孵化扶持平台。\n源源不断的资源供应。每月25-30号结算验收合格的数据\n\n技术培训服务费，第一年\n2.6万/5人费用，第二年\n百分之二十收取5200一\n年\n\n。\n\n前期预计投入 4500+500+500+5000+26000=36500\n\nAI模型智能体训练项目（语音转写）盈利参考分析\n\n平均日产值 个人月产值\n（26天）\n\n语音单价\n200/h\n\n底薪 全勤 绩效奖金 提成10元/h 5人工资支出 5人余利润\n\n1.5h 39h 39*200*5 3000 200 200 390 18950 20050\n\n1.8h 46.8h 46.8*200*5 3000 200 300 468 19840 26960\n\n2h 52h 52*200*5 3000 200 500 520 21100 30900\n\n（注:以下表格按照语音转写均价200/小时，10人团标准计算具体以实际为准）\n\n场地租金 1500-2500/月 一般商住楼或者小区套二套三就行，如果在小区可以中午管饭，员工的留存率\n和积极性会更高\n\n办公桌椅 1000 简单办公卡位75-100一套\n\n场地布置 1000 网络/饮水机/垃圾桶等\n\n电脑10台 10000 网上二手电脑或者网吧处理800-1200/台\n\n提供协助合作伙伴的团队招募成立、培训、培养专业AI大模型训练师，集一体的一站式孵化扶持平台。\n源源不断的资源供应。每月25-30号结算验收合格的数据\n\n技术培训服务费，第一年\n3.2万/10人费用，第二年\n百分之二十收取6400一\n年\n\n。\n\n前期预计投入 5000+1000+1000+10000+32000=49000\n\n平均日产值 个人月产值\n（26天）\n\n语音单价\n200/h\n\n底薪 全勤 绩效奖金 提成10元/h 10人工资支出 10人余利润\n\n1.5h 39h 39*200*10 3000 200 200 390 37900 40100\n\n1.8h 46.8h 46.8*200*10 3000 200 300 468 39680 53920\n\n2h 52h 52*200*10 3000 200 500 520 42200 61800\n\n（注:以下表格按照语音转写均价200/小时，20人团标准计算具体以实际为准）\n\n场地租金 2000-3000/月 一般商住楼或者小区套二套三就行，如果在小区可以中午管饭，员工的留存率\n和积极性会更高', 'parent-6b2887ca-546b-4787-bad1-6ef16d28e536', '2026-08-06 14:41:39', 10, NULL, 'PARENT', '正文', NULL, 594, '3973e3985ccc61b4b6242a47ee378799a6f31a301db3a8ad0659ae782ece1e5f', 'DISABLED', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254958843731971, 13, 3, '办公桌椅 2000 简单办公卡位75-100一套\n\n场地布置 2000 网络/饮水机/垃圾桶等\n\n电脑20台 20000 网上二手电脑或者网吧处理800-1200/台\n\n提供协助合作伙伴的团队招募成立、培训、培养专业AI大模型训练师，集一体的一站式孵化扶持平台。\n源源不断的资源供应。每月25-30号结算验收合格的数据\n\n技术培训服务费，第一年\n6万/20人费用，第二年\n百分之二十收取1.2万一\n年\n\n。\n\n前期预计投入 8000+2000+2000+20000+60000=92000\n\n平均日产值 个人月产值\n（26天）\n\n语音单价200/h 底薪 全勤 绩效奖金 提成10元/h 20人工资支出 20人余利润\n\n1.5h 39h 39*200*20 3000 200 200 390 75800 80200\n\n1.8h 46.8h 46.8*200*20 3000 200 300 468 79360 107840\n\n2h 52h 52*200*20 3000 200 500 520 84400 123600\n\nG O V E R N M E N T  A N D  E N T E R P R I S E  S U P P O R T ,  B L U E  \nO C E A N  S T R A T E G Y\n\n为加快人工智能发展、抢占工业、技术发展先机 ，国\n\n家政企扶持人工智能产业 ，以行业三巨头（百度、阿\n\n里、腾讯）为首 ，重金打造专业AI大模型训练产业群 ，\n\n助力打造一批专业的AI模型和智能体训练师。\n\n政企扶持，蓝海战略\n\n国家十四五规划大力发展人工智能行业，预计在未来5-10\n\n年投放30万亿。\n\n23年至24年成立国家数据局，对该项目进行国家级的管理，\n\n并且在成都、长沙、大同、保定、杭州等7个城市成立国家级\n\n数据处理基地，以及数据交易中心\n\n百度在海东、海口、太原建立AI大模型训练基地。目前有\n\n10000左右AI大模型训练员办公,计划在5年内扩展到5万人！\n\n阿里、腾讯、华为等都分别有再贵州建立大数据中心。\n\n联合大于100家本地数据化工厂服务商抱团创业 ，形成AI\n\n人工智能行业中的“虹吸效应”\n\n发展前景\nD E V E L O P M E N T  P R O S P E C T S\n\n互联网三巨头（百度、腾讯、阿里）都相继\n\n建立自己的AI大模型训练基地和大数据处\n\n理中心，新成立的宇数科技，在智能机器人\n\n行业也取得巨大成就。\n\n各大高校相继开设人工\n\n智能、大数据等专业\n\n（复旦大学今年大数据\n\n专业成了6周年）\n\nAI模型、智能体训练行\n\n业一片蓝海，我们公司\n\n着力打造AI人工智能训\n\n练师基地\n\n国家十四五以及未来30年\n\n规划大力发展人工智能版\n\n块，并成立国家数据进行\n\n管理\n\n人社部公布的创新型工\n\n作岗位13个，其中有6\n\n个都是关于人工智能板\n\n块\n\n据艾瑞咨询调查数据显示 ，2019年\n\n国内AI模型智能体训练市场规模为\n\n300.9亿元 ，根据需求方与供应方\n\n营收增长情况推算 ，2025年国内市\n\n场规模突破1000亿元 ，AI大模型及\n\n职能体训练行业市场前景十分广阔\n\n国家数据局建立7个国家级的\n\n数据处理基地以及数据流转基\n\n地，为人工智能大模型的发展\n\n保驾护航\n\n服务保障\nS E R V I C E  G U A R A N T E E\n\n待甲方收到数据方验收合格的数据结算\n\n后，根据合作伙伴有效数据量，甲方于\n\n次月底25-30号向乙方结算数据项目款\n\n项\n\n结算保障\n\n因AI大模型训练品种繁多 ，价格不一。为保障\n\n客户的利益 ，在合同中保障客户的结算单价，\n\n语音150-260元左右/小时，图片0.04-0.15元\n\n左右，具体价格以实际发包为准。（备注 ：价\n\n格是和处理难易程度是成正比的）\n\n单价保障\n\n公司会安排专门的质检人员协助质检并进行\n\n相关培训 ，从而快速高效的完成数据，提\n\n升产值。\n\n售后保障\n\n合作6个月后方可申请AI大模型训练\n\n师专项技能证的考试。\n\n行业保障\n\n数据保障\n\n技术保障\n有专业的指导老师一对一进行技术指\n\n导和相关的培训学习。\n\n公司确保客户的资源充足 ，在合同\n\n有效期中合作伙伴会有充足的数据可\n\n以去做。\n\n平台保障', 'parent-6f511b65-3530-47a9-b9d9-b2692f4a23c9', '2026-08-06 14:41:39', 10, NULL, 'PARENT', '正文', NULL, 600, '00fb13d23094d283c19a38fcc63f2c53269d7ac5787c27e0c4f61296bc48b2eb', 'DISABLED', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254958843731972, 13, 4, '国家政策保障\n\n公司会提供向合作伙伴提供数据平台和充足的数\n\n据进行操作 （如百度、阿里、腾讯、京东、海天\n\n瑞声等各大平台数据并协助进行对接）\n\n十四五、十五五规划明确提到国家将\n\n大力扶持人工智能板块。各地数据局\n\n也提供相应扶持政策。\n\nChengdu Yuncai Technology Co., Ltd\n\n成都云裁科技有限公司', 'parent-24c1a83f-8227-4944-bf34-d21603d683fe', '2026-08-06 14:41:39', 10, NULL, 'PARENT', '正文', NULL, 56, '3fd4101e1bf2611ece1a09254a8b91ece8ecd399633fab765b0f21a571d218da', 'DISABLED', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247809, 13, 1, 'AI era large model intelligent agent training project\n\n目 录\n\nP R O J E C T  I N T R O D U C T I O N\n\n4\n\n人工智能\n\n人工智能（Artificial Intelligence，简称AI）是指由计算机系\n\n统所表现出的智能行为。它是一门研究、开发用于模拟、延伸和\n\n扩展人的智能的理论、方法、技术及应用系统的科学。AI的目标\n\n是创建能够执行通常需要人类智能的任务的软件或机器，这些任\n\n务包括但不限于学习、推理、解决问题、理解语言、识别图像等。\n\n具体来说，人工智能涵盖了多种技术和算法，如深度学习、机器\n\n学习、计算机视觉、自然语言处理等。这些技术使得机器能够处\n\n理和理解大量的数据，从而模拟人类的智能行为。\n\n例如，通过自然语言处理技术，\n计算机可以理解和生成人类语\n言例如，通过自然语言处理技\n术，计算机可以理解和生成人\n类语言。', '690115e1-75c5-4ca5-bf67-3f0b2d5e914f', '2026-08-06 14:41:39', 10, 2085254958843731969, 'CHILD', '正文', NULL, 140, 'b7d3c5b7366d6cdf5705ad402766770f72b062ad40910080a300c5be90e0f74b', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247810, 13, 2, '例如通过计算机视觉技术，\n机器可以识别图像中的物\n体、人脸 、文字等信息，\n应用于安防监控、自动驾驶\n等领域。\n\n5\n\n.\n\n什么是模型智能体训练\n大模型智能体训练是指使用大规模数据和\n\n强大的计算能力来训练具有海量参数的复\n\n杂人工智能模型和智能体。 这些模型和智\n\n能体通过深度神经网络架构进行学习与训\n\n练，具备处理多种复杂任务的能力，而不\n\n仅仅局限于单一任务。\n\n模型、智能体训练\n数据收集 -> 预训练 -> 微调\n\n数据来源\n系 统 的 日 志 收 集 / 互 联 网 数 据 收 集\n\n/app 移动端数据收集/数据服务机构\n\n进行合作 。\n\n质量标准\n\n对于人眼所见的图像而言 ，计算机所\n\n见的图像只是一堆枯燥的数字 。 图像\n\n标注就是根据需求将这一堆数字划分\n\n区 域 ，让计算机在划分出来的区域中\n\n找寻数字的规律 。多音字标注的质量\n\n标准就是标注一个字的全部读音 ，这\n\n就需要借助字典等专业性工具进行检\n\n验 。', '57b07158-d84b-4b77-86d1-a0108af89206', '2026-08-06 14:41:39', 10, 2085254958843731969, 'CHILD', '正文', NULL, 141, '6ce235daa3e84c260ed3d70437b762a81255e10a4e3377efd0b35fd19bc16acf', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247811, 13, 3, '有多少人工就有多少智能\n数据处理的量级与质量直接关系到机\n\n器的智能程度\n\n6\n\nAI模型、智能体训练行业优势\n\n符合产业发展方向，\n\n紧贴国家政策。\n\n安全落地、无人脉风\n\n险。\n\n副业刚需、市场刚需、\n\n时间自由。\n\n不受大环境的影响。\n\n自主创业，解决就\n\n业人口问题。\n轻资产、投资小、\n\n风险低。\n\n无需跑市场，在家即\n\n可创业。\n\n按劳分配，多劳多得，\n\n少劳少得。\n\n业务稳定、无需维护\n\n社会关系。\n\n一站式服务，无需高\n\n学历高技术，包教包\n\n会。\n\n7\n\n1：图片处理(地图导航无人\n\n驾驶医学刷脸支付 )\n\n2：语音转写(智能机器人/语\n\n音指令/通讯/电影/会议 )\n\n3：文本转写(教育行业/零售\n\n/广告/金融 )\n\n4：视频处理(娱乐/家具/物\n\n联网/无人驾驶 )\n\n5：游戏处理(游戏对话 游戏\n\n人物 游戏地图 )\n\n6：影视处理(语音对白字幕\n\n标志物 )\n\n8', '0f9d2144-44b8-4f45-809c-5aa11e0f11cd', '2026-08-06 14:41:39', 10, 2085254958843731969, 'CHILD', '正文', NULL, 134, '420449eacf273bd2885ee9be07286b0f06fe7fd5867c1c3e2e0de0d7eadc1fe1', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247812, 13, 4, '语音转写，就是把听到的语音内容以文字的形式翻\n译出来就可以，语音数据的应用场景比如：天猫精\n灵小艾同学地图导航手机语音APP等，语音转写价\n格是150-260元左右，新手通过公司5-7天培训就\n可以上手做语音转写，操作很简单，熟能生巧的人\n产值在7000—12000左右。\n\n图片处理 ，根据项目的要求需要把图片中道路上的\n行人/车辆 /车道线/红绿灯等框出来 ，一般是4-8\n分一个框每张图片中的要求拉框目标内容都不同，\n图片处理主要是应用于无人驾驶让汽车的传感器和\n雷达来识别障碍物，还有地图、导航、人脸识别等。\n\n9\n\n智能交通 智能零售 智能医疗 智能安防\n通过AI大模型训练，可以\n确定商品类别、品牌、颜\n色等属性信息，实现自动\n化 识别和分类。此外，还\n可以对零售场 所的监控视\n频进行训练，识别和跟踪\n异常行为、窃盗行为和欺\n诈行为。', '1e4ffe90-36f7-4c47-a4e6-9af2e37a007b', '2026-08-06 14:41:39', 10, 2085254958843731969, 'CHILD', '正文', NULL, 125, '0e73b9b4958595d40e1687f3fc922cf06b4e74f9b6ff0e573329463c2f26f2bf', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247813, 13, 5, '医疗影像处理是对医疗影像\n进行区域及分类处理，多应\n用于辅助临床诊断，人工智\n能通过学习大量的医疗影像\nAI大模型训练数据集，将\n会更好的辅助医生诊断。\n\n智能安防是人工智能与信息\n技术结合的关键领域，AI大\n模型训练提供了一个关键的\n训练数据集，通过对图像、\n视频和音频等数据进行处理，\n标明其中的目标对象、动作\n行为等信息，以供系统算法\n学习和训练。', '300af29b-4861-45ee-97a0-db9fe2de445f', '2026-08-06 14:41:39', 10, 2085254958843731969, 'CHILD', '正文', NULL, 59, '91feb3d5f7de8bfe32a71576504455ae7d0760ed8e6d0c3c9e33f9cc2e3786d7', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247814, 13, 6, '无人驾驶正在慢慢地走进人\n们的生活，想要让汽车本身\n的算法处理更多、更复杂的\n场景，背后就需要有 海量的\n真实道路数据做支撑，而这 \n就需要依靠AI大模型训练。\n\nB R I E F  H I S T O R Y  O F  E N T E R P R I S E  D E V E L O P M E N T\n\n企业荣誉\n\n公 司 自 成 立 以 来 ，取 得 了 一 系 列 的 荣 誉 和 成 绩 ，得 到 了 广 \n大 合 作 伙 伴 以 及 政 府 机 关 和 行 业 的 认 可 和 认 证。\n\n部分合作伙伴展示\n\nP R O J E C T  O P E R A T I O N  M O D E\n\n大模型、智能体训练项目（语音转写）盈利参考分析\n\n转换时长 单价 一天收益（元） 一个月收益（元）\n\n1小时 200 200x1=200 200x30=6000\n\n1.5小时 200 200x1.5=300 300x30=9000', '582e3813-99da-42fe-bf85-0c5f8ab7d4b4', '2026-08-06 14:41:39', 10, 2085254958843731970, 'CHILD', '正文', NULL, 141, 'e7d4fad66758a09e2916757ac26cd267674e5812bec47edc9cefc67471caf938', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247815, 13, 7, '2小时 200 200x2=400 400x30=12000\n\n语音转写：价格是150-260左右每小时，具体价格以实际发包为准。(备注：价格是和转写难\n易程度是成正比的)，正常一天工作8小时正常可以转换1-2小时左右数据。\n\n（注:以下表格按照语音转写均价200/小时，5人团标准计算具体以实际为准）\n\n场地租金 1500-2000/月 一般商住楼或者小区套二套三就行，如果在小区可以中午管饭，员工的留存率\n和积极性会更高。\n\n办公桌椅 500 简单办公卡位75-100一套。\n\n场地布置 500 网络/饮水机/垃圾桶等。\n\n电脑5台 5000 网上二手电脑或者网吧处理800-1200/台。\n\n提供协助合作伙伴的团队招募成立、培训、培养专业AI大模型训练师，集一体的一站式孵化扶持平台。\n源源不断的资源供应。每月25-30号结算验收合格的数据\n\n技术培训服务费，第一年\n2.6万/5人费用，第二年\n百分之二十收取5200一\n年\n\n。', '988f2d52-26c6-48c0-8c0d-69cc5dd2fb20', '2026-08-06 14:41:39', 10, 2085254958843731970, 'CHILD', '正文', NULL, 140, 'b34d94440e6daf51cb2cc5eea8ebee5d3a8fb5effe10f1327e485e72e36cc207', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247816, 13, 8, '前期预计投入 4500+500+500+5000+26000=36500\n\nAI模型智能体训练项目（语音转写）盈利参考分析\n\n平均日产值 个人月产值\n（26天）\n\n语音单价\n200/h\n\n底薪 全勤 绩效奖金 提成10元/h 5人工资支出 5人余利润\n\n1.5h 39h 39*200*5 3000 200 200 390 18950 20050\n\n1.8h 46.8h 46.8*200*5 3000 200 300 468 19840 26960\n\n2h 52h 52*200*5 3000 200 500 520 21100 30900\n\n（注:以下表格按照语音转写均价200/小时，10人团标准计算具体以实际为准）\n\n场地租金 1500-2500/月 一般商住楼或者小区套二套三就行，如果在小区可以中午管饭，员工的留存率\n和积极性会更高\n\n办公桌椅 1000 简单办公卡位75-100一套\n\n场地布置 1000 网络/饮水机/垃圾桶等', '86f0ec93-3b1d-4a4a-b9b4-66ed42bcdf43', '2026-08-06 14:41:39', 10, 2085254958843731970, 'CHILD', '正文', NULL, 141, 'ed174bf73c89b0febce67ddf54085651457738ac32436b83b2d5129d1b7975dc', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247817, 13, 9, '电脑10台 10000 网上二手电脑或者网吧处理800-1200/台\n\n提供协助合作伙伴的团队招募成立、培训、培养专业AI大模型训练师，集一体的一站式孵化扶持平台。\n源源不断的资源供应。每月25-30号结算验收合格的数据\n\n技术培训服务费，第一年\n3.2万/10人费用，第二年\n百分之二十收取6400一\n年\n\n。\n\n前期预计投入 5000+1000+1000+10000+32000=49000\n\n平均日产值 个人月产值\n（26天）\n\n语音单价\n200/h\n\n底薪 全勤 绩效奖金 提成10元/h 10人工资支出 10人余利润\n\n1.5h 39h 39*200*10 3000 200 200 390 37900 40100\n\n1.8h 46.8h 46.8*200*10 3000 200 300 468 39680 53920\n\n2h 52h 52*200*10 3000 200 500 520 42200 61800', '54370b78-655d-4cdc-ba68-3d9783e074eb', '2026-08-06 14:41:39', 10, 2085254958843731970, 'CHILD', '正文', NULL, 138, 'e5bfbdc7accbea104db84b54bb21719e4e05ff586f1e0891602a0ff6ee7ab9de', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247818, 13, 10, '（注:以下表格按照语音转写均价200/小时，20人团标准计算具体以实际为准）\n\n场地租金 2000-3000/月 一般商住楼或者小区套二套三就行，如果在小区可以中午管饭，员工的留存率\n和积极性会更高', '29de6c72-0c00-416b-81c0-a7c335ad5816', '2026-08-06 14:41:39', 10, 2085254958843731970, 'CHILD', '正文', NULL, 33, '30bf0bb47c11fd5a44d508ef67a0a043e02639ff76b9e90230ccaf0317908378', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247819, 13, 11, '办公桌椅 2000 简单办公卡位75-100一套\n\n场地布置 2000 网络/饮水机/垃圾桶等\n\n电脑20台 20000 网上二手电脑或者网吧处理800-1200/台\n\n提供协助合作伙伴的团队招募成立、培训、培养专业AI大模型训练师，集一体的一站式孵化扶持平台。\n源源不断的资源供应。每月25-30号结算验收合格的数据\n\n技术培训服务费，第一年\n6万/20人费用，第二年\n百分之二十收取1.2万一\n年\n\n。\n\n前期预计投入 8000+2000+2000+20000+60000=92000\n\n平均日产值 个人月产值\n（26天）\n\n语音单价200/h 底薪 全勤 绩效奖金 提成10元/h 20人工资支出 20人余利润\n\n1.5h 39h 39*200*20 3000 200 200 390 75800 80200\n\n1.8h 46.8h 46.8*200*20 3000 200 300 468 79360 107840', '30490458-eb33-4752-8c85-5d27728938db', '2026-08-06 14:41:39', 10, 2085254958843731971, 'CHILD', '正文', NULL, 137, '6d6b0da21e0a477521f7396a4c94d5ecce9f61ebcec772cb4718b577cc104090', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247820, 13, 12, '2h 52h 52*200*20 3000 200 500 520 84400 123600\n\nG O V E R N M E N T  A N D  E N T E R P R I S E  S U P P O R T ,  B L U E  \nO C E A N  S T R A T E G Y\n\n为加快人工智能发展、抢占工业、技术发展先机 ，国\n\n家政企扶持人工智能产业 ，以行业三巨头（百度、阿\n\n里、腾讯）为首 ，重金打造专业AI大模型训练产业群 ，\n\n助力打造一批专业的AI模型和智能体训练师。\n\n政企扶持，蓝海战略\n\n国家十四五规划大力发展人工智能行业，预计在未来5-10\n\n年投放30万亿。\n\n23年至24年成立国家数据局，对该项目进行国家级的管理，\n\n并且在成都、长沙、大同、保定、杭州等7个城市成立国家级\n\n数据处理基地，以及数据交易中心\n\n百度在海东、海口、太原建立AI大模型训练基地。目前有\n\n10000左右AI大模型训练员办公,计划在5年内扩展到5万人！', '2514a9a8-0761-4eca-bbbd-e5d3c2745222', '2026-08-06 14:41:39', 10, 2085254958843731971, 'CHILD', '正文', NULL, 147, 'dd80fbe7e07ce7182114b684511e6f0880148119af87523fe192d8e7fbcea880', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247821, 13, 13, '阿里、腾讯、华为等都分别有再贵州建立大数据中心。\n\n联合大于100家本地数据化工厂服务商抱团创业 ，形成AI\n\n人工智能行业中的“虹吸效应”\n\n发展前景\nD E V E L O P M E N T  P R O S P E C T S\n\n互联网三巨头（百度、腾讯、阿里）都相继\n\n建立自己的AI大模型训练基地和大数据处\n\n理中心，新成立的宇数科技，在智能机器人\n\n行业也取得巨大成就。\n\n各大高校相继开设人工\n\n智能、大数据等专业\n\n（复旦大学今年大数据\n\n专业成了6周年）\n\nAI模型、智能体训练行\n\n业一片蓝海，我们公司\n\n着力打造AI人工智能训\n\n练师基地\n\n国家十四五以及未来30年\n\n规划大力发展人工智能版\n\n块，并成立国家数据进行\n\n管理\n\n人社部公布的创新型工\n\n作岗位13个，其中有6\n\n个都是关于人工智能板\n\n块\n\n据艾瑞咨询调查数据显示 ，2019年\n\n国内AI模型智能体训练市场规模为\n\n300.9亿元 ，根据需求方与供应方\n\n营收增长情况推算 ，2025年国内市', 'ced0ea0e-bc9d-4c22-b0da-9e0c29d7d7e5', '2026-08-06 14:41:39', 10, 2085254958843731971, 'CHILD', '正文', NULL, 148, '0344aa0cb1c78ef0c05a8463aa13e224d8fbb00561f8989f0fc89e743707db85', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247822, 13, 14, '场规模突破1000亿元 ，AI大模型及\n\n职能体训练行业市场前景十分广阔\n\n国家数据局建立7个国家级的\n\n数据处理基地以及数据流转基\n\n地，为人工智能大模型的发展\n\n保驾护航\n\n服务保障\nS E R V I C E  G U A R A N T E E\n\n待甲方收到数据方验收合格的数据结算\n\n后，根据合作伙伴有效数据量，甲方于\n\n次月底25-30号向乙方结算数据项目款\n\n项\n\n结算保障\n\n因AI大模型训练品种繁多 ，价格不一。为保障\n\n客户的利益 ，在合同中保障客户的结算单价，\n\n语音150-260元左右/小时，图片0.04-0.15元\n\n左右，具体价格以实际发包为准。（备注 ：价\n\n格是和处理难易程度是成正比的）\n\n单价保障\n\n公司会安排专门的质检人员协助质检并进行\n\n相关培训 ，从而快速高效的完成数据，提\n\n升产值。\n\n售后保障\n\n合作6个月后方可申请AI大模型训练\n\n师专项技能证的考试。\n\n行业保障\n\n数据保障\n\n技术保障\n有专业的指导老师一对一进行技术指\n\n导和相关的培训学习。', 'e12ac74c-4e42-499d-8d0c-86d0c13f4b9f', '2026-08-06 14:41:39', 10, 2085254958843731971, 'CHILD', '正文', NULL, 150, '5c178c99c26f98934dfc281b16d47f24af36356b9e065634b86d783e04af26b6', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247823, 13, 15, '公司确保客户的资源充足 ，在合同\n\n有效期中合作伙伴会有充足的数据可\n\n以去做。\n\n平台保障', 'ef57c45b-baad-4d5d-a5dc-4e392cbb86b8', '2026-08-06 14:41:39', 10, 2085254958843731971, 'CHILD', '正文', NULL, 16, 'd8c99c7038161b7c2faa17019b6f17ffc96a95c91c2c92c55bc5ceb8356bf3c8', 'READY', 'DISABLED');
INSERT INTO `knowledge_chunk` VALUES (2085254959200247824, 13, 16, '国家政策保障\n\n公司会提供向合作伙伴提供数据平台和充足的数\n\n据进行操作 （如百度、阿里、腾讯、京东、海天\n\n瑞声等各大平台数据并协助进行对接）\n\n十四五、十五五规划明确提到国家将\n\n大力扶持人工智能板块。各地数据局\n\n也提供相应扶持政策。\n\nChengdu Yuncai Technology Co., Ltd\n\n成都云裁科技有限公司', 'c0ef953a-3aef-440e-9361-f33276ba7d71', '2026-08-06 14:41:39', 10, 2085254958843731972, 'CHILD', '正文', NULL, 56, '3fd4101e1bf2611ece1a09254a8b91ece8ecd399633fab765b0f21a571d218da', 'READY', 'DISABLED');

-- ----------------------------
-- Table structure for knowledge_document
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_document`;
CREATE TABLE `knowledge_document`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `source_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `file_path` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `document_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `version_no` int NOT NULL DEFAULT 1,
  `chunk_count` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `language` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OTHER',
  `language_confirmed` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_knowledge_document_tenant`(`tenant_id` ASC, `document_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_document
-- ----------------------------
INSERT INTO `knowledge_document` VALUES (6, 4, '1-HR用人申请测试数据.xlsx', 'XLSX', 'data\\uploads\\6-1-HR用人申请测试数据.xlsx', 'DELETED', 5, 13, '2026-07-24 16:03:37', '2026-08-05 14:37:00', 'ZH', 1);
INSERT INTO `knowledge_document` VALUES (7, 4, '1-多酚水凝胶综述_正文提取.txt', 'TXT', 'data\\uploads\\7-1-多酚水凝胶综述_正文提取.txt', 'DELETED', 7, 354, '2026-07-24 17:09:33', '2026-08-05 14:36:59', 'EN', 1);
INSERT INTO `knowledge_document` VALUES (8, 4, 'HR用人申请工作流说明.md', 'MD', 'data\\uploads\\8-HR用人申请工作流说明.md', 'DELETED', 7, 12, '2026-07-24 17:10:37', '2026-08-05 14:36:57', 'ZH', 1);
INSERT INTO `knowledge_document` VALUES (12, 4, '附件简历-雷胜勇-Java开发工程师.pdf', 'PDF', 'data\\uploads\\12-附件简历-雷胜勇-Java开发工程师.pdf', 'DELETED', 3, 10, '2026-08-05 14:37:43', '2026-08-05 14:42:50', 'OTHER', 1);
INSERT INTO `knowledge_document` VALUES (13, 4, 'AI模型智能体训练项目介绍pdf(118).pdf', 'PDF', 'data\\uploads\\13-AI模型智能体训练项目介绍pdf(118).pdf', 'INDEXED', 2, 16, '2026-08-06 14:41:36', '2026-08-06 14:41:40', 'OTHER', 1);

-- ----------------------------
-- Table structure for knowledge_document_index_state
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_document_index_state`;
CREATE TABLE `knowledge_document_index_state`  (
  `document_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `profile_id` bigint NOT NULL,
  `active_generation_id` bigint NULL DEFAULT NULL,
  `index_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `last_error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `indexed_at` datetime NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`document_id`) USING BTREE,
  INDEX `idx_document_index_state_tenant`(`tenant_id` ASC, `index_status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_document_index_state
-- ----------------------------
INSERT INTO `knowledge_document_index_state` VALUES (13, 4, 2, 10, 'INDEXED', NULL, '2026-08-06 14:41:40', '2026-08-06 14:41:40');

-- ----------------------------
-- Table structure for knowledge_document_tag
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_document_tag`;
CREATE TABLE `knowledge_document_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `document_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_knowledge_document_tag`(`tenant_id` ASC, `document_id` ASC, `tag_id` ASC) USING BTREE,
  INDEX `idx_document_tag_document`(`tenant_id` ASC, `document_id` ASC) USING BTREE,
  INDEX `idx_document_tag_tag`(`tenant_id` ASC, `tag_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_document_tag
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_document_version
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_document_version`;
CREATE TABLE `knowledge_document_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `source_id` bigint NOT NULL,
  `document_id` bigint NULL DEFAULT NULL,
  `external_document_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version_no` int NOT NULL,
  `content_fingerprint` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `normalized_length` int NOT NULL DEFAULT 0,
  `version_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DISCOVERED',
  `permission_snapshot_json` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_document_version_fingerprint`(`tenant_id` ASC, `source_id` ASC, `external_document_id` ASC, `content_fingerprint` ASC) USING BTREE,
  INDEX `idx_document_version_status`(`tenant_id` ASC, `source_id` ASC, `version_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_document_version
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_index_generation
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_index_generation`;
CREATE TABLE `knowledge_index_generation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `profile_id` bigint NOT NULL,
  `collection_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `generation_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `total_documents` int NOT NULL DEFAULT 0,
  `total_chunks` int NOT NULL DEFAULT 0,
  `indexed_chunks` int NOT NULL DEFAULT 0,
  `failure_count` int NOT NULL DEFAULT 0,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `completed_at` datetime NULL DEFAULT NULL,
  `activated_at` datetime NULL DEFAULT NULL,
  `embedding_fingerprint` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_knowledge_generation_collection`(`collection_name` ASC) USING BTREE,
  INDEX `idx_knowledge_generation_tenant`(`tenant_id` ASC, `generation_status` ASC, `started_at` ASC) USING BTREE,
  INDEX `idx_generation_profile_fingerprint`(`tenant_id` ASC, `profile_id` ASC, `embedding_fingerprint` ASC, `generation_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_index_generation
-- ----------------------------
INSERT INTO `knowledge_index_generation` VALUES (1, 4, 1, 'agent_studio_rag_t4_p1_v1', 'RETIRED', 0, 0, 0, 21, 'RAG 模型不存在或已停用：bge-m3', '2026-08-03 17:31:14', '2026-08-03 17:59:52', '2026-08-03 17:31:14', NULL);
INSERT INTO `knowledge_index_generation` VALUES (2, 4, 2, 'agent_studio_rag_t4_p2_v1', 'RETIRED', 8, 74663, 74663, 0, NULL, '2026-08-04 10:40:50', '2026-08-04 15:17:52', '2026-08-04 10:40:50', NULL);
INSERT INTO `knowledge_index_generation` VALUES (3, 4, 3, 'agent_studio_rag_t4_p3_v1', 'RETIRED', 3, 379, 379, 0, NULL, '2026-08-04 10:43:52', '2026-08-04 10:46:29', '2026-08-04 10:43:52', NULL);
INSERT INTO `knowledge_index_generation` VALUES (4, 4, 4, 'agent_studio_rag_t4_p4_v1', 'RETIRED', 2, 19, 19, 5, 'dev.ai4j.openai4j.OpenAiHttpException: {\"error\":{\"message\":\"Free quota exhausted. To continue accessing the model on a paid basis, please add funds or disable the \\\"use free tier only\\\" mode in the management console.\",\"type\":\"AllocationQuota.FreeTierOnly\",\"param\":null,\"code\":\"AllocationQuota.FreeTierOnly\"},\"id\":\"958a96a3-edce-970d-b975-3d913eb4e9ed\",\"request_id\":\"958a96a3-edce-970d-b975-3d913eb4e9ed\"}', '2026-08-04 10:52:49', '2026-08-04 11:20:01', '2026-08-04 10:52:49', NULL);
INSERT INTO `knowledge_index_generation` VALUES (5, 4, 5, 'agent_studio_rag_t4_p5_v1', 'RETIRED', 0, 0, 0, 2, 'RAG 模型未配置接口地址：jina-embeddings-v3', '2026-08-04 13:19:01', '2026-08-04 13:19:33', '2026-08-04 13:19:01', NULL);
INSERT INTO `knowledge_index_generation` VALUES (6, 4, 2, 'agent_studio_rag_t4_p2_v1_f50c6bae46523c1ca8b67f3cae9edfc1e71efe069645e252ef474fca2257ed960_r1785902075313', 'FAILED', 0, 0, 0, 0, '500 Internal Server Error: \"{\"status\":{\"error\":\"Service internal error: Gridstore error: 系统找不到指定的路径。 (os error 3)\"},\"time\":0.0757614}\"', '2026-08-05 11:54:35', '2026-08-05 11:54:36', NULL, '50c6bae46523c1ca8b67f3cae9edfc1e71efe069645e252ef474fca2257ed960');
INSERT INTO `knowledge_index_generation` VALUES (7, 4, 2, 'agent_studio_rag_t4_p2_v1_f50c6bae46523c1ca8b67f3cae9edfc1e71efe069645e252ef474fca2257ed960_r1785902106027', 'FAILED', 0, 0, 0, 0, '500 Internal Server Error: \"{\"status\":{\"error\":\"Service internal error: Gridstore error: 系统找不到指定的路径。 (os error 3)\"},\"time\":0.0232431}\"', '2026-08-05 11:55:06', '2026-08-05 11:55:06', NULL, '50c6bae46523c1ca8b67f3cae9edfc1e71efe069645e252ef474fca2257ed960');
INSERT INTO `knowledge_index_generation` VALUES (8, 4, 2, 'agent_studio_rag_t4_p2_v1_fd8973f5f6d2d855f_rmsfnxxmy', 'RETIRED', 3, 379, 379, 0, NULL, '2026-08-05 13:45:23', '2026-08-05 13:45:31', '2026-08-05 13:45:31', 'd8973f5f6d2d855f8ed2f5060771eccce4cc9104efedf73667aace7ed5255dd7');
INSERT INTO `knowledge_index_generation` VALUES (9, 4, 2, 'agent_studio_rag_t4_p2_v1_fd8973f5f6d2d855f_rmsfpsil7', 'RETIRED', 1, 10, 10, 0, NULL, '2026-08-05 14:37:09', '2026-08-05 14:37:45', '2026-08-05 14:37:11', 'd8973f5f6d2d855f8ed2f5060771eccce4cc9104efedf73667aace7ed5255dd7');
INSERT INTO `knowledge_index_generation` VALUES (10, 4, 2, 'agent_studio_rag_t4_p2_v1_fc3461575f890e38f_rmsfpvyet', 'ACTIVE', 2, 26, 26, 0, NULL, '2026-08-05 14:39:49', '2026-08-06 14:41:40', '2026-08-05 14:39:53', 'c3461575f890e38f87ee88a78bb1ad0c388cf3f4baa83e65ce61e9bd3fb87527');

-- ----------------------------
-- Table structure for knowledge_quality_finding
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_quality_finding`;
CREATE TABLE `knowledge_quality_finding`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `knowledge_base_id` bigint NOT NULL,
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `finding_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `severity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `finding_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OPEN',
  `evidence_summary` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `remediation` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `assignee` bigint NULL DEFAULT NULL,
  `audit_note` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_quality_finding_queue`(`tenant_id` ASC, `knowledge_base_id` ASC, `finding_status` ASC, `severity` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_quality_finding
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_source
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_source`;
CREATE TABLE `knowledge_source`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `knowledge_base_id` bigint NOT NULL,
  `source_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `source_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `display_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `connector_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `credential_reference` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CREATED',
  `sync_cursor` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `freshness_policy_json` json NULL,
  `permission_policy_json` json NULL,
  `last_synced_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_knowledge_source_tenant_code`(`tenant_id` ASC, `source_code` ASC) USING BTREE,
  INDEX `idx_knowledge_source_base_status`(`tenant_id` ASC, `knowledge_base_id` ASC, `source_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_source
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_sync_run
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_sync_run`;
CREATE TABLE `knowledge_sync_run`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `source_id` bigint NOT NULL,
  `run_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'QUEUED',
  `cursor_before` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `cursor_after` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `discovered_count` int NOT NULL DEFAULT 0,
  `changed_count` int NOT NULL DEFAULT 0,
  `deleted_count` int NOT NULL DEFAULT 0,
  `skipped_count` int NOT NULL DEFAULT 0,
  `failed_count` int NOT NULL DEFAULT 0,
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `error_message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `started_at` datetime NULL DEFAULT NULL,
  `completed_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_knowledge_sync_source_status`(`tenant_id` ASC, `source_id` ASC, `run_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_sync_run
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_tag
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_tag`;
CREATE TABLE `knowledge_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `tag_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `tag_color` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_knowledge_tag`(`tenant_id` ASC, `tag_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_tag
-- ----------------------------

-- ----------------------------
-- Table structure for marketplace_installation
-- ----------------------------
DROP TABLE IF EXISTS `marketplace_installation`;
CREATE TABLE `marketplace_installation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `draft_revision_id` bigint NOT NULL,
  `marketplace_item_id` bigint NOT NULL,
  `template_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `legacy_source_id` bigint NULL DEFAULT NULL,
  `manifest_fingerprint` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `installation_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT_CREATED',
  `installed_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `installed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_marketplace_installation_app`(`tenant_id` ASC, `application_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_marketplace_legacy_source`(`tenant_id` ASC, `source_type` ASC, `legacy_source_id` ASC) USING BTREE,
  INDEX `idx_marketplace_install_item`(`tenant_id` ASC, `marketplace_item_id` ASC, `template_version` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of marketplace_installation
-- ----------------------------
INSERT INTO `marketplace_installation` VALUES (1, 4, 1025, 1026, 22, 'application-template-v1', 'MARKETPLACE', NULL, 'f884711e39334d43a761b639917bf642aeb11be83fc1c735746a8fe3237a7ce5', 'DRAFT_CREATED', 'raysy', '2026-08-06 09:34:36');
INSERT INTO `marketplace_installation` VALUES (2, 4, 1027, 1029, 22, 'application-template-v1', 'MARKETPLACE', NULL, 'f884711e39334d43a761b639917bf642aeb11be83fc1c735746a8fe3237a7ce5', 'DRAFT_CREATED', 'raysy', '2026-08-06 10:17:40');
INSERT INTO `marketplace_installation` VALUES (3, 4, 1028, 1030, 22, 'application-template-v1', 'MARKETPLACE', NULL, 'f884711e39334d43a761b639917bf642aeb11be83fc1c735746a8fe3237a7ce5', 'DRAFT_CREATED', 'raysy', '2026-08-06 10:17:51');

-- ----------------------------
-- Table structure for marketplace_item
-- ----------------------------
DROP TABLE IF EXISTS `marketplace_item`;
CREATE TABLE `marketplace_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `item_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `publisher` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `item_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'READY',
  `manifest_json` json NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_marketplace_status`(`item_status` ASC, `item_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of marketplace_item
-- ----------------------------
INSERT INTO `marketplace_item` VALUES (22, '基础对话应用模板', 'APPLICATION', 'PLATFORM', 'READY', '{\"graphJson\": {\"edges\": [{\"edgeId\": \"start-end\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"id\": \"start\", \"name\": \"开始\", \"type\": \"START\", \"config\": {}}, {\"id\": \"end\", \"name\": \"结束\", \"type\": \"END\", \"config\": {}}], \"graphType\": \"APPLICATION_WORKFLOW\", \"schemaVersion\": \"1.0\"}, \"graphType\": \"APPLICATION_WORKFLOW\", \"runtimeMode\": \"CHAT\", \"dependencies\": [], \"schemaVersion\": \"application-template-v1\"}', '2026-08-06 09:33:08', '2026-08-06 09:33:08');

-- ----------------------------
-- Table structure for marketplace_migration_issue
-- ----------------------------
DROP TABLE IF EXISTS `marketplace_migration_issue`;
CREATE TABLE `marketplace_migration_issue`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `legacy_source_id` bigint NOT NULL,
  `issue_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `issue_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `issue_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OPEN',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resolved_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_marketplace_migration_issue`(`tenant_id` ASC, `source_type` ASC, `legacy_source_id` ASC, `issue_code` ASC) USING BTREE,
  INDEX `idx_marketplace_migration_status`(`tenant_id` ASC, `issue_status` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of marketplace_migration_issue
-- ----------------------------

-- ----------------------------
-- Table structure for model_call_metric
-- ----------------------------
DROP TABLE IF EXISTS `model_call_metric`;
CREATE TABLE `model_call_metric`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `agent_id` bigint NULL DEFAULT NULL,
  `model_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `call_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `input_tokens` bigint NULL DEFAULT NULL,
  `output_tokens` bigint NULL DEFAULT NULL,
  `latency_ms` bigint NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `estimated_cost` decimal(18, 8) NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_model_metric_tenant_time`(`tenant_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_model_metric_model`(`model_key` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of model_call_metric
-- ----------------------------
INSERT INTO `model_call_metric` VALUES (13, 4, NULL, 'gpt-5.5', 'ORCHESTRATION_LLM', NULL, NULL, 9344, 'SUCCESS', NULL, NULL, '2026-07-27 10:13:12');
INSERT INTO `model_call_metric` VALUES (14, 4, NULL, 'gpt-5.5', 'ORCHESTRATION_LLM', NULL, NULL, 16186, 'SUCCESS', NULL, NULL, '2026-07-27 14:22:20');
INSERT INTO `model_call_metric` VALUES (15, 4, NULL, 'gpt-5.5', 'ORCHESTRATION_LLM', NULL, NULL, 17214, 'SUCCESS', NULL, NULL, '2026-07-28 11:49:48');
INSERT INTO `model_call_metric` VALUES (21, 4, NULL, 'gpt-5.5', 'ORCHESTRATION_LLM', NULL, NULL, 50017, 'SUCCESS', NULL, NULL, '2026-07-29 09:39:40');
INSERT INTO `model_call_metric` VALUES (23, 4, NULL, 'gpt-5.5', 'ORCHESTRATION_LLM', NULL, NULL, 33053, 'SUCCESS', NULL, NULL, '2026-07-29 09:44:35');
INSERT INTO `model_call_metric` VALUES (26, 4, NULL, 'gpt-5.5', 'ORCHESTRATION_LLM', NULL, NULL, 3259, 'SUCCESS', NULL, NULL, '2026-07-29 11:01:46');
INSERT INTO `model_call_metric` VALUES (27, 4, NULL, 'gpt-5.5', 'ORCHESTRATION_LLM', NULL, NULL, 11169, 'SUCCESS', NULL, NULL, '2026-07-29 11:17:13');
INSERT INTO `model_call_metric` VALUES (28, 4, NULL, 'gpt-5.5', 'ORCHESTRATION_LLM', NULL, NULL, 2225, 'SUCCESS', NULL, NULL, '2026-07-29 11:19:56');
INSERT INTO `model_call_metric` VALUES (29, 4, NULL, 'gpt-5.5', 'ORCHESTRATION_LLM', NULL, NULL, 4320, 'SUCCESS', NULL, NULL, '2026-07-29 13:05:53');

-- ----------------------------
-- Table structure for model_price
-- ----------------------------
DROP TABLE IF EXISTS `model_price`;
CREATE TABLE `model_price`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `model_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `input_price_per_1k` decimal(18, 8) NOT NULL,
  `output_price_per_1k` decimal(18, 8) NOT NULL,
  `effective_from` datetime NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_model_price`(`provider` ASC, `model_key` ASC, `effective_from` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of model_price
-- ----------------------------
INSERT INTO `model_price` VALUES (1, 'OPENAI', 'gpt-5.5', 0.03000000, 0.02000000, '2026-07-27 11:23:41', 'ACTIVE');

-- ----------------------------
-- Table structure for orchestration_app
-- ----------------------------
DROP TABLE IF EXISTS `orchestration_app`;
CREATE TABLE `orchestration_app`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `app_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `app_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `graph_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `current_revision_id` bigint NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_orchestration_app_code`(`tenant_id` ASC, `app_code` ASC) USING BTREE,
  INDEX `idx_orchestration_app_tenant_status`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1029 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orchestration_app
-- ----------------------------
INSERT INTO `orchestration_app` VALUES (1025, 4, 'app-00b726acf0c340fe91498eb262f8be27', '基础对话应用模板', 'APPLICATION_WORKFLOW', 1026, 'DRAFT', 6, '2026-08-06 09:34:36', '2026-08-06 09:34:36');
INSERT INTO `orchestration_app` VALUES (1026, 4, 'APP-8acae378182d', '验收测试应用', 'APPLICATION_WORKFLOW', 1032, 'DRAFT', 6, '2026-08-06 10:12:12', '2026-08-07 10:31:02');
INSERT INTO `orchestration_app` VALUES (1027, 4, 'app-3af7a8b357ad4687907282dd1727abaa', '基础对话应用模板', 'APPLICATION_WORKFLOW', 1029, 'DRAFT', 6, '2026-08-06 10:17:40', '2026-08-06 10:17:40');
INSERT INTO `orchestration_app` VALUES (1028, 4, 'app-7ff3bb5bda204a998f7fd30b63249f4c', '基础对话应用模板', 'APPLICATION_WORKFLOW', 1030, 'DRAFT', 6, '2026-08-06 10:17:51', '2026-08-06 10:17:51');

-- ----------------------------
-- Table structure for orchestration_draft_revision
-- ----------------------------
DROP TABLE IF EXISTS `orchestration_draft_revision`;
CREATE TABLE `orchestration_draft_revision`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `app_id` bigint NOT NULL,
  `revision_no` int NOT NULL,
  `graph_json` json NOT NULL,
  `base_version_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `change_summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_orchestration_draft_revision`(`app_id` ASC, `revision_no` ASC) USING BTREE,
  INDEX `idx_orchestration_draft_tenant`(`tenant_id` ASC, `app_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1033 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orchestration_draft_revision
-- ----------------------------
INSERT INTO `orchestration_draft_revision` VALUES (1026, 4, 1025, 1, '{\"edges\": [{\"edgeId\": \"start-end\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"id\": \"start\", \"name\": \"开始\", \"type\": \"START\", \"config\": {}}, {\"id\": \"end\", \"name\": \"结束\", \"type\": \"END\", \"config\": {}}], \"graphType\": \"APPLICATION_WORKFLOW\", \"schemaVersion\": \"1.0\"}', NULL, 'Marketplace 模板安装生成', 6, '2026-08-06 09:34:36');
INSERT INTO `orchestration_draft_revision` VALUES (1027, 4, 1026, 1, '{\"edges\": [{\"edgeId\": \"edge-start-end\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"x\": null, \"y\": null, \"title\": \"开始\", \"config\": {}, \"nodeId\": \"start\", \"nodeType\": \"START\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": null, \"y\": null, \"title\": \"结束\", \"config\": {}, \"nodeId\": \"end\", \"nodeType\": \"END\", \"inputSchema\": {}, \"outputSchema\": {}}], \"graphType\": \"APPLICATION_WORKFLOW\", \"variables\": [], \"inputSchema\": {\"type\": \"object\", \"required\": [\"request\"], \"properties\": {\"request\": {\"type\": \"string\"}}, \"description\": \"验证应用发布前的配置和质量检查流程\"}, \"outputSchema\": {\"type\": \"object\", \"properties\": {\"result\": {\"type\": \"string\", \"format\": \"text\"}}}, \"schemaVersion\": \"1.0\"}', NULL, '创建应用空白主工作流', 6, '2026-08-06 10:12:12');
INSERT INTO `orchestration_draft_revision` VALUES (1028, 4, 1026, 2, '{\"edges\": [{\"edgeId\": \"edge-1\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"user-input\"}, {\"edgeId\": \"edge-2\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"user-input\", \"targetNodeId\": \"reply\"}, {\"edgeId\": \"edge-3\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"reply\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"x\": 80.0, \"y\": 190.0, \"title\": \"开始\", \"config\": {}, \"nodeId\": \"start\", \"nodeType\": \"START\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 300.0, \"y\": 190.0, \"title\": \"接收用户消息\", \"config\": {}, \"nodeId\": \"user-input\", \"nodeType\": \"USER_INPUT\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 520.0, \"y\": 190.0, \"title\": \"返回答复\", \"config\": {\"messageTemplate\": \"已收到：{{variables.user_message}}\"}, \"nodeId\": \"reply\", \"nodeType\": \"DIRECT_REPLY\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 740.0, \"y\": 190.0, \"title\": \"结束\", \"config\": {}, \"nodeId\": \"end\", \"nodeType\": \"END\", \"inputSchema\": {}, \"outputSchema\": {}}], \"graphType\": \"APPLICATION_WORKFLOW\", \"variables\": [{\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"start\"}, {\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"user-input\"}, {\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"reply\"}], \"inputSchema\": {\"type\": \"object\", \"required\": [\"request\"], \"properties\": {\"request\": {\"type\": \"string\"}}, \"description\": \"验证应用发布前的配置和质量检查流程\"}, \"outputSchema\": {\"type\": \"object\", \"properties\": {\"result\": {\"type\": \"string\", \"format\": \"text\"}}}, \"schemaVersion\": \"1.0\"}', NULL, NULL, 6, '2026-08-06 10:15:04');
INSERT INTO `orchestration_draft_revision` VALUES (1029, 4, 1027, 1, '{\"edges\": [{\"edgeId\": \"start-end\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"id\": \"start\", \"name\": \"开始\", \"type\": \"START\", \"config\": {}}, {\"id\": \"end\", \"name\": \"结束\", \"type\": \"END\", \"config\": {}}], \"graphType\": \"APPLICATION_WORKFLOW\", \"schemaVersion\": \"1.0\"}', NULL, 'Marketplace 模板安装生成', 6, '2026-08-06 10:17:40');
INSERT INTO `orchestration_draft_revision` VALUES (1030, 4, 1028, 1, '{\"edges\": [{\"edgeId\": \"start-end\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"id\": \"start\", \"name\": \"开始\", \"type\": \"START\", \"config\": {}}, {\"id\": \"end\", \"name\": \"结束\", \"type\": \"END\", \"config\": {}}], \"graphType\": \"APPLICATION_WORKFLOW\", \"schemaVersion\": \"1.0\"}', NULL, 'Marketplace 模板安装生成', 6, '2026-08-06 10:17:51');
INSERT INTO `orchestration_draft_revision` VALUES (1031, 4, 1026, 3, '{\"edges\": [{\"edgeId\": \"edge-1\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"user-input\"}, {\"edgeId\": \"edge-2\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"user-input\", \"targetNodeId\": \"reply\"}, {\"edgeId\": \"edge-3\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"reply\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"x\": 80.0, \"y\": 190.0, \"title\": \"开始\", \"config\": {}, \"nodeId\": \"start\", \"nodeType\": \"START\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 300.0, \"y\": 190.0, \"title\": \"接收用户消息\", \"config\": {}, \"nodeId\": \"user-input\", \"nodeType\": \"USER_INPUT\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 520.0, \"y\": 190.0, \"title\": \"返回答复\", \"config\": {\"messageTemplate\": \"已收到：{{variables.user_message}}\"}, \"nodeId\": \"reply\", \"nodeType\": \"DIRECT_REPLY\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 740.0, \"y\": 190.0, \"title\": \"结束\", \"config\": {}, \"nodeId\": \"end\", \"nodeType\": \"END\", \"inputSchema\": {}, \"outputSchema\": {}}], \"graphType\": \"APPLICATION_WORKFLOW\", \"variables\": [{\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"start\"}, {\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"user-input\"}, {\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"reply\"}], \"inputSchema\": {\"type\": \"object\", \"required\": [\"request\"], \"properties\": {\"request\": {\"type\": \"string\"}}, \"description\": \"验证应用发布前的配置和质量检查流程\"}, \"outputSchema\": {\"type\": \"object\", \"properties\": {\"result\": {\"type\": \"string\", \"format\": \"text\"}}}, \"schemaVersion\": \"1.0\"}', NULL, NULL, 6, '2026-08-07 10:30:45');
INSERT INTO `orchestration_draft_revision` VALUES (1032, 4, 1026, 4, '{\"edges\": [{\"edgeId\": \"edge-1\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"user-input\"}, {\"edgeId\": \"edge-2\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"user-input\", \"targetNodeId\": \"reply\"}, {\"edgeId\": \"edge-3\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"reply\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"x\": 80.0, \"y\": 190.0, \"title\": \"开始\", \"config\": {}, \"nodeId\": \"start\", \"nodeType\": \"START\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 300.0, \"y\": 190.0, \"title\": \"接收用户消息\", \"config\": {}, \"nodeId\": \"user-input\", \"nodeType\": \"USER_INPUT\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 520.0, \"y\": 190.0, \"title\": \"返回答复\", \"config\": {\"messageTemplate\": \"已收到：{{variables.user_message}}\"}, \"nodeId\": \"reply\", \"nodeType\": \"DIRECT_REPLY\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 740.0, \"y\": 190.0, \"title\": \"结束\", \"config\": {}, \"nodeId\": \"end\", \"nodeType\": \"END\", \"inputSchema\": {}, \"outputSchema\": {}}], \"graphType\": \"APPLICATION_WORKFLOW\", \"variables\": [{\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"start\"}, {\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"user-input\"}, {\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"reply\"}], \"inputSchema\": {\"type\": \"object\", \"required\": [\"request\"], \"properties\": {\"request\": {\"type\": \"string\"}}, \"description\": \"验证应用发布前的配置和质量检查流程\"}, \"outputSchema\": {\"type\": \"object\", \"properties\": {\"result\": {\"type\": \"string\", \"format\": \"text\"}}}, \"schemaVersion\": \"1.0\"}', NULL, NULL, 6, '2026-08-07 10:31:02');

-- ----------------------------
-- Table structure for orchestration_edge
-- ----------------------------
DROP TABLE IF EXISTS `orchestration_edge`;
CREATE TABLE `orchestration_edge`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `app_id` bigint NOT NULL,
  `revision_id` bigint NOT NULL,
  `edge_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `source_node_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `source_port` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `target_node_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `target_port` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_orchestration_edge_revision`(`revision_id` ASC, `edge_id` ASC) USING BTREE,
  INDEX `idx_orchestration_edge_revision`(`revision_id` ASC, `source_node_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 68 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orchestration_edge
-- ----------------------------
INSERT INTO `orchestration_edge` VALUES (58, 4, 1026, 1027, 'edge-start-end', 'start', 'default', 'end', 'default');
INSERT INTO `orchestration_edge` VALUES (59, 4, 1026, 1028, 'edge-1', 'start', 'default', 'user-input', 'default');
INSERT INTO `orchestration_edge` VALUES (60, 4, 1026, 1028, 'edge-2', 'user-input', 'default', 'reply', 'default');
INSERT INTO `orchestration_edge` VALUES (61, 4, 1026, 1028, 'edge-3', 'reply', 'default', 'end', 'default');
INSERT INTO `orchestration_edge` VALUES (62, 4, 1026, 1031, 'edge-1', 'start', 'default', 'user-input', 'default');
INSERT INTO `orchestration_edge` VALUES (63, 4, 1026, 1031, 'edge-2', 'user-input', 'default', 'reply', 'default');
INSERT INTO `orchestration_edge` VALUES (64, 4, 1026, 1031, 'edge-3', 'reply', 'default', 'end', 'default');
INSERT INTO `orchestration_edge` VALUES (65, 4, 1026, 1032, 'edge-1', 'start', 'default', 'user-input', 'default');
INSERT INTO `orchestration_edge` VALUES (66, 4, 1026, 1032, 'edge-2', 'user-input', 'default', 'reply', 'default');
INSERT INTO `orchestration_edge` VALUES (67, 4, 1026, 1032, 'edge-3', 'reply', 'default', 'end', 'default');

-- ----------------------------
-- Table structure for orchestration_environment
-- ----------------------------
DROP TABLE IF EXISTS `orchestration_environment`;
CREATE TABLE `orchestration_environment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `app_id` bigint NOT NULL,
  `environment_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `current_version_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_orchestration_environment`(`tenant_id` ASC, `app_id` ASC, `environment_code` ASC) USING BTREE,
  INDEX `idx_orchestration_environment_version`(`current_version_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orchestration_environment
-- ----------------------------

-- ----------------------------
-- Table structure for orchestration_node
-- ----------------------------
DROP TABLE IF EXISTS `orchestration_node`;
CREATE TABLE `orchestration_node`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `app_id` bigint NOT NULL,
  `revision_id` bigint NOT NULL,
  `node_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `node_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `config_json` json NOT NULL,
  `input_schema_json` json NULL,
  `output_schema_json` json NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_orchestration_node_revision`(`revision_id` ASC, `node_id` ASC) USING BTREE,
  INDEX `idx_orchestration_node_tenant_app`(`tenant_id` ASC, `app_id` ASC, `revision_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 92 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orchestration_node
-- ----------------------------
INSERT INTO `orchestration_node` VALUES (78, 4, 1026, 1027, 'start', 'START', '开始', '{}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (79, 4, 1026, 1027, 'end', 'END', '结束', '{}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (80, 4, 1026, 1028, 'start', 'START', '开始', '{}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (81, 4, 1026, 1028, 'user-input', 'USER_INPUT', '接收用户消息', '{}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (82, 4, 1026, 1028, 'reply', 'DIRECT_REPLY', '返回答复', '{\"messageTemplate\": \"已收到：{{variables.user_message}}\"}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (83, 4, 1026, 1028, 'end', 'END', '结束', '{}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (84, 4, 1026, 1031, 'start', 'START', '开始', '{}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (85, 4, 1026, 1031, 'user-input', 'USER_INPUT', '接收用户消息', '{}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (86, 4, 1026, 1031, 'reply', 'DIRECT_REPLY', '返回答复', '{\"messageTemplate\": \"已收到：{{variables.user_message}}\"}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (87, 4, 1026, 1031, 'end', 'END', '结束', '{}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (88, 4, 1026, 1032, 'start', 'START', '开始', '{}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (89, 4, 1026, 1032, 'user-input', 'USER_INPUT', '接收用户消息', '{}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (90, 4, 1026, 1032, 'reply', 'DIRECT_REPLY', '返回答复', '{\"messageTemplate\": \"已收到：{{variables.user_message}}\"}', '{}', '{}');
INSERT INTO `orchestration_node` VALUES (91, 4, 1026, 1032, 'end', 'END', '结束', '{}', '{}', '{}');

-- ----------------------------
-- Table structure for orchestration_node_property
-- ----------------------------
DROP TABLE IF EXISTS `orchestration_node_property`;
CREATE TABLE `orchestration_node_property`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `node_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `property_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `data_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `widget` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `resource_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sensitive_flag` tinyint(1) NOT NULL DEFAULT 0,
  `required_field` tinyint(1) NOT NULL DEFAULT 0,
  `publish_error_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_orchestration_node_property`(`node_type` ASC, `property_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 72 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orchestration_node_property
-- ----------------------------

-- ----------------------------
-- Table structure for orchestration_node_type
-- ----------------------------
DROP TABLE IF EXISTS `orchestration_node_type`;
CREATE TABLE `orchestration_node_type`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `node_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `label` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `symbol` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `supported_graph_types_json` json NOT NULL,
  `capabilities_json` json NOT NULL,
  `permissions_json` json NOT NULL,
  `required_config_fields_json` json NOT NULL,
  `input_ports_json` json NOT NULL,
  `output_ports_json` json NOT NULL,
  `config_schema_json` json NOT NULL,
  `input_schema_json` json NOT NULL,
  `output_schema_json` json NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `executor_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_orchestration_node_type`(`node_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 101 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orchestration_node_type
-- ----------------------------
INSERT INTO `orchestration_node_type` VALUES (1, 'START', '1.0', '开始节点', '流程入口', '*', '[\"APPLICATION_WORKFLOW\"]', '[]', '[]', '[]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": []}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 10, 'ACTIVE', 'core.start');
INSERT INTO `orchestration_node_type` VALUES (2, 'END', '1.0', '结束节点', '流程出口', '*', '[\"APPLICATION_WORKFLOW\"]', '[]', '[]', '[]', '[\"default\"]', '[]', '{\"type\": \"object\", \"fields\": []}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 15, 'ACTIVE', 'core.end');
INSERT INTO `orchestration_node_type` VALUES (3, 'RAG', '1.0', '知识检索', '召回企业知识', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"knowledge.search\"]', '[]', '[]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"embeddingModelSource\", \"widget\": \"hidden\"}, {\"name\": \"embeddingModelId\", \"widget\": \"hidden\"}, {\"name\": \"embeddingModelKey\", \"widget\": \"resource-select\", \"resourceType\": \"EMBEDDING_MODEL\"}, {\"name\": \"retrievalScope\", \"widget\": \"select\", \"options\": [\"EXPLICIT_DOCUMENTS\", \"VISIBLE_DOCUMENTS\"]}, {\"name\": \"knowledgeDocumentIds\", \"widget\": \"resource-select\", \"multiple\": true, \"resourceType\": \"KNOWLEDGE_DOCUMENT\"}, {\"name\": \"languageStrategy\", \"widget\": \"select\", \"options\": [\"DOCUMENT\", \"QUERY\", \"AUTO\"]}, {\"name\": \"queryLanguage\", \"widget\": \"select\", \"options\": [\"ZH\", \"EN\", \"OTHER\"]}, {\"name\": \"topK\", \"widget\": \"number\"}], \"required\": [\"embeddingModelSource\", \"embeddingModelKey\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 20, 'ACTIVE', 'legacy.rag');
INSERT INTO `orchestration_node_type` VALUES (4, 'LLM', '1.0', '模型生成', '调用模型底座', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"model.invoke\"]', '[]', '[\"modelId\", \"promptTemplate\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"modelId\", \"widget\": \"resource-select\", \"resourceType\": \"MODEL\"}, {\"name\": \"backupModelId\", \"widget\": \"resource-select\", \"resourceType\": \"MODEL\"}, {\"name\": \"promptTemplate\", \"widget\": \"prompt-editor\"}, {\"name\": \"systemMessage\", \"widget\": \"prompt-editor\"}, {\"name\": \"memoryWindow\", \"widget\": \"number\"}, {\"name\": \"maxContextChars\", \"widget\": \"number\"}, {\"name\": \"imageVariables\", \"widget\": \"variable-list\"}, {\"name\": \"structuredOutputSchema\", \"widget\": \"json-schema\"}, {\"name\": \"retryPolicy\", \"widget\": \"json\"}], \"required\": [\"modelId\", \"promptTemplate\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 30, 'ACTIVE', 'legacy.llm');
INSERT INTO `orchestration_node_type` VALUES (5, 'AGENT', '1.0', 'Agent 调用', '调用已配置 Agent', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"agent.invoke\"]', '[]', '[\"agentId\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"agentId\", \"widget\": \"resource-select\", \"resourceType\": \"AGENT\"}], \"required\": [\"agentId\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 40, 'ACTIVE', 'orchestration.agent');
INSERT INTO `orchestration_node_type` VALUES (6, 'CONDITION', '1.0', '条件分支', '根据输入选择路径', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"control.branch\"]', '[]', '[\"inputReference\", \"operator\"]', '[\"default\"]', '[\"true\", \"false\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"inputReference\", \"widget\": \"variable-reference\"}, {\"name\": \"operator\", \"widget\": \"select\", \"options\": [\"CONTAINS\", \"EQUALS\", \"NOT_EMPTY\"]}, {\"name\": \"value\", \"widget\": \"text\"}], \"required\": [\"inputReference\", \"operator\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 50, 'ACTIVE', 'legacy.condition');
INSERT INTO `orchestration_node_type` VALUES (7, 'PARALLEL', '1.0', '并行分支', '并行执行多个分支', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"control.parallel\"]', '[]', '[]', '[\"default\"]', '[\"*\"]', '{\"type\": \"object\", \"fields\": []}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 60, 'ACTIVE', 'orchestration.parallel');
INSERT INTO `orchestration_node_type` VALUES (8, 'JOIN', '1.0', '并行聚合', '等待并汇聚分支', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"control.join\"]', '[]', '[]', '[\"*\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": []}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 70, 'ACTIVE', 'orchestration.join');
INSERT INTO `orchestration_node_type` VALUES (9, 'LOOP', '1.0', '循环节点', '重复执行节点', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"control.loop\"]', '[]', '[]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": []}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 80, 'ACTIVE', 'orchestration.loop');
INSERT INTO `orchestration_node_type` VALUES (10, 'TRANSFORM', '1.0', '转换节点', '转换输入数据', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"data.transform\"]', '[]', '[]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": []}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 90, 'ACTIVE', 'transform.legacy');
INSERT INTO `orchestration_node_type` VALUES (11, 'HUMAN', '1.0', '人工审批', '等待人工审批', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"human.approval\"]', '[]', '[\"approvalTitle\", \"approvalDescription\", \"approvalGroupId\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"approvalTitle\", \"widget\": \"text\"}, {\"name\": \"approvalDescription\", \"widget\": \"textarea\"}, {\"name\": \"approvalGroupId\", \"widget\": \"resource-select\", \"resourceType\": \"ORGANIZATION_UNIT\"}], \"required\": [\"approvalTitle\", \"approvalDescription\", \"approvalGroupId\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 100, 'ACTIVE', 'legacy.human');
INSERT INTO `orchestration_node_type` VALUES (78, 'USER_INPUT', '1.0', 'User input', 'Conversation input schema and file inputs', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"conversation.input\"]', '[]', '[]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"inputSchema\", \"widget\": \"json-schema\"}, {\"name\": \"fileTypes\", \"widget\": \"multi-select\"}, {\"name\": \"defaults\", \"widget\": \"json\"}]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 5, 'ACTIVE', 'conversation.input');
INSERT INTO `orchestration_node_type` VALUES (79, 'DIRECT_REPLY', '1.0', 'Direct reply', 'Render a conversation reply from a template', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"conversation.reply\"]', '[]', '[\"messageTemplate\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"messageTemplate\", \"widget\": \"prompt-editor\"}], \"required\": [\"messageTemplate\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 16, 'ACTIVE', 'conversation.reply');
INSERT INTO `orchestration_node_type` VALUES (80, 'QUESTION_CLASSIFIER', '1.0', 'Question classifier', 'Classify input and select a named branch', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"model.invoke\", \"control.branch\"]', '[]', '[\"modelId\", \"inputVariable\", \"categories\"]', '[\"default\"]', '[\"*\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"modelId\", \"widget\": \"resource-select\", \"resourceType\": \"MODEL\"}, {\"name\": \"inputVariable\", \"widget\": \"variable-select\"}, {\"name\": \"categories\", \"widget\": \"category-list\"}], \"required\": [\"modelId\", \"inputVariable\", \"categories\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 45, 'ACTIVE', 'orchestration.question-classifier');
INSERT INTO `orchestration_node_type` VALUES (81, 'ROUTER', '1.0', 'Router', 'Select a branch using a persisted routing rule', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"control.route\"]', '[]', '[\"ruleSetId\"]', '[\"default\"]', '[\"*\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"ruleSetId\", \"widget\": \"resource-select\", \"resourceType\": \"ROUTING_RULE_SET\"}], \"required\": [\"ruleSetId\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 47, 'ACTIVE', 'orchestration.router');
INSERT INTO `orchestration_node_type` VALUES (82, 'PARAMETER_EXTRACTOR', '1.0', 'Parameter extractor', 'Extract a typed parameter object with an LLM', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"model.invoke\", \"schema.extract\"]', '[]', '[\"modelId\", \"inputVariable\", \"parameterSchema\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"modelId\", \"widget\": \"resource-select\", \"resourceType\": \"MODEL\"}, {\"name\": \"inputVariable\", \"widget\": \"variable-select\"}, {\"name\": \"parameterSchema\", \"widget\": \"json-schema\"}, {\"name\": \"instruction\", \"widget\": \"prompt-editor\"}], \"required\": [\"modelId\", \"inputVariable\", \"parameterSchema\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 46, 'ACTIVE', 'orchestration.parameter-extractor');
INSERT INTO `orchestration_node_type` VALUES (83, 'ITERATION', '1.0', 'Iteration', 'Run a subgraph for each array item', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"control.iterate\"]', '[]', '[\"inputReference\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"inputReference\", \"widget\": \"variable-reference\"}, {\"name\": \"bodyNodeId\", \"widget\": \"node-select\"}, {\"name\": \"itemVariable\", \"widget\": \"text\"}, {\"name\": \"parallel\", \"widget\": \"switch\"}, {\"name\": \"errorStrategy\", \"widget\": \"select\", \"options\": [\"FAIL_FAST\", \"CONTINUE_ON_ERROR\"]}, {\"name\": \"flattenOutput\", \"widget\": \"switch\"}, {\"name\": \"maxItems\", \"widget\": \"number\"}], \"required\": [\"inputReference\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 75, 'ACTIVE', 'orchestration.iteration');
INSERT INTO `orchestration_node_type` VALUES (84, 'CODE', '1.0', 'Code execution', 'Execute sandboxed Python through a governed external connector', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"code.sandbox\"]', '[]', '[\"code\", \"connectorId\", \"outputSchema\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"language\", \"widget\": \"enum\"}, {\"name\": \"code\", \"widget\": \"code-editor\"}, {\"name\": \"connectorId\", \"widget\": \"resource-select\", \"resourceType\": \"TOOL_CONNECTOR\"}, {\"name\": \"inputVariables\", \"widget\": \"variable-list\"}, {\"name\": \"outputSchema\", \"widget\": \"json-schema\"}, {\"name\": \"retryPolicy\", \"widget\": \"json\"}], \"required\": [\"code\", \"connectorId\", \"outputSchema\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 91, 'ACTIVE', 'sandbox.python');
INSERT INTO `orchestration_node_type` VALUES (85, 'TEMPLATE_TRANSFORM', '1.0', 'Template transform', 'Render a Jinja2 template', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"data.transform\"]', '[]', '[\"template\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"template\", \"widget\": \"jinja-editor\"}, {\"name\": \"inputVariables\", \"widget\": \"variable-list\"}], \"required\": [\"template\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 92, 'ACTIVE', 'transform.jinja2');
INSERT INTO `orchestration_node_type` VALUES (86, 'VARIABLE_AGGREGATOR', '1.0', 'Variable aggregator', 'Aggregate values from multiple branches', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"data.aggregate\"]', '[]', '[\"variables\"]', '[\"*\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"variables\", \"widget\": \"variable-list\"}, {\"name\": \"grouping\", \"widget\": \"json\"}], \"required\": [\"variables\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 93, 'ACTIVE', 'transform.variable-aggregator');
INSERT INTO `orchestration_node_type` VALUES (87, 'DOCUMENT_EXTRACTOR', '1.0', 'Document extractor', 'Extract text from supported files', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"document.extract\"]', '[]', '[\"inputReference\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"inputReference\", \"widget\": \"variable-reference\"}, {\"name\": \"fileTypes\", \"widget\": \"multi-select\"}], \"required\": [\"inputReference\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 94, 'ACTIVE', 'document.extractor');
INSERT INTO `orchestration_node_type` VALUES (88, 'VARIABLE_ASSIGNMENT', '1.0', 'Variable assignment', 'Assign values to workflow variables', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"data.assign\"]', '[]', '[\"assignments\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"assignments\", \"widget\": \"assignment-list\"}], \"required\": [\"assignments\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 95, 'ACTIVE', 'transform.variable-assignment');
INSERT INTO `orchestration_node_type` VALUES (89, 'LIST_OPERATOR', '1.0', 'List operator', 'Filter, slice and sort arrays', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"data.list\"]', '[]', '[\"inputReference\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"inputReference\", \"widget\": \"variable-reference\"}, {\"name\": \"filter\", \"widget\": \"expression\"}, {\"name\": \"takeN\", \"widget\": \"number\"}, {\"name\": \"sort\", \"widget\": \"json\"}], \"required\": [\"inputReference\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 96, 'ACTIVE', 'transform.list-operator');
INSERT INTO `orchestration_node_type` VALUES (90, 'HTTP_REQUEST', '1.0', 'HTTP request', 'Call an HTTP endpoint through a connector', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"connector.invoke\"]', '[]', '[\"url\", \"method\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"url\", \"widget\": \"url-template\"}, {\"name\": \"method\", \"widget\": \"enum\"}, {\"name\": \"authRef\", \"widget\": \"credential-select\"}, {\"name\": \"headers\", \"widget\": \"key-value\"}, {\"name\": \"params\", \"widget\": \"key-value\"}, {\"name\": \"body\", \"widget\": \"json\"}, {\"name\": \"verifySsl\", \"widget\": \"switch\"}, {\"name\": \"timeout\", \"widget\": \"duration\"}, {\"name\": \"retryPolicy\", \"widget\": \"json\"}, {\"name\": \"responseSchema\", \"widget\": \"json-schema\"}], \"required\": [\"url\", \"method\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 110, 'ACTIVE', 'connector.http');
INSERT INTO `orchestration_node_type` VALUES (91, 'OPENAPI', '1.0', 'OpenAPI tool', 'Invoke an operation from a persisted OpenAPI connector', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"connector.invoke\"]', '[]', '[\"connectorId\", \"operationId\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"connectorId\", \"widget\": \"resource-select\", \"resourceType\": \"TOOL_CONNECTOR\"}, {\"name\": \"operationId\", \"widget\": \"operation-select\"}, {\"name\": \"arguments\", \"widget\": \"json\"}], \"required\": [\"connectorId\", \"operationId\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 112, 'ACTIVE', 'connector.openapi');
INSERT INTO `orchestration_node_type` VALUES (92, 'MCP', '1.0', 'MCP tool', 'Invoke a persisted MCP server tool', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"connector.invoke\"]', '[]', '[\"connectorId\", \"toolName\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"connectorId\", \"widget\": \"resource-select\", \"resourceType\": \"TOOL_CONNECTOR\"}, {\"name\": \"toolName\", \"widget\": \"tool-select\"}, {\"name\": \"arguments\", \"widget\": \"json\"}], \"required\": [\"connectorId\", \"toolName\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 113, 'ACTIVE', 'connector.mcp');
INSERT INTO `orchestration_node_type` VALUES (93, 'WEBHOOK', '1.0', 'Webhook', 'Call a persisted webhook connector', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"connector.invoke\"]', '[]', '[\"connectorId\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"connectorId\", \"widget\": \"resource-select\", \"resourceType\": \"TOOL_CONNECTOR\"}, {\"name\": \"payload\", \"widget\": \"json\"}, {\"name\": \"idempotencyKey\", \"widget\": \"variable-select\"}], \"required\": [\"connectorId\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 114, 'ACTIVE', 'connector.webhook');
INSERT INTO `orchestration_node_type` VALUES (94, 'INTERNAL_API', '1.0', 'Internal API', 'Call a tenant-scoped internal API connector', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"connector.invoke\"]', '[]', '[\"connectorId\", \"operationId\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"connectorId\", \"widget\": \"resource-select\", \"resourceType\": \"TOOL_CONNECTOR\"}, {\"name\": \"operationId\", \"widget\": \"operation-select\"}, {\"name\": \"arguments\", \"widget\": \"json\"}], \"required\": [\"connectorId\", \"operationId\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 115, 'ACTIVE', 'connector.internal-api');
INSERT INTO `orchestration_node_type` VALUES (95, 'WEB_CRAWLER', '1.0', 'Web crawler', 'Fetch web content through a governed crawler', '*', '[\"APPLICATION_WORKFLOW\"]', '[\"connector.crawl\"]', '[]', '[\"url\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"url\", \"widget\": \"url-template\"}, {\"name\": \"allowedDomains\", \"widget\": \"list\"}, {\"name\": \"maxPages\", \"widget\": \"number\"}, {\"name\": \"extractSummary\", \"widget\": \"switch\"}, {\"name\": \"timeout\", \"widget\": \"duration\"}], \"required\": [\"url\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 116, 'ACTIVE', 'connector.web-crawler');
INSERT INTO `orchestration_node_type` VALUES (96, 'CONTEXT_BUILDER', '1.0', '上下文整理', '把用户问题、会话记忆和知识召回整理成模型可理解的工作上下文。', '▣', '[\"APPLICATION_WORKFLOW\"]', '[\"context.compose\"]', '[]', '[]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"inputVariables\", \"label\": \"要包含的信息\", \"widget\": \"variable-list\"}, {\"name\": \"outputVariable\", \"label\": \"输出变量名\", \"widget\": \"text\"}, {\"name\": \"separator\", \"label\": \"信息分隔符\", \"widget\": \"text\"}, {\"name\": \"maxChars\", \"label\": \"最大上下文长度\", \"widget\": \"number\"}]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 24, 'ACTIVE', 'context.builder');
INSERT INTO `orchestration_node_type` VALUES (97, 'PROMPT_TEMPLATE', '1.0', '提示词模板', '用可复用的模板把业务目标、上下文和变量组合成一次模型任务。', '✎', '[\"APPLICATION_WORKFLOW\"]', '[\"prompt.render\"]', '[]', '[\"template\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"template\", \"label\": \"提示词内容\", \"widget\": \"prompt-editor\"}, {\"name\": \"outputVariable\", \"label\": \"输出变量名\", \"widget\": \"text\"}, {\"name\": \"inputVariables\", \"label\": \"可用变量\", \"widget\": \"variable-list\"}], \"required\": [\"template\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 26, 'ACTIVE', 'prompt.template');
INSERT INTO `orchestration_node_type` VALUES (98, 'SESSION_MEMORY', '1.0', '会话记忆', '从当前会话中选择最近的重要消息，控制上下文窗口并传给后续节点。', '◌', '[\"APPLICATION_WORKFLOW\"]', '[\"conversation.memory\"]', '[]', '[]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"window\", \"label\": \"保留对话轮数\", \"widget\": \"number\"}, {\"name\": \"outputVariable\", \"label\": \"输出变量名\", \"widget\": \"text\"}, {\"name\": \"maxChars\", \"label\": \"最大记忆长度\", \"widget\": \"number\"}]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 28, 'ACTIVE', 'conversation.memory');
INSERT INTO `orchestration_node_type` VALUES (99, 'AGENT_TEAM', '1.0', '智能体团队', '按多角色协作语义执行受治理任务', '◎', '[\"APPLICATION_WORKFLOW\"]', '[\"collaboration.multi-role\"]', '[]', '[\"connectorId\", \"framework\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"framework\", \"label\": \"运行框架\", \"widget\": \"enum\", \"options\": [\"AUTOGEN\", \"LANGGRAPH\", \"CREW\"]}, {\"name\": \"connectorId\", \"label\": \"运行时连接器\", \"widget\": \"resource-select\", \"resourceType\": \"TOOL_CONNECTOR\"}, {\"name\": \"agents\", \"label\": \"团队角色\", \"widget\": \"json\"}, {\"name\": \"path\", \"label\": \"运行时路径\", \"widget\": \"text\"}, {\"name\": \"idempotencyKey\", \"label\": \"幂等变量\", \"widget\": \"variable-select\"}], \"required\": [\"connectorId\", \"framework\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 120, 'ACTIVE', 'agent.team');
INSERT INTO `orchestration_node_type` VALUES (100, 'GRAPH_ORCHESTRATOR', '1.0', '图编排运行时', '按计划编排语义执行受治理任务', '◇', '[\"APPLICATION_WORKFLOW\"]', '[\"collaboration.planning\"]', '[]', '[\"connectorId\", \"framework\"]', '[\"default\"]', '[\"default\"]', '{\"type\": \"object\", \"fields\": [{\"name\": \"framework\", \"label\": \"运行框架\", \"widget\": \"enum\", \"options\": [\"LANGGRAPH\", \"AUTOGEN\", \"CREW\"]}, {\"name\": \"connectorId\", \"label\": \"运行时连接器\", \"widget\": \"resource-select\", \"resourceType\": \"TOOL_CONNECTOR\"}, {\"name\": \"graph\", \"label\": \"子图定义\", \"widget\": \"json\"}, {\"name\": \"path\", \"label\": \"运行时路径\", \"widget\": \"text\"}, {\"name\": \"idempotencyKey\", \"label\": \"幂等变量\", \"widget\": \"variable-select\"}], \"required\": [\"connectorId\", \"framework\"]}', '{\"type\": \"object\", \"namespaces\": [\"input\", \"variables\", \"nodes\", \"context\"]}', '{\"type\": \"object\", \"properties\": {\"output\": {}}}', 122, 'ACTIVE', 'graph.orchestrator');

-- ----------------------------
-- Table structure for orchestration_variable
-- ----------------------------
DROP TABLE IF EXISTS `orchestration_variable`;
CREATE TABLE `orchestration_variable`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `app_id` bigint NOT NULL,
  `revision_id` bigint NOT NULL,
  `source_node_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `output_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `data_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sensitive_flag` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_orchestration_variable_revision`(`revision_id` ASC, `source_node_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orchestration_variable
-- ----------------------------
INSERT INTO `orchestration_variable` VALUES (9, 4, 1026, 1028, 'start', '$.output', 'any', 0);
INSERT INTO `orchestration_variable` VALUES (10, 4, 1026, 1028, 'user-input', '$.output', 'any', 0);
INSERT INTO `orchestration_variable` VALUES (11, 4, 1026, 1028, 'reply', '$.output', 'any', 0);
INSERT INTO `orchestration_variable` VALUES (12, 4, 1026, 1031, 'start', '$.output', 'any', 0);
INSERT INTO `orchestration_variable` VALUES (13, 4, 1026, 1031, 'user-input', '$.output', 'any', 0);
INSERT INTO `orchestration_variable` VALUES (14, 4, 1026, 1031, 'reply', '$.output', 'any', 0);
INSERT INTO `orchestration_variable` VALUES (15, 4, 1026, 1032, 'start', '$.output', 'any', 0);
INSERT INTO `orchestration_variable` VALUES (16, 4, 1026, 1032, 'user-input', '$.output', 'any', 0);
INSERT INTO `orchestration_variable` VALUES (17, 4, 1026, 1032, 'reply', '$.output', 'any', 0);

-- ----------------------------
-- Table structure for orchestration_version
-- ----------------------------
DROP TABLE IF EXISTS `orchestration_version`;
CREATE TABLE `orchestration_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `app_id` bigint NOT NULL,
  `candidate_id` bigint NULL DEFAULT NULL,
  `candidate_fingerprint` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `version_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version_no` int NOT NULL,
  `graph_json` json NOT NULL,
  `change_summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `released_by` bigint NULL DEFAULT NULL,
  `released_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PUBLISHED',
  `release_bundle_json` json NULL,
  `release_bundle_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `gate_report_id` bigint NULL DEFAULT NULL,
  `override_granted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_orchestration_version_id`(`version_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_orchestration_app_version_no`(`app_id` ASC, `version_no` ASC) USING BTREE,
  INDEX `idx_orchestration_version_tenant_app`(`tenant_id` ASC, `app_id` ASC, `released_at` ASC) USING BTREE,
  INDEX `idx_orchestration_version_candidate`(`tenant_id` ASC, `app_id` ASC, `candidate_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1015 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orchestration_version
-- ----------------------------

-- ----------------------------
-- Table structure for platform_agent_app
-- ----------------------------
DROP TABLE IF EXISTS `platform_agent_app`;
CREATE TABLE `platform_agent_app`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `app_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `app_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `app_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `input_schema_json` json NOT NULL,
  `output_schema_json` json NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_agent_app`(`tenant_id` ASC, `app_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_agent_app
-- ----------------------------

-- ----------------------------
-- Table structure for platform_agent_app_version
-- ----------------------------
DROP TABLE IF EXISTS `platform_agent_app_version`;
CREATE TABLE `platform_agent_app_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `app_id` bigint NOT NULL,
  `version_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `graph_version_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `input_schema_json` json NOT NULL,
  `output_schema_json` json NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `released_by` bigint NULL DEFAULT NULL,
  `released_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_agent_version`(`tenant_id` ASC, `app_id` ASC, `version_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_agent_app_version
-- ----------------------------

-- ----------------------------
-- Table structure for platform_app_channel
-- ----------------------------
DROP TABLE IF EXISTS `platform_app_channel`;
CREATE TABLE `platform_app_channel`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `app_id` bigint NOT NULL,
  `channel_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `environment_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version_id` bigint NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_app_channel`(`tenant_id` ASC, `app_id` ASC, `channel_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_app_channel
-- ----------------------------

-- ----------------------------
-- Table structure for platform_audit_review
-- ----------------------------
DROP TABLE IF EXISTS `platform_audit_review`;
CREATE TABLE `platform_audit_review`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `audit_event_id` bigint NOT NULL,
  `reviewer_id` bigint NULL DEFAULT NULL,
  `review_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `review_comment` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `reviewed_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_audit_review`(`tenant_id` ASC, `audit_event_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_audit_review
-- ----------------------------

-- ----------------------------
-- Table structure for platform_conversation
-- ----------------------------
DROP TABLE IF EXISTS `platform_conversation`;
CREATE TABLE `platform_conversation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `conversation_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `app_id` bigint NOT NULL,
  `version_id` bigint NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_conversation`(`tenant_id` ASC, `conversation_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_conversation
-- ----------------------------
INSERT INTO `platform_conversation` VALUES (12, 4, '12', 1003, 1008, 'ACTIVE', 6, '2026-07-27 10:12:59', '2026-07-27 10:12:59');
INSERT INTO `platform_conversation` VALUES (13, 4, '13', 1003, 1008, 'ACTIVE', 6, '2026-07-27 14:22:00', '2026-07-27 14:22:00');
INSERT INTO `platform_conversation` VALUES (14, 4, '14', 1003, 1009, 'ACTIVE', 6, '2026-07-28 11:49:30', '2026-07-28 11:49:30');
INSERT INTO `platform_conversation` VALUES (15, 4, '22908e91-e73d-488e-b2e7-bb1f05deaab1', 1008, NULL, 'ACTIVE', 6, '2026-07-29 09:38:31', '2026-07-29 09:38:31');
INSERT INTO `platform_conversation` VALUES (16, 4, 'a2cd508f-5901-4d78-9456-2c5a0cafdae3', 1007, NULL, 'ACTIVE', 6, '2026-07-29 09:38:37', '2026-07-29 09:38:38');
INSERT INTO `platform_conversation` VALUES (17, 4, '9852aac7-5378-436c-bb2e-de53995d30e7', 1006, NULL, 'ACTIVE', 6, '2026-07-29 09:38:43', '2026-07-29 09:38:43');
INSERT INTO `platform_conversation` VALUES (18, 4, '0e265dc4-86e2-45f0-ba25-9efcd4dc0519', 1003, 1009, 'ACTIVE', 6, '2026-07-29 09:38:48', '2026-07-29 09:38:48');
INSERT INTO `platform_conversation` VALUES (19, 4, 'dfc72169-8ced-48c8-8f85-331953ae9157', 1003, 1009, 'ACTIVE', 6, '2026-07-29 09:42:54', '2026-07-29 09:42:55');
INSERT INTO `platform_conversation` VALUES (20, 4, 'aec2476a-9a01-476f-9c53-33954f9d6e0e', 1003, 1009, 'ACTIVE', 6, '2026-07-29 11:19:51', '2026-07-29 11:19:51');
INSERT INTO `platform_conversation` VALUES (21, 4, 'ac8ba7c8-f86d-4ee5-ac9d-a8d449a7384c', 1019, NULL, 'ACTIVE', 6, '2026-07-29 13:06:15', '2026-07-29 13:06:15');
INSERT INTO `platform_conversation` VALUES (22, 4, '582e8c03-6160-4be1-82bf-b50023631fe1', 1018, NULL, 'ACTIVE', 6, '2026-07-29 13:06:21', '2026-07-29 13:06:21');
INSERT INTO `platform_conversation` VALUES (23, 4, 'c0ab8f2c-29e5-4bd2-9eea-5d6d895e8a2d', 1024, 1014, 'ACTIVE', 6, '2026-07-29 16:59:09', '2026-07-29 16:59:09');
INSERT INTO `platform_conversation` VALUES (24, 4, '60c2d8a0-1e75-4f25-90e8-39809b56fb06', 1024, 1014, 'ACTIVE', 6, '2026-08-04 16:04:08', '2026-08-04 16:04:08');
INSERT INTO `platform_conversation` VALUES (25, 4, '2a5e1fe7-135b-46f9-ad96-415b9a98a585', 1003, 1009, 'ACTIVE', 6, '2026-08-04 16:04:29', '2026-08-04 16:04:29');
INSERT INTO `platform_conversation` VALUES (26, 4, '50cd3395-290e-45ca-a7ab-9ba2f678c045', 1019, NULL, 'ACTIVE', 6, '2026-08-04 16:04:38', '2026-08-04 16:04:38');
INSERT INTO `platform_conversation` VALUES (27, 4, 'a6415c24-118d-4318-9965-0d50a1c5321f', 1024, 1014, 'ACTIVE', 6, '2026-08-04 16:22:57', '2026-08-04 16:22:57');
INSERT INTO `platform_conversation` VALUES (28, 4, '0636e47e-b1eb-4d63-83b6-9e1910df1ad2', 1024, 1014, 'ACTIVE', 6, '2026-08-04 16:44:36', '2026-08-04 16:44:36');
INSERT INTO `platform_conversation` VALUES (29, 4, '4d154b1c-b3f9-450f-b1a6-dcd2d9d6fb8b', 1024, 1014, 'ACTIVE', 6, '2026-08-04 17:14:26', '2026-08-04 17:14:26');
INSERT INTO `platform_conversation` VALUES (30, 4, '4d3b1329-28a8-45fa-a439-2a91b4ebaeb6', 1024, 1014, 'ACTIVE', 6, '2026-08-04 17:19:28', '2026-08-04 17:19:28');
INSERT INTO `platform_conversation` VALUES (31, 4, 'fbc80008-9a47-4b7c-a1ef-152a487bd21a', 1024, 1014, 'ACTIVE', 6, '2026-08-04 17:21:54', '2026-08-04 17:21:54');
INSERT INTO `platform_conversation` VALUES (32, 4, 'b99aa403-1a98-4e80-95d3-7bcaa3d9d8ec', 1024, 1014, 'ACTIVE', 6, '2026-08-04 17:25:03', '2026-08-04 17:25:03');
INSERT INTO `platform_conversation` VALUES (33, 4, '1d510c6a-c559-41f7-943e-68843c14a30a', 1025, NULL, 'ACTIVE', 6, '2026-08-06 09:49:41', '2026-08-06 09:49:41');
INSERT INTO `platform_conversation` VALUES (34, 4, '54357e86-e759-4a6e-9803-575d0ca6f6e7', 1025, NULL, 'ACTIVE', 6, '2026-08-06 09:54:29', '2026-08-06 09:54:29');
INSERT INTO `platform_conversation` VALUES (35, 4, 'df8e0ce6-4c16-41c4-911c-f02b3c0ee675', 1025, NULL, 'ARCHIVED', 6, '2026-08-06 09:54:57', '2026-08-06 09:55:03');
INSERT INTO `platform_conversation` VALUES (36, 4, '3c73c4a4-a7d4-415e-ab27-7cc825abcb47', 1026, NULL, 'ACTIVE', 6, '2026-08-06 16:20:55', '2026-08-06 16:20:55');
INSERT INTO `platform_conversation` VALUES (37, 4, '7ac43b3c-7ed1-42b6-88a9-d73ee8e8b0e5', 1026, NULL, 'ACTIVE', 6, '2026-08-07 10:38:04', '2026-08-07 10:38:04');

-- ----------------------------
-- Table structure for platform_conversation_message
-- ----------------------------
DROP TABLE IF EXISTS `platform_conversation_message`;
CREATE TABLE `platform_conversation_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `conversation_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `message_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `execution_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `role_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content_json` json NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'COMPLETED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_message`(`tenant_id` ASC, `message_id` ASC) USING BTREE,
  INDEX `idx_platform_message_conversation`(`tenant_id` ASC, `conversation_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_conversation_message
-- ----------------------------
INSERT INTO `platform_conversation_message` VALUES (12, 4, '12', 'c558403c-2933-4adf-9924-0ac20d2591ca', 'd89edfdd-3547-4c2e-93d6-20049f485ac8', 'USER', '{\"input\": \"招聘Java开发工程师\", \"query\": \"招聘Java开发工程师\", \"user_message\": \"招聘Java开发工程师\", \"conversationId\": \"12\"}', 'COMPLETED', '2026-07-27 10:12:59');
INSERT INTO `platform_conversation_message` VALUES (13, 4, '12', '23e50684-6d46-4f0b-9d21-d632ce69e162', 'd89edfdd-3547-4c2e-93d6-20049f485ac8', 'ASSISTANT', '\"下面是一份可直接发布的「Java开发工程师」招聘文案：\\n\\n**Java开发工程师**\\n\\n**岗位职责**\\n1. 负责公司业务系统的后端开发、接口设计与功能迭代。\\n2. 参与需求分析、技术方案设计、核心代码开发与性能优化。\\n3. 负责系统稳定性、可扩展性、安全性建设，处理线上问题。\\n4. 配合前端、测试、产品等团队完成项目交付。\\n5. 参与代码评审、技术文档编写及后端工程规范建设。\\n\\n**任职要求**\\n1. 本科及以上学历，计算机、软件工程等相关专业优先。\\n2. 具备 Java 后端开发经验，熟悉 Java 基础、多线程、集合、JVM 等。\\n3. 熟悉 Spring Boot、Spring Cloud、MyBatis/MyBatis-Plus 等主流框架。\\n4. 熟悉 MySQL、Redis，具备 SQL 优化和缓存设计经验。\\n5. 熟悉 RESTful API 设计，了解分布式系统、微服务架构者优先。\\n6. 熟悉 Git、Maven/Gradle、Linux 常用命令。\\n7. 具备良好的编码习惯、问题分析能力和团队协作能力。\\n\\n**加分项**\\n1. 有高并发、高可用系统开发经验。\\n2. 熟悉消息队列，如 Kafka、RabbitMQ、RocketMQ。\\n3. 熟悉 Docker、Kubernetes、CI/CD 流程。\\n4. 有电商、金融、SaaS、ERP、CRM 等业务系统经验。\\n\\n**薪资待遇**\\n薪资面议 / 具体根据经验与能力确定  \\n五险一金、绩效奖金、带薪年假、节日福利、技术成长空间\\n\\n**工作地点**\\n可填写：城市 / 办公地址 / 是否支持远程\\n\\n**招聘人数**\\n若干名\\n\\n也可以简化成一句招聘信息：\\n\\n现招聘 Java 开发工程师，要求熟悉 Java、Spring Boot、MySQL、Redis，有良好的代码能力和项目经验，薪资面议，欢迎投递简历。\"', 'COMPLETED', '2026-07-27 10:13:12');
INSERT INTO `platform_conversation_message` VALUES (14, 4, '13', 'ff11ef82-8508-40cf-a6fd-28cae59c215d', '4b2bb1e2-d072-48c8-a95d-442e32e44d32', 'USER', '{\"input\": \"招聘Java开发工程师\", \"query\": \"招聘Java开发工程师\", \"user_message\": \"招聘Java开发工程师\", \"conversationId\": \"13\"}', 'COMPLETED', '2026-07-27 14:22:01');
INSERT INTO `platform_conversation_message` VALUES (15, 4, '13', '00e86208-d9c5-4051-af76-517991297bf3', '4b2bb1e2-d072-48c8-a95d-442e32e44d32', 'ASSISTANT', '\"以下是一份可直接发布的 Java 开发工程师招聘文案：\\n\\n**Java 开发工程师**\\n\\n**岗位职责：**\\n1. 参与公司核心业务系统的设计、开发、测试与维护；\\n2. 根据业务需求完成后端接口、服务模块及相关功能开发；\\n3. 参与系统架构优化、性能调优及稳定性建设；\\n4. 配合产品、前端、测试等团队完成项目交付；\\n5. 编写和维护相关技术文档，保障代码质量和可维护性。\\n\\n**任职要求：**\\n1. 本科及以上学历，计算机、软件工程等相关专业优先；\\n2. 具备 Java 后端开发经验，熟悉 Java 基础、集合、多线程、JVM 等；\\n3. 熟悉 Spring Boot、Spring Cloud、MyBatis 等主流开发框架；\\n4. 熟悉 MySQL、Redis、消息队列等常用中间件；\\n5. 具备良好的代码规范意识和问题排查能力；\\n6. 有高并发、分布式系统、微服务项目经验者优先；\\n7. 具备良好的沟通能力、责任心和团队协作意识。\\n\\n**加分项：**\\n1. 熟悉 Linux、Docker、Kubernetes 等部署环境；\\n2. 有大型互联网平台、金融、电商、SaaS 系统开发经验；\\n3. 熟悉 Elasticsearch、Kafka、RocketMQ 等技术；\\n4. 了解 DevOps、CI/CD 流程；\\n5. 有开源项目或技术博客经验。\\n\\n**薪资待遇：**\\n薪资面议 / 根据经验能力定薪\\n\\n**工作地点：**\\n可填写具体城市或办公地址\\n\\n**投递方式：**\\n请发送简历至：HR 邮箱 / 联系人微信 / 招聘平台投递\\n\\n也可以根据实际情况补充：公司介绍、薪资范围、工作年限、学历要求、是否双休、五险一金、项目方向等信息。\"', 'COMPLETED', '2026-07-27 14:22:20');
INSERT INTO `platform_conversation_message` VALUES (16, 4, '14', 'a0cd8cc3-d84d-43d8-8074-40593121502d', '406a770b-4b76-45ff-b2d7-2b594247f925', 'USER', '{\"input\": \"招聘java开发\", \"query\": \"招聘java开发\", \"user_message\": \"招聘java开发\", \"conversationId\": \"14\"}', 'COMPLETED', '2026-07-28 11:49:30');
INSERT INTO `platform_conversation_message` VALUES (17, 4, '14', '47e9c6c1-9e08-4ae2-8f87-31d4c9839eb3', '406a770b-4b76-45ff-b2d7-2b594247f925', 'ASSISTANT', '\"您好！我理解您想招聘 Java 开发工程师。\\n\\n我可以帮您整理招聘 JD、候选人筛选标准、面试题、薪资范围或招聘渠道。先给您一版通用 JD：\\n\\n**Java 开发工程师**\\n\\n**岗位职责**\\n- 负责 Java 后端系统的设计、开发、测试和维护\\n- 参与业务需求分析、技术方案设计和接口开发\\n- 负责系统性能优化、问题排查和线上故障处理\\n- 与产品、前端、测试等团队协作，保障项目按时交付\\n- 编写和维护相关技术文档\\n\\n**任职要求**\\n- 本科及以上学历，计算机、软件工程等相关专业优先\\n- 熟悉 Java 基础、集合、多线程、JVM 等核心知识\\n- 熟悉 Spring Boot、Spring Cloud、MyBatis 等主流框架\\n- 熟悉 MySQL、Redis、消息队列等常用中间件\\n- 了解 Linux、Git、Maven/Gradle 等开发工具\\n- 有良好的代码习惯、沟通能力和问题定位能力\\n\\n**加分项**\\n- 有高并发、分布式系统、微服务项目经验\\n- 熟悉 Docker、Kubernetes、CI/CD\\n- 有电商、金融、SaaS、ERP、CRM 等业务系统经验\\n\\n**薪资示例**\\n- 初级：8k-15k/月\\n- 中级：15k-25k/月\\n- 高级：25k-40k+/月\\n\\n您可以告诉我城市、经验年限、薪资范围、是否外包/全职、技术栈要求，我可以继续帮您生成更精准的招聘文案。\"', 'COMPLETED', '2026-07-28 11:49:48');
INSERT INTO `platform_conversation_message` VALUES (18, 4, '22908e91-e73d-488e-b2e7-bb1f05deaab1', '22908e91-e73d-488e-b2e7-bb1f05deaab1-USER-1785289110882', NULL, 'USER', '1', 'ACTIVE', '2026-07-29 09:38:31');
INSERT INTO `platform_conversation_message` VALUES (19, 4, 'a2cd508f-5901-4d78-9456-2c5a0cafdae3', 'a2cd508f-5901-4d78-9456-2c5a0cafdae3-USER-1785289117592', NULL, 'USER', '1', 'ACTIVE', '2026-07-29 09:38:38');
INSERT INTO `platform_conversation_message` VALUES (20, 4, '9852aac7-5378-436c-bb2e-de53995d30e7', '9852aac7-5378-436c-bb2e-de53995d30e7-USER-1785289122957', NULL, 'USER', '1', 'ACTIVE', '2026-07-29 09:38:43');
INSERT INTO `platform_conversation_message` VALUES (21, 4, '0e265dc4-86e2-45f0-ba25-9efcd4dc0519', '0e265dc4-86e2-45f0-ba25-9efcd4dc0519-USER-1785289127921', NULL, 'USER', '1', 'ACTIVE', '2026-07-29 09:38:48');
INSERT INTO `platform_conversation_message` VALUES (22, 4, '0e265dc4-86e2-45f0-ba25-9efcd4dc0519', '0e265dc4-86e2-45f0-ba25-9efcd4dc0519-1785289128190', '7c984c88-d068-4da9-9a27-a42bbad17cca', 'USER', '{\"input\": \"1\", \"query\": \"1\", \"user_message\": \"1\"}', 'COMPLETED', '2026-07-29 09:38:49');
INSERT INTO `platform_conversation_message` VALUES (23, 4, '0e265dc4-86e2-45f0-ba25-9efcd4dc0519', 'd515f546-0058-42ca-83db-a277bc6d6a76', '7c984c88-d068-4da9-9a27-a42bbad17cca', 'ASSISTANT', '\"I see the conversation memory, but I don’t see a task or question attached to it. What would you like me to do with this?\"', 'COMPLETED', '2026-07-29 09:39:40');
INSERT INTO `platform_conversation_message` VALUES (24, 4, 'dfc72169-8ced-48c8-8f85-331953ae9157', '69f815f5-21c3-4fd8-a05e-253bf869c066', NULL, 'USER', '1', 'ACTIVE', '2026-07-29 09:42:54');
INSERT INTO `platform_conversation_message` VALUES (25, 4, 'dfc72169-8ced-48c8-8f85-331953ae9157', 'abdcb7ce-9536-436e-a966-d5840d5320e0', NULL, 'USER', '{\"input\": \"1\", \"query\": \"1\", \"user_message\": \"1\"}', 'ACTIVE', '2026-07-29 09:42:55');
INSERT INTO `platform_conversation_message` VALUES (26, 4, 'dfc72169-8ced-48c8-8f85-331953ae9157', '785bf265-f09b-4c11-91b5-8ef40e8d386b', NULL, 'ASSISTANT', '\"I see the conversation memory, but I don’t see a task or question attached to it. What would you like me to do with this?\"', 'ACTIVE', '2026-07-29 09:42:55');
INSERT INTO `platform_conversation_message` VALUES (28, 4, 'dfc72169-8ced-48c8-8f85-331953ae9157', 'dfc72169-8ced-48c8-8f85-331953ae9157-1785289381071', '3ffd7cfd-428b-4c18-94a2-59502b8669b0', 'USER', '{\"input\": \"你好\", \"query\": \"你好\", \"user_message\": \"你好\"}', 'COMPLETED', '2026-07-29 09:44:01');
INSERT INTO `platform_conversation_message` VALUES (29, 4, 'dfc72169-8ced-48c8-8f85-331953ae9157', '36e63a8a-bc9e-49fd-9ee3-d1c8b06d5dfa', '3ffd7cfd-428b-4c18-94a2-59502b8669b0', 'ASSISTANT', '\"你好。你想让我基于这段 conversation memory 做什么？例如排查消息格式、提取对话内容、写查询 SQL，或者分析为什么上一轮没有正常响应。\"', 'COMPLETED', '2026-07-29 09:44:35');
INSERT INTO `platform_conversation_message` VALUES (30, 4, 'aec2476a-9a01-476f-9c53-33954f9d6e0e', 'aec2476a-9a01-476f-9c53-33954f9d6e0e-1785295191609', 'b6113ddf-9c1d-4270-896f-a399317c1f57', 'USER', '{\"input\": \"请返回一句简短的运行状态说明\", \"query\": \"请返回一句简短的运行状态说明\", \"user_message\": \"请返回一句简短的运行状态说明\"}', 'COMPLETED', '2026-07-29 11:19:53');
INSERT INTO `platform_conversation_message` VALUES (31, 4, 'aec2476a-9a01-476f-9c53-33954f9d6e0e', 'e65afa95-bfb4-4376-8119-c5a058f25fdb', 'b6113ddf-9c1d-4270-896f-a399317c1f57', 'ASSISTANT', '\"系统运行正常。\"', 'COMPLETED', '2026-07-29 11:19:56');

-- ----------------------------
-- Table structure for platform_credential_ref
-- ----------------------------
DROP TABLE IF EXISTS `platform_credential_ref`;
CREATE TABLE `platform_credential_ref`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `credential_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `credential_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `ciphertext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `key_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `rotated_at` datetime NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_credential`(`tenant_id` ASC, `credential_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_credential_ref
-- ----------------------------

-- ----------------------------
-- Table structure for platform_event_fact
-- ----------------------------
DROP TABLE IF EXISTS `platform_event_fact`;
CREATE TABLE `platform_event_fact`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `org_unit_id` bigint NULL DEFAULT NULL,
  `user_id` bigint NULL DEFAULT NULL,
  `role_id` bigint NULL DEFAULT NULL,
  `agent_id` bigint NULL DEFAULT NULL,
  `workflow_id` bigint NULL DEFAULT NULL,
  `model_id` bigint NULL DEFAULT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `result_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `token_count` bigint NOT NULL DEFAULT 0,
  `cost_amount` decimal(20, 8) NOT NULL DEFAULT 0.00000000,
  `latency_ms` bigint NOT NULL DEFAULT 0,
  `occurred_at` datetime NOT NULL,
  `payload_json` json NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_platform_event_fact_tenant_time`(`tenant_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_platform_event_fact_dimensions`(`tenant_id` ASC, `org_unit_id` ASC, `user_id` ASC, `agent_id` ASC, `workflow_id` ASC, `model_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_event_fact
-- ----------------------------
INSERT INTO `platform_event_fact` VALUES (13, 4, NULL, 6, NULL, NULL, NULL, NULL, 'ORCHESTRATION_EXECUTION', 'SUCCEEDED', 0, 0.00000000, 12955, '2026-07-27 10:13:12', '{\"appId\": 1003, \"executionId\": \"d89edfdd-3547-4c2e-93d6-20049f485ac8\"}');
INSERT INTO `platform_event_fact` VALUES (14, 4, NULL, 6, NULL, NULL, NULL, NULL, 'ORCHESTRATION_EXECUTION', 'SUCCEEDED', 0, 0.00000000, 19646, '2026-07-27 14:22:20', '{\"appId\": 1003, \"executionId\": \"4b2bb1e2-d072-48c8-a95d-442e32e44d32\"}');
INSERT INTO `platform_event_fact` VALUES (15, 4, NULL, 6, NULL, NULL, NULL, NULL, 'ORCHESTRATION_EXECUTION', 'SUCCEEDED', 0, 0.00000000, 18074, '2026-07-28 11:49:48', '{\"appId\": 1003, \"executionId\": \"406a770b-4b76-45ff-b2d7-2b594247f925\"}');
INSERT INTO `platform_event_fact` VALUES (22, 4, NULL, 6, NULL, NULL, NULL, NULL, 'ORCHESTRATION_EXECUTION', 'SUCCEEDED', 0, 0.00000000, 51076, '2026-07-29 09:39:40', '{\"appId\": 1003, \"executionId\": \"7c984c88-d068-4da9-9a27-a42bbad17cca\"}');
INSERT INTO `platform_event_fact` VALUES (24, 4, NULL, 6, NULL, NULL, NULL, NULL, 'ORCHESTRATION_EXECUTION', 'SUCCEEDED', 0, 0.00000000, 34015, '2026-07-29 09:44:35', '{\"appId\": 1003, \"executionId\": \"3ffd7cfd-428b-4c18-94a2-59502b8669b0\"}');
INSERT INTO `platform_event_fact` VALUES (29, 4, NULL, 6, NULL, NULL, NULL, NULL, 'ORCHESTRATION_EXECUTION', 'SUCCEEDED', 0, 0.00000000, 4353, '2026-07-29 11:01:46', '{\"appId\": 1018, \"executionId\": \"c9fc142d-b221-4627-b05e-a2982cb418db\"}');
INSERT INTO `platform_event_fact` VALUES (30, 4, NULL, 6, NULL, NULL, NULL, NULL, 'ORCHESTRATION_EXECUTION', 'SUCCEEDED', 0, 0.00000000, 12027, '2026-07-29 11:17:14', '{\"appId\": 1019, \"executionId\": \"50f4ad41-595b-4aa5-8213-4aa63708ed33\"}');
INSERT INTO `platform_event_fact` VALUES (31, 4, NULL, 6, NULL, NULL, NULL, NULL, 'ORCHESTRATION_EXECUTION', 'SUCCEEDED', 0, 0.00000000, 3235, '2026-07-29 11:19:56', '{\"appId\": 1003, \"executionId\": \"b6113ddf-9c1d-4270-896f-a399317c1f57\"}');
INSERT INTO `platform_event_fact` VALUES (32, 4, NULL, 6, NULL, NULL, NULL, NULL, 'ORCHESTRATION_EXECUTION', 'SUCCEEDED', 0, 0.00000000, 5180, '2026-07-29 13:05:53', '{\"appId\": 1019, \"executionId\": \"cbe7ef48-01f9-4955-a3e6-aeff3c925efa\"}');
INSERT INTO `platform_event_fact` VALUES (33, 4, NULL, 6, NULL, NULL, NULL, NULL, 'ORCHESTRATION_EXECUTION', 'SUCCEEDED', 0, 0.00000000, 2040, '2026-07-29 16:48:26', '{\"appId\": 1024, \"executionId\": \"ee2ad645-c410-4464-8601-b5557e6432cb\"}');
INSERT INTO `platform_event_fact` VALUES (34, 4, NULL, 6, NULL, NULL, NULL, NULL, 'ORCHESTRATION_EXECUTION', 'SUCCEEDED', 0, 0.00000000, 1262, '2026-07-29 16:49:49', '{\"appId\": 1024, \"executionId\": \"8c8420c8-6a67-4b0d-9b0f-b5625c4aa9ad\"}');
INSERT INTO `platform_event_fact` VALUES (35, 4, NULL, 6, NULL, NULL, NULL, NULL, 'ORCHESTRATION_EXECUTION', 'SUCCEEDED', 0, 0.00000000, 1179, '2026-08-07 10:33:15', '{\"appId\": 1026, \"executionId\": \"f3199e3e-305f-485a-9bd0-1902cf8d828f\"}');

-- ----------------------------
-- Table structure for platform_execution_context
-- ----------------------------
DROP TABLE IF EXISTS `platform_execution_context`;
CREATE TABLE `platform_execution_context`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `execution_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `app_id` bigint NULL DEFAULT NULL,
  `entrypoint_id` bigint NULL DEFAULT NULL,
  `entrypoint_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `delivery_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `trigger_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `scheduled_fire_time` datetime NULL DEFAULT NULL,
  `version_id` bigint NULL DEFAULT NULL,
  `execution_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `idempotency_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'QUEUED',
  `input_json` json NULL,
  `output_json` json NULL,
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `started_at` datetime NULL DEFAULT NULL,
  `finished_at` datetime NULL DEFAULT NULL,
  `lease_owner` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `lease_until` datetime NULL DEFAULT NULL,
  `heartbeat_at` datetime NULL DEFAULT NULL,
  `cancel_requested` tinyint NOT NULL DEFAULT 0,
  `pause_requested` tinyint NOT NULL DEFAULT 0,
  `retry_count` int NOT NULL DEFAULT 0,
  `max_attempts` int NOT NULL DEFAULT 1,
  `current_node_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `state_json` json NULL,
  `approval_decision` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `compensation_json` json NULL,
  `draft_revision_id` bigint NULL DEFAULT NULL,
  `draft_graph_json` json NULL,
  `draft_revision_no` int NULL DEFAULT NULL,
  `run_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PRODUCTION',
  `runtime_snapshot_json` json NULL,
  `runtime_snapshot_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `span_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `task_id` bigint NULL DEFAULT NULL,
  `retry_of_execution_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `replay_of_execution_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `retry_attempt` int NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_execution_key`(`tenant_id` ASC, `idempotency_key` ASC) USING BTREE,
  INDEX `idx_platform_execution_status`(`tenant_id` ASC, `status` ASC, `started_at` ASC) USING BTREE,
  INDEX `idx_platform_execution_retry`(`tenant_id` ASC, `retry_of_execution_id` ASC) USING BTREE,
  INDEX `idx_platform_execution_run_type`(`tenant_id` ASC, `run_type` ASC, `started_at` ASC) USING BTREE,
  INDEX `idx_execution_replay_source`(`tenant_id` ASC, `replay_of_execution_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_execution_context
-- ----------------------------
INSERT INTO `platform_execution_context` VALUES (13, 4, 'd89edfdd-3547-4c2e-93d6-20049f485ac8', 1003, NULL, NULL, NULL, NULL, NULL, 1008, 'APPLICATION_WORKFLOW', '155864df-0b4b-4be3-b151-69041578489b', 'SUCCEEDED', '{\"input\": \"招聘Java开发工程师\", \"query\": \"招聘Java开发工程师\", \"user_message\": \"招聘Java开发工程师\", \"conversationId\": \"12\"}', '\"下面是一份可直接发布的「Java开发工程师」招聘文案：\\n\\n**Java开发工程师**\\n\\n**岗位职责**\\n1. 负责公司业务系统的后端开发、接口设计与功能迭代。\\n2. 参与需求分析、技术方案设计、核心代码开发与性能优化。\\n3. 负责系统稳定性、可扩展性、安全性建设，处理线上问题。\\n4. 配合前端、测试、产品等团队完成项目交付。\\n5. 参与代码评审、技术文档编写及后端工程规范建设。\\n\\n**任职要求**\\n1. 本科及以上学历，计算机、软件工程等相关专业优先。\\n2. 具备 Java 后端开发经验，熟悉 Java 基础、多线程、集合、JVM 等。\\n3. 熟悉 Spring Boot、Spring Cloud、MyBatis/MyBatis-Plus 等主流框架。\\n4. 熟悉 MySQL、Redis，具备 SQL 优化和缓存设计经验。\\n5. 熟悉 RESTful API 设计，了解分布式系统、微服务架构者优先。\\n6. 熟悉 Git、Maven/Gradle、Linux 常用命令。\\n7. 具备良好的编码习惯、问题分析能力和团队协作能力。\\n\\n**加分项**\\n1. 有高并发、高可用系统开发经验。\\n2. 熟悉消息队列，如 Kafka、RabbitMQ、RocketMQ。\\n3. 熟悉 Docker、Kubernetes、CI/CD 流程。\\n4. 有电商、金融、SaaS、ERP、CRM 等业务系统经验。\\n\\n**薪资待遇**\\n薪资面议 / 具体根据经验与能力确定  \\n五险一金、绩效奖金、带薪年假、节日福利、技术成长空间\\n\\n**工作地点**\\n可填写：城市 / 办公地址 / 是否支持远程\\n\\n**招聘人数**\\n若干名\\n\\n也可以简化成一句招聘信息：\\n\\n现招聘 Java 开发工程师，要求熟悉 Java、Spring Boot、MySQL、Redis，有良好的代码能力和项目经验，薪资面议，欢迎投递简历。\"', NULL, NULL, '2026-07-27 10:12:59', '2026-07-27 10:13:12', 'inline-d89edfdd-3547-4c2e-93d6-20049f485ac8', '2026-07-27 10:15:00', '2026-07-27 10:13:00', 0, 0, 0, 1, 'question_classifier-1', '{\"values\": {\"input\": \"招聘Java开发工程师\", \"query\": \"招聘Java开发工程师\", \"user_message\": \"招聘Java开发工程师\", \"conversationId\": \"12\", \"conversationHistory\": [{\"id\": 12, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-27T10:12:59\", \"messageId\": \"c558403c-2933-4adf-9924-0ac20d2591ca\", \"contentJson\": \"{\\\"input\\\": \\\"招聘Java开发工程师\\\", \\\"query\\\": \\\"招聘Java开发工程师\\\", \\\"user_message\\\": \\\"招聘Java开发工程师\\\", \\\"conversationId\\\": \\\"12\\\"}\", \"executionId\": \"d89edfdd-3547-4c2e-93d6-20049f485ac8\", \"conversationId\": \"12\"}]}, \"visited\": [\"start-1\", \"question_classifier-1\", \"user_input-1\"], \"currentNodeId\": \"question_classifier-1\"}', NULL, NULL, NULL, NULL, NULL, 'PRODUCTION', '{\"legacy\": true, \"schema\": \"release-bundle-legacy\"}', 'a3d7de696663b8cb2166d22d5db4e3adda9e8ab0bec7ed933bfa7424cdf89adc', NULL, NULL, NULL, NULL, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (14, 4, '4b2bb1e2-d072-48c8-a95d-442e32e44d32', 1003, NULL, NULL, NULL, NULL, NULL, 1008, 'APPLICATION_WORKFLOW', '9f5eb612-27d1-407a-bb99-257dab562594', 'SUCCEEDED', '{\"input\": \"招聘Java开发工程师\", \"query\": \"招聘Java开发工程师\", \"user_message\": \"招聘Java开发工程师\", \"conversationId\": \"13\"}', '\"以下是一份可直接发布的 Java 开发工程师招聘文案：\\n\\n**Java 开发工程师**\\n\\n**岗位职责：**\\n1. 参与公司核心业务系统的设计、开发、测试与维护；\\n2. 根据业务需求完成后端接口、服务模块及相关功能开发；\\n3. 参与系统架构优化、性能调优及稳定性建设；\\n4. 配合产品、前端、测试等团队完成项目交付；\\n5. 编写和维护相关技术文档，保障代码质量和可维护性。\\n\\n**任职要求：**\\n1. 本科及以上学历，计算机、软件工程等相关专业优先；\\n2. 具备 Java 后端开发经验，熟悉 Java 基础、集合、多线程、JVM 等；\\n3. 熟悉 Spring Boot、Spring Cloud、MyBatis 等主流开发框架；\\n4. 熟悉 MySQL、Redis、消息队列等常用中间件；\\n5. 具备良好的代码规范意识和问题排查能力；\\n6. 有高并发、分布式系统、微服务项目经验者优先；\\n7. 具备良好的沟通能力、责任心和团队协作意识。\\n\\n**加分项：**\\n1. 熟悉 Linux、Docker、Kubernetes 等部署环境；\\n2. 有大型互联网平台、金融、电商、SaaS 系统开发经验；\\n3. 熟悉 Elasticsearch、Kafka、RocketMQ 等技术；\\n4. 了解 DevOps、CI/CD 流程；\\n5. 有开源项目或技术博客经验。\\n\\n**薪资待遇：**\\n薪资面议 / 根据经验能力定薪\\n\\n**工作地点：**\\n可填写具体城市或办公地址\\n\\n**投递方式：**\\n请发送简历至：HR 邮箱 / 联系人微信 / 招聘平台投递\\n\\n也可以根据实际情况补充：公司介绍、薪资范围、工作年限、学历要求、是否双休、五险一金、项目方向等信息。\"', NULL, NULL, NULL, '2026-07-27 14:22:20', 'inline-4b2bb1e2-d072-48c8-a95d-442e32e44d32', '2026-07-27 14:24:01', '2026-07-27 14:22:01', 0, 0, 0, 1, 'question_classifier-1', '{\"values\": {\"input\": \"招聘Java开发工程师\", \"query\": \"招聘Java开发工程师\", \"user_message\": \"招聘Java开发工程师\", \"conversationId\": \"13\", \"conversationHistory\": [{\"id\": 14, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-27T14:22:01\", \"messageId\": \"ff11ef82-8508-40cf-a6fd-28cae59c215d\", \"contentJson\": \"{\\\"input\\\": \\\"招聘Java开发工程师\\\", \\\"query\\\": \\\"招聘Java开发工程师\\\", \\\"user_message\\\": \\\"招聘Java开发工程师\\\", \\\"conversationId\\\": \\\"13\\\"}\", \"executionId\": \"4b2bb1e2-d072-48c8-a95d-442e32e44d32\", \"conversationId\": \"13\"}]}, \"visited\": [\"start-1\", \"question_classifier-1\", \"user_input-1\"], \"currentNodeId\": \"question_classifier-1\"}', NULL, NULL, NULL, NULL, NULL, 'PRODUCTION', '{\"legacy\": true, \"schema\": \"release-bundle-legacy\"}', 'a3d7de696663b8cb2166d22d5db4e3adda9e8ab0bec7ed933bfa7424cdf89adc', NULL, NULL, NULL, NULL, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (15, 4, '406a770b-4b76-45ff-b2d7-2b594247f925', 1003, NULL, NULL, NULL, NULL, NULL, 1009, 'APPLICATION_WORKFLOW', '2eda4ec6-7aa9-403e-b30f-0756d65d320a', 'SUCCEEDED', '{\"input\": \"招聘java开发\", \"query\": \"招聘java开发\", \"user_message\": \"招聘java开发\", \"conversationId\": \"14\"}', '\"您好！我理解您想招聘 Java 开发工程师。\\n\\n我可以帮您整理招聘 JD、候选人筛选标准、面试题、薪资范围或招聘渠道。先给您一版通用 JD：\\n\\n**Java 开发工程师**\\n\\n**岗位职责**\\n- 负责 Java 后端系统的设计、开发、测试和维护\\n- 参与业务需求分析、技术方案设计和接口开发\\n- 负责系统性能优化、问题排查和线上故障处理\\n- 与产品、前端、测试等团队协作，保障项目按时交付\\n- 编写和维护相关技术文档\\n\\n**任职要求**\\n- 本科及以上学历，计算机、软件工程等相关专业优先\\n- 熟悉 Java 基础、集合、多线程、JVM 等核心知识\\n- 熟悉 Spring Boot、Spring Cloud、MyBatis 等主流框架\\n- 熟悉 MySQL、Redis、消息队列等常用中间件\\n- 了解 Linux、Git、Maven/Gradle 等开发工具\\n- 有良好的代码习惯、沟通能力和问题定位能力\\n\\n**加分项**\\n- 有高并发、分布式系统、微服务项目经验\\n- 熟悉 Docker、Kubernetes、CI/CD\\n- 有电商、金融、SaaS、ERP、CRM 等业务系统经验\\n\\n**薪资示例**\\n- 初级：8k-15k/月\\n- 中级：15k-25k/月\\n- 高级：25k-40k+/月\\n\\n您可以告诉我城市、经验年限、薪资范围、是否外包/全职、技术栈要求，我可以继续帮您生成更精准的招聘文案。\"', NULL, NULL, NULL, '2026-07-28 11:49:48', 'inline-406a770b-4b76-45ff-b2d7-2b594247f925', '2026-07-28 11:51:31', '2026-07-28 11:49:31', 0, 0, 0, 1, 'question_classifier-1', '{\"values\": {\"input\": \"招聘java开发\", \"query\": \"招聘java开发\", \"user_message\": \"招聘java开发\", \"conversationId\": \"14\", \"conversationHistory\": [{\"id\": 16, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-28T11:49:30\", \"messageId\": \"a0cd8cc3-d84d-43d8-8074-40593121502d\", \"contentJson\": \"{\\\"input\\\": \\\"招聘java开发\\\", \\\"query\\\": \\\"招聘java开发\\\", \\\"user_message\\\": \\\"招聘java开发\\\", \\\"conversationId\\\": \\\"14\\\"}\", \"executionId\": \"406a770b-4b76-45ff-b2d7-2b594247f925\", \"conversationId\": \"14\"}]}, \"visited\": [\"start-1\", \"question_classifier-1\", \"user_input-1\"], \"currentNodeId\": \"question_classifier-1\"}', NULL, NULL, NULL, NULL, NULL, 'PRODUCTION', '{\"legacy\": true, \"schema\": \"release-bundle-legacy\"}', 'a3d7de696663b8cb2166d22d5db4e3adda9e8ab0bec7ed933bfa7424cdf89adc', NULL, NULL, NULL, NULL, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (16, 4, '25f65618-949c-4ddb-a2a7-ce6091994779', 1003, NULL, NULL, NULL, NULL, NULL, 1009, 'APPLICATION_WORKFLOW', 'studio-test-1785219494462', 'CANCELLED', '{\"request\": \"1\", \"runtimeMode\": \"CHAT\"}', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, 0, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'PRODUCTION', '{\"legacy\": true, \"schema\": \"release-bundle-legacy\"}', 'a3d7de696663b8cb2166d22d5db4e3adda9e8ab0bec7ed933bfa7424cdf89adc', NULL, NULL, NULL, NULL, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (17, 4, '5e68b551-fbf3-41f4-86b9-4d8723d13dd6', 1003, NULL, NULL, NULL, NULL, NULL, 1009, 'APPLICATION_WORKFLOW', 'conversation-1003-1785223273990', 'CANCELLED', '{\"input\": \"请返回一条简短的运行状态确认\", \"query\": \"请返回一条简短的运行状态确认\", \"user_message\": \"请返回一条简短的运行状态确认\"}', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, 0, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'PRODUCTION', '{\"legacy\": true, \"schema\": \"release-bundle-legacy\"}', 'a3d7de696663b8cb2166d22d5db4e3adda9e8ab0bec7ed933bfa7424cdf89adc', NULL, NULL, NULL, NULL, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (18, 4, 'daa2900e-4532-43a8-bdca-ceb70acb90cd', 1003, NULL, NULL, NULL, NULL, NULL, 1009, 'APPLICATION_WORKFLOW', '25f65618-949c-4ddb-a2a7-ce6091994779:REPLAY:1785224317291', 'FAILED', '{\"request\": \"1\", \"runtimeMode\": \"CHAT\"}', NULL, 'WORKER_TASK_FAILED', '恢复工作流执行失败：\r\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'daa2900e-4532-43a8-bdca-ceb70acb90cd-1\' for key \'platform_execution_event.uk_platform_execution_event\'\r\n### The error may exist in com/acme/agentstudio/infrastructure/persistence/mapper/PlatformExecutionEventMapper.java (best guess)\r\n### The error may involve com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper.insert-Inline\r\n### The error occurred while setting parameters\r\n### SQL: INSERT INTO platform_execution_event  ( tenant_id, execution_id,   span_id, task_id, application_id, version_id, status,    summary_json, node_id, event_type, sequence_no, payload_json, created_at )  VALUES (  ?, ?,   ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?, ?  )\r\n### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'daa2900e-4532-43a8-bdca-ceb70acb90cd-1\' for key \'platform_execution_event.uk_platform_execution_event\'\n; Duplicate entry \'daa2900e-4532-43a8-bdca-ceb70acb90cd-1\' for key \'platform_execution_event.uk_platform_execution_event\'', NULL, '2026-07-29 10:53:16', NULL, NULL, NULL, 0, 0, 2, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'PRODUCTION', '{\"legacy\": true, \"schema\": \"release-bundle-legacy\"}', 'a3d7de696663b8cb2166d22d5db4e3adda9e8ab0bec7ed933bfa7424cdf89adc', NULL, NULL, NULL, 9, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (19, 4, '7c984c88-d068-4da9-9a27-a42bbad17cca', 1003, NULL, NULL, NULL, NULL, NULL, 1009, 'APPLICATION_WORKFLOW', 'conversation-0e265dc4-86e2-45f0-ba25-9efcd4dc0519-1785289128190', 'SUCCEEDED', '{\"input\": \"1\", \"query\": \"1\", \"user_message\": \"1\"}', '\"I see the conversation memory, but I don’t see a task or question attached to it. What would you like me to do with this?\"', NULL, NULL, NULL, '2026-07-29 09:39:40', 'inline-7c984c88-d068-4da9-9a27-a42bbad17cca', '2026-07-29 09:40:50', '2026-07-29 09:38:50', 0, 0, 0, 1, 'question_classifier-1', '{\"input\": {\"input\": \"1\", \"query\": \"1\", \"user_message\": \"1\"}, \"model\": {}, \"output\": \"I see the conversation memory, but I don’t see a task or question attached to it. What would you like me to do with this?\", \"prompt\": {}, \"mappings\": {\"inputToVariable\": \"input\", \"outputFromVariable\": \"input\"}, \"variables\": {\"input\": \"1\", \"query\": \"1\", \"user_message\": \"1\", \"conversationHistory\": [{\"id\": 21, \"status\": \"ACTIVE\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:38:48\", \"messageId\": \"0e265dc4-86e2-45f0-ba25-9efcd4dc0519-USER-1785289127921\", \"contentJson\": \"1\", \"executionId\": null, \"conversationId\": \"0e265dc4-86e2-45f0-ba25-9efcd4dc0519\"}, {\"id\": 22, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:38:49\", \"messageId\": \"0e265dc4-86e2-45f0-ba25-9efcd4dc0519-1785289128190\", \"contentJson\": \"{\\\"input\\\": \\\"1\\\", \\\"query\\\": \\\"1\\\", \\\"user_message\\\": \\\"1\\\"}\", \"executionId\": \"7c984c88-d068-4da9-9a27-a42bbad17cca\", \"conversationId\": \"0e265dc4-86e2-45f0-ba25-9efcd4dc0519\"}], \"question_classifier-1.output\": \"I see the conversation memory, but I don’t see a task or question attached to it. What would you like me to do with this?\"}, \"visitedNodes\": [\"start-1\", \"question_classifier-1\", \"user_input-1\"], \"currentNodeId\": \"question_classifier-1\"}', NULL, NULL, NULL, NULL, NULL, 'PRODUCTION', '{\"legacy\": true, \"schema\": \"release-bundle-legacy\"}', 'a3d7de696663b8cb2166d22d5db4e3adda9e8ab0bec7ed933bfa7424cdf89adc', '557110e9-483d-4898-8c1d-7a96f35eaa7a', '557110e9-483d-4898-8c1d-7a96f35eaa7a', 'bb48138d-e661-4920-98ca-4ebb28597089', 7, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (20, 4, '3ffd7cfd-428b-4c18-94a2-59502b8669b0', 1003, NULL, NULL, NULL, NULL, NULL, 1009, 'APPLICATION_WORKFLOW', 'conversation-dfc72169-8ced-48c8-8f85-331953ae9157-1785289381071', 'SUCCEEDED', '{\"input\": \"你好\", \"query\": \"你好\", \"user_message\": \"你好\"}', '\"你好。你想让我基于这段 conversation memory 做什么？例如排查消息格式、提取对话内容、写查询 SQL，或者分析为什么上一轮没有正常响应。\"', NULL, NULL, NULL, '2026-07-29 09:44:35', 'inline-3ffd7cfd-428b-4c18-94a2-59502b8669b0', '2026-07-29 09:46:02', '2026-07-29 09:44:02', 0, 0, 0, 1, 'question_classifier-1', '{\"input\": {\"input\": \"你好\", \"query\": \"你好\", \"user_message\": \"你好\"}, \"model\": {}, \"output\": \"你好。你想让我基于这段 conversation memory 做什么？例如排查消息格式、提取对话内容、写查询 SQL，或者分析为什么上一轮没有正常响应。\", \"prompt\": {}, \"mappings\": {\"inputToVariable\": \"input\", \"outputFromVariable\": \"input\"}, \"variables\": {\"input\": \"你好\", \"query\": \"你好\", \"user_message\": \"你好\", \"conversationHistory\": [{\"id\": 24, \"status\": \"ACTIVE\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:42:54\", \"messageId\": \"69f815f5-21c3-4fd8-a05e-253bf869c066\", \"contentJson\": \"1\", \"executionId\": null, \"conversationId\": \"dfc72169-8ced-48c8-8f85-331953ae9157\"}, {\"id\": 25, \"status\": \"ACTIVE\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:42:55\", \"messageId\": \"abdcb7ce-9536-436e-a966-d5840d5320e0\", \"contentJson\": \"{\\\"input\\\": \\\"1\\\", \\\"query\\\": \\\"1\\\", \\\"user_message\\\": \\\"1\\\"}\", \"executionId\": null, \"conversationId\": \"dfc72169-8ced-48c8-8f85-331953ae9157\"}, {\"id\": 26, \"status\": \"ACTIVE\", \"roleCode\": \"ASSISTANT\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:42:55\", \"messageId\": \"785bf265-f09b-4c11-91b5-8ef40e8d386b\", \"contentJson\": \"\\\"I see the conversation memory, but I don’t see a task or question attached to it. What would you like me to do with this?\\\"\", \"executionId\": null, \"conversationId\": \"dfc72169-8ced-48c8-8f85-331953ae9157\"}, {\"id\": 28, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:44:01\", \"messageId\": \"dfc72169-8ced-48c8-8f85-331953ae9157-1785289381071\", \"contentJson\": \"{\\\"input\\\": \\\"你好\\\", \\\"query\\\": \\\"你好\\\", \\\"user_message\\\": \\\"你好\\\"}\", \"executionId\": \"3ffd7cfd-428b-4c18-94a2-59502b8669b0\", \"conversationId\": \"dfc72169-8ced-48c8-8f85-331953ae9157\"}], \"question_classifier-1.output\": \"你好。你想让我基于这段 conversation memory 做什么？例如排查消息格式、提取对话内容、写查询 SQL，或者分析为什么上一轮没有正常响应。\"}, \"visitedNodes\": [\"start-1\", \"question_classifier-1\", \"user_input-1\"], \"currentNodeId\": \"question_classifier-1\"}', NULL, NULL, NULL, NULL, NULL, 'PRODUCTION', '{\"legacy\": true, \"schema\": \"release-bundle-legacy\"}', 'a3d7de696663b8cb2166d22d5db4e3adda9e8ab0bec7ed933bfa7424cdf89adc', '3c933b38-0cb7-4676-adbc-72096776740f', '3c933b38-0cb7-4676-adbc-72096776740f', '2ec691f9-238b-4df9-a5b6-1f058b6b44e0', 8, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (21, 4, '4d86e902-e542-46ee-a172-544e83b6366c', 1018, NULL, NULL, NULL, NULL, NULL, NULL, 'APPLICATION_WORKFLOW', 'studio-test-1785294029170', 'FAILED', '{\"request\": \"查看风险\", \"runtimeMode\": \"CHAT\"}', NULL, 'WORKER_TASK_FAILED', '\r\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'4d86e902-e542-46ee-a172-544e83b6366c-2\' for key \'platform_execution_event.uk_platform_execution_event\'\r\n### The error may exist in com/acme/agentstudio/infrastructure/persistence/mapper/PlatformExecutionEventMapper.java (best guess)\r\n### The error may involve com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper.insert-Inline\r\n### The error occurred while setting parameters\r\n### SQL: INSERT INTO platform_execution_event  ( tenant_id, execution_id, request_id, trace_id, span_id, task_id, application_id,  status,  error_code, error_message, summary_json,  event_type, sequence_no, payload_json, created_at )  VALUES (  ?, ?, ?, ?, ?, ?, ?,  ?,  ?, ?, ?,  ?, ?, ?, ?  )\r\n### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'4d86e902-e542-46ee-a172-544e83b6366c-2\' for key \'platform_execution_event.uk_platform_execution_event\'\n; Duplicate entry \'4d86e902-e542-46ee-a172-544e83b6366c-2\' for key \'platform_execution_event.uk_platform_execution_event\'', NULL, '2026-07-29 11:02:06', NULL, NULL, NULL, 0, 0, 4, 1, NULL, NULL, NULL, NULL, 1016, '{\"edges\": [{\"edgeId\": \"e-start-reply\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"reply\"}, {\"edgeId\": \"e-reply-end\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"reply\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"x\": null, \"y\": null, \"title\": \"开始\", \"config\": {}, \"nodeId\": \"start\", \"nodeType\": \"START\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": null, \"y\": null, \"title\": \"生成结果\", \"config\": {\"goal\": \"回答企业用户的业务问题并给出清晰可执行的建议\", \"modelKey\": \"gpt-5.5\", \"inputName\": \"request\", \"outputName\": \"result\", \"contextSource\": \"none\", \"promptTemplate\": \"你是一名专业的企业业务助手。信息不足时明确说明，不要编造事实。\", \"messageTemplate\": \"{{input}}\"}, \"nodeId\": \"reply\", \"nodeType\": \"LLM\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": null, \"y\": null, \"title\": \"结束\", \"config\": {}, \"nodeId\": \"end\", \"nodeType\": \"END\", \"inputSchema\": {}, \"outputSchema\": {}}], \"graphType\": \"AGENT_APP\", \"variables\": [], \"inputSchema\": {\"type\": \"object\", \"required\": [\"request\"], \"properties\": {\"request\": {\"type\": \"string\"}}}, \"outputSchema\": {\"type\": \"object\", \"properties\": {\"result\": {\"type\": \"string\"}}}, \"schemaVersion\": \"1.0\"}', 1, 'DRAFT_TEST', NULL, NULL, '81e90124-a672-41f1-b3ce-736739f66b71', '81e90124-a672-41f1-b3ce-736739f66b71', '64278eae-a5d3-4563-bbdb-05572c984ef6', 10, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (22, 4, 'c9fc142d-b221-4627-b05e-a2982cb418db', 1018, NULL, NULL, NULL, NULL, NULL, NULL, 'APPLICATION_WORKFLOW', 'studio-test-1785294100687', 'SUCCEEDED', '{\"request\": \"测试风险\", \"runtimeMode\": \"CHAT\"}', '\"明白。我会以专业企业业务助手的方式回答，信息不足时会明确说明，不会编造事实。请直接告诉我你的需求。\"', NULL, NULL, NULL, '2026-07-29 11:01:46', 'inline-c9fc142d-b221-4627-b05e-a2982cb418db', '2026-07-29 11:03:46', '2026-07-29 11:01:46', 0, 0, 0, 1, 'end', '{\"input\": {\"request\": \"测试风险\", \"runtimeMode\": \"CHAT\"}, \"model\": {\"modelId\": \"\", \"modelKey\": \"gpt-5.5\"}, \"output\": \"明白。我会以专业企业业务助手的方式回答，信息不足时会明确说明，不会编造事实。请直接告诉我你的需求。\", \"prompt\": {\"template\": \"你是一名专业的企业业务助手。信息不足时明确说明，不要编造事实。\"}, \"mappings\": {\"inputToVariable\": \"input\", \"outputFromVariable\": \"reply.output\"}, \"variables\": {\"input\": \"测试风险\", \"query\": \"测试风险\", \"request\": \"测试风险\", \"runtimeMode\": \"CHAT\", \"reply.output\": \"明白。我会以专业企业业务助手的方式回答，信息不足时会明确说明，不会编造事实。请直接告诉我你的需求。\", \"user_message\": \"测试风险\", \"promptTemplate\": \"你是一名专业的企业业务助手。信息不足时明确说明，不要编造事实。\"}, \"visitedNodes\": [\"start\", \"end\", \"reply\"], \"currentNodeId\": \"end\"}', NULL, NULL, 1016, '{\"edges\": [{\"edgeId\": \"e-start-reply\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"reply\"}, {\"edgeId\": \"e-reply-end\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"reply\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"x\": null, \"y\": null, \"title\": \"开始\", \"config\": {}, \"nodeId\": \"start\", \"nodeType\": \"START\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": null, \"y\": null, \"title\": \"生成结果\", \"config\": {\"goal\": \"回答企业用户的业务问题并给出清晰可执行的建议\", \"modelKey\": \"gpt-5.5\", \"inputName\": \"request\", \"outputName\": \"result\", \"contextSource\": \"none\", \"promptTemplate\": \"你是一名专业的企业业务助手。信息不足时明确说明，不要编造事实。\", \"messageTemplate\": \"{{input}}\"}, \"nodeId\": \"reply\", \"nodeType\": \"LLM\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": null, \"y\": null, \"title\": \"结束\", \"config\": {}, \"nodeId\": \"end\", \"nodeType\": \"END\", \"inputSchema\": {}, \"outputSchema\": {}}], \"graphType\": \"AGENT_APP\", \"variables\": [], \"inputSchema\": {\"type\": \"object\", \"required\": [\"request\"], \"properties\": {\"request\": {\"type\": \"string\"}}}, \"outputSchema\": {\"type\": \"object\", \"properties\": {\"result\": {\"type\": \"string\"}}}, \"schemaVersion\": \"1.0\"}', 1, 'DRAFT_TEST', NULL, NULL, '5f54bb43-3ca5-4333-a245-48939cb2a583', '5f54bb43-3ca5-4333-a245-48939cb2a583', '295ef4c0-1ef8-4941-9305-6e00168d5d4f', 12, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (23, 4, '50f4ad41-595b-4aa5-8213-4aa63708ed33', 1019, NULL, NULL, NULL, NULL, NULL, NULL, 'APPLICATION_WORKFLOW', 'studio-test-1785295020788', 'SUCCEEDED', '{\"request\": \"请输出一条三期验收测试结果\", \"runtimeMode\": \"CHAT\"}', '\"我已准备好处理你的业务问题或任务。请提供具体输入、目标和约束，我会给出清晰、可执行的结果。\\n\"', NULL, NULL, NULL, '2026-07-29 11:17:14', 'inline-50f4ad41-595b-4aa5-8213-4aa63708ed33', '2026-07-29 11:19:13', '2026-07-29 11:17:13', 0, 0, 0, 1, 'end', '{\"input\": {\"request\": \"请输出一条三期验收测试结果\", \"runtimeMode\": \"CHAT\"}, \"model\": {\"modelId\": \"\", \"modelKey\": \"gpt-5.5\"}, \"output\": \"我已准备好处理你的业务问题或任务。请提供具体输入、目标和约束，我会给出清晰、可执行的结果。\\n\", \"prompt\": {\"template\": \"你是一名企业业务助手，请基于输入给出清晰、可执行的结果。\"}, \"mappings\": {\"inputToVariable\": \"input\", \"outputFromVariable\": \"reply.output\"}, \"variables\": {\"input\": \"请输出一条三期验收测试结果\", \"query\": \"请输出一条三期验收测试结果\", \"request\": \"请输出一条三期验收测试结果\", \"runtimeMode\": \"CHAT\", \"reply.output\": \"我已准备好处理你的业务问题或任务。请提供具体输入、目标和约束，我会给出清晰、可执行的结果。\\n\", \"user_message\": \"请输出一条三期验收测试结果\", \"promptTemplate\": \"你是一名企业业务助手，请基于输入给出清晰、可执行的结果。\"}, \"visitedNodes\": [\"start\", \"end\", \"reply\"], \"currentNodeId\": \"end\"}', NULL, NULL, 1017, '{\"edges\": [{\"edgeId\": \"e-start-reply\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"reply\"}, {\"edgeId\": \"e-reply-end\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"reply\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"x\": null, \"y\": null, \"title\": \"开始\", \"config\": {}, \"nodeId\": \"start\", \"nodeType\": \"START\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": null, \"y\": null, \"title\": \"生成结果\", \"config\": {\"goal\": \"验证三期应用从创建到运行的标准黄金路径\", \"modelKey\": \"gpt-5.5\", \"inputName\": \"request\", \"outputName\": \"result\", \"contextSource\": \"none\", \"promptTemplate\": \"你是一名企业业务助手，请基于输入给出清晰、可执行的结果。\", \"messageTemplate\": \"{{input}}\"}, \"nodeId\": \"reply\", \"nodeType\": \"LLM\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": null, \"y\": null, \"title\": \"结束\", \"config\": {}, \"nodeId\": \"end\", \"nodeType\": \"END\", \"inputSchema\": {}, \"outputSchema\": {}}], \"graphType\": \"AGENT_APP\", \"variables\": [], \"inputSchema\": {\"type\": \"object\", \"required\": [\"request\"], \"properties\": {\"request\": {\"type\": \"string\"}}}, \"outputSchema\": {\"type\": \"object\", \"properties\": {\"result\": {\"type\": \"string\"}}}, \"schemaVersion\": \"1.0\"}', 1, 'DRAFT_TEST', NULL, NULL, 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', '1efe1c89-9799-4818-b11b-a4aa4852386b', 13, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (24, 4, 'b6113ddf-9c1d-4270-896f-a399317c1f57', 1003, NULL, NULL, NULL, NULL, NULL, 1009, 'APPLICATION_WORKFLOW', 'conversation-aec2476a-9a01-476f-9c53-33954f9d6e0e-1785295191609', 'SUCCEEDED', '{\"input\": \"请返回一句简短的运行状态说明\", \"query\": \"请返回一句简短的运行状态说明\", \"user_message\": \"请返回一句简短的运行状态说明\"}', '\"系统运行正常。\"', NULL, NULL, NULL, '2026-07-29 11:19:56', 'inline-b6113ddf-9c1d-4270-896f-a399317c1f57', '2026-07-29 11:21:54', '2026-07-29 11:19:54', 0, 0, 0, 1, 'question_classifier-1', '{\"input\": {\"input\": \"请返回一句简短的运行状态说明\", \"query\": \"请返回一句简短的运行状态说明\", \"user_message\": \"请返回一句简短的运行状态说明\"}, \"model\": {}, \"output\": \"系统运行正常。\", \"prompt\": {}, \"mappings\": {\"inputToVariable\": \"input\", \"outputFromVariable\": \"input\"}, \"variables\": {\"input\": \"请返回一句简短的运行状态说明\", \"query\": \"请返回一句简短的运行状态说明\", \"user_message\": \"请返回一句简短的运行状态说明\", \"conversationHistory\": [{\"id\": 30, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T11:19:53\", \"messageId\": \"aec2476a-9a01-476f-9c53-33954f9d6e0e-1785295191609\", \"contentJson\": \"{\\\"input\\\": \\\"请返回一句简短的运行状态说明\\\", \\\"query\\\": \\\"请返回一句简短的运行状态说明\\\", \\\"user_message\\\": \\\"请返回一句简短的运行状态说明\\\"}\", \"executionId\": \"b6113ddf-9c1d-4270-896f-a399317c1f57\", \"conversationId\": \"aec2476a-9a01-476f-9c53-33954f9d6e0e\"}], \"question_classifier-1.output\": \"系统运行正常。\"}, \"visitedNodes\": [\"start-1\", \"question_classifier-1\", \"user_input-1\"], \"currentNodeId\": \"question_classifier-1\"}', NULL, NULL, NULL, NULL, NULL, 'PRODUCTION', '{\"legacy\": true, \"schema\": \"release-bundle-legacy\"}', 'a3d7de696663b8cb2166d22d5db4e3adda9e8ab0bec7ed933bfa7424cdf89adc', '41afb224-bca9-403f-8eab-da00ac7b58ec', '41afb224-bca9-403f-8eab-da00ac7b58ec', '76dde308-530b-49c3-af80-50928ef2e00b', 14, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (25, 4, 'cbe7ef48-01f9-4955-a3e6-aeff3c925efa', 1019, NULL, NULL, NULL, NULL, NULL, NULL, 'APPLICATION_WORKFLOW', 'studio-test-1785301546332', 'SUCCEEDED', '{\"request\": \"测试内容\"}', '\"好的。请提供你的具体业务问题、背景信息和期望输出形式（如方案、流程、表格、邮件、PPT大纲、分析报告等），我会基于输入给出清晰、可执行的结果。\"', NULL, NULL, NULL, '2026-07-29 13:05:53', 'inline-cbe7ef48-01f9-4955-a3e6-aeff3c925efa', '2026-07-29 13:07:53', '2026-07-29 13:05:53', 0, 0, 0, 1, 'end', '{\"input\": {\"request\": \"测试内容\"}, \"model\": {\"modelId\": \"\", \"modelKey\": \"gpt-5.5\"}, \"output\": \"好的。请提供你的具体业务问题、背景信息和期望输出形式（如方案、流程、表格、邮件、PPT大纲、分析报告等），我会基于输入给出清晰、可执行的结果。\", \"prompt\": {\"template\": \"你是一名企业业务助手，请基于输入给出清晰、可执行的结果。\"}, \"mappings\": {\"inputToVariable\": \"input\", \"outputFromVariable\": \"reply.output\"}, \"variables\": {\"input\": \"测试内容\", \"query\": \"测试内容\", \"request\": \"测试内容\", \"reply.output\": \"好的。请提供你的具体业务问题、背景信息和期望输出形式（如方案、流程、表格、邮件、PPT大纲、分析报告等），我会基于输入给出清晰、可执行的结果。\", \"user_message\": \"测试内容\", \"promptTemplate\": \"你是一名企业业务助手，请基于输入给出清晰、可执行的结果。\"}, \"visitedNodes\": [\"start\", \"end\", \"reply\"], \"currentNodeId\": \"end\"}', NULL, NULL, 1017, '{\"edges\": [{\"edgeId\": \"e-start-reply\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"reply\"}, {\"edgeId\": \"e-reply-end\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"reply\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"x\": null, \"y\": null, \"title\": \"开始\", \"config\": {}, \"nodeId\": \"start\", \"nodeType\": \"START\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": null, \"y\": null, \"title\": \"生成结果\", \"config\": {\"goal\": \"验证三期应用从创建到运行的标准黄金路径\", \"modelKey\": \"gpt-5.5\", \"inputName\": \"request\", \"outputName\": \"result\", \"contextSource\": \"none\", \"promptTemplate\": \"你是一名企业业务助手，请基于输入给出清晰、可执行的结果。\", \"messageTemplate\": \"{{input}}\"}, \"nodeId\": \"reply\", \"nodeType\": \"LLM\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": null, \"y\": null, \"title\": \"结束\", \"config\": {}, \"nodeId\": \"end\", \"nodeType\": \"END\", \"inputSchema\": {}, \"outputSchema\": {}}], \"graphType\": \"AGENT_APP\", \"variables\": [], \"inputSchema\": {\"type\": \"object\", \"required\": [\"request\"], \"properties\": {\"request\": {\"type\": \"string\"}}}, \"outputSchema\": {\"type\": \"object\", \"properties\": {\"result\": {\"type\": \"string\"}}}, \"schemaVersion\": \"1.0\"}', 1, 'DRAFT_TEST', NULL, NULL, 'd5014765-e40a-4fb4-bb18-dbe0249bb010', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', '287f2c0b-5561-4a5d-9f34-90470186c8d5', 15, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (26, 4, 'ee2ad645-c410-4464-8601-b5557e6432cb', 1024, NULL, NULL, NULL, NULL, NULL, NULL, 'APPLICATION_WORKFLOW', 'studio-test-1785314902299', 'SUCCEEDED', '{\"request\": \"部门统计\"}', '{\"input.request\": \"部门统计\", \"variables.input\": \"部门统计\", \"variables.query\": \"部门统计\", \"nodes.rag-1.output\": {\"context\": \"（无命中知识片段）\", \"citations\": []}, \"variables.rag_context\": \"（无命中知识片段）\", \"variables.rag_results\": [], \"variables.user_message\": \"部门统计\", \"variables.rag_hit_count\": 0}', NULL, NULL, NULL, '2026-07-29 16:48:26', 'inline-ee2ad645-c410-4464-8601-b5557e6432cb', '2026-07-29 16:50:25', '2026-07-29 16:48:25', 0, 0, 0, 1, 'end', '{\"input\": {\"request\": \"部门统计\"}, \"model\": {}, \"output\": {\"input.request\": \"部门统计\", \"variables.input\": \"部门统计\", \"variables.query\": \"部门统计\", \"nodes.rag-1.output\": {\"context\": \"（无命中知识片段）\", \"citations\": []}, \"variables.rag_context\": \"（无命中知识片段）\", \"variables.rag_results\": [], \"variables.user_message\": \"部门统计\", \"variables.rag_hit_count\": 0}, \"prompt\": {}, \"mappings\": {\"inputToVariable\": \"variables.input\", \"outputFromVariable\": \"variables.input\"}, \"variables\": {\"input.request\": \"部门统计\", \"variables.input\": \"部门统计\", \"variables.query\": \"部门统计\", \"nodes.rag-1.output\": {\"context\": \"（无命中知识片段）\", \"citations\": []}, \"variables.rag_context\": \"（无命中知识片段）\", \"variables.rag_results\": [], \"variables.user_message\": \"部门统计\", \"variables.rag_hit_count\": 0}, \"visitedNodes\": [\"start\", \"rag-1\", \"end\", \"user_input-1\"], \"currentNodeId\": \"end\"}', NULL, NULL, 1024, '{\"edges\": [{\"edgeId\": \"edge-start-user_input-1\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"user_input-1\"}, {\"edgeId\": \"edge-user_input-1-rag-1\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"user_input-1\", \"targetNodeId\": \"rag-1\"}, {\"edgeId\": \"edge-rag-1-end\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"rag-1\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"x\": 12.0, \"y\": 55.56, \"title\": \"开始\", \"config\": {}, \"nodeId\": \"start\", \"nodeType\": \"START\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 12.0, \"y\": 427.21, \"title\": \"结束\", \"config\": {}, \"nodeId\": \"end\", \"nodeType\": \"END\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 12.0, \"y\": 179.58, \"title\": \"用户输入\", \"config\": {}, \"nodeId\": \"user_input-1\", \"nodeType\": \"USER_INPUT\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 12.0, \"y\": 307.91, \"title\": \"知识检索\", \"config\": {\"topK\": 10, \"retrievalStrategy\": \"SCORE\", \"knowledgeDocumentId\": \"8\", \"knowledgeDocumentIds\": [\"6\"]}, \"nodeId\": \"rag-1\", \"nodeType\": \"RAG\", \"inputSchema\": {}, \"outputSchema\": {}}], \"graphType\": \"APPLICATION_WORKFLOW\", \"variables\": [{\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"start\"}, {\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"user_input-1\"}, {\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"rag-1\"}], \"inputSchema\": {\"type\": \"object\", \"required\": [\"request\"], \"properties\": {\"request\": {\"type\": \"string\"}}, \"description\": \"测试\"}, \"outputSchema\": {\"type\": \"object\", \"properties\": {\"result\": {\"type\": \"string\", \"format\": \"text\"}}}, \"schemaVersion\": \"1.0\"}', 2, 'DRAFT_TEST', NULL, NULL, '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', 'f4b7fae5-9bf9-4286-a302-5a0e86cf44d0', 16, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (27, 4, '8c8420c8-6a67-4b0d-9b0f-b5625c4aa9ad', 1024, NULL, NULL, NULL, NULL, NULL, NULL, 'APPLICATION_WORKFLOW', 'studio-test-1785314986521', 'SUCCEEDED', '{\"request\": \"部门\"}', '{\"input.request\": \"部门\", \"variables.input\": \"部门\", \"variables.query\": \"部门\", \"nodes.rag-1.output\": {\"context\": \"（无命中知识片段）\", \"citations\": []}, \"variables.rag_context\": \"（无命中知识片段）\", \"variables.rag_results\": [], \"variables.user_message\": \"部门\", \"variables.rag_hit_count\": 0}', NULL, NULL, NULL, '2026-07-29 16:49:49', 'inline-8c8420c8-6a67-4b0d-9b0f-b5625c4aa9ad', '2026-07-29 16:51:48', '2026-07-29 16:49:48', 0, 0, 0, 1, 'end', '{\"input\": {\"request\": \"部门\"}, \"model\": {}, \"output\": {\"input.request\": \"部门\", \"variables.input\": \"部门\", \"variables.query\": \"部门\", \"nodes.rag-1.output\": {\"context\": \"（无命中知识片段）\", \"citations\": []}, \"variables.rag_context\": \"（无命中知识片段）\", \"variables.rag_results\": [], \"variables.user_message\": \"部门\", \"variables.rag_hit_count\": 0}, \"prompt\": {}, \"mappings\": {\"inputToVariable\": \"variables.input\", \"outputFromVariable\": \"variables.input\"}, \"variables\": {\"input.request\": \"部门\", \"variables.input\": \"部门\", \"variables.query\": \"部门\", \"nodes.rag-1.output\": {\"context\": \"（无命中知识片段）\", \"citations\": []}, \"variables.rag_context\": \"（无命中知识片段）\", \"variables.rag_results\": [], \"variables.user_message\": \"部门\", \"variables.rag_hit_count\": 0}, \"visitedNodes\": [\"start\", \"rag-1\", \"end\"], \"currentNodeId\": \"end\"}', NULL, NULL, 1025, '{\"edges\": [{\"edgeId\": \"edge-rag-1-end\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"rag-1\", \"targetNodeId\": \"end\"}, {\"edgeId\": \"edge-start-rag-1\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"rag-1\"}], \"nodes\": [{\"x\": 12.0, \"y\": 55.56, \"title\": \"开始\", \"config\": {}, \"nodeId\": \"start\", \"nodeType\": \"START\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 12.0, \"y\": 427.21, \"title\": \"结束\", \"config\": {}, \"nodeId\": \"end\", \"nodeType\": \"END\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 12.0, \"y\": 307.91, \"title\": \"知识检索\", \"config\": {\"topK\": 10, \"retrievalStrategy\": \"SCORE\", \"knowledgeDocumentId\": \"8\", \"knowledgeDocumentIds\": [\"6\"]}, \"nodeId\": \"rag-1\", \"nodeType\": \"RAG\", \"inputSchema\": {}, \"outputSchema\": {}}], \"graphType\": \"APPLICATION_WORKFLOW\", \"variables\": [{\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"rag-1\"}, {\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"start\"}], \"inputSchema\": {\"type\": \"object\", \"required\": [\"request\"], \"properties\": {\"request\": {\"type\": \"string\"}}, \"description\": \"测试\"}, \"outputSchema\": {\"type\": \"object\", \"properties\": {\"result\": {\"type\": \"string\", \"format\": \"text\"}}}, \"schemaVersion\": \"1.0\"}', 3, 'DRAFT_TEST', NULL, NULL, '9bcdd0e1-0335-48e8-a6de-91ab046de32b', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', '7aa012d3-25c4-483f-ad8a-152e504307cd', 17, NULL, NULL, 1);
INSERT INTO `platform_execution_context` VALUES (28, 4, 'f3199e3e-305f-485a-9bd0-1902cf8d828f', 1026, NULL, 'TEST', 'REALTIME', 'STUDIO_TEST', NULL, NULL, 'APPLICATION_WORKFLOW', 'studio-test-1786069991698', 'SUCCEEDED', '{\"request\": \"cs\"}', '{\"input.request\": \"cs\", \"variables.input\": \"cs\", \"variables.query\": \"cs\", \"variables.message\": \"已收到：cs\", \"nodes.reply.output\": \"已收到：cs\", \"variables.user_message\": \"cs\"}', NULL, NULL, NULL, '2026-08-07 10:33:15', 'inline-f3199e3e-305f-485a-9bd0-1902cf8d828f', '2026-08-07 10:35:14', '2026-08-07 10:33:14', 0, 0, 0, 1, 'end', '{\"input\": {\"request\": \"cs\"}, \"model\": {}, \"output\": {\"input.request\": \"cs\", \"variables.input\": \"cs\", \"variables.query\": \"cs\", \"variables.message\": \"已收到：cs\", \"nodes.reply.output\": \"已收到：cs\", \"variables.user_message\": \"cs\"}, \"prompt\": {}, \"mappings\": {\"inputToVariable\": \"variables.input\", \"outputFromVariable\": \"variables.input\"}, \"variables\": {\"input.request\": \"cs\", \"variables.input\": \"cs\", \"variables.query\": \"cs\", \"variables.message\": \"已收到：cs\", \"nodes.reply.output\": \"已收到：cs\", \"variables.user_message\": \"cs\"}, \"visitedNodes\": [\"start\", \"user-input\", \"end\", \"reply\"], \"currentNodeId\": \"end\"}', NULL, NULL, 1032, '{\"edges\": [{\"edgeId\": \"edge-1\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"start\", \"targetNodeId\": \"user-input\"}, {\"edgeId\": \"edge-2\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"user-input\", \"targetNodeId\": \"reply\"}, {\"edgeId\": \"edge-3\", \"sourcePort\": \"default\", \"targetPort\": \"default\", \"sourceNodeId\": \"reply\", \"targetNodeId\": \"end\"}], \"nodes\": [{\"x\": 80.0, \"y\": 190.0, \"title\": \"开始\", \"config\": {}, \"nodeId\": \"start\", \"nodeType\": \"START\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 300.0, \"y\": 190.0, \"title\": \"接收用户消息\", \"config\": {}, \"nodeId\": \"user-input\", \"nodeType\": \"USER_INPUT\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 520.0, \"y\": 190.0, \"title\": \"返回答复\", \"config\": {\"messageTemplate\": \"已收到：{{variables.user_message}}\"}, \"nodeId\": \"reply\", \"nodeType\": \"DIRECT_REPLY\", \"inputSchema\": {}, \"outputSchema\": {}}, {\"x\": 740.0, \"y\": 190.0, \"title\": \"结束\", \"config\": {}, \"nodeId\": \"end\", \"nodeType\": \"END\", \"inputSchema\": {}, \"outputSchema\": {}}], \"graphType\": \"APPLICATION_WORKFLOW\", \"variables\": [{\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"start\"}, {\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"user-input\"}, {\"dataType\": \"any\", \"sensitive\": false, \"outputPath\": \"$.output\", \"sourceNodeId\": \"reply\"}], \"inputSchema\": {\"type\": \"object\", \"required\": [\"request\"], \"properties\": {\"request\": {\"type\": \"string\"}}, \"description\": \"验证应用发布前的配置和质量检查流程\"}, \"outputSchema\": {\"type\": \"object\", \"properties\": {\"result\": {\"type\": \"string\", \"format\": \"text\"}}}, \"schemaVersion\": \"1.0\"}', 4, 'DRAFT_TEST', NULL, NULL, 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', '4f1fc26f-b37b-4ef9-a23b-3fb2ebee230e', 57, NULL, NULL, 1);

-- ----------------------------
-- Table structure for platform_execution_event
-- ----------------------------
DROP TABLE IF EXISTS `platform_execution_event`;
CREATE TABLE `platform_execution_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `execution_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `node_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sequence_no` bigint NOT NULL,
  `payload_json` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `trace_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `span_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `task_id` bigint NULL DEFAULT NULL,
  `application_id` bigint NULL DEFAULT NULL,
  `version_id` bigint NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `duration_ms` bigint NULL DEFAULT NULL,
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `summary_json` json NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_execution_event`(`execution_id` ASC, `sequence_no` ASC) USING BTREE,
  INDEX `idx_platform_execution_event_lookup`(`tenant_id` ASC, `execution_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 226 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_execution_event
-- ----------------------------
INSERT INTO `platform_execution_event` VALUES (73, 4, 'd89edfdd-3547-4c2e-93d6-20049f485ac8', 'start-1', 'NODE_STARTED', 1, '{\"nodeType\": \"START\"}', '2026-07-27 10:13:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (74, 4, 'd89edfdd-3547-4c2e-93d6-20049f485ac8', 'start-1', 'NODE_SUCCEEDED', 2, '{\"output\": {\"input\": \"招聘Java开发工程师\", \"query\": \"招聘Java开发工程师\", \"user_message\": \"招聘Java开发工程师\", \"conversationId\": \"12\", \"conversationHistory\": [{\"id\": 12, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-27T10:12:59\", \"messageId\": \"c558403c-2933-4adf-9924-0ac20d2591ca\", \"contentJson\": \"{\\\"input\\\": \\\"招聘Java开发工程师\\\", \\\"query\\\": \\\"招聘Java开发工程师\\\", \\\"user_message\\\": \\\"招聘Java开发工程师\\\", \\\"conversationId\\\": \\\"12\\\"}\", \"executionId\": \"d89edfdd-3547-4c2e-93d6-20049f485ac8\", \"conversationId\": \"12\"}]}}', '2026-07-27 10:13:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (75, 4, 'd89edfdd-3547-4c2e-93d6-20049f485ac8', 'user_input-1', 'NODE_STARTED', 3, '{\"nodeType\": \"USER_INPUT\"}', '2026-07-27 10:13:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (76, 4, 'd89edfdd-3547-4c2e-93d6-20049f485ac8', 'user_input-1', 'NODE_SUCCEEDED', 4, '{\"output\": {\"input\": \"招聘Java开发工程师\", \"query\": \"招聘Java开发工程师\", \"user_message\": \"招聘Java开发工程师\", \"conversationId\": \"12\", \"conversationHistory\": [{\"id\": 12, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-27T10:12:59\", \"messageId\": \"c558403c-2933-4adf-9924-0ac20d2591ca\", \"contentJson\": \"{\\\"input\\\": \\\"招聘Java开发工程师\\\", \\\"query\\\": \\\"招聘Java开发工程师\\\", \\\"user_message\\\": \\\"招聘Java开发工程师\\\", \\\"conversationId\\\": \\\"12\\\"}\", \"executionId\": \"d89edfdd-3547-4c2e-93d6-20049f485ac8\", \"conversationId\": \"12\"}]}}', '2026-07-27 10:13:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (77, 4, 'd89edfdd-3547-4c2e-93d6-20049f485ac8', 'question_classifier-1', 'NODE_STARTED', 5, '{\"nodeType\": \"QUESTION_CLASSIFIER\"}', '2026-07-27 10:13:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (78, 4, 'd89edfdd-3547-4c2e-93d6-20049f485ac8', 'question_classifier-1', 'NODE_SUCCEEDED', 6, '{\"output\": \"下面是一份可直接发布的「Java开发工程师」招聘文案：\\n\\n**Java开发工程师**\\n\\n**岗位职责**\\n1. 负责公司业务系统的后端开发、接口设计与功能迭代。\\n2. 参与需求分析、技术方案设计、核心代码开发与性能优化。\\n3. 负责系统稳定性、可扩展性、安全性建设，处理线上问题。\\n4. 配合前端、测试、产品等团队完成项目交付。\\n5. 参与代码评审、技术文档编写及后端工程规范建设。\\n\\n**任职要求**\\n1. 本科及以上学历，计算机、软件工程等相关专业优先。\\n2. 具备 Java 后端开发经验，熟悉 Java 基础、多线程、集合、JVM 等。\\n3. 熟悉 Spring Boot、Spring Cloud、MyBatis/MyBatis-Plus 等主流框架。\\n4. 熟悉 MySQL、Redis，具备 SQL 优化和缓存设计经验。\\n5. 熟悉 RESTful API 设计，了解分布式系统、微服务架构者优先。\\n6. 熟悉 Git、Maven/Gradle、Linux 常用命令。\\n7. 具备良好的编码习惯、问题分析能力和团队协作能力。\\n\\n**加分项**\\n1. 有高并发、高可用系统开发经验。\\n2. 熟悉消息队列，如 Kafka、RabbitMQ、RocketMQ。\\n3. 熟悉 Docker、Kubernetes、CI/CD 流程。\\n4. 有电商、金融、SaaS、ERP、CRM 等业务系统经验。\\n\\n**薪资待遇**\\n薪资面议 / 具体根据经验与能力确定  \\n五险一金、绩效奖金、带薪年假、节日福利、技术成长空间\\n\\n**工作地点**\\n可填写：城市 / 办公地址 / 是否支持远程\\n\\n**招聘人数**\\n若干名\\n\\n也可以简化成一句招聘信息：\\n\\n现招聘 Java 开发工程师，要求熟悉 Java、Spring Boot、MySQL、Redis，有良好的代码能力和项目经验，薪资面议，欢迎投递简历。\"}', '2026-07-27 10:13:12', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (79, 4, '4b2bb1e2-d072-48c8-a95d-442e32e44d32', 'start-1', 'NODE_STARTED', 1, '{\"nodeType\": \"START\"}', '2026-07-27 14:22:01', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (80, 4, '4b2bb1e2-d072-48c8-a95d-442e32e44d32', 'start-1', 'NODE_SUCCEEDED', 2, '{\"output\": {\"input\": \"招聘Java开发工程师\", \"query\": \"招聘Java开发工程师\", \"user_message\": \"招聘Java开发工程师\", \"conversationId\": \"13\", \"conversationHistory\": [{\"id\": 14, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-27T14:22:01\", \"messageId\": \"ff11ef82-8508-40cf-a6fd-28cae59c215d\", \"contentJson\": \"{\\\"input\\\": \\\"招聘Java开发工程师\\\", \\\"query\\\": \\\"招聘Java开发工程师\\\", \\\"user_message\\\": \\\"招聘Java开发工程师\\\", \\\"conversationId\\\": \\\"13\\\"}\", \"executionId\": \"4b2bb1e2-d072-48c8-a95d-442e32e44d32\", \"conversationId\": \"13\"}]}}', '2026-07-27 14:22:01', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (81, 4, '4b2bb1e2-d072-48c8-a95d-442e32e44d32', 'user_input-1', 'NODE_STARTED', 3, '{\"nodeType\": \"USER_INPUT\"}', '2026-07-27 14:22:01', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (82, 4, '4b2bb1e2-d072-48c8-a95d-442e32e44d32', 'user_input-1', 'NODE_SUCCEEDED', 4, '{\"output\": {\"input\": \"招聘Java开发工程师\", \"query\": \"招聘Java开发工程师\", \"user_message\": \"招聘Java开发工程师\", \"conversationId\": \"13\", \"conversationHistory\": [{\"id\": 14, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-27T14:22:01\", \"messageId\": \"ff11ef82-8508-40cf-a6fd-28cae59c215d\", \"contentJson\": \"{\\\"input\\\": \\\"招聘Java开发工程师\\\", \\\"query\\\": \\\"招聘Java开发工程师\\\", \\\"user_message\\\": \\\"招聘Java开发工程师\\\", \\\"conversationId\\\": \\\"13\\\"}\", \"executionId\": \"4b2bb1e2-d072-48c8-a95d-442e32e44d32\", \"conversationId\": \"13\"}]}}', '2026-07-27 14:22:01', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (83, 4, '4b2bb1e2-d072-48c8-a95d-442e32e44d32', 'question_classifier-1', 'NODE_STARTED', 5, '{\"nodeType\": \"QUESTION_CLASSIFIER\"}', '2026-07-27 14:22:01', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (84, 4, '4b2bb1e2-d072-48c8-a95d-442e32e44d32', 'question_classifier-1', 'NODE_SUCCEEDED', 6, '{\"output\": \"以下是一份可直接发布的 Java 开发工程师招聘文案：\\n\\n**Java 开发工程师**\\n\\n**岗位职责：**\\n1. 参与公司核心业务系统的设计、开发、测试与维护；\\n2. 根据业务需求完成后端接口、服务模块及相关功能开发；\\n3. 参与系统架构优化、性能调优及稳定性建设；\\n4. 配合产品、前端、测试等团队完成项目交付；\\n5. 编写和维护相关技术文档，保障代码质量和可维护性。\\n\\n**任职要求：**\\n1. 本科及以上学历，计算机、软件工程等相关专业优先；\\n2. 具备 Java 后端开发经验，熟悉 Java 基础、集合、多线程、JVM 等；\\n3. 熟悉 Spring Boot、Spring Cloud、MyBatis 等主流开发框架；\\n4. 熟悉 MySQL、Redis、消息队列等常用中间件；\\n5. 具备良好的代码规范意识和问题排查能力；\\n6. 有高并发、分布式系统、微服务项目经验者优先；\\n7. 具备良好的沟通能力、责任心和团队协作意识。\\n\\n**加分项：**\\n1. 熟悉 Linux、Docker、Kubernetes 等部署环境；\\n2. 有大型互联网平台、金融、电商、SaaS 系统开发经验；\\n3. 熟悉 Elasticsearch、Kafka、RocketMQ 等技术；\\n4. 了解 DevOps、CI/CD 流程；\\n5. 有开源项目或技术博客经验。\\n\\n**薪资待遇：**\\n薪资面议 / 根据经验能力定薪\\n\\n**工作地点：**\\n可填写具体城市或办公地址\\n\\n**投递方式：**\\n请发送简历至：HR 邮箱 / 联系人微信 / 招聘平台投递\\n\\n也可以根据实际情况补充：公司介绍、薪资范围、工作年限、学历要求、是否双休、五险一金、项目方向等信息。\"}', '2026-07-27 14:22:20', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (85, 4, '4b2bb1e2-d072-48c8-a95d-442e32e44d32', NULL, 'EXECUTION_SUCCEEDED', 7, '{\"appId\": 1003, \"status\": \"SUCCEEDED\"}', '2026-07-27 14:22:20', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (86, 4, '406a770b-4b76-45ff-b2d7-2b594247f925', 'start-1', 'NODE_STARTED', 1, '{\"nodeType\": \"START\"}', '2026-07-28 11:49:30', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (87, 4, '406a770b-4b76-45ff-b2d7-2b594247f925', 'start-1', 'NODE_SUCCEEDED', 2, '{\"output\": {\"input\": \"招聘java开发\", \"query\": \"招聘java开发\", \"user_message\": \"招聘java开发\", \"conversationId\": \"14\", \"conversationHistory\": [{\"id\": 16, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-28T11:49:30\", \"messageId\": \"a0cd8cc3-d84d-43d8-8074-40593121502d\", \"contentJson\": \"{\\\"input\\\": \\\"招聘java开发\\\", \\\"query\\\": \\\"招聘java开发\\\", \\\"user_message\\\": \\\"招聘java开发\\\", \\\"conversationId\\\": \\\"14\\\"}\", \"executionId\": \"406a770b-4b76-45ff-b2d7-2b594247f925\", \"conversationId\": \"14\"}]}}', '2026-07-28 11:49:31', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (88, 4, '406a770b-4b76-45ff-b2d7-2b594247f925', 'user_input-1', 'NODE_STARTED', 3, '{\"nodeType\": \"USER_INPUT\"}', '2026-07-28 11:49:31', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (89, 4, '406a770b-4b76-45ff-b2d7-2b594247f925', 'user_input-1', 'NODE_SUCCEEDED', 4, '{\"output\": {\"input\": \"招聘java开发\", \"query\": \"招聘java开发\", \"user_message\": \"招聘java开发\", \"conversationId\": \"14\", \"conversationHistory\": [{\"id\": 16, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-28T11:49:30\", \"messageId\": \"a0cd8cc3-d84d-43d8-8074-40593121502d\", \"contentJson\": \"{\\\"input\\\": \\\"招聘java开发\\\", \\\"query\\\": \\\"招聘java开发\\\", \\\"user_message\\\": \\\"招聘java开发\\\", \\\"conversationId\\\": \\\"14\\\"}\", \"executionId\": \"406a770b-4b76-45ff-b2d7-2b594247f925\", \"conversationId\": \"14\"}]}}', '2026-07-28 11:49:31', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (90, 4, '406a770b-4b76-45ff-b2d7-2b594247f925', 'question_classifier-1', 'NODE_STARTED', 5, '{\"nodeType\": \"QUESTION_CLASSIFIER\"}', '2026-07-28 11:49:31', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (91, 4, '406a770b-4b76-45ff-b2d7-2b594247f925', 'question_classifier-1', 'NODE_SUCCEEDED', 6, '{\"output\": \"您好！我理解您想招聘 Java 开发工程师。\\n\\n我可以帮您整理招聘 JD、候选人筛选标准、面试题、薪资范围或招聘渠道。先给您一版通用 JD：\\n\\n**Java 开发工程师**\\n\\n**岗位职责**\\n- 负责 Java 后端系统的设计、开发、测试和维护\\n- 参与业务需求分析、技术方案设计和接口开发\\n- 负责系统性能优化、问题排查和线上故障处理\\n- 与产品、前端、测试等团队协作，保障项目按时交付\\n- 编写和维护相关技术文档\\n\\n**任职要求**\\n- 本科及以上学历，计算机、软件工程等相关专业优先\\n- 熟悉 Java 基础、集合、多线程、JVM 等核心知识\\n- 熟悉 Spring Boot、Spring Cloud、MyBatis 等主流框架\\n- 熟悉 MySQL、Redis、消息队列等常用中间件\\n- 了解 Linux、Git、Maven/Gradle 等开发工具\\n- 有良好的代码习惯、沟通能力和问题定位能力\\n\\n**加分项**\\n- 有高并发、分布式系统、微服务项目经验\\n- 熟悉 Docker、Kubernetes、CI/CD\\n- 有电商、金融、SaaS、ERP、CRM 等业务系统经验\\n\\n**薪资示例**\\n- 初级：8k-15k/月\\n- 中级：15k-25k/月\\n- 高级：25k-40k+/月\\n\\n您可以告诉我城市、经验年限、薪资范围、是否外包/全职、技术栈要求，我可以继续帮您生成更精准的招聘文案。\"}', '2026-07-28 11:49:48', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (92, 4, '406a770b-4b76-45ff-b2d7-2b594247f925', NULL, 'EXECUTION_SUCCEEDED', 7, '{\"appId\": 1003, \"status\": \"SUCCEEDED\"}', '2026-07-28 11:49:48', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (113, 4, '25f65618-949c-4ddb-a2a7-ce6091994779', NULL, 'EXECUTION_REPLAY_REQUESTED', 1, '{\"sourceExecutionId\": \"25f65618-949c-4ddb-a2a7-ce6091994779\"}', '2026-07-28 15:38:37', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `platform_execution_event` VALUES (126, 4, '7c984c88-d068-4da9-9a27-a42bbad17cca', 'start-1', 'NODE_STARTED', 1, '{\"nodeType\": \"START\"}', '2026-07-29 09:38:49', '557110e9-483d-4898-8c1d-7a96f35eaa7a', '557110e9-483d-4898-8c1d-7a96f35eaa7a', 'bed54ee2-4325-4b98-a337-978a68edf389', 7, 1003, 1009, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"START\"}');
INSERT INTO `platform_execution_event` VALUES (127, 4, '7c984c88-d068-4da9-9a27-a42bbad17cca', 'start-1', 'NODE_SUCCEEDED', 2, '{\"output\": {\"input\": \"1\", \"query\": \"1\", \"user_message\": \"1\", \"conversationHistory\": [{\"id\": 21, \"status\": \"ACTIVE\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:38:48\", \"messageId\": \"0e265dc4-86e2-45f0-ba25-9efcd4dc0519-USER-1785289127921\", \"contentJson\": \"1\", \"executionId\": null, \"conversationId\": \"0e265dc4-86e2-45f0-ba25-9efcd4dc0519\"}, {\"id\": 22, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:38:49\", \"messageId\": \"0e265dc4-86e2-45f0-ba25-9efcd4dc0519-1785289128190\", \"contentJson\": \"{\\\"input\\\": \\\"1\\\", \\\"query\\\": \\\"1\\\", \\\"user_message\\\": \\\"1\\\"}\", \"executionId\": \"7c984c88-d068-4da9-9a27-a42bbad17cca\", \"conversationId\": \"0e265dc4-86e2-45f0-ba25-9efcd4dc0519\"}]}}', '2026-07-29 09:38:49', '557110e9-483d-4898-8c1d-7a96f35eaa7a', '557110e9-483d-4898-8c1d-7a96f35eaa7a', 'bed54ee2-4325-4b98-a337-978a68edf389', 7, 1003, 1009, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (128, 4, '7c984c88-d068-4da9-9a27-a42bbad17cca', 'user_input-1', 'NODE_STARTED', 3, '{\"nodeType\": \"USER_INPUT\"}', '2026-07-29 09:38:49', '557110e9-483d-4898-8c1d-7a96f35eaa7a', '557110e9-483d-4898-8c1d-7a96f35eaa7a', 'bed54ee2-4325-4b98-a337-978a68edf389', 7, 1003, 1009, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"USER_INPUT\"}');
INSERT INTO `platform_execution_event` VALUES (129, 4, '7c984c88-d068-4da9-9a27-a42bbad17cca', 'user_input-1', 'NODE_SUCCEEDED', 4, '{\"output\": {\"input\": \"1\", \"query\": \"1\", \"user_message\": \"1\", \"conversationHistory\": [{\"id\": 21, \"status\": \"ACTIVE\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:38:48\", \"messageId\": \"0e265dc4-86e2-45f0-ba25-9efcd4dc0519-USER-1785289127921\", \"contentJson\": \"1\", \"executionId\": null, \"conversationId\": \"0e265dc4-86e2-45f0-ba25-9efcd4dc0519\"}, {\"id\": 22, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:38:49\", \"messageId\": \"0e265dc4-86e2-45f0-ba25-9efcd4dc0519-1785289128190\", \"contentJson\": \"{\\\"input\\\": \\\"1\\\", \\\"query\\\": \\\"1\\\", \\\"user_message\\\": \\\"1\\\"}\", \"executionId\": \"7c984c88-d068-4da9-9a27-a42bbad17cca\", \"conversationId\": \"0e265dc4-86e2-45f0-ba25-9efcd4dc0519\"}]}}', '2026-07-29 09:38:50', '557110e9-483d-4898-8c1d-7a96f35eaa7a', '557110e9-483d-4898-8c1d-7a96f35eaa7a', 'bed54ee2-4325-4b98-a337-978a68edf389', 7, 1003, 1009, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (130, 4, '7c984c88-d068-4da9-9a27-a42bbad17cca', 'question_classifier-1', 'NODE_STARTED', 5, '{\"nodeType\": \"QUESTION_CLASSIFIER\"}', '2026-07-29 09:38:50', '557110e9-483d-4898-8c1d-7a96f35eaa7a', '557110e9-483d-4898-8c1d-7a96f35eaa7a', 'bed54ee2-4325-4b98-a337-978a68edf389', 7, 1003, 1009, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"QUESTION_CLASSIFIER\"}');
INSERT INTO `platform_execution_event` VALUES (131, 4, '7c984c88-d068-4da9-9a27-a42bbad17cca', 'question_classifier-1', 'NODE_SUCCEEDED', 6, '{\"output\": \"I see the conversation memory, but I don’t see a task or question attached to it. What would you like me to do with this?\"}', '2026-07-29 09:39:40', '557110e9-483d-4898-8c1d-7a96f35eaa7a', '557110e9-483d-4898-8c1d-7a96f35eaa7a', 'bed54ee2-4325-4b98-a337-978a68edf389', 7, 1003, 1009, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (132, 4, '7c984c88-d068-4da9-9a27-a42bbad17cca', NULL, 'EXECUTION_SUCCEEDED', 7, '{\"appId\": 1003, \"status\": \"SUCCEEDED\"}', '2026-07-29 09:39:40', '557110e9-483d-4898-8c1d-7a96f35eaa7a', '557110e9-483d-4898-8c1d-7a96f35eaa7a', 'bed54ee2-4325-4b98-a337-978a68edf389', 7, 1003, 1009, 'SUCCEEDED', NULL, NULL, NULL, '{\"appId\": 1003, \"status\": \"SUCCEEDED\"}');
INSERT INTO `platform_execution_event` VALUES (140, 4, '3ffd7cfd-428b-4c18-94a2-59502b8669b0', 'start-1', 'NODE_STARTED', 1, '{\"nodeType\": \"START\"}', '2026-07-29 09:44:01', '3c933b38-0cb7-4676-adbc-72096776740f', '3c933b38-0cb7-4676-adbc-72096776740f', 'cbf0a1d4-ea49-40c5-a6fa-762a0d031a4e', 8, 1003, 1009, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"START\"}');
INSERT INTO `platform_execution_event` VALUES (141, 4, '3ffd7cfd-428b-4c18-94a2-59502b8669b0', 'start-1', 'NODE_SUCCEEDED', 2, '{\"output\": {\"input\": \"你好\", \"query\": \"你好\", \"user_message\": \"你好\", \"conversationHistory\": [{\"id\": 24, \"status\": \"ACTIVE\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:42:54\", \"messageId\": \"69f815f5-21c3-4fd8-a05e-253bf869c066\", \"contentJson\": \"1\", \"executionId\": null, \"conversationId\": \"dfc72169-8ced-48c8-8f85-331953ae9157\"}, {\"id\": 25, \"status\": \"ACTIVE\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:42:55\", \"messageId\": \"abdcb7ce-9536-436e-a966-d5840d5320e0\", \"contentJson\": \"{\\\"input\\\": \\\"1\\\", \\\"query\\\": \\\"1\\\", \\\"user_message\\\": \\\"1\\\"}\", \"executionId\": null, \"conversationId\": \"dfc72169-8ced-48c8-8f85-331953ae9157\"}, {\"id\": 26, \"status\": \"ACTIVE\", \"roleCode\": \"ASSISTANT\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:42:55\", \"messageId\": \"785bf265-f09b-4c11-91b5-8ef40e8d386b\", \"contentJson\": \"\\\"I see the conversation memory, but I don’t see a task or question attached to it. What would you like me to do with this?\\\"\", \"executionId\": null, \"conversationId\": \"dfc72169-8ced-48c8-8f85-331953ae9157\"}, {\"id\": 28, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:44:01\", \"messageId\": \"dfc72169-8ced-48c8-8f85-331953ae9157-1785289381071\", \"contentJson\": \"{\\\"input\\\": \\\"你好\\\", \\\"query\\\": \\\"你好\\\", \\\"user_message\\\": \\\"你好\\\"}\", \"executionId\": \"3ffd7cfd-428b-4c18-94a2-59502b8669b0\", \"conversationId\": \"dfc72169-8ced-48c8-8f85-331953ae9157\"}]}}', '2026-07-29 09:44:01', '3c933b38-0cb7-4676-adbc-72096776740f', '3c933b38-0cb7-4676-adbc-72096776740f', 'cbf0a1d4-ea49-40c5-a6fa-762a0d031a4e', 8, 1003, 1009, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (142, 4, '3ffd7cfd-428b-4c18-94a2-59502b8669b0', 'user_input-1', 'NODE_STARTED', 3, '{\"nodeType\": \"USER_INPUT\"}', '2026-07-29 09:44:01', '3c933b38-0cb7-4676-adbc-72096776740f', '3c933b38-0cb7-4676-adbc-72096776740f', 'cbf0a1d4-ea49-40c5-a6fa-762a0d031a4e', 8, 1003, 1009, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"USER_INPUT\"}');
INSERT INTO `platform_execution_event` VALUES (143, 4, '3ffd7cfd-428b-4c18-94a2-59502b8669b0', 'user_input-1', 'NODE_SUCCEEDED', 4, '{\"output\": {\"input\": \"你好\", \"query\": \"你好\", \"user_message\": \"你好\", \"conversationHistory\": [{\"id\": 24, \"status\": \"ACTIVE\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:42:54\", \"messageId\": \"69f815f5-21c3-4fd8-a05e-253bf869c066\", \"contentJson\": \"1\", \"executionId\": null, \"conversationId\": \"dfc72169-8ced-48c8-8f85-331953ae9157\"}, {\"id\": 25, \"status\": \"ACTIVE\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:42:55\", \"messageId\": \"abdcb7ce-9536-436e-a966-d5840d5320e0\", \"contentJson\": \"{\\\"input\\\": \\\"1\\\", \\\"query\\\": \\\"1\\\", \\\"user_message\\\": \\\"1\\\"}\", \"executionId\": null, \"conversationId\": \"dfc72169-8ced-48c8-8f85-331953ae9157\"}, {\"id\": 26, \"status\": \"ACTIVE\", \"roleCode\": \"ASSISTANT\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:42:55\", \"messageId\": \"785bf265-f09b-4c11-91b5-8ef40e8d386b\", \"contentJson\": \"\\\"I see the conversation memory, but I don’t see a task or question attached to it. What would you like me to do with this?\\\"\", \"executionId\": null, \"conversationId\": \"dfc72169-8ced-48c8-8f85-331953ae9157\"}, {\"id\": 28, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T09:44:01\", \"messageId\": \"dfc72169-8ced-48c8-8f85-331953ae9157-1785289381071\", \"contentJson\": \"{\\\"input\\\": \\\"你好\\\", \\\"query\\\": \\\"你好\\\", \\\"user_message\\\": \\\"你好\\\"}\", \"executionId\": \"3ffd7cfd-428b-4c18-94a2-59502b8669b0\", \"conversationId\": \"dfc72169-8ced-48c8-8f85-331953ae9157\"}]}}', '2026-07-29 09:44:02', '3c933b38-0cb7-4676-adbc-72096776740f', '3c933b38-0cb7-4676-adbc-72096776740f', 'cbf0a1d4-ea49-40c5-a6fa-762a0d031a4e', 8, 1003, 1009, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (144, 4, '3ffd7cfd-428b-4c18-94a2-59502b8669b0', 'question_classifier-1', 'NODE_STARTED', 5, '{\"nodeType\": \"QUESTION_CLASSIFIER\"}', '2026-07-29 09:44:02', '3c933b38-0cb7-4676-adbc-72096776740f', '3c933b38-0cb7-4676-adbc-72096776740f', 'cbf0a1d4-ea49-40c5-a6fa-762a0d031a4e', 8, 1003, 1009, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"QUESTION_CLASSIFIER\"}');
INSERT INTO `platform_execution_event` VALUES (145, 4, '3ffd7cfd-428b-4c18-94a2-59502b8669b0', 'question_classifier-1', 'NODE_SUCCEEDED', 6, '{\"output\": \"你好。你想让我基于这段 conversation memory 做什么？例如排查消息格式、提取对话内容、写查询 SQL，或者分析为什么上一轮没有正常响应。\"}', '2026-07-29 09:44:35', '3c933b38-0cb7-4676-adbc-72096776740f', '3c933b38-0cb7-4676-adbc-72096776740f', 'cbf0a1d4-ea49-40c5-a6fa-762a0d031a4e', 8, 1003, 1009, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (146, 4, '3ffd7cfd-428b-4c18-94a2-59502b8669b0', NULL, 'EXECUTION_SUCCEEDED', 7, '{\"appId\": 1003, \"status\": \"SUCCEEDED\"}', '2026-07-29 09:44:35', '3c933b38-0cb7-4676-adbc-72096776740f', '3c933b38-0cb7-4676-adbc-72096776740f', 'cbf0a1d4-ea49-40c5-a6fa-762a0d031a4e', 8, 1003, 1009, 'SUCCEEDED', NULL, NULL, NULL, '{\"appId\": 1003, \"status\": \"SUCCEEDED\"}');
INSERT INTO `platform_execution_event` VALUES (153, 4, 'daa2900e-4532-43a8-bdca-ceb70acb90cd', NULL, 'WORKER_TASK_RETRYING', 1, '{\"error\": \"恢复工作流执行失败：所有 LLM 候选模型均调用失败：text cannot be null or blank\", \"taskId\": 9, \"errorCode\": \"WORKER_TASK_RETRYING\", \"retryCount\": 1}', '2026-07-29 10:52:46', NULL, NULL, '1424a49b-bcc6-46f3-afe5-79b8427cd703', 9, 1003, 1009, 'RETRYING', NULL, NULL, NULL, '{\"error\": \"恢复工作流执行失败：所有 LLM 候选模型均调用失败：text cannot be null or blank\", \"taskId\": 9, \"errorCode\": \"WORKER_TASK_RETRYING\", \"retryCount\": 1}');
INSERT INTO `platform_execution_event` VALUES (156, 4, 'daa2900e-4532-43a8-bdca-ceb70acb90cd', NULL, 'WORKER_TASK_FAILED', 2, '{\"error\": \"恢复工作流执行失败：\\r\\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'daa2900e-4532-43a8-bdca-ceb70acb90cd-1\' for key \'platform_execution_event.uk_platform_execution_event\'\\r\\n### The error may exist in com/acme/agentstudio/infrastructure/persistence/mapper/PlatformExecutionEventMapper.java (best guess)\\r\\n### The error may involve com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO platform_execution_event  ( tenant_id, execution_id,   span_id, task_id, application_id, version_id, status,    summary_json, node_id, event_type, sequence_no, payload_json, created_at )  VALUES (  ?, ?,   ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?, ?  )\\r\\n### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'daa2900e-4532-43a8-bdca-ceb70acb90cd-1\' for key \'platform_execution_event.uk_platform_execution_event\'\\n; Duplicate entry \'daa2900e-4532-43a8-bdca-ceb70acb90cd-1\' for key \'platform_execution_event.uk_platform_execution_event\'\", \"taskId\": 9, \"errorCode\": \"WORKER_TASK_FAILED\", \"retryCount\": 2}', '2026-07-29 10:53:16', NULL, NULL, '68cdec68-5d5b-4241-907c-e20bbc9e2fbd', 9, 1003, 1009, 'FAILED', NULL, 'WORKER_TASK_FAILED', '恢复工作流执行失败：\r\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'daa2900e-4532-43a8-bdca-ceb70acb90cd-1\' for key \'platform_execution_event.uk_platform_execution_event\'\r\n### The error may exist in com/acme/agentstudio/infrastructure/persistence/mapper/PlatformExecutionEventMapper.java (best guess)\r\n### The error may involve com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper.insert-Inline\r\n### The error occurred while setting parameters\r\n### SQL: INSERT INTO platform_execution_event  ( tenant_id, execution_id,   span_id, task_id, application_id, version_id, status,    summary_json, node_id, event_type, sequence_no, payload_json, created_at )  VALUES (  ?, ?,   ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?, ?  )\r\n### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'daa2900e-4532-43a8-bdca-ceb70acb90cd-1\' for key \'platform_execution_event.uk_platform_execution_event\'\n; Duplicate entry \'daa2900e-4532-43a8-bdca-ceb70acb90cd-1\' for key \'platform_execution_event.uk_platform_execution_event\'', '{\"error\": \"恢复工作流执行失败：\\r\\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'daa2900e-4532-43a8-bdca-ceb70acb90cd-1\' for key \'platform_execution_event.uk_platform_execution_event\'\\r\\n### The error may exist in com/acme/agentstudio/infrastructure/persistence/mapper/PlatformExecutionEventMapper.java (best guess)\\r\\n### The error may involve com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO platform_execution_event  ( tenant_id, execution_id,   span_id, task_id, application_id, version_id, status,    summary_json, node_id, event_type, sequence_no, payload_json, created_at )  VALUES (  ?, ?,   ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?, ?  )\\r\\n### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'daa2900e-4532-43a8-bdca-ceb70acb90cd-1\' for key \'platform_execution_event.uk_platform_execution_event\'\\n; Duplicate entry \'daa2900e-4532-43a8-bdca-ceb70acb90cd-1\' for key \'platform_execution_event.uk_platform_execution_event\'\", \"taskId\": 9, \"errorCode\": \"WORKER_TASK_FAILED\", \"retryCount\": 2}');
INSERT INTO `platform_execution_event` VALUES (161, 4, '4d86e902-e542-46ee-a172-544e83b6366c', NULL, 'WORKER_TASK_RETRYING', 1, '{\"error\": \"所有 LLM 候选模型均调用失败：dev.ai4j.openai4j.OpenAiHttpException: {\\\"code\\\":\\\"GROUP_DISABLED\\\",\\\"message\\\":\\\"API Key 所属分组已停用\\\"}\", \"taskId\": 10, \"errorCode\": \"WORKER_TASK_RETRYING\", \"retryCount\": 1}', '2026-07-29 11:00:36', '81e90124-a672-41f1-b3ce-736739f66b71', '81e90124-a672-41f1-b3ce-736739f66b71', '00dada14-e14a-42b7-975f-8fd8610fc6d3', 10, 1018, NULL, 'RETRYING', NULL, NULL, NULL, '{\"error\": \"所有 LLM 候选模型均调用失败：dev.ai4j.openai4j.OpenAiHttpException: {\\\"code\\\":\\\"GROUP_DISABLED\\\",\\\"message\\\":\\\"API Key 所属分组已停用\\\"}\", \"taskId\": 10, \"errorCode\": \"WORKER_TASK_RETRYING\", \"retryCount\": 1}');
INSERT INTO `platform_execution_event` VALUES (165, 4, '4d86e902-e542-46ee-a172-544e83b6366c', NULL, 'WORKER_TASK_RETRYING', 2, '{\"error\": \"恢复工作流执行失败：执行上下文缺少版本主键：4d86e902-e542-46ee-a172-544e83b6366c\", \"taskId\": 11, \"errorCode\": \"WORKER_TASK_RETRYING\", \"retryCount\": 1}', '2026-07-29 11:01:30', '81e90124-a672-41f1-b3ce-736739f66b71', '81e90124-a672-41f1-b3ce-736739f66b71', 'b86d725a-1337-4771-b3fd-a53327637996', 11, 1018, NULL, 'RETRYING', NULL, NULL, NULL, '{\"error\": \"恢复工作流执行失败：执行上下文缺少版本主键：4d86e902-e542-46ee-a172-544e83b6366c\", \"taskId\": 11, \"errorCode\": \"WORKER_TASK_RETRYING\", \"retryCount\": 1}');
INSERT INTO `platform_execution_event` VALUES (169, 4, 'c9fc142d-b221-4627-b05e-a2982cb418db', 'start', 'NODE_STARTED', 1, '{\"nodeType\": \"START\"}', '2026-07-29 11:01:42', '5f54bb43-3ca5-4333-a245-48939cb2a583', '5f54bb43-3ca5-4333-a245-48939cb2a583', '3baf95aa-a080-4340-828f-d78f7defcb1f', 12, 1018, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"START\"}');
INSERT INTO `platform_execution_event` VALUES (170, 4, 'c9fc142d-b221-4627-b05e-a2982cb418db', 'start', 'NODE_SUCCEEDED', 2, '{\"output\": {\"input\": \"测试风险\", \"query\": \"测试风险\", \"request\": \"测试风险\", \"runtimeMode\": \"CHAT\", \"user_message\": \"测试风险\", \"promptTemplate\": \"你是一名专业的企业业务助手。信息不足时明确说明，不要编造事实。\"}}', '2026-07-29 11:01:42', '5f54bb43-3ca5-4333-a245-48939cb2a583', '5f54bb43-3ca5-4333-a245-48939cb2a583', '3baf95aa-a080-4340-828f-d78f7defcb1f', 12, 1018, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (171, 4, 'c9fc142d-b221-4627-b05e-a2982cb418db', 'reply', 'NODE_STARTED', 3, '{\"nodeType\": \"LLM\"}', '2026-07-29 11:01:42', '5f54bb43-3ca5-4333-a245-48939cb2a583', '5f54bb43-3ca5-4333-a245-48939cb2a583', '3baf95aa-a080-4340-828f-d78f7defcb1f', 12, 1018, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"LLM\"}');
INSERT INTO `platform_execution_event` VALUES (172, 4, 'c9fc142d-b221-4627-b05e-a2982cb418db', 'reply', 'NODE_SUCCEEDED', 4, '{\"output\": \"明白。我会以专业企业业务助手的方式回答，信息不足时会明确说明，不会编造事实。请直接告诉我你的需求。\"}', '2026-07-29 11:01:46', '5f54bb43-3ca5-4333-a245-48939cb2a583', '5f54bb43-3ca5-4333-a245-48939cb2a583', '3baf95aa-a080-4340-828f-d78f7defcb1f', 12, 1018, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (173, 4, 'c9fc142d-b221-4627-b05e-a2982cb418db', 'end', 'NODE_STARTED', 5, '{\"nodeType\": \"END\"}', '2026-07-29 11:01:46', '5f54bb43-3ca5-4333-a245-48939cb2a583', '5f54bb43-3ca5-4333-a245-48939cb2a583', '3baf95aa-a080-4340-828f-d78f7defcb1f', 12, 1018, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"END\"}');
INSERT INTO `platform_execution_event` VALUES (174, 4, 'c9fc142d-b221-4627-b05e-a2982cb418db', 'end', 'NODE_SUCCEEDED', 6, '{\"output\": \"明白。我会以专业企业业务助手的方式回答，信息不足时会明确说明，不会编造事实。请直接告诉我你的需求。\"}', '2026-07-29 11:01:46', '5f54bb43-3ca5-4333-a245-48939cb2a583', '5f54bb43-3ca5-4333-a245-48939cb2a583', '3baf95aa-a080-4340-828f-d78f7defcb1f', 12, 1018, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (175, 4, 'c9fc142d-b221-4627-b05e-a2982cb418db', NULL, 'EXECUTION_SUCCEEDED', 7, '{\"appId\": 1018, \"status\": \"SUCCEEDED\"}', '2026-07-29 11:01:46', '5f54bb43-3ca5-4333-a245-48939cb2a583', '5f54bb43-3ca5-4333-a245-48939cb2a583', '3baf95aa-a080-4340-828f-d78f7defcb1f', 12, 1018, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"appId\": 1018, \"status\": \"SUCCEEDED\"}');
INSERT INTO `platform_execution_event` VALUES (179, 4, '4d86e902-e542-46ee-a172-544e83b6366c', NULL, 'WORKER_TASK_FAILED', 3, '{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'4d86e902-e542-46ee-a172-544e83b6366c-2\' for key \'platform_execution_event.uk_platform_execution_event\'\\r\\n### The error may exist in com/acme/agentstudio/infrastructure/persistence/mapper/PlatformExecutionEventMapper.java (best guess)\\r\\n### The error may involve com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO platform_execution_event  ( tenant_id, execution_id, request_id, trace_id, span_id, task_id, application_id,  status,  error_code, error_message, summary_json,  event_type, sequence_no, payload_json, created_at )  VALUES (  ?, ?, ?, ?, ?, ?, ?,  ?,  ?, ?, ?,  ?, ?, ?, ?  )\\r\\n### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'4d86e902-e542-46ee-a172-544e83b6366c-2\' for key \'platform_execution_event.uk_platform_execution_event\'\\n; Duplicate entry \'4d86e902-e542-46ee-a172-544e83b6366c-2\' for key \'platform_execution_event.uk_platform_execution_event\'\", \"taskId\": 10, \"errorCode\": \"WORKER_TASK_FAILED\", \"retryCount\": 4}', '2026-07-29 11:02:06', '81e90124-a672-41f1-b3ce-736739f66b71', '81e90124-a672-41f1-b3ce-736739f66b71', 'd772f7ad-1975-4f25-b4e1-8ff6ed6f3e1b', 10, 1018, NULL, 'FAILED', NULL, 'WORKER_TASK_FAILED', '\r\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'4d86e902-e542-46ee-a172-544e83b6366c-2\' for key \'platform_execution_event.uk_platform_execution_event\'\r\n### The error may exist in com/acme/agentstudio/infrastructure/persistence/mapper/PlatformExecutionEventMapper.java (best guess)\r\n### The error may involve com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper.insert-Inline\r\n### The error occurred while setting parameters\r\n### SQL: INSERT INTO platform_execution_event  ( tenant_id, execution_id, request_id, trace_id, span_id, task_id, application_id,  status,  error_code, error_message, summary_json,  event_type, sequence_no, payload_json, created_at )  VALUES (  ?, ?, ?, ?, ?, ?, ?,  ?,  ?, ?, ?,  ?, ?, ?, ?  )\r\n### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'4d86e902-e542-46ee-a172-544e83b6366c-2\' for key \'platform_execution_event.uk_platform_execution_event\'\n; Duplicate entry \'4d86e902-e542-46ee-a172-544e83b6366c-2\' for key \'platform_execution_event.uk_platform_execution_event\'', '{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'4d86e902-e542-46ee-a172-544e83b6366c-2\' for key \'platform_execution_event.uk_platform_execution_event\'\\r\\n### The error may exist in com/acme/agentstudio/infrastructure/persistence/mapper/PlatformExecutionEventMapper.java (best guess)\\r\\n### The error may involve com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO platform_execution_event  ( tenant_id, execution_id, request_id, trace_id, span_id, task_id, application_id,  status,  error_code, error_message, summary_json,  event_type, sequence_no, payload_json, created_at )  VALUES (  ?, ?, ?, ?, ?, ?, ?,  ?,  ?, ?, ?,  ?, ?, ?, ?  )\\r\\n### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'4d86e902-e542-46ee-a172-544e83b6366c-2\' for key \'platform_execution_event.uk_platform_execution_event\'\\n; Duplicate entry \'4d86e902-e542-46ee-a172-544e83b6366c-2\' for key \'platform_execution_event.uk_platform_execution_event\'\", \"taskId\": 10, \"errorCode\": \"WORKER_TASK_FAILED\", \"retryCount\": 4}');
INSERT INTO `platform_execution_event` VALUES (180, 4, '50f4ad41-595b-4aa5-8213-4aa63708ed33', 'start', 'NODE_STARTED', 1, '{\"nodeType\": \"START\"}', '2026-07-29 11:17:02', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', '48d5e29c-5904-435c-a008-21797d632c69', 13, 1019, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"START\"}');
INSERT INTO `platform_execution_event` VALUES (181, 4, '50f4ad41-595b-4aa5-8213-4aa63708ed33', 'start', 'NODE_SUCCEEDED', 2, '{\"output\": {\"input\": \"请输出一条三期验收测试结果\", \"query\": \"请输出一条三期验收测试结果\", \"request\": \"请输出一条三期验收测试结果\", \"runtimeMode\": \"CHAT\", \"user_message\": \"请输出一条三期验收测试结果\", \"promptTemplate\": \"你是一名企业业务助手，请基于输入给出清晰、可执行的结果。\"}}', '2026-07-29 11:17:02', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', '48d5e29c-5904-435c-a008-21797d632c69', 13, 1019, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (182, 4, '50f4ad41-595b-4aa5-8213-4aa63708ed33', 'reply', 'NODE_STARTED', 3, '{\"nodeType\": \"LLM\"}', '2026-07-29 11:17:02', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', '48d5e29c-5904-435c-a008-21797d632c69', 13, 1019, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"LLM\"}');
INSERT INTO `platform_execution_event` VALUES (183, 4, '50f4ad41-595b-4aa5-8213-4aa63708ed33', 'reply', 'NODE_SUCCEEDED', 4, '{\"output\": \"我已准备好处理你的业务问题或任务。请提供具体输入、目标和约束，我会给出清晰、可执行的结果。\\n\"}', '2026-07-29 11:17:13', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', '48d5e29c-5904-435c-a008-21797d632c69', 13, 1019, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (184, 4, '50f4ad41-595b-4aa5-8213-4aa63708ed33', 'end', 'NODE_STARTED', 5, '{\"nodeType\": \"END\"}', '2026-07-29 11:17:13', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', '48d5e29c-5904-435c-a008-21797d632c69', 13, 1019, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"END\"}');
INSERT INTO `platform_execution_event` VALUES (185, 4, '50f4ad41-595b-4aa5-8213-4aa63708ed33', 'end', 'NODE_SUCCEEDED', 6, '{\"output\": \"我已准备好处理你的业务问题或任务。请提供具体输入、目标和约束，我会给出清晰、可执行的结果。\\n\"}', '2026-07-29 11:17:14', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', '48d5e29c-5904-435c-a008-21797d632c69', 13, 1019, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (186, 4, '50f4ad41-595b-4aa5-8213-4aa63708ed33', NULL, 'EXECUTION_SUCCEEDED', 7, '{\"appId\": 1019, \"status\": \"SUCCEEDED\"}', '2026-07-29 11:17:14', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', 'b79e42e1-c303-4f35-ae21-f85d06b95b1e', '48d5e29c-5904-435c-a008-21797d632c69', 13, 1019, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"appId\": 1019, \"status\": \"SUCCEEDED\"}');
INSERT INTO `platform_execution_event` VALUES (187, 4, 'b6113ddf-9c1d-4270-896f-a399317c1f57', 'start-1', 'NODE_STARTED', 1, '{\"nodeType\": \"START\"}', '2026-07-29 11:19:53', '41afb224-bca9-403f-8eab-da00ac7b58ec', '41afb224-bca9-403f-8eab-da00ac7b58ec', 'fc65ab74-88b1-4a2d-9a6f-905ce5293a11', 14, 1003, 1009, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"START\"}');
INSERT INTO `platform_execution_event` VALUES (188, 4, 'b6113ddf-9c1d-4270-896f-a399317c1f57', 'start-1', 'NODE_SUCCEEDED', 2, '{\"output\": {\"input\": \"请返回一句简短的运行状态说明\", \"query\": \"请返回一句简短的运行状态说明\", \"user_message\": \"请返回一句简短的运行状态说明\", \"conversationHistory\": [{\"id\": 30, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T11:19:53\", \"messageId\": \"aec2476a-9a01-476f-9c53-33954f9d6e0e-1785295191609\", \"contentJson\": \"{\\\"input\\\": \\\"请返回一句简短的运行状态说明\\\", \\\"query\\\": \\\"请返回一句简短的运行状态说明\\\", \\\"user_message\\\": \\\"请返回一句简短的运行状态说明\\\"}\", \"executionId\": \"b6113ddf-9c1d-4270-896f-a399317c1f57\", \"conversationId\": \"aec2476a-9a01-476f-9c53-33954f9d6e0e\"}]}}', '2026-07-29 11:19:53', '41afb224-bca9-403f-8eab-da00ac7b58ec', '41afb224-bca9-403f-8eab-da00ac7b58ec', 'fc65ab74-88b1-4a2d-9a6f-905ce5293a11', 14, 1003, 1009, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (189, 4, 'b6113ddf-9c1d-4270-896f-a399317c1f57', 'user_input-1', 'NODE_STARTED', 3, '{\"nodeType\": \"USER_INPUT\"}', '2026-07-29 11:19:53', '41afb224-bca9-403f-8eab-da00ac7b58ec', '41afb224-bca9-403f-8eab-da00ac7b58ec', 'fc65ab74-88b1-4a2d-9a6f-905ce5293a11', 14, 1003, 1009, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"USER_INPUT\"}');
INSERT INTO `platform_execution_event` VALUES (190, 4, 'b6113ddf-9c1d-4270-896f-a399317c1f57', 'user_input-1', 'NODE_SUCCEEDED', 4, '{\"output\": {\"input\": \"请返回一句简短的运行状态说明\", \"query\": \"请返回一句简短的运行状态说明\", \"user_message\": \"请返回一句简短的运行状态说明\", \"conversationHistory\": [{\"id\": 30, \"status\": \"COMPLETED\", \"roleCode\": \"USER\", \"tenantId\": 4, \"createdAt\": \"2026-07-29T11:19:53\", \"messageId\": \"aec2476a-9a01-476f-9c53-33954f9d6e0e-1785295191609\", \"contentJson\": \"{\\\"input\\\": \\\"请返回一句简短的运行状态说明\\\", \\\"query\\\": \\\"请返回一句简短的运行状态说明\\\", \\\"user_message\\\": \\\"请返回一句简短的运行状态说明\\\"}\", \"executionId\": \"b6113ddf-9c1d-4270-896f-a399317c1f57\", \"conversationId\": \"aec2476a-9a01-476f-9c53-33954f9d6e0e\"}]}}', '2026-07-29 11:19:54', '41afb224-bca9-403f-8eab-da00ac7b58ec', '41afb224-bca9-403f-8eab-da00ac7b58ec', 'fc65ab74-88b1-4a2d-9a6f-905ce5293a11', 14, 1003, 1009, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (191, 4, 'b6113ddf-9c1d-4270-896f-a399317c1f57', 'question_classifier-1', 'NODE_STARTED', 5, '{\"nodeType\": \"QUESTION_CLASSIFIER\"}', '2026-07-29 11:19:54', '41afb224-bca9-403f-8eab-da00ac7b58ec', '41afb224-bca9-403f-8eab-da00ac7b58ec', 'fc65ab74-88b1-4a2d-9a6f-905ce5293a11', 14, 1003, 1009, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"QUESTION_CLASSIFIER\"}');
INSERT INTO `platform_execution_event` VALUES (192, 4, 'b6113ddf-9c1d-4270-896f-a399317c1f57', 'question_classifier-1', 'NODE_SUCCEEDED', 6, '{\"output\": \"系统运行正常。\"}', '2026-07-29 11:19:56', '41afb224-bca9-403f-8eab-da00ac7b58ec', '41afb224-bca9-403f-8eab-da00ac7b58ec', 'fc65ab74-88b1-4a2d-9a6f-905ce5293a11', 14, 1003, 1009, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (193, 4, 'b6113ddf-9c1d-4270-896f-a399317c1f57', NULL, 'EXECUTION_SUCCEEDED', 7, '{\"appId\": 1003, \"status\": \"SUCCEEDED\"}', '2026-07-29 11:19:56', '41afb224-bca9-403f-8eab-da00ac7b58ec', '41afb224-bca9-403f-8eab-da00ac7b58ec', 'fc65ab74-88b1-4a2d-9a6f-905ce5293a11', 14, 1003, 1009, 'SUCCEEDED', NULL, NULL, NULL, '{\"appId\": 1003, \"status\": \"SUCCEEDED\"}');
INSERT INTO `platform_execution_event` VALUES (194, 4, 'cbe7ef48-01f9-4955-a3e6-aeff3c925efa', 'start', 'NODE_STARTED', 1, '{\"nodeType\": \"START\"}', '2026-07-29 13:05:48', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', '6ced5ea4-7715-4da2-87a7-cdc34cbfd35b', 15, 1019, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"START\"}');
INSERT INTO `platform_execution_event` VALUES (195, 4, 'cbe7ef48-01f9-4955-a3e6-aeff3c925efa', 'start', 'NODE_SUCCEEDED', 2, '{\"output\": {\"input\": \"测试内容\", \"query\": \"测试内容\", \"request\": \"测试内容\", \"user_message\": \"测试内容\", \"promptTemplate\": \"你是一名企业业务助手，请基于输入给出清晰、可执行的结果。\"}}', '2026-07-29 13:05:48', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', '6ced5ea4-7715-4da2-87a7-cdc34cbfd35b', 15, 1019, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (196, 4, 'cbe7ef48-01f9-4955-a3e6-aeff3c925efa', 'reply', 'NODE_STARTED', 3, '{\"nodeType\": \"LLM\"}', '2026-07-29 13:05:48', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', '6ced5ea4-7715-4da2-87a7-cdc34cbfd35b', 15, 1019, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"LLM\"}');
INSERT INTO `platform_execution_event` VALUES (197, 4, 'cbe7ef48-01f9-4955-a3e6-aeff3c925efa', 'reply', 'NODE_SUCCEEDED', 4, '{\"output\": \"好的。请提供你的具体业务问题、背景信息和期望输出形式（如方案、流程、表格、邮件、PPT大纲、分析报告等），我会基于输入给出清晰、可执行的结果。\"}', '2026-07-29 13:05:53', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', '6ced5ea4-7715-4da2-87a7-cdc34cbfd35b', 15, 1019, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (198, 4, 'cbe7ef48-01f9-4955-a3e6-aeff3c925efa', 'end', 'NODE_STARTED', 5, '{\"nodeType\": \"END\"}', '2026-07-29 13:05:53', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', '6ced5ea4-7715-4da2-87a7-cdc34cbfd35b', 15, 1019, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"END\"}');
INSERT INTO `platform_execution_event` VALUES (199, 4, 'cbe7ef48-01f9-4955-a3e6-aeff3c925efa', 'end', 'NODE_SUCCEEDED', 6, '{\"output\": \"好的。请提供你的具体业务问题、背景信息和期望输出形式（如方案、流程、表格、邮件、PPT大纲、分析报告等），我会基于输入给出清晰、可执行的结果。\"}', '2026-07-29 13:05:53', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', '6ced5ea4-7715-4da2-87a7-cdc34cbfd35b', 15, 1019, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (200, 4, 'cbe7ef48-01f9-4955-a3e6-aeff3c925efa', NULL, 'EXECUTION_SUCCEEDED', 7, '{\"appId\": 1019, \"status\": \"SUCCEEDED\"}', '2026-07-29 13:05:53', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', 'd5014765-e40a-4fb4-bb18-dbe0249bb010', '6ced5ea4-7715-4da2-87a7-cdc34cbfd35b', 15, 1019, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"appId\": 1019, \"status\": \"SUCCEEDED\"}');
INSERT INTO `platform_execution_event` VALUES (201, 4, 'ee2ad645-c410-4464-8601-b5557e6432cb', 'start', 'NODE_STARTED', 1, '{\"nodeType\": \"START\"}', '2026-07-29 16:48:24', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '1b8de721-96a9-4095-afba-a14491c13b02', 16, 1024, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"START\"}');
INSERT INTO `platform_execution_event` VALUES (202, 4, 'ee2ad645-c410-4464-8601-b5557e6432cb', 'start', 'NODE_SUCCEEDED', 2, '{\"output\": {\"input.request\": \"部门统计\", \"variables.input\": \"部门统计\", \"variables.query\": \"部门统计\", \"variables.user_message\": \"部门统计\"}}', '2026-07-29 16:48:24', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '1b8de721-96a9-4095-afba-a14491c13b02', 16, 1024, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (203, 4, 'ee2ad645-c410-4464-8601-b5557e6432cb', 'user_input-1', 'NODE_STARTED', 3, '{\"nodeType\": \"USER_INPUT\"}', '2026-07-29 16:48:24', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '1b8de721-96a9-4095-afba-a14491c13b02', 16, 1024, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"USER_INPUT\"}');
INSERT INTO `platform_execution_event` VALUES (204, 4, 'ee2ad645-c410-4464-8601-b5557e6432cb', 'user_input-1', 'NODE_SUCCEEDED', 4, '{\"output\": {\"input.request\": \"部门统计\", \"variables.input\": \"部门统计\", \"variables.query\": \"部门统计\", \"variables.user_message\": \"部门统计\"}}', '2026-07-29 16:48:24', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '1b8de721-96a9-4095-afba-a14491c13b02', 16, 1024, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (205, 4, 'ee2ad645-c410-4464-8601-b5557e6432cb', 'rag-1', 'NODE_STARTED', 5, '{\"nodeType\": \"RAG\"}', '2026-07-29 16:48:24', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '1b8de721-96a9-4095-afba-a14491c13b02', 16, 1024, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"RAG\"}');
INSERT INTO `platform_execution_event` VALUES (206, 4, 'ee2ad645-c410-4464-8601-b5557e6432cb', 'rag-1', 'NODE_SUCCEEDED', 6, '{\"output\": {\"context\": \"（无命中知识片段）\", \"citations\": []}}', '2026-07-29 16:48:25', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '1b8de721-96a9-4095-afba-a14491c13b02', 16, 1024, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (207, 4, 'ee2ad645-c410-4464-8601-b5557e6432cb', 'end', 'NODE_STARTED', 7, '{\"nodeType\": \"END\"}', '2026-07-29 16:48:25', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '1b8de721-96a9-4095-afba-a14491c13b02', 16, 1024, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"END\"}');
INSERT INTO `platform_execution_event` VALUES (208, 4, 'ee2ad645-c410-4464-8601-b5557e6432cb', 'end', 'NODE_SUCCEEDED', 8, '{\"output\": {\"input.request\": \"部门统计\", \"variables.input\": \"部门统计\", \"variables.query\": \"部门统计\", \"nodes.rag-1.output\": {\"context\": \"（无命中知识片段）\", \"citations\": []}, \"variables.rag_context\": \"（无命中知识片段）\", \"variables.rag_results\": [], \"variables.user_message\": \"部门统计\", \"variables.rag_hit_count\": 0}}', '2026-07-29 16:48:25', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '1b8de721-96a9-4095-afba-a14491c13b02', 16, 1024, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (209, 4, 'ee2ad645-c410-4464-8601-b5557e6432cb', NULL, 'EXECUTION_SUCCEEDED', 9, '{\"appId\": 1024, \"status\": \"SUCCEEDED\"}', '2026-07-29 16:48:26', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '25a1bb1f-bd3f-4269-b4f2-ba27d617d8f8', '1b8de721-96a9-4095-afba-a14491c13b02', 16, 1024, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"appId\": 1024, \"status\": \"SUCCEEDED\"}');
INSERT INTO `platform_execution_event` VALUES (210, 4, '8c8420c8-6a67-4b0d-9b0f-b5625c4aa9ad', 'start', 'NODE_STARTED', 1, '{\"nodeType\": \"START\"}', '2026-07-29 16:49:48', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', 'd7535d03-bfdf-432c-9deb-7750fb06dc06', 17, 1024, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"START\"}');
INSERT INTO `platform_execution_event` VALUES (211, 4, '8c8420c8-6a67-4b0d-9b0f-b5625c4aa9ad', 'start', 'NODE_SUCCEEDED', 2, '{\"output\": {\"input.request\": \"部门\", \"variables.input\": \"部门\", \"variables.query\": \"部门\", \"variables.user_message\": \"部门\"}}', '2026-07-29 16:49:48', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', 'd7535d03-bfdf-432c-9deb-7750fb06dc06', 17, 1024, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (212, 4, '8c8420c8-6a67-4b0d-9b0f-b5625c4aa9ad', 'rag-1', 'NODE_STARTED', 3, '{\"nodeType\": \"RAG\"}', '2026-07-29 16:49:48', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', 'd7535d03-bfdf-432c-9deb-7750fb06dc06', 17, 1024, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"RAG\"}');
INSERT INTO `platform_execution_event` VALUES (213, 4, '8c8420c8-6a67-4b0d-9b0f-b5625c4aa9ad', 'rag-1', 'NODE_SUCCEEDED', 4, '{\"output\": {\"context\": \"（无命中知识片段）\", \"citations\": []}}', '2026-07-29 16:49:48', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', 'd7535d03-bfdf-432c-9deb-7750fb06dc06', 17, 1024, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (214, 4, '8c8420c8-6a67-4b0d-9b0f-b5625c4aa9ad', 'end', 'NODE_STARTED', 5, '{\"nodeType\": \"END\"}', '2026-07-29 16:49:49', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', 'd7535d03-bfdf-432c-9deb-7750fb06dc06', 17, 1024, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"END\"}');
INSERT INTO `platform_execution_event` VALUES (215, 4, '8c8420c8-6a67-4b0d-9b0f-b5625c4aa9ad', 'end', 'NODE_SUCCEEDED', 6, '{\"output\": {\"input.request\": \"部门\", \"variables.input\": \"部门\", \"variables.query\": \"部门\", \"nodes.rag-1.output\": {\"context\": \"（无命中知识片段）\", \"citations\": []}, \"variables.rag_context\": \"（无命中知识片段）\", \"variables.rag_results\": [], \"variables.user_message\": \"部门\", \"variables.rag_hit_count\": 0}}', '2026-07-29 16:49:49', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', 'd7535d03-bfdf-432c-9deb-7750fb06dc06', 17, 1024, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (216, 4, '8c8420c8-6a67-4b0d-9b0f-b5625c4aa9ad', NULL, 'EXECUTION_SUCCEEDED', 7, '{\"appId\": 1024, \"status\": \"SUCCEEDED\"}', '2026-07-29 16:49:49', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', '9bcdd0e1-0335-48e8-a6de-91ab046de32b', 'd7535d03-bfdf-432c-9deb-7750fb06dc06', 17, 1024, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"appId\": 1024, \"status\": \"SUCCEEDED\"}');
INSERT INTO `platform_execution_event` VALUES (217, 4, 'f3199e3e-305f-485a-9bd0-1902cf8d828f', 'start', 'NODE_STARTED', 1, '{\"nodeType\": \"START\"}', '2026-08-07 10:33:14', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', '78ea3f76-3e6a-4ce2-91d1-14a9db2af11e', 57, 1026, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"START\"}');
INSERT INTO `platform_execution_event` VALUES (218, 4, 'f3199e3e-305f-485a-9bd0-1902cf8d828f', 'start', 'NODE_SUCCEEDED', 2, '{\"output\": {\"input.request\": \"cs\", \"variables.input\": \"cs\", \"variables.query\": \"cs\", \"variables.user_message\": \"cs\"}}', '2026-08-07 10:33:14', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', '78ea3f76-3e6a-4ce2-91d1-14a9db2af11e', 57, 1026, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (219, 4, 'f3199e3e-305f-485a-9bd0-1902cf8d828f', 'user-input', 'NODE_STARTED', 3, '{\"nodeType\": \"USER_INPUT\"}', '2026-08-07 10:33:14', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', '78ea3f76-3e6a-4ce2-91d1-14a9db2af11e', 57, 1026, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"USER_INPUT\"}');
INSERT INTO `platform_execution_event` VALUES (220, 4, 'f3199e3e-305f-485a-9bd0-1902cf8d828f', 'user-input', 'NODE_SUCCEEDED', 4, '{\"output\": {\"input.request\": \"cs\", \"variables.input\": \"cs\", \"variables.query\": \"cs\", \"variables.user_message\": \"cs\"}}', '2026-08-07 10:33:14', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', '78ea3f76-3e6a-4ce2-91d1-14a9db2af11e', 57, 1026, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (221, 4, 'f3199e3e-305f-485a-9bd0-1902cf8d828f', 'reply', 'NODE_STARTED', 5, '{\"nodeType\": \"DIRECT_REPLY\"}', '2026-08-07 10:33:14', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', '78ea3f76-3e6a-4ce2-91d1-14a9db2af11e', 57, 1026, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"DIRECT_REPLY\"}');
INSERT INTO `platform_execution_event` VALUES (222, 4, 'f3199e3e-305f-485a-9bd0-1902cf8d828f', 'reply', 'NODE_SUCCEEDED', 6, '{\"output\": \"已收到：cs\"}', '2026-08-07 10:33:14', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', '78ea3f76-3e6a-4ce2-91d1-14a9db2af11e', 57, 1026, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (223, 4, 'f3199e3e-305f-485a-9bd0-1902cf8d828f', 'end', 'NODE_STARTED', 7, '{\"nodeType\": \"END\"}', '2026-08-07 10:33:14', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', '78ea3f76-3e6a-4ce2-91d1-14a9db2af11e', 57, 1026, NULL, 'RUNNING', NULL, NULL, NULL, '{\"nodeType\": \"END\"}');
INSERT INTO `platform_execution_event` VALUES (224, 4, 'f3199e3e-305f-485a-9bd0-1902cf8d828f', 'end', 'NODE_SUCCEEDED', 8, '{\"output\": {\"input.request\": \"cs\", \"variables.input\": \"cs\", \"variables.query\": \"cs\", \"variables.message\": \"已收到：cs\", \"nodes.reply.output\": \"已收到：cs\", \"variables.user_message\": \"cs\"}}', '2026-08-07 10:33:15', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', '78ea3f76-3e6a-4ce2-91d1-14a9db2af11e', 57, 1026, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"output\": \"*** 已脱敏 ***\"}');
INSERT INTO `platform_execution_event` VALUES (225, 4, 'f3199e3e-305f-485a-9bd0-1902cf8d828f', NULL, 'EXECUTION_SUCCEEDED', 9, '{\"appId\": 1026, \"status\": \"SUCCEEDED\"}', '2026-08-07 10:33:15', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', 'a307af8d-9b0b-4a4b-9ee5-43bbf9307ccb', '78ea3f76-3e6a-4ce2-91d1-14a9db2af11e', 57, 1026, NULL, 'SUCCEEDED', NULL, NULL, NULL, '{\"appId\": 1026, \"status\": \"SUCCEEDED\"}');

-- ----------------------------
-- Table structure for platform_price_version
-- ----------------------------
DROP TABLE IF EXISTS `platform_price_version`;
CREATE TABLE `platform_price_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NULL DEFAULT NULL,
  `model_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `input_price` decimal(20, 8) NOT NULL DEFAULT 0.00000000,
  `output_price` decimal(20, 8) NOT NULL DEFAULT 0.00000000,
  `currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CNY',
  `valid_from` datetime NOT NULL,
  `valid_until` datetime NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_platform_price_lookup`(`tenant_id` ASC, `model_code` ASC, `valid_from` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_price_version
-- ----------------------------

-- ----------------------------
-- Table structure for platform_tool_connector
-- ----------------------------
DROP TABLE IF EXISTS `platform_tool_connector`;
CREATE TABLE `platform_tool_connector`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `connector_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `connector_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `endpoint` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `credential_ref_id` bigint NULL DEFAULT NULL,
  `timeout_ms` int NOT NULL DEFAULT 30000,
  `retry_policy_json` json NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_connector`(`tenant_id` ASC, `connector_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_tool_connector
-- ----------------------------
INSERT INTO `platform_tool_connector` VALUES (1, 2, 'http-default', 'HTTP', NULL, NULL, 30000, '{\"backoffMs\": 0, \"maxAttempts\": 1}', 'DRAFT', 2, '2026-07-23 15:04:57');
INSERT INTO `platform_tool_connector` VALUES (2, 2, 'openapi-default', 'OPENAPI', NULL, NULL, 30000, '{\"backoffMs\": 500, \"maxAttempts\": 2}', 'DRAFT', 2, '2026-07-23 15:04:57');
INSERT INTO `platform_tool_connector` VALUES (3, 2, 'mcp-default', 'MCP', NULL, NULL, 30000, '{\"backoffMs\": 0, \"maxAttempts\": 1}', 'DRAFT', 2, '2026-07-23 15:04:57');
INSERT INTO `platform_tool_connector` VALUES (4, 2, 'code-sandbox-default', 'CODE_SANDBOX', NULL, NULL, 30000, '{\"backoffMs\": 0, \"maxAttempts\": 1}', 'DRAFT', 2, '2026-07-23 15:04:57');
INSERT INTO `platform_tool_connector` VALUES (5, 2, 'webhook-default', 'WEBHOOK', NULL, NULL, 30000, '{\"backoffMs\": 0, \"maxAttempts\": 1}', 'DRAFT', 2, '2026-07-23 15:04:57');
INSERT INTO `platform_tool_connector` VALUES (6, 2, 'internal-api-default', 'INTERNAL_API', NULL, NULL, 30000, '{\"backoffMs\": 500, \"maxAttempts\": 2}', 'DRAFT', 2, '2026-07-23 15:04:57');
INSERT INTO `platform_tool_connector` VALUES (7, 2, 'web-crawler-default', 'WEB_CRAWLER', NULL, NULL, 60000, '{\"backoffMs\": 0, \"maxAttempts\": 1}', 'DRAFT', 2, '2026-07-23 15:04:57');

-- ----------------------------
-- Table structure for platform_tool_invocation
-- ----------------------------
DROP TABLE IF EXISTS `platform_tool_invocation`;
CREATE TABLE `platform_tool_invocation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `execution_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `connector_id` bigint NOT NULL,
  `request_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `response_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `status_code` int NULL DEFAULT NULL,
  `result_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `latency_ms` bigint NOT NULL DEFAULT 0,
  `attempt_count` int NOT NULL DEFAULT 1,
  `error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_platform_tool_invocation`(`tenant_id` ASC, `connector_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_tool_invocation
-- ----------------------------

-- ----------------------------
-- Table structure for rag_embedding_profile
-- ----------------------------
DROP TABLE IF EXISTS `rag_embedding_profile`;
CREATE TABLE `rag_embedding_profile`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `profile_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `profile_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `embedding_model_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `reranker_model_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `query_rewrite_model_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `vector_dimension` int NOT NULL,
  `distance_metric` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'COSINE',
  `candidate_limit` int NOT NULL DEFAULT 80,
  `rerank_threshold` double NOT NULL DEFAULT 0.35,
  `no_hit_threshold` double NOT NULL DEFAULT 0.2,
  `score_gap_threshold` double NOT NULL DEFAULT 0.03,
  `max_context_tokens` int NOT NULL DEFAULT 6000,
  `max_documents` int NOT NULL DEFAULT 5,
  `max_parent_chunks` int NOT NULL DEFAULT 8,
  `per_document_context_limit` int NOT NULL DEFAULT 3,
  `query_rewrite_enabled` tinyint NOT NULL DEFAULT 0,
  `late_interaction_enabled` tinyint NOT NULL DEFAULT 0,
  `degrade_policy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'VECTOR_ONLY',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `version_no` int NOT NULL DEFAULT 1,
  `is_active` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `embedding_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'LOCAL',
  `embedding_model_id` bigint NULL DEFAULT NULL,
  `supported_languages` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ZH,EN,OTHER',
  `cross_language_enabled` tinyint NOT NULL DEFAULT 0,
  `model_version` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `query_instruction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `document_instruction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `reranker_enabled` tinyint NOT NULL DEFAULT 0,
  `reranker_candidate_limit` int NOT NULL DEFAULT 30,
  `reranker_timeout_seconds` int NOT NULL DEFAULT 30,
  `reranker_budget` double NOT NULL DEFAULT 0,
  `vector_recall_weight` double NOT NULL DEFAULT 0.6,
  `lexical_recall_weight` double NOT NULL DEFAULT 0.4,
  `exact_recall_boost` double NOT NULL DEFAULT 0.25,
  `supported_scripts` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'HAN,LATIN,OTHER',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rag_profile_code`(`tenant_id` ASC, `profile_code` ASC) USING BTREE,
  INDEX `idx_rag_profile_active`(`tenant_id` ASC, `is_active` ASC, `status` ASC) USING BTREE,
  INDEX `idx_rag_profile_embedding_source`(`tenant_id` ASC, `embedding_source` ASC, `embedding_model_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_embedding_profile
-- ----------------------------
INSERT INTO `rag_embedding_profile` VALUES (1, 4, 'multilingual-accuracy', '平台本地多语言兜底配置', 'local-all-minilm-l6-v2', 'bge-reranker-v2-m3', NULL, 384, 'COSINE', 80, 0.35, 0.2, 0.03, 6000, 5, 8, 3, 0, 0, 'VECTOR_ONLY', 'ACTIVE', 1, 0, '2026-08-03 17:29:31', '2026-08-05 10:44:40', 'LOCAL', NULL, 'ZH,EN,OTHER', 0, NULL, NULL, NULL, 1, 30, 30, 0, 0.6, 0.4, 0.25, 'HAN,LATIN,OTHER');
INSERT INTO `rag_embedding_profile` VALUES (2, 4, 'local-local-all-minilm-l6-v2', '本地多语言 MiniLM 检索配置', 'local-all-minilm-l6-v2', 'qwen3-vl-rerank', NULL, 384, 'COSINE', 80, 0.35, 0.2, 0.03, 6000, 5, 8, 3, 0, 0, 'VECTOR_ONLY', 'ACTIVE', 1, 0, '2026-08-04 10:40:40', '2026-08-05 10:44:40', 'LOCAL', NULL, 'ZH,EN,OTHER', 0, NULL, NULL, NULL, 1, 30, 30, 0, 0.6, 0.4, 0.25, 'HAN,LATIN,OTHER');
INSERT INTO `rag_embedding_profile` VALUES (3, 4, 'local-local-bge-small-zh', '本地中文 BGE Small 检索配置', 'local-bge-small-zh', NULL, NULL, 512, 'COSINE', 80, 0.35, 0.2, 0.03, 6000, 5, 8, 3, 0, 0, 'VECTOR_ONLY', 'ACTIVE', 1, 0, '2026-08-04 10:43:40', '2026-08-04 10:43:40', 'LOCAL', NULL, 'ZH,EN,OTHER', 0, NULL, NULL, NULL, 0, 30, 30, 0, 0.6, 0.4, 0.25, 'HAN,LATIN,OTHER');
INSERT INTO `rag_embedding_profile` VALUES (4, 4, 'api-tenant_private-4-d1024-9bbf6f7', 'qwen-embedding 检索配置', 'qwen3.7-text-embedding', 'qwen3-vl-rerank', NULL, 1024, 'COSINE', 80, 0.35, 0.2, 0.03, 6000, 5, 8, 3, 0, 0, 'VECTOR_ONLY', 'ACTIVE', 1, 0, '2026-08-04 10:52:38', '2026-08-05 10:44:40', 'TENANT_PRIVATE', 4, 'ZH,EN,OTHER', 0, NULL, NULL, NULL, 1, 30, 30, 0, 0.6, 0.4, 0.25, 'HAN,LATIN,OTHER');
INSERT INTO `rag_embedding_profile` VALUES (5, 4, 'api-platform_shared-8-d1024-da707a93', 'Jina Embeddings v3 多语言向量模型 检索配置', 'jina-embeddings-v3', 'qwen3-vl-rerank', NULL, 1024, 'COSINE', 80, 0.35, 0.2, 0.03, 6000, 5, 8, 3, 0, 0, 'VECTOR_ONLY', 'ACTIVE', 1, 0, '2026-08-04 13:18:41', '2026-08-05 10:44:40', 'PLATFORM_SHARED', 8, 'ZH,EN,OTHER', 0, NULL, NULL, NULL, 1, 30, 30, 0, 0.6, 0.4, 0.25, 'HAN,LATIN,OTHER');
INSERT INTO `rag_embedding_profile` VALUES (6, 1, 'api-platform_shared-6-d1024-ad150193', 'BGE-M3 多语言向量模型 检索配置', 'bge-m3', 'bge-reranker-v2-m3', NULL, 1024, 'COSINE', 80, 0.35, 0.2, 0.03, 6000, 5, 8, 3, 0, 0, 'VECTOR_ONLY', 'ACTIVE', 1, 0, '2026-08-04 15:10:56', '2026-08-05 10:44:40', 'PLATFORM_SHARED', 6, 'ZH,EN,OTHER', 0, NULL, NULL, NULL, 1, 30, 30, 0, 0.6, 0.4, 0.25, 'HAN,LATIN,OTHER');
INSERT INTO `rag_embedding_profile` VALUES (7, 1, 'local-local-all-minilm-l6-v2', '本地多语言 MiniLM 检索配置', 'local-all-minilm-l6-v2', NULL, NULL, 384, 'COSINE', 80, 0.35, 0.2, 0.03, 6000, 5, 8, 3, 0, 0, 'VECTOR_ONLY', 'ACTIVE', 1, 0, '2026-08-06 10:33:01', '2026-08-06 10:33:01', 'LOCAL', NULL, 'ZH,EN,OTHER', 1, '1', '', '', 0, 30, 60, 0, 0.6, 0.4, 0.25, 'HAN,LATIN,OTHER');

-- ----------------------------
-- Table structure for rag_profile_version
-- ----------------------------
DROP TABLE IF EXISTS `rag_profile_version`;
CREATE TABLE `rag_profile_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `profile_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `embedding_fingerprint` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `configuration_json` json NOT NULL,
  `version_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `activated_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rag_profile_version`(`tenant_id` ASC, `profile_id` ASC, `version_no` ASC) USING BTREE,
  INDEX `idx_rag_profile_version_status`(`tenant_id` ASC, `profile_id` ASC, `version_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_profile_version
-- ----------------------------

-- ----------------------------
-- Table structure for rag_retrieval_metric
-- ----------------------------
DROP TABLE IF EXISTS `rag_retrieval_metric`;
CREATE TABLE `rag_retrieval_metric`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `call_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `hit_count` int NOT NULL DEFAULT 0,
  `top_score` double NULL DEFAULT NULL,
  `hit` tinyint(1) NOT NULL DEFAULT 0,
  `grounded` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actual_language` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `language_source` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `retrieval_scope` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `retrieval_channels` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `candidate_count` int NOT NULL DEFAULT 0,
  `elapsed_ms` bigint NOT NULL DEFAULT 0,
  `embedding_profile` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `index_generation_id` bigint NULL DEFAULT NULL,
  `initial_candidate_count` int NOT NULL DEFAULT 0,
  `reranked_candidate_count` int NOT NULL DEFAULT 0,
  `score_gap` double NULL DEFAULT NULL,
  `query_rewritten` tinyint NOT NULL DEFAULT 0,
  `degraded` tinyint NOT NULL DEFAULT 0,
  `parent_coverage` double NULL DEFAULT NULL,
  `embedding_elapsed_ms` bigint NOT NULL DEFAULT 0,
  `vector_search_elapsed_ms` bigint NOT NULL DEFAULT 0,
  `rerank_elapsed_ms` bigint NOT NULL DEFAULT 0,
  `context_elapsed_ms` bigint NOT NULL DEFAULT 0,
  `model_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `degrade_reason` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `reranker_estimated_cost` decimal(18, 8) NULL DEFAULT NULL,
  `reranker_budget_exceeded` tinyint NOT NULL DEFAULT 0,
  `dense_candidate_count` int NOT NULL DEFAULT 0,
  `sparse_candidate_count` int NOT NULL DEFAULT 0,
  `exact_candidate_count` int NOT NULL DEFAULT 0,
  `fusion_candidate_count` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rag_metric_tenant_time`(`tenant_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_rag_metric_hit`(`tenant_id` ASC, `hit` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 144 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_retrieval_metric
-- ----------------------------
INSERT INTO `rag_retrieval_metric` VALUES (1, 4, 'KNOWLEDGE_SEARCH', 6, 0.8792002433065174, 1, 0, '2026-07-24 15:28:38', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (2, 4, 'KNOWLEDGE_SEARCH', 6, 0.8792002433065174, 1, 0, '2026-07-24 15:41:43', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (3, 4, 'KNOWLEDGE_SEARCH', 6, 0.8375825640445589, 1, 0, '2026-07-24 16:03:44', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (4, 4, 'KNOWLEDGE_SEARCH', 6, 0.8375825640445589, 1, 0, '2026-07-24 16:03:51', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (5, 4, 'KNOWLEDGE_SEARCH', 6, 0.8375825640445589, 1, 0, '2026-07-24 16:03:52', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (6, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-24 16:08:49', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (7, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-24 16:08:51', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (8, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-24 16:08:52', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (9, 4, 'KNOWLEDGE_SEARCH', 4, 0.9122766111891222, 1, 0, '2026-07-24 16:09:01', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (10, 4, 'KNOWLEDGE_SEARCH', 6, 0.9873015873015875, 1, 0, '2026-07-24 16:19:35', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (11, 4, 'KNOWLEDGE_SEARCH', 1, 0.9834990000668666, 1, 0, '2026-07-24 16:25:13', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (12, 4, 'KNOWLEDGE_SEARCH', 1, 0.9834990000668666, 1, 0, '2026-07-24 16:26:51', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (13, 4, 'KNOWLEDGE_SEARCH', 5, 0.9646969486408256, 1, 0, '2026-07-24 16:26:58', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (14, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-24 17:10:42', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (15, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-24 17:10:44', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (16, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-24 17:10:52', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (17, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-24 17:11:25', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (18, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-24 17:11:27', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (19, 4, 'KNOWLEDGE_SEARCH', 2, 0.9666590910233961, 1, 0, '2026-07-27 17:17:48', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (20, 4, 'KNOWLEDGE_SEARCH', 2, 0.9666590910233961, 1, 0, '2026-07-27 17:17:49', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (21, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-27 17:18:01', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (22, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 09:29:26', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (23, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 09:29:29', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (24, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 09:29:40', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (25, 4, 'KNOWLEDGE_SEARCH', 2, 1, 1, 0, '2026-07-29 09:29:55', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (26, 4, 'KNOWLEDGE_SEARCH', 5, 0.9890916983080547, 1, 0, '2026-07-29 09:30:06', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (27, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 13:07:25', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (28, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 13:07:27', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (29, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 13:09:37', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (30, 4, 'KNOWLEDGE_SEARCH', 1, 1, 1, 0, '2026-07-29 13:12:23', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (31, 4, 'KNOWLEDGE_SEARCH', 1, 1, 1, 0, '2026-07-29 13:12:55', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (32, 4, 'ORCHESTRATION_RAG', 0, 0, 0, 0, '2026-07-29 16:48:25', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (33, 4, 'ORCHESTRATION_RAG', 0, 0, 0, 0, '2026-07-29 16:49:48', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (34, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 17:51:52', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'VECTOR', 15, 5837, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (35, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 17:51:55', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'VECTOR', 15, 6838, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (36, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 17:52:41', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 18, 5499, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (37, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 17:52:53', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 21, 4520, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (38, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 17:54:10', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 21, 5277, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (39, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 17:54:33', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 20, 12213, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (40, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 17:56:06', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 20, 30765, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (41, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-07-29 17:57:48', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 20, 29666, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (42, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-03 10:19:25', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 16, 5192, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (43, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-03 10:22:10', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 16, 149129, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (44, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-03 10:23:31', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 16, 38162, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (45, 4, 'KNOWLEDGE_SEARCH', 5, 1, 1, 0, '2026-08-03 10:34:10', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 16, 9401, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (46, 4, 'KNOWLEDGE_SEARCH', 5, 1, 1, 0, '2026-08-03 10:35:33', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 21, 4880, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (47, 4, 'KNOWLEDGE_SEARCH', 5, 1, 1, 0, '2026-08-03 10:38:06', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 18, 6746, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (48, 4, 'KNOWLEDGE_SEARCH', 5, 1, 1, 0, '2026-08-03 10:39:18', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL,VECTOR', 16, 6862, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (49, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-03 14:38:21', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', '', 0, 368, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (50, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-03 14:38:48', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', '', 0, 120, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (51, 4, 'KNOWLEDGE_SEARCH', 5, 1, 1, 0, '2026-08-03 14:42:46', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL', 6, 162, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (52, 4, 'KNOWLEDGE_SEARCH', 5, 1, 1, 0, '2026-08-03 14:42:55', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL', 7, 128, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (53, 4, 'KNOWLEDGE_SEARCH', 1, 1, 1, 0, '2026-08-03 14:43:04', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL', 1, 116, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (54, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-03 14:43:21', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', '', 0, 159, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (55, 4, 'KNOWLEDGE_SEARCH', 1, 1, 1, 0, '2026-08-03 14:43:46', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'LEXICAL_GENERAL', 1, 119, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (56, 4, 'KNOWLEDGE_SEARCH', 3, 1, 1, 0, '2026-08-03 15:16:59', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'VECTOR', 3, 52, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (57, 4, 'KNOWLEDGE_SEARCH', 5, 1, 1, 0, '2026-08-03 15:18:20', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'VECTOR', 15, 57, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (58, 4, 'KNOWLEDGE_SEARCH', 5, 1, 1, 0, '2026-08-03 15:18:48', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'VECTOR', 15, 31, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (59, 4, 'KNOWLEDGE_SEARCH', 5, 1, 1, 0, '2026-08-03 16:08:59', 'OTHER', 'DOCUMENT', 'VISIBLE_DOCUMENTS', 'VECTOR', 15, 139, NULL, NULL, 0, 0, NULL, 0, 0, NULL, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (60, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-03 17:34:32', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 0, 52, 'multilingual-accuracy', NULL, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (61, 4, 'KNOWLEDGE_SEARCH', 0, 0.42721456, 0, 0, '2026-08-04 10:41:55', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 264, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.05549594000000002, 0, 1, 0, 3, 45, 2, 81, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (62, 4, 'KNOWLEDGE_SEARCH', 0, 0.19242808, 0, 0, '2026-08-04 10:42:12', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 160, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.05883791999999999, 0, 1, 0, 2, 8, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (63, 4, 'KNOWLEDGE_SEARCH', 0, 0.13217938, 0, 0, '2026-08-04 10:42:16', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 175, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.00599876000000002, 0, 1, 0, 2, 21, 1, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (64, 4, 'KNOWLEDGE_SEARCH', 3, 0.44754308, 1, 0, '2026-08-04 10:43:06', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 209, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.007030539999999974, 0, 1, 0.75, 2, 8, 0, 71, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (65, 4, 'KNOWLEDGE_SEARCH', 3, 0.5626941, 1, 0, '2026-08-04 10:43:28', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 212, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.01765109999999992, 0, 1, 0.75, 2, 5, 0, 71, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (66, 4, 'KNOWLEDGE_SEARCH', 3, 0.74368274, 1, 0, '2026-08-04 10:44:01', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 228, 'local-local-bge-small-zh', 3, 50, 50, 0.016150740000000052, 0, 1, 0.75, 1, 15, 0, 68, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (67, 4, 'KNOWLEDGE_SEARCH', 3, 0.74368274, 1, 0, '2026-08-04 10:44:16', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 215, 'local-local-bge-small-zh', 3, 50, 50, 0.016150740000000052, 0, 1, 0.75, 1, 4, 0, 76, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (68, 4, 'KNOWLEDGE_SEARCH', 3, 0.74368274, 1, 0, '2026-08-04 10:45:15', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 11704, 'local-local-bge-small-zh', 3, 50, 50, 0.016150740000000052, 0, 1, 0.75, 1, 5, 11449, 119, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (69, 4, 'KNOWLEDGE_SEARCH', 5, 0.77009016, 1, 0, '2026-08-04 10:46:52', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 3650, 'local-local-bge-small-zh', 3, 50, 50, 0.00952705999999992, 0, 1, 1, 1, 4, 3460, 55, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (70, 4, 'KNOWLEDGE_SEARCH', 1, 0.5882014, 1, 0, '2026-08-04 10:54:09', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 3, 10016, 'api-tenant_private-4-d1024-9bbf6f7', 4, 3, 3, 0.004371160000000041, 0, 1, 1, 247, 5, 9588, 49, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (71, 4, 'KNOWLEDGE_SEARCH', 1, 0.58829695, 1, 0, '2026-08-04 10:57:20', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 3, 22046, 'api-tenant_private-4-d1024-9bbf6f7', 4, 3, 3, 0.004375149999999994, 0, 1, 1, 18080, 9, 3660, 48, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (72, 4, 'KNOWLEDGE_SEARCH', 1, 0.58829695, 1, 0, '2026-08-04 11:00:28', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 3, 21924, 'api-tenant_private-4-d1024-9bbf6f7', 4, 3, 3, 0.004375149999999994, 0, 1, 1, 2715, 6, 19009, 48, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (73, 4, 'KNOWLEDGE_SEARCH', 1, 0.5882014, 1, 0, '2026-08-04 11:01:23', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 3, 29738, 'api-tenant_private-4-d1024-9bbf6f7', 4, 3, 3, 0.004371160000000041, 0, 1, 1, 9998, 5, 19531, 55, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (74, 4, 'KNOWLEDGE_SEARCH', 1, 0.4018013, 1, 0, '2026-08-04 11:11:51', 'OTHER', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 19, 10008, 'api-tenant_private-4-d1024-9bbf6f7', 4, 19, 19, 0.05295825999999998, 0, 1, 1, 2933, 42, 6799, 50, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (75, 4, 'KNOWLEDGE_SEARCH', 1, 0.4018013, 1, 0, '2026-08-04 11:12:09', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 19, 474, 'api-tenant_private-4-d1024-9bbf6f7', 4, 19, 19, 0.05295825999999998, 0, 1, 1, 236, 57, 0, 24, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (76, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-04 11:19:23', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 4947, 'local-local-all-minilm-l6-v2', 2, 50, 0, 0, 0, 0, 0, 4761, 10, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (77, 4, 'KNOWLEDGE_SEARCH', 5, 0.7556912, 1, 0, '2026-08-04 11:19:40', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 537, 'local-local-bge-small-zh', 3, 50, 50, 0.008877499999999983, 0, 1, 1, 375, 8, 0, 24, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (78, 4, 'KNOWLEDGE_SEARCH', 5, 0.7556912, 1, 0, '2026-08-04 11:20:19', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 180, 'local-local-bge-small-zh', 3, 50, 50, 0.008877499999999983, 0, 1, 1, 2, 30, 0, 27, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (79, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-04 11:56:19', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 1063, 'local-local-all-minilm-l6-v2', 2, 50, 0, 0, 0, 0, 0, 782, 156, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (80, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-04 11:56:34', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 119, 'local-local-all-minilm-l6-v2', 2, 50, 0, 0, 0, 0, 0, 2, 15, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (81, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-04 11:57:16', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 118, 'local-local-all-minilm-l6-v2', 2, 50, 0, 0, 0, 0, 0, 2, 11, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (82, 4, 'KNOWLEDGE_SEARCH', 5, 0.7507143, 1, 0, '2026-08-04 11:57:21', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 578, 'local-local-bge-small-zh', 3, 50, 50, 0.007047059999999994, 0, 0, 1, 407, 11, 0, 28, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (83, 4, 'KNOWLEDGE_SEARCH', 4, 0.77300024, 1, 0, '2026-08-04 11:57:31', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 264, 'local-local-bge-small-zh', 3, 50, 50, 0.014391240000000027, 0, 0, 0.8, 2, 47, 0, 89, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (84, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-04 11:58:16', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 238, 'local-local-all-minilm-l6-v2', 2, 50, 0, 0, 0, 0, 0, 2, 129, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (85, 4, 'KNOWLEDGE_SEARCH', 2, 0.7760396, 1, 0, '2026-08-04 11:58:19', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 238, 'local-local-bge-small-zh', 3, 50, 50, 0.00015545000000005693, 0, 0, 0.6666666666666666, 18, 22, 0, 71, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (86, 4, 'KNOWLEDGE_SEARCH', 2, 0.7760396, 1, 0, '2026-08-04 11:58:32', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 227, 'local-local-bge-small-zh', 3, 50, 50, 0.00015545000000005693, 0, 0, 0.6666666666666666, 3, 13, 0, 75, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (87, 4, 'KNOWLEDGE_SEARCH', 2, 0.7760396, 1, 0, '2026-08-04 11:59:13', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 220, 'local-local-bge-small-zh', 3, 50, 50, 0.00015545000000005693, 0, 0, 0.6666666666666666, 2, 9, 0, 72, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (88, 4, 'KNOWLEDGE_SEARCH', 0, 0, 0, 0, '2026-08-04 11:59:17', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 126, 'local-local-all-minilm-l6-v2', 2, 50, 0, 0, 0, 0, 0, 2, 21, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (89, 4, 'KNOWLEDGE_SEARCH', 2, 0.7760396, 1, 0, '2026-08-04 13:03:45', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 216, 'local-local-bge-small-zh', 3, 50, 50, 0.00015545000000005693, 0, 0, 0.6666666666666666, 9, 14, 0, 68, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (90, 4, 'KNOWLEDGE_SEARCH', 0, 0.19246112, 0, 0, '2026-08-04 13:06:45', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 175, 'local-local-all-minilm-l6-v2', 2, 50, 25, 0.027378840000000015, 0, 0, 0, 1, 19, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (91, 4, 'KNOWLEDGE_SEARCH', 0, 0.19246112, 0, 0, '2026-08-04 13:06:47', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 160, 'local-local-all-minilm-l6-v2', 2, 50, 25, 0.027378840000000015, 0, 0, 0, 2, 12, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (92, 4, 'KNOWLEDGE_SEARCH', 5, 0.5626941, 1, 0, '2026-08-04 13:06:57', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 217, 'local-local-all-minilm-l6-v2', 2, 50, 31, 0.01765109999999992, 0, 0, 1, 5, 31, 0, 29, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (93, 4, 'KNOWLEDGE_SEARCH', 1, 0.43337107, 1, 0, '2026-08-04 13:19:48', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 1277, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.0003289700000000395, 0, 0, 0.25, 4, 88, 0, 874, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (94, 4, 'KNOWLEDGE_SEARCH', 1, 0.43337107, 1, 0, '2026-08-04 13:30:26', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 1174, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.0003289700000000395, 0, 0, 0.25, 38, 99, 0, 822, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (95, 4, 'KNOWLEDGE_SEARCH', 1, 0.43337107, 1, 0, '2026-08-04 13:31:19', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 20709, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.0003289700000000395, 0, 0, 0.25, 2, 68, 0, 531, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (96, 4, 'KNOWLEDGE_SEARCH', 1, 0.43337107, 1, 0, '2026-08-04 13:36:06', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 10796, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.0003289700000000395, 0, 0, 0.25, 684, 104, 365, 182, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (97, 4, 'KNOWLEDGE_SEARCH', 1, 0.43337107, 1, 0, '2026-08-04 13:43:49', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 25455, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.0003289700000000395, 0, 0, 0.25, 11, 77, 20647, 166, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (98, 4, 'KNOWLEDGE_SEARCH', 1, 0.6133589943667417, 1, 0, '2026-08-04 13:44:37', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 17493, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0, 0, 0, 0.25, 3, 63, 14651, 205, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (99, 4, 'KNOWLEDGE_SEARCH', 1, 0.43337107, 1, 0, '2026-08-04 13:45:53', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 45256, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.0003289700000000395, 0, 0, 0.25, 593, 105, 39194, 179, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (100, 4, 'KNOWLEDGE_SEARCH', 1, 0.43337107, 1, 0, '2026-08-04 13:46:59', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 21720, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.0003289700000000395, 0, 0, 0.25, 3, 100, 18696, 250, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (101, 4, 'KNOWLEDGE_SEARCH', 1, 0.43337107, 1, 0, '2026-08-04 13:47:44', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 5385, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.0003289700000000395, 0, 0, 0.25, 3, 63, 2620, 191, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (102, 4, 'KNOWLEDGE_SEARCH', 1, 0.43337107, 1, 0, '2026-08-04 13:51:10', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 75708, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.0003289700000000395, 0, 0, 0.25, 2, 59, 68879, 174, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (103, 4, 'KNOWLEDGE_SEARCH', 1, 0.6133589943667417, 1, 0, '2026-08-04 13:57:46', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 5903, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0, 0, 0, 0.25, 627, 100, 884, 201, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (104, 4, 'KNOWLEDGE_SEARCH', 0, 0.6458756460611403, 0, 0, '2026-08-04 14:13:29', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 8671, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.009897266438438357, 0, 0, 0, 11, 102, 1105, 481, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (105, 4, 'KNOWLEDGE_SEARCH', 0, 0.6472807976241458, 0, 0, '2026-08-04 14:13:49', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 5457, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.002473652676945992, 0, 0, 0, 21, 90, 1016, 200, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (106, 4, 'KNOWLEDGE_SEARCH', 0, 0.6472807976241458, 0, 0, '2026-08-04 14:15:01', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 52072, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.002473652676945992, 0, 0, 0, 2, 64, 51619, 177, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (107, 4, 'KNOWLEDGE_SEARCH', 1, 0.6126218191958277, 1, 0, '2026-08-04 14:15:12', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 2602, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.000000020797360944513343, 0, 0, 0.25, 2, 82, 2189, 173, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (108, 4, 'KNOWLEDGE_SEARCH', 5, 0.7225324378256839, 1, 0, '2026-08-04 14:15:41', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 3075, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.018584187112553607, 0, 0, 1, 3, 87, 2800, 49, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (109, 4, 'KNOWLEDGE_SEARCH', 5, 0.786827409267213, 1, 0, '2026-08-04 14:16:22', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 1225, 'local-local-all-minilm-l6-v2', 2, 50, 50, 0.008508563148702297, 0, 0, 1, 3, 61, 905, 75, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (110, 4, 'KNOWLEDGE_SEARCH', 5, 0.8003163462715435, 1, 0, '2026-08-04 15:51:14', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 2060, 'local-local-all-minilm-l6-v2', 2, 50, 32, 0.005497812452688988, 0, 0, 1, 868, 166, 786, 49, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (111, 4, 'KNOWLEDGE_SEARCH', 5, 0.8015303933421194, 1, 0, '2026-08-04 16:11:53', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 727, 'local-local-all-minilm-l6-v2', 2, 50, 31, 0.052685029718363974, 0, 0, 1, 41, 67, 466, 27, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (112, 4, 'KNOWLEDGE_SEARCH', 0, 0.15230185, 0, 0, '2026-08-05 09:09:54', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 5150, 'local-local-all-minilm-l6-v2', 2, 50, 25, 0.010883209999999977, 0, 1, 0, 4703, 218, 54, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (113, 4, 'KNOWLEDGE_SEARCH', 1, 0.35553408, 1, 0, '2026-08-05 09:10:10', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 300, 'local-local-all-minilm-l6-v2', 2, 50, 31, 0.05158957999999997, 0, 1, 1, 3, 83, 46, 26, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (114, 4, 'KNOWLEDGE_SEARCH', 1, 0.23132472, 1, 0, '2026-08-05 09:14:15', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 780, 'local-local-all-minilm-l6-v2', 2, 50, 28, 0.03399862000000001, 0, 1, 1, 44, 294, 60, 25, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (115, 4, 'KNOWLEDGE_SEARCH', 4, 0.44754308, 1, 0, '2026-08-05 09:14:38', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 625, 'local-local-all-minilm-l6-v2', 2, 50, 31, 0.007030539999999974, 0, 1, 0.8, 4, 102, 55, 245, NULL, NULL, NULL, 0, 0, 0, 0, 0);
INSERT INTO `rag_retrieval_metric` VALUES (116, 4, 'KNOWLEDGE_SEARCH', 5, 0.9903225806451612, 1, 0, '2026-08-05 13:46:38', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'LEXICAL_ZH,VECTOR', 50, 248, 'local-local-all-minilm-l6-v2', 8, 50, 50, 0.028120199692780345, 0, 1, 1, 2, 33, 50, 26, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 50, 6, 0, 50);
INSERT INTO `rag_retrieval_metric` VALUES (117, 4, 'KNOWLEDGE_SEARCH', 5, 0.9903225806451612, 1, 0, '2026-08-05 13:46:55', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'LEXICAL_ZH,VECTOR', 50, 218, 'local-local-all-minilm-l6-v2', 8, 50, 50, 0.028120199692780345, 0, 1, 1, 1, 23, 48, 23, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 50, 6, 0, 50);
INSERT INTO `rag_retrieval_metric` VALUES (118, 4, 'KNOWLEDGE_SEARCH', 2, 0.8066666666666666, 1, 0, '2026-08-05 13:47:15', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'LEXICAL_ZH,VECTOR', 54, 562, 'local-local-all-minilm-l6-v2', 8, 54, 54, 0.20666666666666678, 0, 1, 0.4, 2, 12, 72, 75, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 50, 5, 0, 54);
INSERT INTO `rag_retrieval_metric` VALUES (119, 4, 'KNOWLEDGE_SEARCH', 3, 1, 1, 0, '2026-08-05 13:47:45', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 285, 'local-local-all-minilm-l6-v2', 8, 50, 50, 0.01612903225806439, 0, 1, 0.6, 3, 20, 71, 69, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 50, 0, 0, 50);
INSERT INTO `rag_retrieval_metric` VALUES (120, 4, 'KNOWLEDGE_SEARCH', 5, 0.924569155987627, 1, 0, '2026-08-05 13:53:13', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'LEXICAL_ZH,VECTOR', 50, 248, 'local-local-all-minilm-l6-v2', 8, 50, 50, 0.0029158838865936954, 0, 1, 1, 1, 20, 50, 24, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 50, 16, 0, 50);
INSERT INTO `rag_retrieval_metric` VALUES (121, 4, 'KNOWLEDGE_SEARCH', 5, 0.9903225806451612, 1, 0, '2026-08-05 14:34:26', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'LEXICAL_ZH,VECTOR', 50, 411, 'local-local-all-minilm-l6-v2', 8, 50, 50, 0.028120199692780345, 0, 1, 1, 63, 44, 57, 47, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 50, 6, 0, 50);
INSERT INTO `rag_retrieval_metric` VALUES (122, 4, 'KNOWLEDGE_SEARCH', 2, 0.8066666666666666, 1, 0, '2026-08-05 14:34:38', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'LEXICAL_ZH,VECTOR', 54, 344, 'local-local-all-minilm-l6-v2', 8, 54, 54, 0.20666666666666678, 0, 1, 0.4, 4, 11, 89, 76, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 50, 5, 0, 54);
INSERT INTO `rag_retrieval_metric` VALUES (123, 4, 'KNOWLEDGE_SEARCH', 3, 1, 1, 0, '2026-08-05 14:35:03', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 516, 'local-local-all-minilm-l6-v2', 8, 50, 50, 0.01612903225806439, 0, 1, 0.6, 11, 10, 48, 267, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 50, 0, 0, 50);
INSERT INTO `rag_retrieval_metric` VALUES (124, 4, 'KNOWLEDGE_SEARCH', 4, 1, 1, 0, '2026-08-05 14:35:55', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'VECTOR', 50, 435, 'local-local-all-minilm-l6-v2', 8, 50, 50, 0.01612903225806439, 0, 1, 0.8, 12, 15, 61, 186, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 50, 0, 0, 50);
INSERT INTO `rag_retrieval_metric` VALUES (125, 4, 'KNOWLEDGE_SEARCH', 2, 0.7549362305148795, 1, 0, '2026-08-05 14:36:43', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'LEXICAL_ZH,VECTOR', 61, 362, 'local-local-all-minilm-l6-v2', 8, 61, 61, 0.04095895778760683, 0, 1, 0.4, 5, 15, 49, 144, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 50, 13, 0, 61);
INSERT INTO `rag_retrieval_metric` VALUES (126, 4, 'KNOWLEDGE_SEARCH', 3, 0.9999999999999998, 1, 0, '2026-08-05 14:37:54', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'LEXICAL_ZH,VECTOR', 10, 228, 'local-local-all-minilm-l6-v2', 9, 10, 10, 0.01612903225806439, 0, 1, 1, 1, 7, 50, 46, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 10, 2, 0, 10);
INSERT INTO `rag_retrieval_metric` VALUES (127, 4, 'KNOWLEDGE_SEARCH', 3, 0.9903225806451612, 1, 0, '2026-08-05 14:38:23', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'LEXICAL_ZH,VECTOR', 10, 351, 'local-local-all-minilm-l6-v2', 9, 10, 10, 0.04222873900293256, 0, 1, 1, 2, 5, 163, 53, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 10, 2, 0, 10);
INSERT INTO `rag_retrieval_metric` VALUES (128, 4, 'KNOWLEDGE_SEARCH', 3, 0.9999999999999998, 1, 0, '2026-08-05 14:38:44', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'LEXICAL_ZH,VECTOR', 10, 261, 'local-local-all-minilm-l6-v2', 9, 10, 10, 0.02237583205325111, 0, 1, 1, 2, 20, 53, 49, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 10, 5, 0, 10);
INSERT INTO `rag_retrieval_metric` VALUES (129, 4, 'KNOWLEDGE_SEARCH', 2, 0.9999999999999998, 1, 0, '2026-08-05 14:39:58', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 10, 263, 'local-local-all-minilm-l6-v2', 10, 10, 10, 0.02237583205325111, 0, 1, 1, 2, 15, 68, 34, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 10, 8, 0, 10);
INSERT INTO `rag_retrieval_metric` VALUES (130, 4, 'KNOWLEDGE_SEARCH', 2, 0.9812500000000001, 1, 0, '2026-08-06 14:41:56', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 316, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.024913003663003863, 0, 1, 1, 2, 47, 78, 33, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 12, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (131, 4, 'KNOWLEDGE_SEARCH', 3, 0.9812500000000001, 1, 0, '2026-08-06 14:42:27', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 341, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.012996031746031922, 0, 1, 1, 2, 11, 60, 56, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 9, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (132, 4, 'KNOWLEDGE_SEARCH', 3, 0.9812500000000001, 1, 0, '2026-08-06 15:29:00', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 5297, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.0067492319508449805, 0, 1, 1, 4824, 132, 61, 86, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 11, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (133, 4, 'KNOWLEDGE_SEARCH', 2, 0.9472596153846153, 1, 0, '2026-08-06 15:29:33', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 262, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.021722733965381047, 0, 1, 1, 2, 19, 80, 27, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 5, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (134, 4, 'KNOWLEDGE_SEARCH', 3, 1, 1, 0, '2026-08-06 15:29:45', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 219, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.01612903225806439, 0, 1, 1, 2, 13, 48, 24, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 0, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (135, 4, 'KNOWLEDGE_SEARCH', 2, 1, 1, 0, '2026-08-06 15:29:55', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 215, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.01612903225806439, 0, 1, 1, 3, 9, 51, 23, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 0, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (136, 4, 'KNOWLEDGE_SEARCH', 3, 1, 1, 0, '2026-08-06 15:30:02', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 210, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.01612903225806439, 0, 1, 1, 2, 7, 50, 24, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 0, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (137, 4, 'KNOWLEDGE_SEARCH', 3, 1, 1, 0, '2026-08-06 15:32:24', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 207, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.01612903225806439, 0, 1, 1, 1, 4, 53, 25, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 0, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (138, 4, 'KNOWLEDGE_SEARCH', 3, 1, 1, 0, '2026-08-06 15:33:01', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 247, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.01612903225806439, 0, 1, 1, 1, 20, 71, 26, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 0, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (139, 4, 'KNOWLEDGE_SEARCH', 3, 1, 1, 0, '2026-08-06 15:33:21', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 206, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.01612903225806439, 0, 1, 1, 1, 5, 48, 25, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 0, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (140, 4, 'KNOWLEDGE_SEARCH', 3, 1, 1, 0, '2026-08-06 15:34:17', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 196, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.01612903225806439, 0, 1, 1, 1, 3, 49, 24, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 0, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (141, 4, 'KNOWLEDGE_SEARCH', 3, 1, 1, 0, '2026-08-06 15:36:38', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 212, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.01612903225806439, 0, 1, 1, 2, 7, 50, 26, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 0, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (142, 4, 'KNOWLEDGE_SEARCH', 3, 1, 1, 0, '2026-08-06 15:37:12', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 235, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.01612903225806439, 0, 1, 1, 3, 4, 73, 23, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 0, 0, 16);
INSERT INTO `rag_retrieval_metric` VALUES (143, 4, 'KNOWLEDGE_SEARCH', 3, 1, 1, 0, '2026-08-06 15:37:34', 'ZH', 'REQUEST', 'VISIBLE_DOCUMENTS', 'EXACT,LEXICAL_ZH,VECTOR', 16, 257, 'local-local-all-minilm-l6-v2', 10, 16, 16, 0.01612903225806439, 0, 1, 1, 4, 5, 50, 24, 'LOCAL', 'RERANKER_FAILURE', NULL, 0, 16, 0, 0, 16);

-- ----------------------------
-- Table structure for runtime_dead_letter
-- ----------------------------
DROP TABLE IF EXISTS `runtime_dead_letter`;
CREATE TABLE `runtime_dead_letter`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NULL DEFAULT NULL,
  `release_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `task_id` bigint NOT NULL,
  `run_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `failed_node_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `error_category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `error_summary` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `snapshot_json` json NULL,
  `dead_letter_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OPEN',
  `assigned_to` bigint NULL DEFAULT NULL,
  `replay_run_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_runtime_dlq_queue`(`tenant_id` ASC, `dead_letter_status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_runtime_dlq_run`(`tenant_id` ASC, `run_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of runtime_dead_letter
-- ----------------------------

-- ----------------------------
-- Table structure for runtime_dead_letter_disposition
-- ----------------------------
DROP TABLE IF EXISTS `runtime_dead_letter_disposition`;
CREATE TABLE `runtime_dead_letter_disposition`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `dead_letter_id` bigint NOT NULL,
  `disposition_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `previous_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `next_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `reason` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `operator_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_runtime_disposition_dlq`(`tenant_id` ASC, `dead_letter_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of runtime_dead_letter_disposition
-- ----------------------------

-- ----------------------------
-- Table structure for runtime_framework_strategy
-- ----------------------------
DROP TABLE IF EXISTS `runtime_framework_strategy`;
CREATE TABLE `runtime_framework_strategy`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `framework_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `adapter_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT 0,
  `capability_json` json NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_runtime_framework_tenant_code`(`tenant_id` ASC, `framework_code` ASC) USING BTREE,
  INDEX `idx_runtime_framework_tenant_status`(`tenant_id` ASC, `enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of runtime_framework_strategy
-- ----------------------------
INSERT INTO `runtime_framework_strategy` VALUES (1, 1, 'AUTOGEN', '1.0', 1, '{\"modes\": [\"MULTI_AGENT\"], \"streaming\": true, \"checkpoint\": true, \"humanHandoff\": true}', '2026-07-28 17:59:10', '2026-07-28 17:59:10');
INSERT INTO `runtime_framework_strategy` VALUES (2, 1, 'LANGGRAPH', '1.0', 1, '{\"modes\": [\"WORKFLOW\", \"REACT\", \"PLAN\"], \"streaming\": true, \"checkpoint\": true, \"humanHandoff\": true}', '2026-07-28 17:59:10', '2026-07-28 17:59:10');
INSERT INTO `runtime_framework_strategy` VALUES (3, 1, 'CAMEL', '1.0', 1, '{\"modes\": [\"MULTI_AGENT\"], \"streaming\": true, \"checkpoint\": false, \"humanHandoff\": false}', '2026-07-28 17:59:10', '2026-07-28 17:59:10');
INSERT INTO `runtime_framework_strategy` VALUES (4, 1, 'AGENTSCOPE', '1.0', 1, '{\"modes\": [\"CHAT\", \"REACT\", \"MULTI_AGENT\"], \"streaming\": true, \"checkpoint\": true, \"humanHandoff\": true}', '2026-07-28 17:59:10', '2026-07-28 17:59:10');
INSERT INTO `runtime_framework_strategy` VALUES (5, 2, 'AUTOGEN', '1.0', 1, '{\"modes\": [\"MULTI_AGENT\"], \"streaming\": true, \"checkpoint\": true, \"humanHandoff\": true}', '2026-07-28 17:59:10', '2026-07-28 17:59:10');
INSERT INTO `runtime_framework_strategy` VALUES (6, 2, 'LANGGRAPH', '1.0', 1, '{\"modes\": [\"WORKFLOW\", \"REACT\", \"PLAN\"], \"streaming\": true, \"checkpoint\": true, \"humanHandoff\": true}', '2026-07-28 17:59:10', '2026-07-28 17:59:10');
INSERT INTO `runtime_framework_strategy` VALUES (7, 2, 'CAMEL', '1.0', 1, '{\"modes\": [\"MULTI_AGENT\"], \"streaming\": true, \"checkpoint\": false, \"humanHandoff\": false}', '2026-07-28 17:59:10', '2026-07-28 17:59:10');
INSERT INTO `runtime_framework_strategy` VALUES (8, 2, 'AGENTSCOPE', '1.0', 1, '{\"modes\": [\"CHAT\", \"REACT\", \"MULTI_AGENT\"], \"streaming\": true, \"checkpoint\": true, \"humanHandoff\": true}', '2026-07-28 17:59:10', '2026-07-28 17:59:10');
INSERT INTO `runtime_framework_strategy` VALUES (9, 3, 'AUTOGEN', '1.0', 1, '{\"modes\": [\"MULTI_AGENT\"], \"streaming\": true, \"checkpoint\": true, \"humanHandoff\": true}', '2026-07-28 17:59:10', '2026-07-28 17:59:10');
INSERT INTO `runtime_framework_strategy` VALUES (10, 3, 'LANGGRAPH', '1.0', 1, '{\"modes\": [\"WORKFLOW\", \"REACT\", \"PLAN\"], \"streaming\": true, \"checkpoint\": true, \"humanHandoff\": true}', '2026-07-28 17:59:10', '2026-07-28 17:59:10');
INSERT INTO `runtime_framework_strategy` VALUES (11, 3, 'CAMEL', '1.0', 1, '{\"modes\": [\"MULTI_AGENT\"], \"streaming\": true, \"checkpoint\": false, \"humanHandoff\": false}', '2026-07-28 17:59:10', '2026-07-28 17:59:10');
INSERT INTO `runtime_framework_strategy` VALUES (12, 3, 'AGENTSCOPE', '1.0', 1, '{\"modes\": [\"CHAT\", \"REACT\", \"MULTI_AGENT\"], \"streaming\": true, \"checkpoint\": true, \"humanHandoff\": true}', '2026-07-28 17:59:10', '2026-07-28 17:59:10');

-- ----------------------------
-- Table structure for runtime_outbox_event
-- ----------------------------
DROP TABLE IF EXISTS `runtime_outbox_event`;
CREATE TABLE `runtime_outbox_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `aggregate_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `aggregate_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `payload_json` json NOT NULL,
  `outbox_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `retry_count` int NOT NULL DEFAULT 0,
  `available_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `sent_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_runtime_outbox_claim`(`outbox_status` ASC, `available_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of runtime_outbox_event
-- ----------------------------

-- ----------------------------
-- Table structure for runtime_recovery_request
-- ----------------------------
DROP TABLE IF EXISTS `runtime_recovery_request`;
CREATE TABLE `runtime_recovery_request`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NULL DEFAULT NULL,
  `release_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `run_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `action_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `request_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'REQUESTED',
  `reason` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `requested_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `completed_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_runtime_recovery_request`(`tenant_id` ASC, `run_id` ASC, `action_type` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of runtime_recovery_request
-- ----------------------------

-- ----------------------------
-- Table structure for runtime_side_effect_checkpoint
-- ----------------------------
DROP TABLE IF EXISTS `runtime_side_effect_checkpoint`;
CREATE TABLE `runtime_side_effect_checkpoint`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `run_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `task_id` bigint NULL DEFAULT NULL,
  `operation_key` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `operation_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `side_effect_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'NOT_STARTED',
  `request_summary` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `provider_receipt` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `result_summary` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_runtime_side_effect_operation`(`tenant_id` ASC, `operation_key` ASC) USING BTREE,
  INDEX `idx_runtime_side_effect_run`(`tenant_id` ASC, `run_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of runtime_side_effect_checkpoint
-- ----------------------------

-- ----------------------------
-- Table structure for runtime_slo_alert
-- ----------------------------
DROP TABLE IF EXISTS `runtime_slo_alert`;
CREATE TABLE `runtime_slo_alert`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NULL DEFAULT NULL,
  `release_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `alert_key` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sli_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `alert_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `observed_value` decimal(18, 6) NOT NULL,
  `target_value` decimal(18, 6) NOT NULL,
  `safe_summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `opened_at` datetime NOT NULL,
  `acknowledged_at` datetime NULL DEFAULT NULL,
  `acknowledged_by` bigint NULL DEFAULT NULL,
  `recovered_at` datetime NULL DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_runtime_slo_alert`(`tenant_id` ASC, `alert_key` ASC) USING BTREE,
  INDEX `idx_runtime_slo_alert_status`(`tenant_id` ASC, `alert_status` ASC, `updated_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of runtime_slo_alert
-- ----------------------------

-- ----------------------------
-- Table structure for runtime_slo_policy
-- ----------------------------
DROP TABLE IF EXISTS `runtime_slo_policy`;
CREATE TABLE `runtime_slo_policy`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NULL DEFAULT NULL,
  `release_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `policy_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `target_json` json NOT NULL,
  `window_minutes` int NOT NULL DEFAULT 60,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_runtime_slo_policy`(`tenant_id` ASC, `application_id` ASC, `policy_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of runtime_slo_policy
-- ----------------------------

-- ----------------------------
-- Table structure for runtime_task_attempt
-- ----------------------------
DROP TABLE IF EXISTS `runtime_task_attempt`;
CREATE TABLE `runtime_task_attempt`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `run_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `attempt_no` int NOT NULL,
  `lease_owner` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `attempt_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `lease_until` datetime NULL DEFAULT NULL,
  `heartbeat_at` datetime NULL DEFAULT NULL,
  `started_at` datetime NULL DEFAULT NULL,
  `finished_at` datetime NULL DEFAULT NULL,
  `error_category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `error_summary` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `retry_decision` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_runtime_attempt_claim`(`tenant_id` ASC, `task_id` ASC, `attempt_status` ASC, `lease_until` ASC) USING BTREE,
  INDEX `idx_runtime_attempt_run`(`tenant_id` ASC, `run_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of runtime_task_attempt
-- ----------------------------
INSERT INTO `runtime_task_attempt` VALUES (1, 4, 56, NULL, 1, 'dad7a75c-c9ca-4ce1-83cc-0dfe39f05f90', 'SUCCEEDED', '2026-08-06 14:43:37', '2026-08-06 14:41:37', '2026-08-06 14:41:37', '2026-08-06 14:41:40', NULL, NULL, NULL, '2026-08-06 14:41:37');
INSERT INTO `runtime_task_attempt` VALUES (2, 4, 57, 'f3199e3e-305f-485a-9bd0-1902cf8d828f', 1, 'df81a26c-7871-4526-8f7f-f017e6402ae6', 'SUCCEEDED', '2026-08-07 10:35:13', '2026-08-07 10:33:13', '2026-08-07 10:33:13', '2026-08-07 10:33:15', NULL, NULL, NULL, '2026-08-07 10:33:13');

-- ----------------------------
-- Table structure for saas_admission_decision
-- ----------------------------
DROP TABLE IF EXISTS `saas_admission_decision`;
CREATE TABLE `saas_admission_decision`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NULL DEFAULT NULL,
  `feature_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `decision` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `current_usage` bigint NOT NULL,
  `requested_quantity` bigint NOT NULL,
  `usage_limit` bigint NOT NULL,
  `overage_policy` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `reason_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `shadow_mode` tinyint(1) NOT NULL DEFAULT 1,
  `reset_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_admission_tenant`(`tenant_id` ASC, `feature_code` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_admission_request`(`tenant_id` ASC, `request_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_admission_decision
-- ----------------------------
INSERT INTO `saas_admission_decision` VALUES (1, 4, NULL, 'MODEL_TOKEN', 'ALLOW', 0, 1000, 1000000, 'HARD_STOP', 'WITHIN_LIMIT', 'preview-1786069700401', 0, '2026-09-01 00:00:00', '2026-08-07 10:28:21');
INSERT INTO `saas_admission_decision` VALUES (2, 4, 1026, 'WORKFLOW_RUN', 'ALLOW', 0, 1, 1000, 'HARD_STOP', 'WITHIN_LIMIT', 'studio-test-1786069991698', 0, '2026-09-01 00:00:00', '2026-08-07 10:33:12');

-- ----------------------------
-- Table structure for saas_adoption_event
-- ----------------------------
DROP TABLE IF EXISTS `saas_adoption_event`;
CREATE TABLE `saas_adoption_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NULL DEFAULT NULL,
  `release_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `run_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `event_type` varchar(48) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `schema_version` int NOT NULL,
  `event_source` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `idempotency_key` varchar(192) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `properties_json` json NULL,
  `occurred_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_adoption_event`(`tenant_id` ASC, `idempotency_key` ASC) USING BTREE,
  INDEX `idx_adoption_funnel`(`tenant_id` ASC, `event_type` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_adoption_application`(`tenant_id` ASC, `application_id` ASC, `occurred_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_adoption_event
-- ----------------------------
INSERT INTO `saas_adoption_event` VALUES (1, 4, 1026, NULL, NULL, 'DRAFT_CREATED', 1, 'PRODUCTION', 'draft:1026:1031', NULL, '2026-08-07 10:30:45', '2026-08-07 10:30:46');
INSERT INTO `saas_adoption_event` VALUES (2, 4, 1026, NULL, NULL, 'DRAFT_CREATED', 1, 'PRODUCTION', 'draft:1026:1032', NULL, '2026-08-07 10:31:02', '2026-08-07 10:31:03');
INSERT INTO `saas_adoption_event` VALUES (3, 4, 1026, NULL, 'f3199e3e-305f-485a-9bd0-1902cf8d828f', 'TEST_SUCCEEDED', 1, 'DRAFT_TEST', 'run-success:f3199e3e-305f-485a-9bd0-1902cf8d828f', NULL, '2026-08-07 10:33:15', '2026-08-07 10:33:15');

-- ----------------------------
-- Table structure for saas_billing_note
-- ----------------------------
DROP TABLE IF EXISTS `saas_billing_note`;
CREATE TABLE `saas_billing_note`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `invoice_id` bigint NOT NULL,
  `note_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `note_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `amount` decimal(24, 10) NOT NULL,
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `reason` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_billing_note_number`(`note_number` ASC) USING BTREE,
  INDEX `idx_billing_note_invoice`(`tenant_id` ASC, `invoice_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_billing_note
-- ----------------------------

-- ----------------------------
-- Table structure for saas_billing_period
-- ----------------------------
DROP TABLE IF EXISTS `saas_billing_period`;
CREATE TABLE `saas_billing_period`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `period_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OPEN',
  `finalized_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_billing_period`(`tenant_id` ASC, `period_start` ASC, `period_end` ASC, `currency` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_billing_period
-- ----------------------------

-- ----------------------------
-- Table structure for saas_billing_sync
-- ----------------------------
DROP TABLE IF EXISTS `saas_billing_sync`;
CREATE TABLE `saas_billing_sync`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `invoice_id` bigint NOT NULL,
  `adapter_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `idempotency_key` varchar(192) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sync_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `external_reference` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `safe_summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_at` datetime NULL DEFAULT NULL,
  `confirmed_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_billing_sync`(`tenant_id` ASC, `idempotency_key` ASC) USING BTREE,
  INDEX `idx_billing_sync_retry`(`sync_status` ASC, `next_retry_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_billing_sync
-- ----------------------------

-- ----------------------------
-- Table structure for saas_data_retention_policy
-- ----------------------------
DROP TABLE IF EXISTS `saas_data_retention_policy`;
CREATE TABLE `saas_data_retention_policy`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `data_category` varchar(48) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version_no` int NOT NULL,
  `retention_days` int NOT NULL,
  `legal_basis` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `effective_at` datetime NULL DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_retention_policy_version`(`tenant_id` ASC, `data_category` ASC, `version_no` ASC) USING BTREE,
  INDEX `idx_retention_policy_active`(`tenant_id` ASC, `data_category` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_data_retention_policy
-- ----------------------------

-- ----------------------------
-- Table structure for saas_emergency_access
-- ----------------------------
DROP TABLE IF EXISTS `saas_emergency_access`;
CREATE TABLE `saas_emergency_access`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `reason_summary` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `access_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `used_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_emergency_access`(`tenant_id` ASC, `user_id` ASC, `access_status` ASC, `expires_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_emergency_access
-- ----------------------------

-- ----------------------------
-- Table structure for saas_entitlement
-- ----------------------------
DROP TABLE IF EXISTS `saas_entitlement`;
CREATE TABLE `saas_entitlement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `subscription_id` bigint NOT NULL,
  `feature_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `hard_limit` bigint NOT NULL DEFAULT 0,
  `soft_limit` bigint NOT NULL DEFAULT 0,
  `unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `overage_policy` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `reset_at` datetime NULL DEFAULT NULL,
  `version_no` int NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_entitlement_feature`(`tenant_id` ASC, `subscription_id` ASC, `feature_code` ASC) USING BTREE,
  INDEX `idx_entitlement_lookup`(`tenant_id` ASC, `feature_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_entitlement
-- ----------------------------
INSERT INTO `saas_entitlement` VALUES (1, 2, 2, 'MODEL_TOKEN', 10000000, 8000000, 'TOKEN', 'HARD_STOP', '2026-09-01 00:00:00', 1, '2026-08-07 10:10:29', '2026-08-07 10:10:29');
INSERT INTO `saas_entitlement` VALUES (2, 3, 3, 'MODEL_TOKEN', 2000000, 1600000, 'TOKEN', 'HARD_STOP', '2026-09-01 00:00:00', 1, '2026-08-07 10:10:29', '2026-08-07 10:10:29');
INSERT INTO `saas_entitlement` VALUES (3, 1, 1, 'MODEL_TOKEN', 1000000, 800000, 'TOKEN', 'HARD_STOP', '2026-09-01 00:00:00', 1, '2026-08-07 10:10:29', '2026-08-07 10:10:29');
INSERT INTO `saas_entitlement` VALUES (4, 4, 4, 'MODEL_TOKEN', 1000000, 800000, 'TOKEN', 'HARD_STOP', '2026-09-01 00:00:00', 1, '2026-08-07 10:10:29', '2026-08-07 10:10:29');
INSERT INTO `saas_entitlement` VALUES (8, 2, 2, 'WORKFLOW_RUN', 10000, 8000, 'RUN', 'HARD_STOP', '2026-09-01 00:00:00', 1, '2026-08-07 10:10:30', '2026-08-07 10:10:30');
INSERT INTO `saas_entitlement` VALUES (9, 3, 3, 'WORKFLOW_RUN', 2000, 1600, 'RUN', 'HARD_STOP', '2026-09-01 00:00:00', 1, '2026-08-07 10:10:30', '2026-08-07 10:10:30');
INSERT INTO `saas_entitlement` VALUES (10, 1, 1, 'WORKFLOW_RUN', 1000, 800, 'RUN', 'HARD_STOP', '2026-09-01 00:00:00', 1, '2026-08-07 10:10:30', '2026-08-07 10:10:30');
INSERT INTO `saas_entitlement` VALUES (11, 4, 4, 'WORKFLOW_RUN', 1000, 800, 'RUN', 'HARD_STOP', '2026-09-01 00:00:00', 1, '2026-08-07 10:10:30', '2026-08-07 10:10:30');
INSERT INTO `saas_entitlement` VALUES (15, 2, 2, 'KNOWLEDGE_STORAGE', 200, 160, 'MB', 'HARD_STOP', NULL, 1, '2026-08-07 10:10:30', '2026-08-07 10:10:30');
INSERT INTO `saas_entitlement` VALUES (16, 3, 3, 'KNOWLEDGE_STORAGE', 50, 40, 'MB', 'HARD_STOP', NULL, 1, '2026-08-07 10:10:30', '2026-08-07 10:10:30');
INSERT INTO `saas_entitlement` VALUES (17, 1, 1, 'KNOWLEDGE_STORAGE', 100, 80, 'MB', 'HARD_STOP', NULL, 1, '2026-08-07 10:10:30', '2026-08-07 10:10:30');
INSERT INTO `saas_entitlement` VALUES (18, 4, 4, 'KNOWLEDGE_STORAGE', 100, 80, 'MB', 'HARD_STOP', NULL, 1, '2026-08-07 10:10:30', '2026-08-07 10:10:30');

-- ----------------------------
-- Table structure for saas_governance_evidence
-- ----------------------------
DROP TABLE IF EXISTS `saas_governance_evidence`;
CREATE TABLE `saas_governance_evidence`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `request_id` bigint NOT NULL,
  `store_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `execution_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `object_count` bigint NOT NULL DEFAULT 0,
  `checksum` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `safe_summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `completed_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_governance_evidence_store`(`tenant_id` ASC, `request_id` ASC, `store_type` ASC) USING BTREE,
  INDEX `idx_governance_evidence_status`(`tenant_id` ASC, `execution_status` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_governance_evidence
-- ----------------------------

-- ----------------------------
-- Table structure for saas_governance_request
-- ----------------------------
DROP TABLE IF EXISTS `saas_governance_request`;
CREATE TABLE `saas_governance_request`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `request_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `request_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `scope_json` json NOT NULL,
  `requested_by` bigint NOT NULL,
  `approved_by` bigint NULL DEFAULT NULL,
  `progress_json` json NULL,
  `evidence_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `protected_reference` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `expires_at` datetime NULL DEFAULT NULL,
  `approved_at` datetime NULL DEFAULT NULL,
  `completed_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_governance_queue`(`tenant_id` ASC, `request_type` ASC, `request_status` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_governance_request
-- ----------------------------

-- ----------------------------
-- Table structure for saas_invoice
-- ----------------------------
DROP TABLE IF EXISTS `saas_invoice`;
CREATE TABLE `saas_invoice`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `billing_period_id` bigint NOT NULL,
  `invoice_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `subtotal` decimal(24, 10) NOT NULL DEFAULT 0.0000000000,
  `total` decimal(24, 10) NOT NULL DEFAULT 0.0000000000,
  `snapshot_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `finalized_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_invoice_period`(`tenant_id` ASC, `billing_period_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_invoice_number`(`invoice_number` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_invoice
-- ----------------------------

-- ----------------------------
-- Table structure for saas_invoice_line
-- ----------------------------
DROP TABLE IF EXISTS `saas_invoice_line`;
CREATE TABLE `saas_invoice_line`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `invoice_id` bigint NOT NULL,
  `feature_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `cost_center` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `quantity` decimal(24, 8) NOT NULL,
  `unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `unit_price` decimal(24, 10) NULL DEFAULT NULL,
  `amount` decimal(24, 10) NOT NULL,
  `source_summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_invoice_line`(`tenant_id` ASC, `invoice_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_invoice_line
-- ----------------------------

-- ----------------------------
-- Table structure for saas_legal_hold
-- ----------------------------
DROP TABLE IF EXISTS `saas_legal_hold`;
CREATE TABLE `saas_legal_hold`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `hold_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `scope_json` json NOT NULL,
  `reason_summary` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `hold_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `authorized_by` bigint NOT NULL,
  `released_by` bigint NULL DEFAULT NULL,
  `starts_at` datetime NOT NULL,
  `released_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_legal_hold_code`(`tenant_id` ASC, `hold_code` ASC) USING BTREE,
  INDEX `idx_legal_hold_active`(`tenant_id` ASC, `hold_status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_legal_hold
-- ----------------------------

-- ----------------------------
-- Table structure for saas_mfa_policy
-- ----------------------------
DROP TABLE IF EXISTS `saas_mfa_policy`;
CREATE TABLE `saas_mfa_policy`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `required` tinyint(1) NOT NULL DEFAULT 0,
  `step_up_minutes` int NOT NULL DEFAULT 15,
  `high_risk_actions_json` json NOT NULL,
  `version_no` int NOT NULL,
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `updated_by` bigint NOT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mfa_policy_role`(`tenant_id` ASC, `role_code` ASC, `version_no` ASC) USING BTREE,
  INDEX `idx_mfa_policy_active`(`tenant_id` ASC, `role_code` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_mfa_policy
-- ----------------------------

-- ----------------------------
-- Table structure for saas_plan_version
-- ----------------------------
DROP TABLE IF EXISTS `saas_plan_version`;
CREATE TABLE `saas_plan_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version_no` int NOT NULL,
  `plan_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `monthly_base_price` decimal(18, 6) NOT NULL DEFAULT 0.000000,
  `feature_json` json NOT NULL,
  `entitlement_json` json NOT NULL,
  `effective_at` datetime NULL DEFAULT NULL,
  `retired_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_saas_plan_version`(`plan_code` ASC, `version_no` ASC) USING BTREE,
  INDEX `idx_saas_plan_status`(`status` ASC, `effective_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_plan_version
-- ----------------------------
INSERT INTO `saas_plan_version` VALUES (1, 'DEVELOPMENT_DEFAULT', 1, '开发版默认套餐', 'ACTIVE', 'CNY', 0.000000, '[\"RUNTIME\", \"MODEL_TOKEN\", \"WORKFLOW_RUN\", \"KNOWLEDGE_STORAGE\"]', '{}', '2026-08-07 10:10:29', NULL, '2026-08-07 10:10:29');

-- ----------------------------
-- Table structure for saas_scim_group
-- ----------------------------
DROP TABLE IF EXISTS `saas_scim_group`;
CREATE TABLE `saas_scim_group`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `source_system` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `external_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `display_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `mapped_role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sync_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `last_synced_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scim_group`(`tenant_id` ASC, `source_system` ASC, `external_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_scim_group
-- ----------------------------

-- ----------------------------
-- Table structure for saas_scim_identity
-- ----------------------------
DROP TABLE IF EXISTS `saas_scim_identity`;
CREATE TABLE `saas_scim_identity`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `external_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `external_user_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_system` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sync_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `last_synced_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scim_external`(`tenant_id` ASC, `source_system` ASC, `external_id` ASC) USING BTREE,
  INDEX `idx_scim_user`(`tenant_id` ASC, `user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_scim_identity
-- ----------------------------

-- ----------------------------
-- Table structure for saas_subscription
-- ----------------------------
DROP TABLE IF EXISTS `saas_subscription`;
CREATE TABLE `saas_subscription`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `plan_version_id` bigint NOT NULL,
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `overage_policy` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `starts_on` date NOT NULL,
  `ends_on` date NULL DEFAULT NULL,
  `billing_anchor` date NOT NULL,
  `external_reference` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `cancelled_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_subscription_external`(`tenant_id` ASC, `external_reference` ASC) USING BTREE,
  INDEX `idx_subscription_active`(`tenant_id` ASC, `status` ASC, `starts_on` ASC, `ends_on` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_subscription
-- ----------------------------
INSERT INTO `saas_subscription` VALUES (1, 1, 1, 'ACTIVE', 'HARD_STOP', '2026-08-07', NULL, '2026-08-07', 'DEV-1', NULL, NULL, '2026-08-07 10:10:29', '2026-08-07 10:10:29');
INSERT INTO `saas_subscription` VALUES (2, 2, 1, 'ACTIVE', 'HARD_STOP', '2026-08-07', NULL, '2026-08-07', 'DEV-2', NULL, NULL, '2026-08-07 10:10:29', '2026-08-07 10:10:29');
INSERT INTO `saas_subscription` VALUES (3, 3, 1, 'ACTIVE', 'HARD_STOP', '2026-08-07', NULL, '2026-08-07', 'DEV-3', NULL, NULL, '2026-08-07 10:10:29', '2026-08-07 10:10:29');
INSERT INTO `saas_subscription` VALUES (4, 4, 1, 'ACTIVE', 'HARD_STOP', '2026-08-07', NULL, '2026-08-07', 'DEV-4', NULL, NULL, '2026-08-07 10:10:29', '2026-08-07 10:10:29');

-- ----------------------------
-- Table structure for saas_usage_adjustment
-- ----------------------------
DROP TABLE IF EXISTS `saas_usage_adjustment`;
CREATE TABLE `saas_usage_adjustment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `usage_event_id` bigint NOT NULL,
  `adjustment_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `quantity` decimal(24, 8) NOT NULL,
  `amount` decimal(24, 10) NULL DEFAULT NULL,
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `reason` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `idempotency_key` varchar(192) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_usage_adjustment`(`tenant_id` ASC, `idempotency_key` ASC) USING BTREE,
  INDEX `idx_adjustment_usage`(`tenant_id` ASC, `usage_event_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_usage_adjustment
-- ----------------------------

-- ----------------------------
-- Table structure for saas_usage_event
-- ----------------------------
DROP TABLE IF EXISTS `saas_usage_event`;
CREATE TABLE `saas_usage_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `application_id` bigint NULL DEFAULT NULL,
  `release_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `run_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `model_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `feature_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `cost_center` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `idempotency_key` varchar(192) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `quantity` decimal(24, 8) NOT NULL,
  `unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `usage_source` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `input_tokens` int NULL DEFAULT NULL,
  `output_tokens` int NULL DEFAULT NULL,
  `price_version_id` bigint NULL DEFAULT NULL,
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `unit_price` decimal(24, 10) NULL DEFAULT NULL,
  `cost_amount` decimal(24, 10) NULL DEFAULT NULL,
  `cost_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `cost_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `occurred_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_usage_idempotency`(`tenant_id` ASC, `idempotency_key` ASC) USING BTREE,
  INDEX `idx_usage_period`(`tenant_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_usage_allocation`(`tenant_id` ASC, `application_id` ASC, `cost_center` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_usage_run`(`tenant_id` ASC, `run_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of saas_usage_event
-- ----------------------------

-- ----------------------------
-- Table structure for sys_api_key
-- ----------------------------
DROP TABLE IF EXISTS `sys_api_key`;
CREATE TABLE `sys_api_key`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `api_key_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `api_key_mask` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `owner_user` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_api_key_hash`(`api_key_hash` ASC) USING BTREE,
  INDEX `idx_api_key_tenant_status`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_api_key
-- ----------------------------

-- ----------------------------
-- Table structure for sys_compliance_rule
-- ----------------------------
DROP TABLE IF EXISTS `sys_compliance_rule`;
CREATE TABLE `sys_compliance_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `rule_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sensitive_word` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `action_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_compliance_rule_tenant`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_compliance_rule
-- ----------------------------
INSERT INTO `sys_compliance_rule` VALUES (1, 2, '涉密及赔付安全词拦截', '机密,保密,内部配方,赔偿,赔付,合同漏洞', 'BLOCK', 'ACTIVE', '2026-07-23 15:05:30');
INSERT INTO `sys_compliance_rule` VALUES (2, 2, '手机号脱敏规则', '\\b1[3-9]\\d{9}\\b', 'DESENSITIZE', 'ACTIVE', '2026-07-23 15:05:30');
INSERT INTO `sys_compliance_rule` VALUES (3, 2, '涉密及赔付安全词拦截', '机密,保密,内部配方,赔偿,赔付,合同漏洞', 'BLOCK', 'ACTIVE', '2026-07-23 15:06:25');
INSERT INTO `sys_compliance_rule` VALUES (4, 2, '手机号脱敏规则', '\\b1[3-9]\\d{9}\\b', 'DESENSITIZE', 'ACTIVE', '2026-07-23 15:06:25');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NOT NULL DEFAULT 0,
  `menu_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `perms` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `menu_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `icon` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sys_menu_parent`(`parent_id` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, 0, '我的工作台', '/dashboard', 'views/DashboardView.vue', 'dashboard:view', 'C', 'el-icon-odometer', 1, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-27 15:36:02');
INSERT INTO `sys_menu` VALUES (2, 0, '业务应用', '/applications', 'views/AgentStudioView.vue', 'agent:view', 'C', 'el-icon-cpu', 2, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-28 13:11:05');
INSERT INTO `sys_menu` VALUES (3, 0, '发起业务', '/chat', 'views/ChatConsoleView.vue', 'chat:view', 'C', 'el-icon-chat-line-round', 3, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-27 15:36:02');
INSERT INTO `sys_menu` VALUES (4, 0, '知识资源', '/knowledge', 'views/KnowledgeBaseView.vue', 'knowledge:view', 'C', 'el-icon-folder-opened', 4, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-27 15:36:02');
INSERT INTO `sys_menu` VALUES (5, 0, '流程设计', '/workflow', 'views/WorkflowStudioView.vue', 'workflow:view', 'C', 'el-icon-connection', 5, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-27 15:36:02');
INSERT INTO `sys_menu` VALUES (6, 0, '运行观测台', '/ops', 'views/OpsCenterView.vue', 'ops:view', 'C', 'el-icon-monitor', 6, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56');
INSERT INTO `sys_menu` VALUES (7, 0, '任务中心', '/approval', 'views/ApprovalCenterView.vue', 'approval:view', 'C', 'el-icon-stamp', 7, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-27 15:36:02');
INSERT INTO `sys_menu` VALUES (8, 0, '评测中心', '/evaluation', 'views/EvaluationCenterView.vue', 'evaluation:view', 'C', 'el-icon-document-checked', 8, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56');
INSERT INTO `sys_menu` VALUES (9, 0, '能力市场', '/marketplace', 'views/MarketplaceView.vue', 'marketplace:view', 'C', 'el-icon-shopping-bag', 9, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56');
INSERT INTO `sys_menu` VALUES (10, 0, '审计风控', '/audit', 'views/AuditCenterView.vue', 'audit:view', 'C', 'el-icon-lock', 10, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56');
INSERT INTO `sys_menu` VALUES (11, 0, '租户控制台', '/tenants', 'views/TenantManagementView.vue', 'tenant:manage', 'C', 'el-icon-office-building', 11, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56');
INSERT INTO `sys_menu` VALUES (12, 0, '组织与权限', '/users', 'views/UserManagementView.vue', 'user:view', 'C', 'el-icon-user', 12, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-27 15:36:02');
INSERT INTO `sys_menu` VALUES (13, 0, '模型底座', '/models', 'views/SystemModelConfigView.vue', 'model:view', 'C', 'el-icon-setting', 13, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-27 15:36:02');
INSERT INTO `sys_menu` VALUES (14, 0, '安全合规配置', '/compliance', 'views/ComplianceConfigView.vue', 'compliance:view', 'C', 'el-icon-shield', 14, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56');
INSERT INTO `sys_menu` VALUES (15, 0, '智能路由配置', '/router', 'views/ModelRouterView.vue', 'router:view', 'C', 'el-icon-guide', 15, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56');
INSERT INTO `sys_menu` VALUES (16, 0, '开发者中心', '/developer', 'views/DeveloperView.vue', 'developer:view', 'C', 'el-icon-link', 16, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56');
INSERT INTO `sys_menu` VALUES (17, 0, '计量计费中心', '/billing', 'views/BillingView.vue', 'billing:view', 'C', 'el-icon-wallet', 17, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56');
INSERT INTO `sys_menu` VALUES (18, 0, '角色与权限', '/authorization', 'views/AuthorizationCenterView.vue', 'iam:manage', 'C', 'el-icon-key', 18, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56');
INSERT INTO `sys_menu` VALUES (19, 0, '运行中心', '/workflow-executions', 'views/WorkflowExecutionView.vue', 'workflow:execution:view', 'C', 'el-icon-video-play', 19, 'ACTIVE', '2026-07-23 15:04:56', '2026-07-27 15:36:02');
INSERT INTO `sys_menu` VALUES (20, 0, '设备会话', '/security-sessions', 'views/SecuritySessionView.vue', 'session:view', 'C', 'el-icon-monitor', 20, 'ACTIVE', '2026-07-27 15:36:02', '2026-07-27 15:36:02');
INSERT INTO `sys_menu` VALUES (21, 0, '模型计费', '/model-prices', 'views/ModelPriceView.vue', 'billing:view', 'C', 'el-icon-wallet', 21, 'ACTIVE', '2026-07-27 15:36:02', '2026-07-27 15:36:02');
INSERT INTO `sys_menu` VALUES (22, 0, '平台就绪诊断', '/diagnostics', 'views/PlatformReadinessView.vue', 'platform:readiness:view', 'C', 'el-icon-circle-check', 22, 'ACTIVE', '2026-07-28 16:07:47', '2026-07-28 16:07:47');
INSERT INTO `sys_menu` VALUES (23, 0, '故障处理', '/runtime-operations', 'views/RuntimeOperationsView.vue', 'runtime:operations:view', 'C', 'el-icon-warning-outline', 23, 'ACTIVE', '2026-08-06 16:20:10', '2026-08-06 16:20:10');
INSERT INTO `sys_menu` VALUES (24, 0, '企业身份', '/identity', 'views/IdentityGovernanceView.vue', 'saas:identity:manage', 'C', 'el-icon-key', 24, 'ACTIVE', '2026-08-07 10:10:29', '2026-08-07 10:10:29');
INSERT INTO `sys_menu` VALUES (25, 0, '套餐、权益与用量', '/entitlements', 'views/EntitlementUsageView.vue', 'saas:entitlement:view', 'C', 'el-icon-data-analysis', 25, 'ACTIVE', '2026-08-07 10:10:29', '2026-08-07 10:10:29');
INSERT INTO `sys_menu` VALUES (26, 0, '账期与账单', '/billing', 'views/BillingFinanceView.vue', 'saas:billing:view', 'C', 'el-icon-wallet', 26, 'ACTIVE', '2026-08-07 10:10:29', '2026-08-07 10:10:29');
INSERT INTO `sys_menu` VALUES (27, 0, '数据治理', '/data-governance', 'views/DataGovernanceView.vue', 'saas:governance:view', 'C', 'el-icon-document-checked', 27, 'ACTIVE', '2026-08-07 10:10:29', '2026-08-07 10:10:29');
INSERT INTO `sys_menu` VALUES (28, 0, '产品采用分析', '/adoption', 'views/AdoptionAnalyticsView.vue', 'saas:adoption:view', 'C', 'el-icon-trend-charts', 28, 'ACTIVE', '2026-08-07 10:10:29', '2026-08-07 10:10:29');

-- ----------------------------
-- Table structure for sys_model_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_model_config`;
CREATE TABLE `sys_model_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `model_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `model_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `api_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `base_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `model_capability` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CHAT',
  `vector_dimension` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_model_tenant_key`(`tenant_id` ASC, `model_key` ASC) USING BTREE,
  INDEX `idx_sys_model_status`(`tenant_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_sys_model_capability`(`tenant_id` ASC, `model_capability` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_model_config
-- ----------------------------
INSERT INTO `sys_model_config` VALUES (1, 1, 'gpt-5.5', 'OpenAI GPT-5.5', 'OPENAI', 'ENC:3zFKbUPpvk1gvdSG:TYylJVGonW3U8W9sbPVAs7J2GLNMjOIzTd3+HjPzZeiUDESw5bfJyGg/cFK9B5OAqEgl3PlY019m4wHsM5FaA4nopYZ4NXYTngtxTwdVmzCIsyw=', 'https://ai.helunox.cc.cd/v1', 'ACTIVE', '2026-07-23 15:05:26', '2026-08-04 15:06:37', 'CHAT', NULL);
INSERT INTO `sys_model_config` VALUES (4, 4, 'qwen3.7-text-embedding', 'qwen-embedding', 'ALIBABA_DASHSCOPE', 'ENC:GnqF96IJoBUxirt1:/sh3/H1TYsuKhycoeFyT0g/SbpalhzjXG3kEF0/jxn7tmTYtw5Ss47gD8WvgQMTrVrchGrlIuibz0EGohivuhyZYPm1sKV2CZpM+n8z3ktOuQNl39XjG7FuAd5FmGIhbwU73pKLrhKklPLI5jck7cGTZhhlRSHwLw9BHUvvQcgvNsVgQ', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'INACTIVE', '2026-08-04 10:52:31', '2026-08-06 10:33:47', 'EMBEDDING', 1024);
INSERT INTO `sys_model_config` VALUES (5, 4, 'qwen3-vl-rerank', 'qwen-rerank', 'ALIBABA_DASHSCOPE', 'ENC:DaZRIZ4LGq3dFfI/:Cz/sOboEOQcR89ZE3l7/xl5noXldkaEPesiiDBwrz0qVoOeZdPg1bklPa5gWFcbAJg2mrBRF1yG2pmlG6Yoj1UGa73KYFdr0XjrewQssvUVU1u8ImqfOskhG/oowKozC7rrJcsn/JqIlGSEnZXtdYuU7QgLQ9IXRsZX4jdwKpbG7TwXK', 'https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank', 'INACTIVE', '2026-08-04 10:59:47', '2026-08-05 11:55:44', 'RERANKER', NULL);

-- ----------------------------
-- Table structure for sys_model_router_rule
-- ----------------------------
DROP TABLE IF EXISTS `sys_model_router_rule`;
CREATE TABLE `sys_model_router_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `rule_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `pattern_regex` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `primary_model_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `backup_model_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_router_rule_tenant`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_model_router_rule
-- ----------------------------
INSERT INTO `sys_model_router_rule` VALUES (1, 2, '快捷简单词路由', '^(你好|您好|在吗|再见|拜拜|hello|hi|thanks|谢谢)$', 'gpt-4o-mini', 'gpt-4o-mini-private', 'ACTIVE', '2026-07-23 15:05:30');
INSERT INTO `sys_model_router_rule` VALUES (2, 2, '快捷简单词路由', '^(你好|您好|在吗|再见|拜拜|hello|hi|thanks|谢谢)$', 'gpt-4o-mini', 'gpt-4o-mini-private', 'ACTIVE', '2026-07-23 15:06:25');

-- ----------------------------
-- Table structure for sys_org_unit
-- ----------------------------
DROP TABLE IF EXISTS `sys_org_unit`;
CREATE TABLE `sys_org_unit`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `parent_id` bigint NOT NULL DEFAULT 0,
  `org_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `org_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `org_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `leader_user_id` bigint NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `sort_order` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_org_unit_code`(`tenant_id` ASC, `org_code` ASC) USING BTREE,
  INDEX `idx_org_unit_parent`(`tenant_id` ASC, `parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_org_unit
-- ----------------------------
INSERT INTO `sys_org_unit` VALUES (1, 2, 0, 'default', '默认企业', 'COMPANY', 2, 'ACTIVE', 1, '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `sys_org_unit` VALUES (2, 2, 1, 'customer-success', '客户成功中心', 'DEPARTMENT', 2, 'ACTIVE', 1, '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `sys_org_unit` VALUES (3, 2, 1, 'delivery', '项目交付部', 'DEPARTMENT', 2, 'ACTIVE', 2, '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `sys_org_unit` VALUES (4, 3, 0, 'delivery-bu', '交付事业部', 'COMPANY', 5, 'ACTIVE', 1, '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `sys_org_unit` VALUES (5, 4, 0, 'com', 'test公司', 'COMPANY', 6, 'ACTIVE', 0, '2026-07-24 13:48:50', '2026-07-24 13:48:50');
INSERT INTO `sys_org_unit` VALUES (6, 4, 5, 'test-hr', '人事部门', 'DEPARTMENT', NULL, 'ACTIVE', 0, '2026-07-24 13:49:04', '2026-07-24 13:49:04');
INSERT INTO `sys_org_unit` VALUES (7, 4, 5, 'customer', '客户服务中心', 'DEPARTMENT', NULL, 'ACTIVE', 0, '2026-07-24 13:49:22', '2026-07-24 13:49:22');
INSERT INTO `sys_org_unit` VALUES (8, 4, 5, 'fw', '法务部', 'DEPARTMENT', NULL, 'ACTIVE', 0, '2026-07-24 13:49:44', '2026-07-24 13:49:44');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `role_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `data_scope_json` json NULL,
  `approval_scope_json` json NULL,
  `resource_actions_json` json NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_role_code`(`tenant_id` ASC, `role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 1, 'SUPER_ADMIN', '系统超级管理员', 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56', NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (2, 2, 'ADMIN', '企业管理员', 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56', NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (3, 2, 'OPERATOR', '业务运营', 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56', NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (4, 2, 'STAFF', '普通员工', 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56', NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (5, 3, 'ADMIN', '企业管理员', 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56', NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (6, 3, 'OPERATOR', '业务运营', 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56', NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (7, 3, 'STAFF', '普通员工', 'ACTIVE', '2026-07-23 15:04:56', '2026-07-23 15:04:56', NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (8, 4, 'ADMIN', '企业管理员', 'ACTIVE', '2026-07-24 13:41:23', '2026-07-24 13:41:23', NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (9, 4, 'OPERATOR', '业务运营', 'ACTIVE', '2026-07-24 13:41:24', '2026-07-24 13:41:24', NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (10, 4, 'STAFF', '普通员工', 'ACTIVE', '2026-07-24 13:41:24', '2026-07-24 13:41:24', NULL, NULL, NULL);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_role_menu`(`role_id` ASC, `menu_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 258 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 1, 1);
INSERT INTO `sys_role_menu` VALUES (2, 1, 2);
INSERT INTO `sys_role_menu` VALUES (3, 1, 3);
INSERT INTO `sys_role_menu` VALUES (4, 1, 4);
INSERT INTO `sys_role_menu` VALUES (5, 1, 5);
INSERT INTO `sys_role_menu` VALUES (6, 1, 6);
INSERT INTO `sys_role_menu` VALUES (7, 1, 7);
INSERT INTO `sys_role_menu` VALUES (8, 1, 8);
INSERT INTO `sys_role_menu` VALUES (9, 1, 9);
INSERT INTO `sys_role_menu` VALUES (10, 1, 10);
INSERT INTO `sys_role_menu` VALUES (11, 1, 11);
INSERT INTO `sys_role_menu` VALUES (12, 1, 12);
INSERT INTO `sys_role_menu` VALUES (13, 1, 13);
INSERT INTO `sys_role_menu` VALUES (14, 1, 14);
INSERT INTO `sys_role_menu` VALUES (15, 1, 15);
INSERT INTO `sys_role_menu` VALUES (16, 1, 16);
INSERT INTO `sys_role_menu` VALUES (17, 1, 17);
INSERT INTO `sys_role_menu` VALUES (18, 1, 18);
INSERT INTO `sys_role_menu` VALUES (19, 1, 19);
INSERT INTO `sys_role_menu` VALUES (161, 1, 20);
INSERT INTO `sys_role_menu` VALUES (165, 1, 21);
INSERT INTO `sys_role_menu` VALUES (177, 1, 22);
INSERT INTO `sys_role_menu` VALUES (205, 1, 23);
INSERT INTO `sys_role_menu` VALUES (215, 1, 24);
INSERT INTO `sys_role_menu` VALUES (219, 1, 25);
INSERT INTO `sys_role_menu` VALUES (223, 1, 26);
INSERT INTO `sys_role_menu` VALUES (227, 1, 27);
INSERT INTO `sys_role_menu` VALUES (231, 1, 28);
INSERT INTO `sys_role_menu` VALUES (20, 2, 1);
INSERT INTO `sys_role_menu` VALUES (21, 2, 2);
INSERT INTO `sys_role_menu` VALUES (22, 2, 3);
INSERT INTO `sys_role_menu` VALUES (23, 2, 4);
INSERT INTO `sys_role_menu` VALUES (24, 2, 5);
INSERT INTO `sys_role_menu` VALUES (25, 2, 6);
INSERT INTO `sys_role_menu` VALUES (26, 2, 7);
INSERT INTO `sys_role_menu` VALUES (27, 2, 8);
INSERT INTO `sys_role_menu` VALUES (28, 2, 9);
INSERT INTO `sys_role_menu` VALUES (29, 2, 10);
INSERT INTO `sys_role_menu` VALUES (30, 2, 12);
INSERT INTO `sys_role_menu` VALUES (31, 2, 13);
INSERT INTO `sys_role_menu` VALUES (32, 2, 14);
INSERT INTO `sys_role_menu` VALUES (33, 2, 15);
INSERT INTO `sys_role_menu` VALUES (34, 2, 16);
INSERT INTO `sys_role_menu` VALUES (35, 2, 17);
INSERT INTO `sys_role_menu` VALUES (36, 2, 18);
INSERT INTO `sys_role_menu` VALUES (37, 2, 19);
INSERT INTO `sys_role_menu` VALUES (160, 2, 20);
INSERT INTO `sys_role_menu` VALUES (164, 2, 21);
INSERT INTO `sys_role_menu` VALUES (176, 2, 22);
INSERT INTO `sys_role_menu` VALUES (206, 2, 23);
INSERT INTO `sys_role_menu` VALUES (214, 2, 24);
INSERT INTO `sys_role_menu` VALUES (218, 2, 25);
INSERT INTO `sys_role_menu` VALUES (222, 2, 26);
INSERT INTO `sys_role_menu` VALUES (226, 2, 27);
INSERT INTO `sys_role_menu` VALUES (230, 2, 28);
INSERT INTO `sys_role_menu` VALUES (38, 3, 1);
INSERT INTO `sys_role_menu` VALUES (39, 3, 2);
INSERT INTO `sys_role_menu` VALUES (40, 3, 3);
INSERT INTO `sys_role_menu` VALUES (41, 3, 4);
INSERT INTO `sys_role_menu` VALUES (42, 3, 5);
INSERT INTO `sys_role_menu` VALUES (43, 3, 8);
INSERT INTO `sys_role_menu` VALUES (44, 3, 9);
INSERT INTO `sys_role_menu` VALUES (45, 3, 13);
INSERT INTO `sys_role_menu` VALUES (46, 3, 16);
INSERT INTO `sys_role_menu` VALUES (245, 3, 17);
INSERT INTO `sys_role_menu` VALUES (47, 3, 19);
INSERT INTO `sys_role_menu` VALUES (202, 3, 20);
INSERT INTO `sys_role_menu` VALUES (207, 3, 23);
INSERT INTO `sys_role_menu` VALUES (248, 3, 25);
INSERT INTO `sys_role_menu` VALUES (251, 3, 26);
INSERT INTO `sys_role_menu` VALUES (254, 3, 28);
INSERT INTO `sys_role_menu` VALUES (48, 4, 1);
INSERT INTO `sys_role_menu` VALUES (49, 4, 3);
INSERT INTO `sys_role_menu` VALUES (50, 4, 19);
INSERT INTO `sys_role_menu` VALUES (51, 5, 1);
INSERT INTO `sys_role_menu` VALUES (52, 5, 2);
INSERT INTO `sys_role_menu` VALUES (53, 5, 3);
INSERT INTO `sys_role_menu` VALUES (54, 5, 4);
INSERT INTO `sys_role_menu` VALUES (55, 5, 5);
INSERT INTO `sys_role_menu` VALUES (56, 5, 6);
INSERT INTO `sys_role_menu` VALUES (57, 5, 7);
INSERT INTO `sys_role_menu` VALUES (58, 5, 8);
INSERT INTO `sys_role_menu` VALUES (59, 5, 9);
INSERT INTO `sys_role_menu` VALUES (60, 5, 10);
INSERT INTO `sys_role_menu` VALUES (61, 5, 12);
INSERT INTO `sys_role_menu` VALUES (62, 5, 13);
INSERT INTO `sys_role_menu` VALUES (63, 5, 14);
INSERT INTO `sys_role_menu` VALUES (64, 5, 15);
INSERT INTO `sys_role_menu` VALUES (65, 5, 16);
INSERT INTO `sys_role_menu` VALUES (66, 5, 17);
INSERT INTO `sys_role_menu` VALUES (67, 5, 18);
INSERT INTO `sys_role_menu` VALUES (68, 5, 19);
INSERT INTO `sys_role_menu` VALUES (159, 5, 20);
INSERT INTO `sys_role_menu` VALUES (163, 5, 21);
INSERT INTO `sys_role_menu` VALUES (175, 5, 22);
INSERT INTO `sys_role_menu` VALUES (208, 5, 23);
INSERT INTO `sys_role_menu` VALUES (213, 5, 24);
INSERT INTO `sys_role_menu` VALUES (217, 5, 25);
INSERT INTO `sys_role_menu` VALUES (221, 5, 26);
INSERT INTO `sys_role_menu` VALUES (225, 5, 27);
INSERT INTO `sys_role_menu` VALUES (229, 5, 28);
INSERT INTO `sys_role_menu` VALUES (69, 6, 1);
INSERT INTO `sys_role_menu` VALUES (70, 6, 2);
INSERT INTO `sys_role_menu` VALUES (71, 6, 3);
INSERT INTO `sys_role_menu` VALUES (72, 6, 4);
INSERT INTO `sys_role_menu` VALUES (73, 6, 5);
INSERT INTO `sys_role_menu` VALUES (74, 6, 8);
INSERT INTO `sys_role_menu` VALUES (75, 6, 9);
INSERT INTO `sys_role_menu` VALUES (76, 6, 13);
INSERT INTO `sys_role_menu` VALUES (77, 6, 16);
INSERT INTO `sys_role_menu` VALUES (244, 6, 17);
INSERT INTO `sys_role_menu` VALUES (78, 6, 19);
INSERT INTO `sys_role_menu` VALUES (203, 6, 20);
INSERT INTO `sys_role_menu` VALUES (209, 6, 23);
INSERT INTO `sys_role_menu` VALUES (247, 6, 25);
INSERT INTO `sys_role_menu` VALUES (250, 6, 26);
INSERT INTO `sys_role_menu` VALUES (253, 6, 28);
INSERT INTO `sys_role_menu` VALUES (79, 7, 1);
INSERT INTO `sys_role_menu` VALUES (80, 7, 3);
INSERT INTO `sys_role_menu` VALUES (81, 7, 19);
INSERT INTO `sys_role_menu` VALUES (181, 8, 1);
INSERT INTO `sys_role_menu` VALUES (182, 8, 2);
INSERT INTO `sys_role_menu` VALUES (183, 8, 3);
INSERT INTO `sys_role_menu` VALUES (184, 8, 4);
INSERT INTO `sys_role_menu` VALUES (185, 8, 5);
INSERT INTO `sys_role_menu` VALUES (186, 8, 6);
INSERT INTO `sys_role_menu` VALUES (187, 8, 7);
INSERT INTO `sys_role_menu` VALUES (188, 8, 8);
INSERT INTO `sys_role_menu` VALUES (189, 8, 9);
INSERT INTO `sys_role_menu` VALUES (190, 8, 10);
INSERT INTO `sys_role_menu` VALUES (191, 8, 12);
INSERT INTO `sys_role_menu` VALUES (192, 8, 13);
INSERT INTO `sys_role_menu` VALUES (193, 8, 14);
INSERT INTO `sys_role_menu` VALUES (194, 8, 15);
INSERT INTO `sys_role_menu` VALUES (195, 8, 16);
INSERT INTO `sys_role_menu` VALUES (196, 8, 17);
INSERT INTO `sys_role_menu` VALUES (197, 8, 18);
INSERT INTO `sys_role_menu` VALUES (198, 8, 19);
INSERT INTO `sys_role_menu` VALUES (199, 8, 20);
INSERT INTO `sys_role_menu` VALUES (200, 8, 21);
INSERT INTO `sys_role_menu` VALUES (201, 8, 22);
INSERT INTO `sys_role_menu` VALUES (210, 8, 23);
INSERT INTO `sys_role_menu` VALUES (212, 8, 24);
INSERT INTO `sys_role_menu` VALUES (216, 8, 25);
INSERT INTO `sys_role_menu` VALUES (220, 8, 26);
INSERT INTO `sys_role_menu` VALUES (224, 8, 27);
INSERT INTO `sys_role_menu` VALUES (228, 8, 28);
INSERT INTO `sys_role_menu` VALUES (107, 9, 1);
INSERT INTO `sys_role_menu` VALUES (108, 9, 2);
INSERT INTO `sys_role_menu` VALUES (109, 9, 3);
INSERT INTO `sys_role_menu` VALUES (110, 9, 4);
INSERT INTO `sys_role_menu` VALUES (111, 9, 5);
INSERT INTO `sys_role_menu` VALUES (112, 9, 8);
INSERT INTO `sys_role_menu` VALUES (113, 9, 9);
INSERT INTO `sys_role_menu` VALUES (114, 9, 13);
INSERT INTO `sys_role_menu` VALUES (115, 9, 16);
INSERT INTO `sys_role_menu` VALUES (243, 9, 17);
INSERT INTO `sys_role_menu` VALUES (116, 9, 19);
INSERT INTO `sys_role_menu` VALUES (204, 9, 20);
INSERT INTO `sys_role_menu` VALUES (211, 9, 23);
INSERT INTO `sys_role_menu` VALUES (246, 9, 25);
INSERT INTO `sys_role_menu` VALUES (249, 9, 26);
INSERT INTO `sys_role_menu` VALUES (252, 9, 28);
INSERT INTO `sys_role_menu` VALUES (117, 10, 1);
INSERT INTO `sys_role_menu` VALUES (118, 10, 3);
INSERT INTO `sys_role_menu` VALUES (119, 10, 19);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `username` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `nickname` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `phone` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `manager_user_id` bigint NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_tenant_username`(`tenant_id` ASC, `username` ASC) USING BTREE,
  INDEX `idx_sys_user_manager`(`tenant_id` ASC, `manager_user_id` ASC) USING BTREE,
  INDEX `idx_sys_user_status`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 1, 'admin', '$2a$10$HHOy/QnrJkZmttye69grb.OBqb6aUbWfUpJlaL/c3/YC6gpeslV8u', 'SaaS 系统超管', 'superadmin@acme.com', '18888888888', NULL, 'ACTIVE', '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `sys_user` VALUES (2, 2, 'tenant-admin', '$2a$10$HHOy/QnrJkZmttye69grb.OBqb6aUbWfUpJlaL/c3/YC6gpeslV8u', '默认租户管理员', 'admin@default.com', '18800001111', NULL, 'ACTIVE', '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `sys_user` VALUES (3, 2, 'tenant-op', '$2a$10$HHOy/QnrJkZmttye69grb.OBqb6aUbWfUpJlaL/c3/YC6gpeslV8u', '租户运营专员', 'op@default.com', '18800002222', 2, 'ACTIVE', '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `sys_user` VALUES (4, 2, 'tenant-staff', '$2a$10$HHOy/QnrJkZmttye69grb.OBqb6aUbWfUpJlaL/c3/YC6gpeslV8u', '租户一线员工', 'staff@default.com', '18800003333', 3, 'ACTIVE', '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `sys_user` VALUES (5, 3, 'bu-admin', '$2a$10$HHOy/QnrJkZmttye69grb.OBqb6aUbWfUpJlaL/c3/YC6gpeslV8u', '部门管理员', 'bu-admin@delivery.com', '18800004444', NULL, 'ACTIVE', '2026-07-23 15:04:57', '2026-07-23 15:04:57');
INSERT INTO `sys_user` VALUES (6, 4, 'raysy', '$2a$10$waU0fdKcleXKYpC.2QHEYuxaoTesv5zvb4TDtVdGTZtxquOZ/vT5.', '雷', NULL, NULL, NULL, 'ACTIVE', '2026-07-24 13:41:26', '2026-07-24 13:49:55');

-- ----------------------------
-- Table structure for sys_user_org
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_org`;
CREATE TABLE `sys_user_org`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `org_unit_id` bigint NOT NULL,
  `is_primary` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_org`(`tenant_id` ASC, `user_id` ASC, `org_unit_id` ASC) USING BTREE,
  INDEX `idx_sys_user_org_primary`(`tenant_id` ASC, `user_id` ASC, `is_primary` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_org
-- ----------------------------
INSERT INTO `sys_user_org` VALUES (1, 2, 2, 1, 1, '2026-07-23 15:05:26');
INSERT INTO `sys_user_org` VALUES (2, 2, 3, 2, 1, '2026-07-23 15:05:26');
INSERT INTO `sys_user_org` VALUES (3, 2, 4, 2, 1, '2026-07-23 15:05:26');
INSERT INTO `sys_user_org` VALUES (4, 2, 2, 3, 0, '2026-07-23 15:05:26');
INSERT INTO `sys_user_org` VALUES (5, 3, 5, 4, 1, '2026-07-23 15:05:26');
INSERT INTO `sys_user_org` VALUES (6, 4, 6, 5, 1, '2026-07-24 13:49:55');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1, 1);
INSERT INTO `sys_user_role` VALUES (2, 2, 2);
INSERT INTO `sys_user_role` VALUES (3, 3, 3);
INSERT INTO `sys_user_role` VALUES (4, 4, 4);
INSERT INTO `sys_user_role` VALUES (5, 5, 5);
INSERT INTO `sys_user_role` VALUES (6, 6, 8);

-- ----------------------------
-- Table structure for tenant
-- ----------------------------
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `tenant_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `plan_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_limit` int NOT NULL DEFAULT 10,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_code`(`tenant_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tenant
-- ----------------------------
INSERT INTO `tenant` VALUES (1, 'system', 'SaaS 系统运营商', 'ENTERPRISE', 999, 'ACTIVE', '2026-07-23 15:04:56');
INSERT INTO `tenant` VALUES (2, 'default', '默认企业租户', 'ENTERPRISE', 100, 'ACTIVE', '2026-07-23 15:04:56');
INSERT INTO `tenant` VALUES (3, 'delivery-bu', '交付事业部', 'PROFESSIONAL', 30, 'ACTIVE', '2026-07-23 15:04:56');
INSERT INTO `tenant` VALUES (4, 'test', '软件测试公司', 'STARTER', 20, 'ACTIVE', '2026-07-24 13:41:23');

-- ----------------------------
-- Table structure for tenant_billing_quota
-- ----------------------------
DROP TABLE IF EXISTS `tenant_billing_quota`;
CREATE TABLE `tenant_billing_quota`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `monthly_token_limit` bigint NOT NULL DEFAULT 0,
  `monthly_token_used` bigint NOT NULL DEFAULT 0,
  `monthly_workflow_limit` int NOT NULL DEFAULT 0,
  `monthly_workflow_used` int NOT NULL DEFAULT 0,
  `storage_limit_mb` int NOT NULL DEFAULT 0,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_billing_quota`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tenant_billing_quota
-- ----------------------------
INSERT INTO `tenant_billing_quota` VALUES (1, 2, 10000000, 15000, 10000, 20, 200, '2026-07-23 15:05:30');
INSERT INTO `tenant_billing_quota` VALUES (2, 3, 2000000, 0, 2000, 0, 50, '2026-07-23 15:05:30');
INSERT INTO `tenant_billing_quota` VALUES (7, 1, 1000000, 0, 1000, 0, 100, '2026-07-23 15:43:16');
INSERT INTO `tenant_billing_quota` VALUES (8, 4, 1000000, 0, 1000, 0, 100, '2026-07-24 13:41:26');

-- ----------------------------
-- Table structure for tenant_cost_alert_rule
-- ----------------------------
DROP TABLE IF EXISTS `tenant_cost_alert_rule`;
CREATE TABLE `tenant_cost_alert_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `monthly_cost_limit` decimal(18, 8) NOT NULL,
  `alert_percent` decimal(5, 2) NOT NULL DEFAULT 80.00,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_cost_alert`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tenant_cost_alert_rule
-- ----------------------------
INSERT INTO `tenant_cost_alert_rule` VALUES (1, 2, 1000.00000000, 80.00, 'ACTIVE', '2026-07-23 15:05:30');
INSERT INTO `tenant_cost_alert_rule` VALUES (2, 3, 200.00000000, 80.00, 'ACTIVE', '2026-07-23 15:05:30');

-- ----------------------------
-- Table structure for user_session
-- ----------------------------
DROP TABLE IF EXISTS `user_session`;
CREATE TABLE `user_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `refresh_token_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `device_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_agent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_used_at` datetime NULL DEFAULT NULL,
  `revoked_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_session_token`(`refresh_token_hash` ASC) USING BTREE,
  INDEX `idx_user_session_user`(`tenant_id` ASC, `user_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 61 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_session
-- ----------------------------
INSERT INTO `user_session` VALUES (1, 1, 1, 'fbc1c441b155818c42d2005067522c74f0d917a666b9e046e9f635e2b25400f5', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', 'REVOKED', '2026-07-30 15:19:53', '2026-07-23 15:19:53', '2026-07-23 15:19:53', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (2, 1, 1, 'ac701b875c5b6de86ef85489ee4f7c97ba93fa1bc3422892fe12152c4267f840', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', 'REVOKED', '2026-07-30 15:20:05', '2026-07-23 15:20:05', '2026-07-23 15:20:05', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (3, 1, 1, '1c2d44c6059c2855869416810100b4f773079cc1830bb16ce41d486f6303e17b', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', 'REVOKED', '2026-07-30 15:21:53', '2026-07-23 15:21:53', '2026-07-23 15:21:53', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (4, 1, 1, '95f478ec074b218a69458771a261c04880aa2dbb3023da715e861a29fb770e32', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', 'REVOKED', '2026-07-30 15:22:05', '2026-07-23 15:22:05', '2026-07-23 15:22:05', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (5, 1, 1, 'a0e40e7399ba6584d23c2088bfd1ee3786c5a6b2583aac462307f3ddf318f644', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', 'REVOKED', '2026-07-30 15:33:17', '2026-07-23 15:33:17', '2026-07-23 15:33:17', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (6, 1, 1, 'fb5129bc2b93ccdde817e87bf9188b367a0a201b6a77987302513c827d60a2e3', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', 'REVOKED', '2026-07-30 15:33:42', '2026-07-23 15:33:42', '2026-07-23 15:33:42', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (7, 1, 1, 'b9829a008e65bd3b9b8d787d0ae26e973c4bbbe789a757b74a2b592450e4d95c', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', 'REVOKED', '2026-07-30 15:33:55', '2026-07-23 15:33:55', '2026-07-23 15:33:55', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (8, 1, 1, '3f2d767ee1fd67ebaf785fe0f5a1d0724994693e5e0e9f3eb42556c3d34b98c1', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-07-30 15:41:24', '2026-07-23 15:41:24', '2026-07-23 15:41:24', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (9, 1, 1, 'acbbb720e4793e610afd6f9671e75bd4669123d0d7a07da28ced978f7f6efc1d', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-07-30 16:01:56', '2026-07-23 16:01:56', '2026-07-23 16:01:56', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (10, 1, 1, 'a3cc1c394ce25ace737c9b8c35b7dec428a4e39df2f2c42432ca42f3bc9c567b', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-07-30 16:17:30', '2026-07-23 16:17:30', '2026-07-23 16:17:30', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (11, 1, 1, '8f6de6de314f1769010c5802f12fd70200ceb7197f2ed1a4887d3eca5d748595', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-07-30 16:34:03', '2026-07-23 16:34:03', '2026-07-23 16:34:03', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (12, 1, 1, '3591eaf9b5f8db0be9ec02223c9ce4e3d5ae83c9fed1bb0371c0877240d9b006', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-07-30 16:56:28', '2026-07-23 16:56:28', '2026-07-23 16:56:28', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (13, 1, 1, '50117143717f86833363f4d996b8efa8edc697a54d79fccd9444c9bbd939778c', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-07-30 17:16:39', '2026-07-23 17:16:39', '2026-07-23 17:16:39', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (14, 1, 1, '5104a162eff95037221f15d021c373cbca2c7874e05a3feee9d633fa8d668e98', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-07-31 13:16:06', '2026-07-24 13:16:06', '2026-07-24 13:16:06', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (15, 4, 6, '360fe4b97ef7bbf806b8975893563374d7f26cdfc08b76a52e5cffd7ba81efe4', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 13:43:20', '2026-07-24 13:43:20', '2026-07-24 13:43:20', NULL);
INSERT INTO `user_session` VALUES (16, 4, 6, '9cdafc333146988b6592a61289370364be4ec3df6e5e8df9123499dfb5174ab1', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 13:47:04', '2026-07-24 13:47:04', '2026-07-24 13:47:04', NULL);
INSERT INTO `user_session` VALUES (17, 4, 6, '9111b1ef74603d4020f90ec53b17a534021fcfbe502178b8268366b624aba741', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 14:07:51', '2026-07-24 14:07:51', '2026-07-24 14:07:51', NULL);
INSERT INTO `user_session` VALUES (18, 4, 6, '2279b9529425e248e0acc2e2ca2de483b97770fcc4ee1d09d6436ba5c8b0a60c', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 14:24:15', '2026-07-24 14:24:15', '2026-07-24 14:24:15', NULL);
INSERT INTO `user_session` VALUES (19, 2, 2, '961bee2daefec73da75f52c1841085a6b5f5198f28a6ee15a6eb1def516edbd1', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', 'ACTIVE', '2026-07-31 14:24:59', '2026-07-24 14:24:59', '2026-07-24 14:24:59', NULL);
INSERT INTO `user_session` VALUES (20, 2, 2, '2c98d7a14cfe5594c780f57ea16a7da176f9d5d76b9cfc09ffeb41a8e39cef13', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', 'ACTIVE', '2026-07-31 14:34:07', '2026-07-24 14:34:07', '2026-07-24 14:34:07', NULL);
INSERT INTO `user_session` VALUES (21, 4, 6, 'a4ca5cc4af925d20287fc1cb41f85f58522d3604b8a4e527e458297c1eb591ee', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 15:08:59', '2026-07-24 15:08:59', '2026-07-24 15:08:59', NULL);
INSERT INTO `user_session` VALUES (22, 2, 2, 'c917b9b3f92979d7718d236fcfb9498d15ea3f20efebb58e13a4c1810d32f183', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', 'ACTIVE', '2026-07-31 15:09:42', '2026-07-24 15:09:42', '2026-07-24 15:09:42', NULL);
INSERT INTO `user_session` VALUES (23, 2, 2, 'dafe0470c47dcd67031f0e15036b879df50d00b542120428b1835e8d021e799f', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', 'ACTIVE', '2026-07-31 15:12:29', '2026-07-24 15:12:29', '2026-07-24 15:12:29', NULL);
INSERT INTO `user_session` VALUES (24, 4, 6, 'abfb1ba0c822714f6a12b2c4c77a7300c3674d47ea31db0936b76d2521fd6448', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 15:13:43', '2026-07-24 15:13:43', '2026-07-28 08:55:56', NULL);
INSERT INTO `user_session` VALUES (25, 4, 6, '8b3e566cbc15829e3320633fa615e7c69a29467515a05dc7932bc8fed0bf8274', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 15:15:56', '2026-07-24 15:15:56', '2026-07-24 15:15:56', NULL);
INSERT INTO `user_session` VALUES (26, 4, 6, 'ee0be315642d670883cd747098ffc0e39bec4658e15f28e14d70b7f41dec413f', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 15:41:10', '2026-07-24 15:41:10', '2026-07-24 15:41:10', NULL);
INSERT INTO `user_session` VALUES (27, 4, 6, 'ea2d6bfc53ab2f154f025e48963c7993e0096495137701db10fda4a854595970', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 15:58:37', '2026-07-24 15:58:37', '2026-07-24 15:58:37', NULL);
INSERT INTO `user_session` VALUES (28, 4, 6, '760c8f9e8731c42a5a1386e9852a3e0c0dd83329b0bdb3963fcc952045445580', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 16:19:21', '2026-07-24 16:19:21', '2026-07-24 16:19:21', NULL);
INSERT INTO `user_session` VALUES (29, 4, 6, 'af100eb2cf787d45c11ab7555f2d779d2c385572e2110cbf889555793f3d93a3', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 16:37:17', '2026-07-24 16:37:17', '2026-07-24 16:37:17', NULL);
INSERT INTO `user_session` VALUES (30, 1, 1, 'f37ae1b4c44c5eb566fdbbf826389efca94a0d0a88ed57f955d733d6a01ecc7f', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-07-31 16:41:33', '2026-07-24 16:41:33', '2026-07-24 16:41:33', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (31, 1, 1, '3f1cb651cdcbfb47d3aba467e4c2604a1fa86b0d7536c10085d08560581cbccb', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-07-31 16:53:08', '2026-07-24 16:53:08', '2026-07-24 16:53:08', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (32, 1, 1, '31b40c15cfc95d8a8a8197c31e2b6f711a75bebc87445f42b75f9707079474d0', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-07-31 16:54:43', '2026-07-24 16:54:43', '2026-07-24 16:54:43', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (33, 4, 6, 'e0be891252b8db742a1a1eb7bb2fb4959b4c24e5d7b4ba8dc58f451e30a378bc', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 17:09:27', '2026-07-24 17:09:27', '2026-07-24 17:09:27', NULL);
INSERT INTO `user_session` VALUES (34, 4, 6, '1165208c5521ce84f02087b2386a61f0ef81b3858968873cef1e3d5c77d683e5', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 17:32:44', '2026-07-24 17:32:44', '2026-07-24 17:32:44', NULL);
INSERT INTO `user_session` VALUES (35, 4, 6, '34876ab0473aa02d1a634e2e3084de927cd2deb3b632981b58f34906c8668008', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-07-31 17:53:42', '2026-07-24 17:53:42', '2026-07-27 10:09:45', NULL);
INSERT INTO `user_session` VALUES (36, 4, 6, 'f1ebabbcee4a668588ba95f37b21ae8ff5fe609f113492080b4f12395fa339e7', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-08-03 10:10:01', '2026-07-27 10:10:01', '2026-07-27 10:10:01', NULL);
INSERT INTO `user_session` VALUES (37, 1, 1, '3962d0fd95aa35bf0fe45f8ac431a5647d228fb9b80140dd836b2d52f7d4233e', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-03 10:11:45', '2026-07-27 10:11:45', '2026-07-27 10:11:45', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (38, 4, 6, '9a8d22f5c7b243f4d49444dfdb615a89175b83c41b0949b2bef52ad59f2160d5', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-08-03 10:12:11', '2026-07-27 10:12:11', '2026-07-27 10:12:11', NULL);
INSERT INTO `user_session` VALUES (39, 1, 1, '2e533b2534bd94268a0aa6cbf88c35828ca58d9140a5f1c98572b1783dbfbede', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-03 11:23:11', '2026-07-27 11:23:11', '2026-07-27 14:03:47', '2026-07-27 14:04:07');
INSERT INTO `user_session` VALUES (40, 4, 6, 'f0f73233d5b76356a018f1a213b1b5241036134a0f0423109e8d3cad14c6c530', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-03 14:21:00', '2026-07-27 14:21:00', '2026-07-27 14:48:37', '2026-07-27 14:59:45');
INSERT INTO `user_session` VALUES (41, 1, 1, '348d221431b7d44d37854d45c082af372611a93e21bbe8daa0998e3b8fa33390', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-08-03 14:48:56', '2026-07-27 14:48:56', '2026-07-27 14:48:56', NULL);
INSERT INTO `user_session` VALUES (42, 1, 1, '8a976d2fb75af1827260ac8061b76ce523e0710c5580fd0001fdea13805083fe', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-03 14:59:58', '2026-07-27 14:59:58', '2026-07-27 14:59:58', '2026-07-27 15:09:46');
INSERT INTO `user_session` VALUES (43, 4, 6, '04850094c6baad4d414df898a3540f781b9ca77dfd80048099fb5e43c8cd1a57', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-08-03 15:09:55', '2026-07-27 15:09:55', '2026-07-28 09:21:37', NULL);
INSERT INTO `user_session` VALUES (44, 1, 1, 'b7e5aedb98d601399153b9d378a17cc5adc9dabbdd9292f154a4ad3ba8b595f6', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Mobile Safari/537.36', 'ACTIVE', '2026-08-03 15:48:19', '2026-07-27 15:48:19', '2026-07-27 15:48:19', NULL);
INSERT INTO `user_session` VALUES (45, 1, 1, '30590fb55eda93d157d7bd6bb29246f0506a560981dce121c48ba40d90d11f22', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-08-03 16:07:32', '2026-07-27 16:07:32', '2026-07-27 16:07:32', NULL);
INSERT INTO `user_session` VALUES (46, 1, 1, '31b629ce06393d4144a52d0354316b93e1bdbef545e34a66b60d6e4eb1f0d345', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-04 09:48:38', '2026-07-28 09:48:38', '2026-07-28 11:05:03', '2026-07-28 11:13:46');
INSERT INTO `user_session` VALUES (47, 4, 6, 'de25287fde7f9c5a734fcb59a190f9b2b0736292b93a00692591f3e951e51434', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-08-04 11:13:53', '2026-07-28 11:13:53', '2026-07-28 16:34:54', NULL);
INSERT INTO `user_session` VALUES (48, 4, 6, '02d570ed66d47c23beeed2d0adec5750d04ebe4a090bfb3a4abd4f3e64e2f49f', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-04 17:59:34', '2026-07-28 17:59:34', '2026-07-29 14:36:02', '2026-07-29 14:36:10');
INSERT INTO `user_session` VALUES (49, 4, 6, '6b7e869e0a91baf2099ff366f6848e0d04f08f2dd06f329984cf3799d16cfefc', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-05 14:36:20', '2026-07-29 14:36:20', '2026-08-04 15:03:25', '2026-08-04 15:06:12');
INSERT INTO `user_session` VALUES (50, 4, 6, '966657c3b4d58c5a01cedcc641c81e555b3dbb167d1993fbea394ad3f121c7de', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-08-11 11:39:45', '2026-08-04 11:39:45', '2026-08-04 11:56:00', NULL);
INSERT INTO `user_session` VALUES (51, 1, 1, 'be5f749ef549cf20a5f296c9cce8353c27b2866fba46f1045f810f63f1f1883c', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-11 15:06:20', '2026-08-04 15:06:20', '2026-08-04 15:06:20', '2026-08-04 15:12:08');
INSERT INTO `user_session` VALUES (52, 4, 6, '6c75144f48275e53fee653cee7350b2237ecf618b10983ec1eb585ea5b54ac9f', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-11 15:12:15', '2026-08-04 15:12:15', '2026-08-04 18:09:54', '2026-08-04 18:10:26');
INSERT INTO `user_session` VALUES (53, 4, 6, '168a76405a47689895fb8d54ca3adbae49979eac4d82150affaa9fc06cee3e41', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-12 09:00:20', '2026-08-05 09:00:20', '2026-08-06 10:26:56', '2026-08-06 10:32:41');
INSERT INTO `user_session` VALUES (54, 1, 1, '2b57bfe08bbbe817ad7d4253b378f3299fa00d94f09f003585123514bb685b16', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-13 10:32:45', '2026-08-06 10:32:45', '2026-08-06 10:32:45', '2026-08-06 10:34:19');
INSERT INTO `user_session` VALUES (55, 4, 6, '3455b0bf02348516f387e5b53d8f925176a8eab4fdfd8eb9ef2ecda00a9af7e8', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-13 10:34:31', '2026-08-06 10:34:31', '2026-08-06 16:14:30', '2026-08-06 16:14:40');
INSERT INTO `user_session` VALUES (56, 4, 6, '50c49d06aca721b7ef197b2ec521b6b8578bb12887dcad9c5de8f0bddb7042a3', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-13 16:14:49', '2026-08-06 16:14:49', '2026-08-06 16:14:49', '2026-08-06 16:17:34');
INSERT INTO `user_session` VALUES (57, 1, 1, '4f95f53a7a637fa9a6655e98fb738e042337c3b73ab2df1d49f0e64ce2def635', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-13 16:17:37', '2026-08-06 16:17:37', '2026-08-06 16:17:37', '2026-08-06 16:17:41');
INSERT INTO `user_session` VALUES (58, 1, 1, 'cb07d730ea239199de9c5cb2e0725bfcf06be64090f6426d46dda665b99edba1', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'REVOKED', '2026-08-13 16:18:09', '2026-08-06 16:18:09', '2026-08-06 16:18:09', '2026-08-06 16:20:28');
INSERT INTO `user_session` VALUES (59, 4, 6, '2206594bb0d846b66506206843d74dc4195a5e0a757f6f47fa76de3945731356', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-08-13 16:20:35', '2026-08-06 16:20:35', '2026-08-06 16:59:57', NULL);
INSERT INTO `user_session` VALUES (60, 4, 6, '769e6c1e5783aaa509d97240333e0f77e8b5a4231bc8ba918d2030da27e65ea9', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', 'ACTIVE', '2026-08-14 10:27:26', '2026-08-07 10:27:26', '2026-08-07 10:28:28', NULL);

-- ----------------------------
-- Table structure for workflow_definition
-- ----------------------------
DROP TABLE IF EXISTS `workflow_definition`;
CREATE TABLE `workflow_definition`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `workflow_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `workflow_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version_no` int NOT NULL DEFAULT 1,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `graph_json` json NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_workflow_tenant_code`(`tenant_id` ASC, `workflow_code` ASC) USING BTREE,
  INDEX `idx_workflow_tenant_status`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workflow_definition
-- ----------------------------

-- ----------------------------
-- Table structure for workflow_execution
-- ----------------------------
DROP TABLE IF EXISTS `workflow_execution`;
CREATE TABLE `workflow_execution`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `session_id` bigint NULL DEFAULT NULL,
  `workflow_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `input_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `execution_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `heartbeat_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_workflow_execution_key`(`execution_key` ASC) USING BTREE,
  INDEX `idx_workflow_execution_tenant`(`tenant_id` ASC, `status` ASC, `started_at` ASC) USING BTREE,
  INDEX `idx_workflow_execution_heartbeat`(`status` ASC, `heartbeat_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workflow_execution
-- ----------------------------

-- ----------------------------
-- Table structure for workflow_execution_node
-- ----------------------------
DROP TABLE IF EXISTS `workflow_execution_node`;
CREATE TABLE `workflow_execution_node`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `execution_id` bigint NOT NULL,
  `node_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `node_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `attempt` int NOT NULL DEFAULT 0,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `input_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `output_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `trace_json` json NULL,
  `started_at` datetime NULL DEFAULT NULL,
  `finished_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_workflow_execution_node`(`execution_id` ASC, `node_id` ASC) USING BTREE,
  INDEX `idx_workflow_execution_node_status`(`execution_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workflow_execution_node
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;

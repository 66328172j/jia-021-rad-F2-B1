-- rad 辐射工作场所安全监测与源库管理 -- schema (jia-021)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：jia_021

CREATE TABLE IF NOT EXISTS t_rad_alarm_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '预警单号',
  site_id int DEFAULT NULL COMMENT '所属场所',
  qty decimal(12,2) DEFAULT NULL COMMENT '剂量率(μSv/h)',
  alarm_level decimal(14,4) DEFAULT NULL COMMENT '预警档位',
  status int DEFAULT NULL COMMENT '状态 0待处理 1已处理 2已办结',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='剂量预警单';

CREATE TABLE IF NOT EXISTS t_rad_dispose (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '处置单号',
  stage int DEFAULT NULL COMMENT '当前环节 0..3',
  status int DEFAULT NULL COMMENT '处置状态 0待发起 1在办 2已办结',
  content varchar(255) DEFAULT NULL COMMENT '备注',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次处置动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分区剂量异常处置单';

CREATE TABLE IF NOT EXISTS t_rad_dose_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '批次号',
  row_no int DEFAULT NULL COMMENT '原始行号',
  item_code varchar(64) DEFAULT NULL COMMENT '明细编码',
  qty decimal(12,2) DEFAULT NULL COMMENT '个人剂量当量(mSv)',
  status int DEFAULT NULL COMMENT '行状态 0待处理 1成功 2失败',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='个人剂量巡测明细';

CREATE TABLE IF NOT EXISTS t_rad_dose_rule (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(32) DEFAULT NULL COMMENT '规则编号',
  rule_name varchar(64) DEFAULT NULL COMMENT '规则名称',
  th1_max decimal(8,2) DEFAULT NULL COMMENT '剂量率正常档上限(μSv/h)',
  th2_max decimal(8,2) DEFAULT NULL COMMENT '剂量率关注档上限(μSv/h)',
  th3_max decimal(8,2) DEFAULT NULL COMMENT '剂量率管控档上限(μSv/h)',
  eff_start datetime DEFAULT NULL COMMENT '生效起始时刻',
  eff_end datetime DEFAULT NULL COMMENT '生效截止时刻(不含)',
  priority int DEFAULT NULL COMMENT '优先级(数值越大越优先)',
  status int DEFAULT NULL COMMENT '规则状态 0启用 1停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作场所剂量率判定规则';

CREATE TABLE IF NOT EXISTS t_rad_shield_plan (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '方案编号',
  node_no int DEFAULT NULL COMMENT '当前环节 0..2',
  sign_mode int DEFAULT NULL COMMENT '签批模式 0或签 1会签',
  need_count int DEFAULT NULL COMMENT '本环节应签人数',
  sign_count int DEFAULT NULL COMMENT '本环节已签票数',
  status int DEFAULT NULL COMMENT '单据状态 0审批中 1已通过 2已否决',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='屏蔽改造方案';

CREATE TABLE IF NOT EXISTS t_rad_site (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(128) DEFAULT NULL COMMENT '场所编号',
  site_name varchar(128) DEFAULT NULL COMMENT '场所名称',
  site_type varchar(128) DEFAULT NULL COMMENT '场所类型',
  area_name varchar(128) DEFAULT NULL COMMENT '所属分区',
  status int DEFAULT NULL COMMENT '档案状态 0在用 1停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='辐射工作场所档案';

CREATE TABLE IF NOT EXISTS t_rad_source_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '条目编号',
  due_at datetime DEFAULT NULL COMMENT '到期时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '活度/计量值',
  status int DEFAULT NULL COMMENT '状态 0待处理 1已处理 2失败',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='放射源出入库任务条目';

CREATE TABLE IF NOT EXISTS t_rad_dose_summary (
  id bigint NOT NULL COMMENT '主键',
  period varchar(7) DEFAULT NULL COMMENT '统计月份 yyyy-MM',
  site_id int DEFAULT NULL COMMENT '场所/机房',
  total_qty decimal(14,2) DEFAULT NULL COMMENT '剂量合计(mSv)',
  row_count int DEFAULT NULL COMMENT '参与汇总的明细行数',
  status int DEFAULT NULL COMMENT '汇总状态 0待生成 1已生成',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建人',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新人',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='场所剂量按月汇总';
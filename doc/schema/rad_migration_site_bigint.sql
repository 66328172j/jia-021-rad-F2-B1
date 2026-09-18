-- rad 辐射工作场所安全监测与源库管理 -- 存量数据迁移
-- 目的：把业务单据上的场所外键 site_id 与场所档案主键 t_rad_site.id 的口径对齐。
-- 背景：t_rad_site.id 为 bigint(雪花 ID)，而 t_rad_alarm_bill.site_id /
--       t_rad_dose_summary.site_id 历史上建成了 int，大 ID 会截断，
--       导致单据↔场所、明细↔汇总两处口径对不上。
-- 本脚本可重复执行（MODIFY 列本身幂等）；请在月底结账前执行。
-- 库：jia_021

-- 1) 外键列类型对齐为 bigint（与 t_rad_site.id 完全一致）
ALTER TABLE t_rad_alarm_bill
  MODIFY COLUMN site_id bigint DEFAULT NULL COMMENT '所属场所（对齐 t_rad_site.id）';

ALTER TABLE t_rad_dose_summary
  MODIFY COLUMN site_id bigint DEFAULT NULL COMMENT '场所/机房（对齐 t_rad_site.id）';

-- 2) 存量口径核对：列出「引用了已删除/停用/不存在场所」的明细，
--    这类数据在统一口径下不应继续参与业务；先 SELECT 核对，再按业务决定人工处理，
--    不做物理删除（保留环节链/台账，与应用层逻辑删除口径一致）。
-- SELECT b.id, b.bill_no, b.site_id
-- FROM t_rad_alarm_bill b
-- LEFT JOIN t_rad_site s
--   ON s.id = b.site_id
--  AND s.del_flag = 0
--  AND (s.status = 0 OR s.status IS NULL)
-- WHERE b.del_flag = 0
--   AND (b.site_id IS NOT NULL AND s.id IS NULL);

-- 3) 迁移后按月份重算汇总，使「月度汇总」与「明细台账」两处口径一致。
--    逐月调用（示例为 2026-08）：
--      定时任务配置 invokeTarget：radDoseSummaryTask.rebuildMonth('2026-08')
--    rebuild 为覆盖式重算（逻辑作废旧汇总行后整体重写），不会累加。

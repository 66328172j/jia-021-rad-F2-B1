-- rad 场所档案口径历史数据迁移（jia-021）
-- 背景：场所档案有效口径收敛为唯一一份（RadSiteCaliber）：
--   有效场所 = del_flag <> 1 AND status <> 1（0/NULL 中 NULL 按存量兼容视为正常/在用）
-- 新增、修改两处业务入口以及读取/汇总入口都以该口径为准。
-- 本脚本把存量 NULL 回填为显式 0，使迁移后的存量数据与新代码两处口径完全对上，
-- 不再依赖运行时的 NULL 兼容分支。迁移可重复执行（幂等）。

-- 1. 档案主表：NULL 回填为显式正常值
UPDATE t_rad_site SET del_flag = 0 WHERE del_flag IS NULL;
UPDATE t_rad_site SET status   = 0 WHERE status   IS NULL;

-- 2. 异常值收敛：历史脏数据若出现非 0/1 的状态，按停用挂起需人工复核，不在本脚本自动处理；
--    仅保证 0/1 两态。删除标记同理（只允许 0/1）。
-- SELECT id, site_no, site_name, status, del_flag FROM t_rad_site
--   WHERE status NOT IN (0,1) OR del_flag NOT IN (0,1);

-- 3. 关联一致性：预警单/月汇总挂在「已删除场所」上的历史行不改写业务数据
--    （历史月份汇总不随档案状态追溯调整）；读取侧口径只排除显式停用/删除场所，
--    档案缺失的历史场所不否决，与 RadSiteCaliber#isBlockedSiteId 对齐，无需数据订正。

-- 4. 校验：迁移后不应再有 NULL（应为 0 行）
-- SELECT COUNT(*) AS null_flag_cnt FROM t_rad_site WHERE del_flag IS NULL OR status IS NULL;

/*
 Navicat Premium Data Transfer

 Source Server         : 本地mysql
 Source Server Type    : MySQL
 Source Server Version : 50731
 Source Host           : localhost:3306
 Source Schema         : x-downloader

 Target Server Type    : MySQL
 Target Server Version : 50731
 File Encoding         : 65001

 Date: 18/06/2022 16:25:15
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for download_task
-- ----------------------------
DROP TABLE IF EXISTS `download_task`;
CREATE TABLE `download_task`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '原始url',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '下载协议 1-http 2-ftp 3-m3u8',
  `redo_count` int(6) NULL DEFAULT NULL COMMENT '下载次数',
  `node` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '下载节点',
  `status` int(4) NULL DEFAULT 1 COMMENT '下载状态\r\n 1 待执行\r\n 2 执行中\r\n 3  任务完成\r\n 4 任务异常',
  `cost_time` bigint(20) NULL DEFAULT NULL COMMENT '下载耗时',
  `create_time` datetime(0) NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_t`(`node`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of download_task
-- ----------------------------
INSERT INTO `download_task` VALUES (1, 'http://1257120875.vod2.myqcloud.com/0ef121cdvodtransgzp1257120875/3055695e5285890780828799271/v.f230.m3u8', '1', NULL, 'Thread-26', 2, NULL, NULL, '2022-06-15 09:25:59');
INSERT INTO `download_task` VALUES (2, 'http://1257120875.vod2.myqcloud.com/0ef121cdvodtransgzp1257120875/3055695e5285890780828799271/v.f230.m3u8', '1', NULL, 'Thread-26', 2, NULL, NULL, '2022-06-15 09:25:59');
INSERT INTO `download_task` VALUES (3, 'http://1257120875.vod2.myqcloud.com/0ef121cdvodtransgzp1257120875/3055695e5285890780828799271/v.f230.m3u8', '1', NULL, 'Thread-21', 2, NULL, NULL, '2022-06-15 09:25:59');
INSERT INTO `download_task` VALUES (4, 'http://1257120875.vod2.myqcloud.com/0ef121cdvodtransgzp1257120875/3055695e5285890780828799271/v.f230.m3u8', '1', NULL, 'Thread-21', 2, NULL, NULL, '2022-06-15 09:25:59');
INSERT INTO `download_task` VALUES (5, 'http://1257120875.vod2.myqcloud.com/0ef121cdvodtransgzp1257120875/3055695e5285890780828799271/v.f230.m3u8', '1', NULL, 'Thread-22', 2, NULL, NULL, '2022-06-15 09:25:59');
INSERT INTO `download_task` VALUES (6, 'http://1257120875.vod2.myqcloud.com/0ef121cdvodtransgzp1257120875/3055695e5285890780828799271/v.f230.m3u8', '1', NULL, 'Thread-22', 2, NULL, NULL, '2022-06-15 09:25:59');
INSERT INTO `download_task` VALUES (7, 'http://1257120875.vod2.myqcloud.com/0ef121cdvodtransgzp1257120875/3055695e5285890780828799271/v.f230.m3u8', '1', NULL, 'Thread-30', 2, NULL, NULL, '2022-06-15 09:25:59');
INSERT INTO `download_task` VALUES (8, 'http://1257120875.vod2.myqcloud.com/0ef121cdvodtransgzp1257120875/3055695e5285890780828799271/v.f230.m3u8', '1', NULL, 'Thread-30', 2, NULL, NULL, '2022-06-15 09:25:59');
INSERT INTO `download_task` VALUES (9, 'http://1257120875.vod2.myqcloud.com/0ef121cdvodtransgzp1257120875/3055695e5285890780828799271/v.f230.m3u8', '1', NULL, 'Thread-28', 2, NULL, NULL, '2022-06-15 09:25:59');

-- ----------------------------
-- Table structure for task_dispatcher
-- ----------------------------
DROP TABLE IF EXISTS `task_dispatcher`;
CREATE TABLE `task_dispatcher`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `task_id` int(11) NULL DEFAULT NULL COMMENT '任务id',
  `status` int(4) NULL DEFAULT NULL COMMENT '下载状态',
  `finished_time` datetime(0) NULL DEFAULT NULL COMMENT '下载完成时间',
  `node` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '下载节点',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务调度表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of task_dispatcher
-- ----------------------------

-- ----------------------------
-- Table structure for x_lock
-- ----------------------------
DROP TABLE IF EXISTS `x_lock`;
CREATE TABLE `x_lock`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `lock` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `holder` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '锁的持有者',
  `ttl` bigint(64) NULL DEFAULT NULL COMMENT '锁的过期时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of x_lock
-- ----------------------------
INSERT INTO `x_lock` VALUES (1, 'search_lock', NULL, 1653375831);
INSERT INTO `x_lock` VALUES (3, 'search_lock_2', NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;

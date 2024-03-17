package com.nageoffer.shotlink.admin.test;

public class UserTableShardingTest {
    public static final String SQL = "CREATE TABLE `t_user_%d` (\n" +
            "  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',\n" +
            "  `username` varchar(256) DEFAULT NULL COMMENT '用户名',\n" +
            "  `password` varchar(512) DEFAULT NULL COMMENT '密码',\n" +
            "  `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',\n" +
            "  `phone` varchar(128) DEFAULT NULL COMMENT '手机号',\n" +
            "  `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',\n" +
            "  `deletion_time` bigint(20) DEFAULT NULL COMMENT '注销时间戳',\n" +
            "  `create_time` datetime DEFAULT NULL COMMENT '创建时间',\n" +
            "  `update_time` datetime DEFAULT NULL COMMENT '修改时间',\n" +
            "  `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',\n" +
            "  PRIMARY KEY (`id`),\n" +
            "  UNIQUE KEY `idx_unique_username` (`username`) USING BTREE\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=1715030926162935810 DEFAULT CHARSET=utf8mb4;";

    public static String linkSQL = "CREATE TABLE `t_link_goto_%d`  (\n" +
            "  `id` bigint NOT NULL COMMENT 'ID',\n" +
            "  `gid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分组标识',\n" +
            "  `full_short_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '完整短链接',\n" +
            "  PRIMARY KEY (`id`) USING BTREE\n" +
            ") ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;";
    public static void main(String[] args) {
        for (int i = 0; i < 16; i++) {
            System.out.printf((linkSQL) + "%n", i);
        }
    }
}

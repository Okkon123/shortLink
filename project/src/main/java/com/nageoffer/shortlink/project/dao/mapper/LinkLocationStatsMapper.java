package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkLocationStatsDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 地区统计访问持久层
 */
@Mapper
public interface LinkLocationStatsMapper extends BaseMapper<LinkLocationStatsDO> {
    @Insert("INSERT INTO t_link_locale_stats (full_short_url, gid, date, cnt, country, province, city, adcode, create_time, update_time, del_flag) " +
            "VALUES( #{linkLocationStats.fullShortUrl}, #{linkLocationStats.gid}, #{linkLocationStats.date}, #{linkLocationStats.cnt}, #{linkLocationStats.country}, #{linkLocationStats.province}, #{linkLocationStats.city}, #{linkLocationStats.adcode}, NOW(), NOW(), 0) " +
            "ON DUPLICATE KEY UPDATE cnt = cnt +  #{linkLocationStats.cnt};")
    void shortLinkLocationStats(@Param("linkLocationStats") LinkLocationStatsDO linkLocationStatsDO);
}

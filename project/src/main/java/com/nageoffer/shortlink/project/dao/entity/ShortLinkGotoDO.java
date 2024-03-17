package com.nageoffer.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_link_goto")
public class ShortLinkGotoDO {
    @TableId(type = IdType.AUTO)
    /**
     * id
     */
    private Long id;
    /**
     * 分组ID
     */
    private String gid;
    /**
     * 完整短链接
     */
    private String fullShortUrl;
}

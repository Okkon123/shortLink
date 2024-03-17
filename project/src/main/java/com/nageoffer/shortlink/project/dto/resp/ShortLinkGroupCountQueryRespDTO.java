package com.nageoffer.shortlink.project.dto.resp;

import lombok.Data;

@Data
public class ShortLinkGroupCountQueryRespDTO {
    /**
     * 短链接分组标识
     */
    private String gid;
    /**
     * 分组下短连接数量
     */
    private Integer shortLinkCount;
}

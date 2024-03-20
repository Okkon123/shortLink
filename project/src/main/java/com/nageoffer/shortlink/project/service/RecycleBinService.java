package com.nageoffer.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLInkPageRespDTO;

public interface RecycleBinService extends IService<ShortLinkDO> {
    /**
     * 将短链接移至回收站
     */
    void saveRecycleBin(RecycleBinSaveReqDTO requestParam);
    /**
     * 分页查询回收站短链接
     */
    IPage<ShortLInkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);

}

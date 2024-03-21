package com.nageoffer.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import com.nageoffer.shortlink.project.dto.req.RecycleBinRemoveReqDTO;
import com.nageoffer.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLInkPageRespDTO;

public interface RecycleBinService extends IService<ShortLinkDO> {
    /**
     * 将短链接移至回收站
     */
    void saveRecycleBin(RecycleBinSaveReqDTO requestParam);
    /**
     * 分页查询回收站短链接
     */
    IPage<ShortLInkPageRespDTO> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam);
    /**
     * 从回收站恢复短链接
     */
    void recoverRecycleBin(RecycleBinRecoverReqDTO requestParam);

    /**
     * 移除短链接
     */
    void removeRecycleBin(RecycleBinRemoveReqDTO requestParam);
}

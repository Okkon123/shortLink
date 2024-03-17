package com.nageoffer.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLInkPageRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.util.List;

public interface ShortLinkService extends IService<ShortLinkDO> {
    /**
     * 新增短链接
     */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);

    /**
     * 分页查询短链接
     */
    IPage<ShortLInkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);

    /**
     * 查询短链接分组内数量
     * @param requestParam
     * @return
     */
    List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam);

    /**
     * 更新短链接
     * @param requestParam
     */
    void updateShortLink(ShortLinkUpdateReqDTO requestParam);

    /**
     * 短链接跳转
     * @param shortUri 短链接后缀
     * @param request   HTTP请求
     * @param response  HTTP响应
     */
    void restoreUrl(String shortUri, ServletRequest request, ServletResponse response);
}

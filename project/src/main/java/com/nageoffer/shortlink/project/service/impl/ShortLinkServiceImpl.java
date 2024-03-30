package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.Config.GotoDomainWhiteListConfiguration;
import com.nageoffer.shortlink.project.common.convention.exception.ClientException;
import com.nageoffer.shortlink.project.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.project.common.database.BaseDO;
import com.nageoffer.shortlink.project.common.enums.ValidDateTypeEum;
import com.nageoffer.shortlink.project.dao.entity.*;
import com.nageoffer.shortlink.project.dao.mapper.*;
import com.nageoffer.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.*;
import com.nageoffer.shortlink.project.mq.producer.ShortLinkStatsSaveProducer;
import com.nageoffer.shortlink.project.service.LinkStatsTodayService;
import com.nageoffer.shortlink.project.service.ShortLinkService;
import com.nageoffer.shortlink.project.toolkit.HashUtil;
import com.nageoffer.shortlink.project.toolkit.LinkUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.*;

/**
 * 短链接实现接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {
    private final RBloomFilter<String> shortUriCreatePenetrationBloomFilter;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private  final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocationStatsMapper linkLocationStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;
    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfiguration;
    private final LinkStatsTodayService linkStatsTodayService;
    private final ShortLinkStatsSaveProducer shortLinkStatsSaveProducer;

    @Value("${short-link.stats.location.amap-key}")
    private String amapKey;
    @Value("${short-link.domain.default}")
    private String createShortLinkDefaultDomain;
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl = StrBuilder.create(createShortLinkDefaultDomain)
                .append("/")
                .append(shortLinkSuffix)
                .toString();
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(createShortLinkDefaultDomain)
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .shortUri(shortLinkSuffix)
                .enableStatus(0)
                .fullShortUrl(fullShortUrl)
                .favicon(getFavicon(requestParam.getOriginUrl()))
                .totalUv(0)
                .totalPv(0)
                .totalUip(0)
                .build();
        ShortLinkGotoDO shortLinkGoto = ShortLinkGotoDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(requestParam.getGid())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGoto);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(String.format("短链接：%s 生成重复", fullShortUrl));
        }
        stringRedisTemplate.opsForValue().set(
                String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                requestParam.getOriginUrl(),
                LinkUtil.getLinkCacheValidDate(requestParam.getValidDate()),
                TimeUnit.MILLISECONDS);
        shortUriCreatePenetrationBloomFilter.add(fullShortUrl);
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    @Override
    public IPage<ShortLInkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        IPage<ShortLinkDO> resultPage = baseMapper.pageLink(requestParam);
        return resultPage.convert(each -> {
            ShortLInkPageRespDTO result = BeanUtil.toBean(each, ShortLInkPageRespDTO.class);
            result.setDomain("http:// + result.getDomain");
            return result;
        });
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid, count(*) as shortLinkCount")
                .eq("enable_status", 0)
                .in("gid", requestParam)
                .groupBy("gid");
        List<Map<String, Object>> shortLinkDOList = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(shortLinkDOList, ShortLinkGroupCountQueryRespDTO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        // verificationWhitelist(requestParam.getOriginUrl());
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO == null) {
            throw new ClientException("短链接记录不存在");
        }
        // 未改变原来gid
        if (Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid())) {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), ValidDateTypeEum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
            ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                    .domain(hasShortLinkDO.getDomain())
                    .shortUri(hasShortLinkDO.getShortUri())
                    .favicon(hasShortLinkDO.getFavicon())
                    .createdType(hasShortLinkDO.getCreatedType())
                    .gid(requestParam.getGid())
                    .originUrl(requestParam.getOriginUrl())
                    .describe(requestParam.getDescribe())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .build();
            baseMapper.update(shortLinkDO, updateWrapper);
        } else {    // 改变gid
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, requestParam.getFullShortUrl()));
            RLock rLock = readWriteLock.writeLock();
            if (!rLock.tryLock()) {
                throw new ServiceException("短链接正在被访问,请稍后再试");
            }
            try {
                // 逻辑删除
                LambdaUpdateWrapper<ShortLinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                        .eq(ShortLinkDO::getDelTime, 0)
                        .eq(ShortLinkDO::getEnableStatus, 0);
                ShortLinkDO delShortLinkDO = ShortLinkDO.builder()
                        .delTime(new Date())
                        .build();
                delShortLinkDO.setDelFlag(1);
                baseMapper.update(delShortLinkDO, linkUpdateWrapper);
                ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                        .domain(createShortLinkDefaultDomain)
                        .originUrl(requestParam.getOriginUrl())
                        .gid(requestParam.getGid())
                        .createdType(hasShortLinkDO.getCreatedType())
                        .validDate(requestParam.getValidDate())
                        .describe(requestParam.getDescribe())
                        .shortUri(hasShortLinkDO.getShortUri())
                        .enableStatus(hasShortLinkDO.getEnableStatus())
                        .totalPv(hasShortLinkDO.getTotalPv())
                        .totalUv(hasShortLinkDO.getTodayUv())
                        .totalUip(hasShortLinkDO.getTotalUip())
                        .fullShortUrl(hasShortLinkDO.getFullShortUrl())
                        .favicon(getFavicon(requestParam.getOriginUrl()))
                        .delTime(new Date())
                        .build();
                baseMapper.insert(shortLinkDO);
                // 更新LinkStatsToday表
                LambdaQueryWrapper<LinkStatsTodayDO> statsTodayQueryWrapper = Wrappers.lambdaQuery(LinkStatsTodayDO.class)
                        .eq(LinkStatsTodayDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkStatsTodayDO::getGid, hasShortLinkDO.getGid())
                        .eq(BaseDO::getDelFlag, 0);
                List<LinkStatsTodayDO> linkStatsTodayDOList = linkStatsTodayMapper.selectList(statsTodayQueryWrapper);
                if (CollUtil.isNotEmpty(linkStatsTodayDOList)) {
                    linkStatsTodayMapper.deleteBatchIds(linkStatsTodayDOList.stream()
                            .map(LinkStatsTodayDO::getGid)
                            .toList());
                    linkStatsTodayDOList.forEach(each -> each.setGid(requestParam.getGid()));
                    linkStatsTodayService.saveBatch(linkStatsTodayDOList);
                }
                // 更新link_goto表
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoDOQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkGotoDO::getGid, hasShortLinkDO.getGid());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoDOQueryWrapper);
                shortLinkGotoMapper.deleteById(hasShortLinkDO.getGid());
                shortLinkGotoDO.setGid(requestParam.getGid());
                shortLinkGotoMapper.insert(shortLinkGotoDO);
                // 更新link_access_stats表
                LambdaUpdateWrapper<LinkAccessStatsDO> linkAccessStatsUpdateWrapper =Wrappers.lambdaUpdate(LinkAccessStatsDO.class)
                        .eq(LinkAccessStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkAccessStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(BaseDO::getDelFlag, 0);
                LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkAccessStatsMapper.update(linkAccessStatsDO, linkAccessStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkLocationStatsDO> linkLocaleStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkLocationStatsDO.class)
                        .eq(LinkLocationStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkLocationStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkLocationStatsDO::getDelFlag, 0);
                // 更新link_locale_stats表
                LinkLocationStatsDO linkLocaleStatsDO = LinkLocationStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkLocationStatsMapper.update(linkLocaleStatsDO, linkLocaleStatsUpdateWrapper);
                // 更新link_os_stats 表
                LambdaUpdateWrapper<LinkOsStatsDO> linkOsStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkOsStatsDO.class)
                        .eq(LinkOsStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkOsStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkOsStatsDO::getDelFlag, 0);
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkOsStatsMapper.update(linkOsStatsDO, linkOsStatsUpdateWrapper);
                // 更新link_browser_stats表
                LambdaUpdateWrapper<LinkBrowserStatsDO> linkBrowserStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkBrowserStatsDO.class)
                        .eq(LinkBrowserStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkBrowserStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkBrowserStatsDO::getDelFlag, 0);
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkBrowserStatsMapper.update(linkBrowserStatsDO, linkBrowserStatsUpdateWrapper);
                // 更新link_device_stats表
                LambdaUpdateWrapper<LinkDeviceStatsDO> linkDeviceStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkDeviceStatsDO.class)
                        .eq(LinkDeviceStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkDeviceStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkDeviceStatsDO::getDelFlag, 0);
                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkDeviceStatsMapper.update(linkDeviceStatsDO, linkDeviceStatsUpdateWrapper);
                // 更新link_network_stats 表
                LambdaUpdateWrapper<LinkNetworkStatsDO> linkNetworkStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkNetworkStatsDO.class)
                        .eq(LinkNetworkStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkNetworkStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkNetworkStatsDO::getDelFlag, 0);
                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkNetworkStatsMapper.update(linkNetworkStatsDO, linkNetworkStatsUpdateWrapper);
                // 更新link_access_logs 表
                LambdaUpdateWrapper<LinkAccessLogsDO> linkAccessLogsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessLogsDO.class)
                        .eq(LinkAccessLogsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkAccessLogsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkAccessLogsDO::getDelFlag, 0);
                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkAccessLogsMapper.update(linkAccessLogsDO, linkAccessLogsUpdateWrapper);
            } finally {
                rLock.unlock();
            }
        }
        if (!Objects.equals(hasShortLinkDO.getValidDateType(), requestParam.getValidDateType())
        || !Objects.equals(hasShortLinkDO.getValidDate(), requestParam.getValidDate())) {
            stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
            if (hasShortLinkDO.getValidDate() != null && hasShortLinkDO.getValidDate().before(new Date())) {
                if (Objects.equals(requestParam.getValidDateType(), ValidDateTypeEum.PERMANENT.getType())
                || requestParam.getValidDate().after(new Date())) {
                    stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
                }
            }
        }

    }



    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        String serverName = request.getServerName();
        String serverPort = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, 80))
                .map(String::valueOf)
                .map(each -> ":" + each)
                .orElse("");
        String fullShortUrl = serverName + serverPort + "/" + shortUri;
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(originalLink)) {
            ShortLinkStatsRecordDTO statsRecord = buildStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStats(fullShortUrl, null, statsRecord);
            ((HttpServletResponse) response).sendRedirect(originalLink);
            return;
        }
        boolean hasLink = shortUriCreatePenetrationBloomFilter.contains(fullShortUrl);
        if (!hasLink) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(gotoIsNullShortLink)) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        RLock rLock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        rLock.lock();
        originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        try {
            if (StrUtil.isNotBlank(originalLink)) {
                ShortLinkStatsRecordDTO statsRecord = buildStatsRecordAndSetUser(fullShortUrl, request, response);
                shortLinkStats(fullShortUrl, null, statsRecord);
                ((HttpServletResponse) response).sendRedirect(originalLink);
                return;
            }
            LambdaQueryWrapper<ShortLinkGotoDO> linkGotoDOLambdaQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoDOLambdaQueryWrapper);
            if (shortLinkGotoDO == null) {
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-",  30, TimeUnit.MINUTES);
                // 没有记录
                return;
            }
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(BaseDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
            if (shortLinkDO == null || shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date())) {
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                return;
            }
            originalLink = shortLinkDO.getOriginUrl();
            stringRedisTemplate.opsForValue().set(
                    String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                    originalLink,
                    LinkUtil.getLinkCacheValidDate(shortLinkDO.getValidDate()),
                    TimeUnit.MILLISECONDS);
            ((HttpServletResponse) response).sendRedirect(originalLink);
            ShortLinkStatsRecordDTO statsRecord = buildStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStats(fullShortUrl, shortLinkDO.getGid(), statsRecord);
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        List<String> originUrls = requestParam.getOriginUrls();
        List<String> describes = requestParam.getDescribes();
        List<ShortLinkBaseInfoRespDTO> result = new ArrayList<>();
        for (int i = 0; i < originUrls.size(); i++) {
            ShortLinkCreateReqDTO shortLinkCreateReqDTO = BeanUtil.toBean(requestParam, ShortLinkCreateReqDTO.class);
            shortLinkCreateReqDTO.setOriginUrl(originUrls.get(i));
            shortLinkCreateReqDTO.setDescribe(describes.get(i));
            try {
                ShortLinkCreateRespDTO shortLink = createShortLink(shortLinkCreateReqDTO);
                ShortLinkBaseInfoRespDTO linkBaseInfoRespDTO = ShortLinkBaseInfoRespDTO.builder()
                        .fullShortUrl(shortLink.getFullShortUrl())
                        .originUrl(shortLink.getOriginUrl())
                        .describe(describes.get(i))
                        .build();
                result.add(linkBaseInfoRespDTO);
            } catch (Throwable ex) {
                log.error("批量创建短链接失败，原始参数:{}", originUrls.get(i));
            }
        }
        return ShortLinkBatchCreateRespDTO.builder()
                .total(result.size())
                .baseLinkInfos(result)
                .build();
    }
    @Override
    public void shortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecord) {
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("fullShortUrl", fullShortUrl);
        producerMap.put("gid", gid);
        producerMap.put("statsRecord", JSON.toJSONString(statsRecord));
        shortLinkStatsSaveProducer.send(producerMap);
    }

//    private void shortLinkStats(String fullShortUrl, String gid, ServletRequest request, ServletResponse response) {
//        // uv统计
//        AtomicBoolean uvFirstFlag = new AtomicBoolean();
//        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
//        try {
//            AtomicReference<String> uv = new AtomicReference<>();
//            Runnable addResponseCookieTask = () -> {
//                String actualUV = UUID.fastUUID().toString();
//                uv.set(actualUV);
//                Cookie uvCookie = new Cookie("uv", actualUV);
//                uvCookie.setMaxAge(60 * 60 * 24 * 30);
//                uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
//                ((HttpServletResponse) response).addCookie(uvCookie);
//                uvFirstFlag.set(Boolean.TRUE);
//                stringRedisTemplate.opsForSet().add(String.format(UV_FIRST_KEY, fullShortUrl), actualUV);
//            };
//            if (ArrayUtil.isNotEmpty(cookies)) {
//                Arrays.stream(cookies)
//                        .filter(each -> Objects.equals(each.getName(), "uv"))
//                        .findFirst()
//                        .map(Cookie::getValue)
//                        .ifPresentOrElse(each -> {
//                            uv.set(each);
//                            Long added = stringRedisTemplate.opsForSet().add(String.format(UV_FIRST_KEY, fullShortUrl), each);
//                            uvFirstFlag.set(added != null && added > 0L);
//                        }, addResponseCookieTask);
//            } else {
//                addResponseCookieTask.run();
//            }
//            // uip 统计
//            String remoteAddr = LinkUtil.getActualIp(((HttpServletRequest) request));
//            Long uipAdded = stringRedisTemplate.opsForSet().add(String.format(UIP_FIRST_KEY, remoteAddr), remoteAddr);
//            boolean uipFirstFlag = (uipAdded != null && uipAdded > 0L);
//            if (StrUtil.isBlank(gid)) {
//                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
//                                .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
//                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
//                gid = shortLinkGotoDO.getGid();
//            }
//            int hour = DateUtil.hour(new Date(), true);
//            Week week = DateUtil.dayOfWeekEnum(new Date());
//            int weekValue = week.getValue();
//            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
//                    .pv(1)
//                    .uv(uvFirstFlag.get() ? 1 : 0)
//                    .uip(uipFirstFlag ? 1 : 0)
//                    .hour(hour)
//                    .weekday(weekValue)
//                    .fullShortUrl(fullShortUrl)
//                    .gid(gid)
//                    .date(new Date())
//                    .build();
//            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
//            // 地区统计
//            Map<String, Object> locationParamMap = new HashMap<>();
//            locationParamMap.put("key", amapKey);
//            locationParamMap.put("ip", "remoteAddr");
//            String locationResultStr = HttpUtil.get(AMAP_REMOTE_URL, locationParamMap);
//            JSONObject locationResultObj = JSON.parseObject(locationResultStr);
//            String infoCode = locationResultObj.getString("infocode");
//            String actualProvince = "", actualCity = "";
//            if (StrUtil.isNotBlank(infoCode) && "10000".equals(infoCode)) {
//                String province = locationResultObj.getString("province");
//                String city = locationResultObj.getString("city");
//                boolean unknownFlag = "[]".equals(province);
//                actualProvince = unknownFlag ? "未知省" : province;
//                actualCity = unknownFlag ? "未知市" : city;
//                LinkLocationStatsDO linkLocationStatsDO = LinkLocationStatsDO.builder()
//                        .province(province)
//                        .city(city)
//                        .adcode(unknownFlag ? "未知" : locationResultObj.getString("adcode"))
//                        .cnt(1)
//                        .fullShortUrl(fullShortUrl)
//                        .country("中国")
//                        .gid(gid)
//                        .date(new Date())
//                        .build();
//                linkLocationStatsMapper.shortLinkLocationStats(linkLocationStatsDO);
//            }
//            // 操作系统统计
//            String os = LinkUtil.getOs(((HttpServletRequest) request));
//            LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
//                    .os(os)
//                    .cnt(1)
//                    .gid(gid)
//                    .date(new Date())
//                    .fullShortUrl(fullShortUrl)
//                    .build();
//            linkOsStatsMapper.shortLinkOsState(linkOsStatsDO);
//            // 浏览器访问统计
//            String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
//            LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
//                    .browser(browser)
//                    .cnt(1)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .date(new Date())
//                    .build();
//            linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);
//            // 访问设备统计
//            String device = LinkUtil.getDevice(((HttpServletRequest) request));
//            LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
//                    .device(device)
//                    .cnt(1)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .date(new Date())
//                    .build();
//            linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
//            // 访问网络统计
//            String network = LinkUtil.getNetwork(((HttpServletRequest) request));
//            LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
//                    .network(network)
//                    .cnt(1)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .date(new Date())
//                    .build();
//            linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
//            // 访问日志统计
//            LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
//                    .user(uv.get())
//                    .ip(remoteAddr)
//                    .browser(browser)
//                    .device(device)
//                    .network(network)
//                    .locale(StrUtil.join("-", "中国", actualProvince, actualCity))
//                    .os(os)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .build();
//            linkAccessLogsMapper.insert(linkAccessLogsDO);
//            // totalUv、totalPv、totalUip统计
//            baseMapper.incrementStats(gid, fullShortUrl, 1, uvFirstFlag.get() ? 1 : 0, uipFirstFlag ? 1 : 0);
//            // todayUv、todayPv、todayUip统计
//            LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
//                    .todayPv(1)
//                    .todayUv(uvFirstFlag.get() ? 1 : 0)
//                    .todayUip(uipFirstFlag ? 1 : 0)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .date(new Date())
//                    .build();
//            linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
//        } catch (Throwable ex) {
//            log.error("短链接访问量统计异常", ex);
//        }
//    }

    private String generateSuffix(ShortLinkCreateReqDTO requestParam) {
        int num = 0;
        String res;
        String originUrl = requestParam.getOriginUrl();
        while (true) {
            originUrl += System.currentTimeMillis() + UUID.randomUUID().toString();
            res = HashUtil.hashToBase62(originUrl);
            String fullShortUri = createShortLinkDefaultDomain + "/" + res;
            if (!shortUriCreatePenetrationBloomFilter.contains(fullShortUri)) {
                break;
            }
            num++;
            if (num > 10) {
                throw new ServiceException("短链接频繁生成，请稍后重试");
            }
        }
        return res;
    }

    private ShortLinkStatsRecordDTO buildStatsRecordAndSetUser(String fullShortUrl, ServletRequest request, ServletResponse response) {
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        AtomicReference<String> uv = new AtomicReference<>();
        Runnable addResponseCookieTask = () -> {
            uv.set(UUID.fastUUID().toString());
            Cookie uvCookie = new Cookie("uv", uv.get());
            uvCookie.setMaxAge(60 * 60 * 24 * 30);
            uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
            ((HttpServletResponse) response).addCookie(uvCookie);
            uvFirstFlag.set(Boolean.TRUE);
            stringRedisTemplate.opsForSet().add(UV_FIRST_KEY + fullShortUrl, uv.get());
        };
        if (ArrayUtil.isNotEmpty(cookies)) {
            Arrays.stream(cookies)
                    .filter(each -> Objects.equals(each.getName(), "uv"))
                    .findFirst()
                    .map(Cookie::getValue)
                    .ifPresentOrElse(each -> {
                        uv.set(each);
                        Long uvAdded = stringRedisTemplate.opsForSet().add(UV_FIRST_KEY + fullShortUrl, each);
                        uvFirstFlag.set(uvAdded != null && uvAdded > 0L);
                    }, addResponseCookieTask);
        } else {
            addResponseCookieTask.run();
        }
        String remoteAddr = LinkUtil.getActualIp(((HttpServletRequest) request));
        String os = LinkUtil.getOs((HttpServletRequest) request);
        String browser = LinkUtil.getBrowser((HttpServletRequest) request);
        String device = LinkUtil.getDevice((HttpServletRequest) request);
        String network = LinkUtil.getNetwork((HttpServletRequest) request);
        Long uipAdded = stringRedisTemplate.opsForSet().add(UIP_FIRST_KEY + fullShortUrl, remoteAddr);
        boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
        return ShortLinkStatsRecordDTO.builder()
                .fullShortUrl(fullShortUrl)
                .uv(uv.get())
                .uvFirstFlag(uvFirstFlag.get())
                .uipFirstFlag(uipFirstFlag)
                .remoteAddr(remoteAddr)
                .os(os)
                .browser(browser)
                .device(device)
                .network(network)
                .build();
    }
    @SneakyThrows
    private String getFavicon(String url) {
        URL targetUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (HttpURLConnection.HTTP_OK == responseCode) {
            Document document = Jsoup.connect(url).get();
            Element faviconLink = document.select("link[rel~=(?i)^(shortcut )?icon]").first();
            if (faviconLink != null) {
                return faviconLink.attr("abs:href");
            }
        }
        return null;
    }

    private void verificationWhitelist(String originUrl) {
        Boolean enable = gotoDomainWhiteListConfiguration.getEnable();
        if (enable == null | !enable) {
            return;
        }
        String domain = LinkUtil.extractDomain(originUrl);
        if (StrUtil.isBlank(domain)) {
            throw new ClientException("跳转链接填写错误");
        }
        List<String> details = gotoDomainWhiteListConfiguration.getDetails();
        if (!details.contains(domain)) {
            throw new ClientException("演示环境为避免恶意攻击，请生成以下网站跳转链接：" + gotoDomainWhiteListConfiguration.getNames());
        }
    }
}

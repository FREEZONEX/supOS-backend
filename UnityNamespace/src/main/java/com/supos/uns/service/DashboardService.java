package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.*;
import com.supos.common.dto.grafana.DashboardDto;
import com.supos.common.dto.grafana.PgDashboardParam;
import com.supos.common.event.CreateDashboardEvent;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.GrafanaUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.dao.mapper.DashboardMapper;
import com.supos.uns.dao.po.DashboardPo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘服务
 *
 * @author xinwangji@supos.com
 * @date 2024/10/29 10:37
 * @description
 */
@Service
@Slf4j
public class DashboardService extends ServiceImpl<DashboardMapper, DashboardPo> {

    @Resource
    private DashboardMapper dashboardMapper;
    @Resource
    private SystemConfig systemConfig;

    public PageResultDTO<DashboardDto> pageList(String keyword, PaginationDTO params) {
        Page<DashboardPo> page = new Page<>(params.getPageNo(), params.getPageSize());
        LambdaQueryWrapper<DashboardPo> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            qw.and(w -> w.like(DashboardPo::getName, keyword).or().like(DashboardPo::getDescription, keyword));
        }
        qw.orderByDesc(DashboardPo::getCreateTime);
        Page<DashboardPo> iPage = page(page, qw);
        List<DashboardDto> list = BeanUtil.copyToList(iPage.getRecords(), DashboardDto.class);
        PageResultDTO.PageResultDTOBuilder<DashboardDto> pageBuilder = PageResultDTO.<DashboardDto>builder().total(iPage.getTotal()).pageNo(params.getPageNo()).pageSize(params.getPageSize());
        return pageBuilder.code(0).data(list).build();
    }

    @Transactional(rollbackFor = Exception.class, timeout = 300)
    public JsonResult<DashboardPo> create(DashboardDto dashboardDto) {
        LambdaQueryWrapper<DashboardPo> qw = new LambdaQueryWrapper<>();
        qw.eq(DashboardPo::getName, dashboardDto.getName());
        qw.eq(DashboardPo::getType, dashboardDto.getType());
        if (count(qw) > 0) {
            return new JsonResult(500, I18nUtils.getMessage("uns.dashboard.name.duplicate"));
        }

        Date now = new Date();
        dashboardDto.setId(IdUtil.fastUUID());
        DashboardPo po = BeanUtil.copyProperties(dashboardDto, DashboardPo.class);
        po.setCreateTime(now);
        po.setUpdateTime(now);
        if (dashboardDto.getType() == 1) {
            String template = ResourceUtil.readUtf8Str("templates/dashboard-blank.json");
            PgDashboardParam pgParams = new PgDashboardParam();
            pgParams.setUid(dashboardDto.getId());
            pgParams.setTitle(dashboardDto.getName());
            Map<String, Object> dbParams = BeanUtil.beanToMap(pgParams);
            String dashboardJson = StrUtil.format(template, dbParams);
            log.info(">>>>>>>>>>>>>>>dashboardJson :{}", dashboardJson);
            HttpResponse dashboardResponse = HttpUtil.createPost(GrafanaUtils.getGrafanaUrl() + "/api/dashboards/db").body(dashboardJson).execute();
            log.info(dashboardResponse.body());
            if (200 != dashboardResponse.getStatus()) {
                return new JsonResult(500, I18nUtils.getMessage("uns.dashboard.create.failed"));
            }
        }
        save(po);
        return new JsonResult(0, "success", po);
    }

    public BaseResult edit(DashboardDto dashboardDto) {
        DashboardPo dashboardPo = getById(dashboardDto.getId());
        if (null == dashboardPo) {
            throw new BuzException("uns.dashboard.not.exit");
        }
        if (dashboardPo.getType() == 1) {
            JSONObject dbJson = GrafanaUtils.getDashboardByUuid(dashboardDto.getId());
            if (null == dbJson) {
                throw new BuzException("uns.dashboard.not.exit");
            }

            dbJson.getJSONObject("dashboard").put("title", dashboardDto.getName());
            dbJson.getJSONObject("dashboard").put("description", dashboardDto.getDescription());

            log.info(">>>>>>>>>>>>>>>dashboardJson :{}", dbJson);
            HttpResponse dashboardResponse = HttpUtil.createPost(GrafanaUtils.getGrafanaUrl() + "/api/dashboards/db").body(dbJson.toJSONString()).execute();
            log.info(dashboardResponse.body());
        }
        BeanUtil.copyProperties(dashboardDto, dashboardPo, "id", "type");
        updateById(dashboardPo);
        return new BaseResult(0, "success");
    }

    public boolean delete(String uid) {
        DashboardPo dashboardPo = getById(uid);
        if (null == dashboardPo) {
            return true;
        }
        if (1 == dashboardPo.getType() && null != systemConfig.getContainerMap().get("grafana")) {
            GrafanaUtils.deleteDashboard(uid);
        } else if (2 == dashboardPo.getType() && null != systemConfig.getContainerMap().get("fuxa")) {
            String url = Constants.FUXA_API_URL + "/api/project/" + uid;
            HttpResponse response = HttpRequest.delete(url).execute();
            log.info(">>>>>>>>>>>>>>>dashboard fuxa delete response code:{}", response.getStatus());
        }
        return removeById(uid);
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    public void onRemoveTopics(RemoveTopicsEvent event) {
        if (CollectionUtil.isEmpty(event.topics)) {
            return;
        }
        Collection<String> tables = event.topics.values().stream()
                .filter(even -> ObjectUtil.isNotNull(event.jdbcType))
                .map(SimpleUnsInstance::getTableName).collect(Collectors.toSet());
        List<String> ids = tables.stream().map(tableName -> {
            //tableName = alias
            return GrafanaUtils.getDashboardUuidByAlias(tableName);
        }).collect(Collectors.toList());

        for (List<String> idList : Lists.partition(ids, Constants.SQL_BATCH_SIZE)) {
            dashboardMapper.deleteBatchIds(idList);
        }
    }

    /**
     * 获取grafana详情
     */
    public ResultVO<JSONObject> getByUuid(String uuid) {
        JSONObject dbJson = GrafanaUtils.getDashboardByUuid(uuid);
        if (null == dbJson) {
            return ResultVO.fail(I18nUtils.getMessage("uns.dashboard.not.exit"));
        }
        return ResultVO.successWithData(dbJson);
    }

    @EventListener(classes = CreateDashboardEvent.class)
    public void createByHandler(CreateDashboardEvent event) {
        log.info("创建数据看板参数：{}", event);
        Date now = new Date();
        DashboardPo po = new DashboardPo();
        po.setId(event.uuid);
        po.setName(event.name);
        po.setCreateTime(now);
        po.setUpdateTime(now);
        boolean flag = dashboardMapper.insert(po) > 0;
        log.info("结束创建数据看板 name：{}，创建状态：{}", event.name, flag);
    }

}

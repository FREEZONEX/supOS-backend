package com.supos.adpter.kong.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.extra.validation.ValidationUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.adpter.kong.dao.ResourceMapper;
import com.supos.adpter.kong.dto.ResourceQuery;
import com.supos.common.dto.SaveResourceDto;
import com.supos.adpter.kong.po.ResourcePo;
import com.supos.adpter.kong.vo.ResourceVo;
import com.supos.adpter.kong.vo.ResultVO;
import com.supos.common.service.IResourceService;
import com.supos.common.utils.I18nUtils;
import jakarta.validation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResourceService extends ServiceImpl<ResourceMapper, ResourcePo> implements IResourceService {

    @Autowired
    private ResourceMapper resourceMapper;

    private static final long PLUG_NAV_PARENT_ID = 60L;//groupType = 1  导航 系统配置
    private static final long PLUG_MENU_PARENT_ID = 3;//groupType = 2  菜单 系统配置
    private static final long APP_NAV_PARENT_ID = 4L;//groupType = 1 导航 应用集
    private static final long APP_MENU_PARENT_ID = 90L;//groupType = 2  菜单 应用集

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    public ResultVO<List<ResourceVo>> getResourceList(ResourceQuery query) {
        var qw = new LambdaQueryWrapper<ResourcePo>();
        qw.eq(Objects.nonNull(query.getGroupType()), ResourcePo::getGroupType, query.getGroupType());
        qw.eq(Objects.nonNull(query.getType()), ResourcePo::getType, query.getType());
        qw.eq(Objects.nonNull(query.getParentId()), ResourcePo::getParentId, query.getParentId());
        List<ResourcePo> poList = list(qw);
        List<ResourceVo> voList = poList.stream().map(po -> {
            ResourceVo vo = BeanUtil.copyProperties(po, ResourceVo.class);
            vo.setShowName(I18nUtils.getMessage(po.getCode()));
            vo.setShowDescription(I18nUtils.getMessage(po.getDescription()));
            return vo;
        }).collect(Collectors.toList());
        return ResultVO.success(voList);
    }

    @Override
    public Long saveResource(SaveResourceDto saveResourceDto) {
        Set<ConstraintViolation<Object>> violations =  validator.validate(saveResourceDto);
        if (!violations.isEmpty()) {
            StringBuilder er = new StringBuilder(128);
            addValidErrMsg(er, violations);
            throw new ValidationException(er.toString());
        }
        var qw = new LambdaQueryWrapper<ResourcePo>();
        qw.eq(ResourcePo::getGroupType, saveResourceDto.getGroupType());
        qw.eq(ResourcePo::getCode, saveResourceDto.getCode());
        ResourcePo resource = resourceMapper.selectOne(qw);
        if (resource != null) {
            BeanUtil.copyProperties(saveResourceDto, resource, CopyOptions.create().ignoreNullValue());
            log.warn("已有相同code的资源 code:{} groupType:{}", saveResourceDto.getCode(), saveResourceDto.getGroupType());
        } else {
            resource = BeanUtil.copyProperties(saveResourceDto, ResourcePo.class);
        }
        saveOrUpdate(resource);
        return resource.getId();
    }

    /**
     * 根据资源编码删除及其子资源
     *
     * @param code 资源编码
     * @return
     */
    @Override
    public boolean deleteByCode(String code) {
        List<ResourcePo> resList = this.baseMapper.selectList(new QueryWrapper<ResourcePo>().eq("code", code));
        for (ResourcePo resourcePo : resList) {
            remove(new QueryWrapper<ResourcePo>().eq("parent_id", resourcePo.getId()));
        }
        return remove(new QueryWrapper<ResourcePo>().eq("code", code));
    }


    public void setButton() {
        String json = """
                {
                    "Home": [{ "code": "Home.import" }, { "code": "Home.export" }],
                    "Namespace": [
                      { "code": "Namespace.uns_import" },
                      { "code": "Namespace.uns_export" },
                      { "code": "Namespace.uns_batch_generation" },
                      { "code": "Namespace.label_detail" },
                      { "code": "Namespace.label_delete" },
                      { "code": "Namespace.label_add" },
                      { "code": "Namespace.folder_detail" },
                      { "code": "Namespace.folder_delete" },
                      { "code": "Namespace.folder_copy" },
                      { "code": "Namespace.folder_paste" },
                      { "code": "Namespace.folder_add" },
                      { "code": "Namespace.file_detail" },
                      { "code": "Namespace.file_delete" },
                      { "code": "Namespace.file_copy" },
                      { "code": "Namespace.file_paste" },
                      { "code": "Namespace.file_add" },
                      { "code": "Namespace.template_detail" },
                      { "code": "Namespace.template_delete" },
                      { "code": "Namespace.template_copy" },
                      { "code": "Namespace.template_add" }
                    ],
                    "SourceFlow": [
                      { "code": "SourceFlow.add" },
                      { "code": "SourceFlow.edit" },
                      { "code": "SourceFlow.delete" },
                      { "code": "SourceFlow.design" },
                      { "code": "SourceFlow.save" },
                      { "code": "SourceFlow.deploy" },
                      { "code": "SourceFlow.import" },
                      { "code": "SourceFlow.export" },
                      { "code": "SourceFlow.node_management" },
                      { "code": "SourceFlow.process" },
                      { "code": "SourceFlow.copy" }
                    ],
                    "EventFlow": [
                      { "code": "EventFlow.add" },
                      { "code": "EventFlow.edit" },
                      { "code": "EventFlow.delete" },
                      { "code": "EventFlow.design" },
                      { "code": "EventFlow.save" },
                      { "code": "EventFlow.deploy" },
                      { "code": "EventFlow.import" },
                      { "code": "EventFlow.export" },
                      { "code": "EventFlow.nodeManagement" },
                      { "code": "EventFlow.process" },
                      { "code": "EventFlow.copy" }
                    ],
                    "Dashboards": [
                      { "code": "Dashboards.add" },
                      { "code": "Dashboards.preview" },
                      { "code": "Dashboards.design" },
                      { "code": "Dashboards.edit" },
                      { "code": "Dashboards.delete" },
                      { "code": "Dashboards.save" },
                      { "code": "Dashboards.export" },
                      { "code": "Dashboards.import" }
                    ],
                    "UserManagement": [
                      { "code": "UserManagement.add" },
                      { "code": "UserManagement.edit" },
                      { "code": "UserManagement.enable" },
                      { "code": "UserManagement.disable" },
                      { "code": "UserManagement.resetPassword" },
                      { "code": "UserManagement.delete" },
                      { "code": "UserManagement.role_setting" }
                    ],
                    "PluginManagement": [
                      { "code": "PluginManagement.install" },
                      { "code": "PluginManagement.unInstall" },
                      { "code": "PluginManagement.update" }
                    ],
                    "Alert": [
                      { "code": "Alert.edit" },
                      { "code": "Alert.show" },
                      { "code": "Alert.delete" },
                      { "code": "Alert.add" },
                      { "code": "Alert.confirm" }
                    ],
                    "AppManagement": [
                      { "code": "AppManagement.install" },
                      { "code": "AppManagement.unInstall" },
                      { "code": "AppManagement.config_update" },
                      { "code": "AppManagement.pause" },
                      { "code": "AppManagement.start" },
                      { "code": "AppManagement.upload" },
                      { "code": "AppManagement.delete" }
                    ],
                    "CodeManagement": [
                      { "code": "CodeManagement.code_add" },
                      { "code": "CodeManagement.code_delete" },
                      { "code": "CodeManagement.code_edit" },
                      { "code": "CodeManagement.codeValue_add" },
                      { "code": "CodeManagement.codeValue_edit" },
                      { "code": "CodeManagement.codeValue_delete" }
                    ],
                    "CollectionGatewayManagement": [
                      { "code": "CollectionGatewayManagement.edit" },
                      { "code": "CollectionGatewayManagement.view" },
                      { "code": "CollectionGatewayManagement.copy" },
                      { "code": "CollectionGatewayManagement.add" },
                      { "code": "CollectionGatewayManagement.delete" }
                    ],
                    "NotificationManagement": [
                      { "code": "NotificationManagement.setting" },
                      { "code": "NotificationManagement.add" },
                      { "code": "NotificationManagement.edit" },
                      { "code": "NotificationManagement.delete" }
                    ],
                    "OpenData": [
                      { "code": "OpenData.addKey" },
                      { "code": "OpenData.enable" },
                      { "code": "OpenData.disable" },
                      { "code": "OpenData.delete" }
                    ],
                    "WebHooks": [
                      { "code": "WebHooks.copy" },
                      { "code": "WebHooks.edit" },
                      { "code": "WebHooks.delete" },
                      { "code": "WebHooks.add" }
                    ]
                  }
                """;

        JSONObject obj = JSONObject.parseObject(json);
        for (String parent : obj.keySet()) {
            ResourcePo parentRes = this.baseMapper.selectOne(new QueryWrapper<ResourcePo>().eq("code", parent).eq("group_type", 1));
            if (parentRes == null) {
                System.out.println();
            }
            JSONArray child = obj.getJSONArray(parent);
            for (int i = 0; i < child.size(); i++) {
                JSONObject c = child.getJSONObject(i);
                String code = c.getString("code");
                System.out.println();
                ResourcePo childRes = new ResourcePo();
                childRes.setGroupType(1);
                if (parentRes != null) {
                    childRes.setParentId(parentRes.getId());
                }
                childRes.setType(3);
                childRes.setCode(code);
                this.baseMapper.insert(childRes);
            }
        }
        System.out.println();
    }

    private static void addValidErrMsg(StringBuilder er, Set<ConstraintViolation<Object>> violations) {
        for (ConstraintViolation<Object> v : violations) {
            String t = v.getRootBeanClass().getSimpleName();
            er.append('[').append(t).append('.').append(v.getPropertyPath()).append(' ').append(I18nUtils.getMessage(v.getMessage())).append(']');
        }
    }

}

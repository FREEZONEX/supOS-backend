package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.adpter.kong.service.MenuService;
import com.supos.common.dto.resource.SaveResource4ExternalDto;
import com.supos.i18n.dao.po.I18nResourcePO;
import com.supos.uns.dao.mapper.ResourceMapper;
import com.supos.adpter.kong.dto.MenuDto;
import com.supos.adpter.kong.dto.ResourceQuery;
import com.supos.uns.dao.po.ResourcePo;
import com.supos.adpter.kong.vo.ResourceVo;
import com.supos.adpter.kong.vo.ResultVO;
import com.supos.common.dto.resource.BatchUpdateResourceDto;
import com.supos.common.dto.resource.SaveResourceDto;
import com.supos.common.exception.BuzException;
import com.supos.common.service.IResourceService;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.UserContext;
import com.supos.i18n.common.Constants;
import com.supos.i18n.service.I18nResourceService;
import com.supos.uns.service.PersonConfigService;
import com.supos.uns.vo.PersonConfigVo;
import jakarta.validation.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResourceService extends ServiceImpl<ResourceMapper, ResourcePo> implements IResourceService {

    @Autowired
    private ResourceMapper resourceMapper;
    @Autowired
    private MenuService menuService;
    @Autowired
    private I18nResourceService i18nResourceService;
    @Autowired
    private PersonConfigService personConfigService;

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    public ResultVO<List<ResourceVo>> getResourceList(ResourceQuery query) {
        var qw = new LambdaQueryWrapper<ResourcePo>();
        qw.eq(Objects.nonNull(query.getType()), ResourcePo::getType, query.getType());
        qw.eq(Objects.nonNull(query.getParentId()), ResourcePo::getParentId, query.getParentId());
        List<ResourcePo> poList = list(qw);
        List<ResourceVo> voList = poList.stream().map(po -> {
            ResourceVo vo = BeanUtil.copyProperties(po, ResourceVo.class);
            vo.setShowName(I18nUtils.getMessage(po.getNameCode()));
            vo.setShowDescription(I18nUtils.getMessage(po.getDescriptionCode()));
            return vo;
        }).collect(Collectors.toList());
        return ResultVO.success(voList);
    }
    

    @Transactional
    public ResultVO saveResourceAndChildren(SaveResourceDto dto){
        //父级
        Long parentId = saveOrUpdate(dto);
        List<SaveResourceDto> children = dto.getChildren();
        //子集新增
        if (CollectionUtil.isNotEmpty(children)) {
            for (SaveResourceDto child : children) {
                child.setParentId(parentId);
                saveOrUpdate(child);
            }
        }
        return ResultVO.success("ok");
    }


    @Override
    public Long saveByExternal(SaveResource4ExternalDto dto) {
        Set<ConstraintViolation<Object>> violations =  validator.validate(dto);
        if (!violations.isEmpty()) {
            StringBuilder er = new StringBuilder(128);
            addValidErrMsg(er, violations);
            throw new ValidationException(er.toString());
        }
        LambdaQueryWrapper<ResourcePo> qw = new LambdaQueryWrapper<>();
        qw.eq(ResourcePo::getCode, dto.getCode());
        ResourcePo resource = resourceMapper.selectOne(qw);
        if (resource != null) {
            BeanUtil.copyProperties(dto, resource, CopyOptions.create().ignoreNullValue());
            log.warn("saveResource 已有相同code的资源 code:{} , update!", dto.getCode());
        } else {
            resource = BeanUtil.copyProperties(dto, ResourcePo.class);
            resource.setRouteSource(2);
        }
        boolean status = saveOrUpdate(resource);
        log.info(">>>>>>>>>>>>>菜单资源source:{},code:{},保存状态：{}",dto.getSource(),dto.getCode(),status);
        return resource.getId();
    }

    @Transactional
    public ResultVO batchUpdate(List<BatchUpdateResourceDto> dtos){
        List<ResourcePo> poList = dtos.stream().map(dto -> BeanUtil.copyProperties(dto, ResourcePo.class)).collect(Collectors.toList());
//        saveOrUpdateBatch(poList);
        updateBatchById(poList);
        return ResultVO.success("ok");
    }


    public ResultVO deleteById(Long id) {
        ResourcePo po = getById(id);
        if (po != null) {
            removeById(id);
            remove(new QueryWrapper<ResourcePo>().eq("parent_id", id));
            try {
                i18nResourceService.deleteResources(Constants.DEFAULT_MODULE_CODE, po.getNameCode());
                i18nResourceService.deleteResources(Constants.DEFAULT_MODULE_CODE, po.getDescriptionCode());
            } catch (Exception e) {
                log.warn("", e);
            }
        }
        return ResultVO.success("ok");
    }

    public ResultVO batchDelete(List<Long> ids){
        remove(new LambdaQueryWrapper<ResourcePo>().in(ResourcePo::getId, ids));
        remove(new LambdaQueryWrapper<ResourcePo>().in(ResourcePo::getParentId, ids));
        return ResultVO.success("ok");
    }


    @Override
    public boolean deleteByCode(String code) {
        List<ResourcePo> resList = this.baseMapper.selectList(new QueryWrapper<ResourcePo>().eq("code", code));
        for (ResourcePo resourcePo : resList) {
            remove(new QueryWrapper<ResourcePo>().eq("parent_id", resourcePo.getId()));
        }
        return remove(new QueryWrapper<ResourcePo>().eq("code", code));
    }

    @Override
    public boolean deleteBySource(String source) {
        boolean status = remove(new LambdaQueryWrapper<ResourcePo>().eq(ResourcePo::getSource, source));
        log.info(">>>>>>>>>>>>>菜单资源source:{},删除状态：{}",source, status);
        return status;
    }

    @Transactional
    public Long saveOrUpdate(SaveResourceDto dto){
        PersonConfigVo person = personConfigService.getByUserId(UserContext.get().getSub());
        String lang = person.getMainLanguage().replaceAll("-","_");
        Long id = dto.getId();
        String code = dto.getCode();
        String url = dto.getUrl();
        String name = dto.getName();
        String description = dto.getDescription();
        //新增
        if (id == null) {
            long countCode = count(new LambdaQueryWrapper<ResourcePo>().eq(ResourcePo::getCode, code));
            if (countCode > 0) {
                throw new BuzException("resource.code.duplicate", code);
            }
            ResourcePo po = BeanUtil.copyProperties(dto, ResourcePo.class);
            //名称和描述国际化
            String nameCode = addI18n(lang, code, name);
            po.setNameCode(nameCode);
            if (StringUtils.isNotBlank(description)) {
                String descCode = addI18n(lang, code + "#desc", description);
                po.setDescriptionCode(descCode);
            }
//            registerRoute(code ,url);
            save(po);
            id = po.getId();
        } else {
            //修改
            ResourcePo po = getById(id);
            if (po == null) {
                throw new BuzException("resource.id.not.found", id);
            }
            BeanUtil.copyProperties(dto, po, CopyOptions.create().ignoreNullValue());
            String nameCode = po.getNameCode();
            //菜单名称国际化
            updateI18nResource(lang, name, nameCode);

            //描述 国际化
            if (StringUtils.isNotBlank(description)) {
                String descCode = po.getDescriptionCode();
                if (StringUtils.isNotBlank(descCode)) {
                    //更新
                    updateI18nResource(lang, description, descCode);
                } else {
                    //新增
                    descCode = addI18n(lang, code + "#desc", description);
                    po.setDescriptionCode(descCode);
                }
            }
            LambdaUpdateWrapper<ResourcePo> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(ResourcePo::getId, id)
                    .set(Objects.isNull(po.getParentId()), ResourcePo::getParentId, null)
                    .set(StringUtils.isBlank(po.getIcon()), ResourcePo::getIcon, null)
                    .set(StringUtils.isBlank(po.getDescriptionCode()), ResourcePo::getDescriptionCode, null);
            this.baseMapper.update(po, wrapper);
        }
        return id;
    }

    private void updateI18nResource(String lang, String name, String code) {
        List<I18nResourcePO> i18nResList = i18nResourceService.getResourceByKey(Constants.DEFAULT_MODULE_CODE, code);
        //key=lang value=值
        Map<String, String> i18nValues = new HashMap<>();
        for (I18nResourcePO i18nPO : i18nResList) {
            if (i18nPO.getLanguageCode().equals(lang)) {
                i18nPO.setI18nValue(name);
            }
            i18nValues.put(i18nPO.getLanguageCode(), i18nPO.getI18nValue());
        }
        com.supos.i18n.dto.SaveResourceDto i18nRes = new com.supos.i18n.dto.SaveResourceDto();
        i18nRes.setKey(code);
        i18nRes.setModuleCode(Constants.DEFAULT_MODULE_CODE);
        i18nRes.setValues(i18nValues);
        i18nResourceService.updateResources(i18nRes);
    }

    private String addI18n(String lang, String code, String name) {
        String i18nCode = generateI18nCode(code);
        String i18nVal = i18nResourceService.getResourceByKey(lang, Constants.DEFAULT_MODULE_CODE, i18nCode);
        //创建 国际化
        if (StringUtils.isBlank(i18nVal)) {
            com.supos.i18n.dto.SaveResourceDto i18nRes = new com.supos.i18n.dto.SaveResourceDto();
            i18nRes.setModuleCode(Constants.DEFAULT_MODULE_CODE);
            i18nRes.setKey(i18nCode);
            i18nRes.setValues(Map.of(lang, name));
            i18nResourceService.addResources(i18nRes);
        }
        return i18nCode;
    }

    private void registerRoute(String code, String url) {
        if (StringUtils.isNotBlank(url)) {
            boolean absolutePath = isAbsolutePath(url);
            //绝对路径 需要注册kong
            if (absolutePath) {
                MenuDto menuDto = new MenuDto();
                menuDto.setName(code);
                menuDto.setServiceName("frontend");
                menuDto.setBaseUrl(url);
                try {
                    menuService.createRoutewithNoService(menuDto, false, false);
                } catch (Exception e) {
                    log.error("资源注册Kong路由异常", e);
                    throw new BuzException("resource.register.kong.route.failed");
                }
            }
        }
    }

    public static boolean isAbsolutePath(String path) {
        // 1. 完整 URL (带协议)
        if (path.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            return true;
        }

        // 2. "/" 开头的绝对路径
        if (path.startsWith("/")) {
            return true;
        }

        // 3. 用户写的 demo/xx/ss 这种情况
        //    规则：不以 "./" 或 "../" 开头的，都认为是绝对路径
        if (!(path.startsWith("./") || path.startsWith("../"))) {
            return true;
        }

        // 4. 否则是相对路径
        return false;
    }

    private static void addValidErrMsg(StringBuilder er, Set<ConstraintViolation<Object>> violations) {
        for (ConstraintViolation<Object> v : violations) {
            String t = v.getRootBeanClass().getSimpleName();
            er.append('[').append(t).append('.').append(v.getPropertyPath()).append(' ').append(I18nUtils.getMessage(v.getMessage())).append(']');
        }
    }

    private String generateI18nCode(String code){
        return code + "#" + RandomUtil.randomString(6);
    }
}

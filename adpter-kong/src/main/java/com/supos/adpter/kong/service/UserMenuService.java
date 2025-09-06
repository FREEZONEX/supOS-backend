package com.supos.adpter.kong.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.adpter.kong.dao.ResourceMapper;
import com.supos.adpter.kong.dao.UserMenuMapper;
import com.supos.adpter.kong.po.ResourcePo;
import com.supos.adpter.kong.po.UserMenuPo;
import com.supos.adpter.kong.vo.ResultVO;
import com.supos.adpter.kong.vo.RouteVO;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.UserMenuDto;
import com.supos.common.dto.protocol.KeyValuePair;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.SuposIdUtil;
import com.supos.common.utils.UserContext;
import com.supos.common.vo.UserInfoVo;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xinwangji@supos.com
 * @date 2024/12/10 10:31
 * @description
 */
@Service
public class UserMenuService extends ServiceImpl<UserMenuMapper, UserMenuPo> {

    @Resource
    private KongAdapterService kongAdapterService;
    @Resource
    private SystemConfig systemConfig;
    @Autowired
    private ResourceMapper resourceMapper;

    public List<RouteVO> getUserRouteList(){
        List<RouteVO> routeList = kongAdapterService.queryRoutes();

        if (BooleanUtil.isTrue(systemConfig.getAuthEnable())) {
            UserInfoVo userInfoVo = UserContext.get();
            if (null == userInfoVo){
                return routeList;
            }
            Date now = new Date();
            List<UserMenuPo> userMenuList = getUserMenuList(userInfoVo.getSub());
            if (CollectionUtil.isEmpty(userMenuList)) {
                userMenuList = routeList.stream().map(route -> {
                    UserMenuPo po = new UserMenuPo();
                    po.setId(IdUtil.getSnowflake().nextId());
                    po.setUserId(userInfoVo.getSub());
                    po.setMenuName(route.getName());
                    po.setPicked(true);
                    po.setUpdateTime(now);
                    po.setCreateTime(now);
                    return po;
                }).collect(Collectors.toList());
                saveBatch(userMenuList);
            }
            Map<String, Boolean> menuPickedMap = userMenuList.stream().collect(Collectors.toMap(UserMenuPo::getMenuName, UserMenuPo::getPicked));
            routeList.forEach(routeVO -> {
                if (ObjectUtil.isNotNull(routeVO.getMenu())){
                    routeVO.getMenu().setPicked(menuPickedMap.getOrDefault(routeVO.getName(),true));
                }
            });
        } else {
            routeList.forEach(routeVO -> {
                if (ObjectUtil.isNotNull(routeVO.getMenu())){
                    routeVO.getMenu().setPicked(true);
                }
            });
        }
        return routeList;
    }


    public List<UserMenuPo> getUserMenuList(String userId){
        LambdaQueryWrapper<UserMenuPo> qw = new LambdaQueryWrapper<>();
        qw.eq(UserMenuPo::getUserId,userId);
        return list(qw);
    }

    public ResultVO setUserMenu(List<UserMenuDto> userMenuList){
        UserInfoVo userInfoVo = UserContext.get();
        if (null == userInfoVo){
            return ResultVO.fail(I18nUtils.getMessage("user.not.login"));
        }

        Date now = new Date();
        for (UserMenuDto userMenuDto : userMenuList) {
            UserMenuPo po = this.baseMapper.getByMenuName(userInfoVo.getSub(),userMenuDto.getMenuName());
            if (po == null){
                po = new UserMenuPo();
                po.setId(SuposIdUtil.nextId());
                po.setUserId(userInfoVo.getSub());
                po.setMenuName(userMenuDto.getMenuName());
            }
            po.setPicked(userMenuDto.getPicked());
            po.setUpdateTime(now);
            saveOrUpdate(po);
        }
        return ResultVO.success("ok");
    }
    
    public void saveResourceByRoutes(){
        Map<String,String> parentMap = new HashMap<>();
        List<RouteVO> routeList = kongAdapterService.queryRoutes();
        List<ResourcePo> resourcePoList = new ArrayList<>();
        for (RouteVO route : routeList) {
            ResourcePo resource = new ResourcePo();
            resource.setGroupType(1);
//            resource.setParentId();
            resource.setType(2);
            resource.setCode(route.getName());
            resource.setUrl(route.getMenu().getUrl());
            resource.setUrlType(1);
            resource.setOpenType(1);
            resource.setRemark(route.getShowName());
            resource.setEnable(true);

            if (StringUtils.isBlank(resource.getIcon())){
                resource.setIcon(route.getName() + ".svg");
            }
            parserTag(resource, route.getTags(), parentMap,resourcePoList);
        }

        for (String parentKey : parentMap.keySet()) {
            ResourcePo parent = new ResourcePo();
            parent.setType(1);
            parent.setGroupType(2);
            parent.setCode(parentKey);
            parent.setIcon(parentKey + ".svg");
            parent.setRemark(parentMap.get(parentKey));
            resourcePoList.add(parent);
        }
        
        resourceMapper.insert(resourcePoList);
    }

    private void parserTag(ResourcePo resource ,List<KeyValuePair<String>> tags,Map<String,String> parentMap,List<ResourcePo> resourcePoList){
        for (KeyValuePair<String> tag : tags) {
            String tagKey = tag.getKey();
            String tagVal = tag.getValue();
            if (tagKey.startsWith("homeParentName:")){//导航栏
                String code = tagKey.split(":")[1];
                parentMap.putIfAbsent(code,I18nUtils.getMessage(code));
                resource.setGroupType(1);
            } else if (tagKey.startsWith("parentName:")){
                String code = tagKey.split(":")[1];
                parentMap.putIfAbsent(code,I18nUtils.getMessage(code));
                resource.setGroupType(2);
            } else if (tagKey.startsWith("description:")){
                String code = tagKey.split(":")[1];
                resource.setDescription(code);
            }

            if (StringUtils.isNotBlank(tagVal)){
                if (tagVal.startsWith("sort:")){
                    resource.setSort(Integer.valueOf(tagVal.split(":")[1]));
                } else if (tagVal.startsWith("iconUrl:")){
                    resource.setIcon(tagVal.split(":")[1]);
                } else if (tagVal.startsWith("homeIconUrl:")){
                    String homeIcon = tagVal.split(":")[1];
                    resource.setIcon(homeIcon + ".svg");
                }
            }
        }

        if (resource.getGroupType() == 1){
            ResourcePo menu = BeanUtil.copyProperties(resource,ResourcePo.class);
            menu.setGroupType(2);
            menu.setIcon(resource.getCode() + ".svg");
            resourcePoList.add(menu);
        }
        resourcePoList.add(resource);
    }

}

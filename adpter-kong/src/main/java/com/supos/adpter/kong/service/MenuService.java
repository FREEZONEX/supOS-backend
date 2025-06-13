package com.supos.adpter.kong.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONObject;
import com.supos.adpter.kong.dto.MenuDto;
import com.supos.adpter.kong.vo.RoutResponseVO;
import com.supos.adpter.kong.vo.ServiceResponseVO;
import com.supos.common.Constants;
import com.supos.common.exception.BuzException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: MenuService
 * @date 2025/4/17 9:21
 */
@Slf4j
@Service
public class MenuService {

    private static final String iCON_ROOT_PATH = Constants.ROOT_PATH + "/system/resource/supos/";

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Autowired
    private KongAdapterService kongAdapterService;

    public void createRoutewithNoService(MenuDto menuDto, boolean needApiKey, boolean updateService) {

        // 获取或者创建服务
        Pair<String, String> service = fetchOrCreateService(menuDto.getServiceName(), updateService, null, null, 0);;

        List<String> tags = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(menuDto.getTags())) {
            tags.addAll(menuDto.getTags());
        }

        RoutResponseVO existRoute = kongAdapterService.fetchRoute(menuDto.getName());
        try {
            if (existRoute != null) {
                // 更新路由
                kongAdapterService.updateRoute(service.getLeft(), menuDto.getName(), menuDto.getBaseUrl(), tags);
            } else {
                kongAdapterService.createRoute(service.getLeft(), menuDto.getName(), menuDto.getBaseUrl(), tags);
                if (needApiKey) {
                    kongAdapterService.addApiKey(menuDto.getName());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BuzException("menu.save.failed");
        }
    }

    public void createRoute(MenuDto menuDto, boolean needApiKey, boolean updateService) {
        String host, protocol, path;
        int port;
        try {
            URL url = new URL(menuDto.getBaseUrl());
            host = url.getHost();
            protocol = url.getProtocol();

            port = url.getPort();
            if (port < 0) {
                port = url.getDefaultPort();
            }
            path = StringUtils.isNotBlank(url.getFile()) ? url.getFile() : "/";

            // 校验host
            InetAddress inetAddress = InetAddress.getByName(host);
        } catch (MalformedURLException e) {
            log.error(e.getMessage(), e);
            throw new BuzException("menu.baseurl.invalid");
        } catch (UnknownHostException e) {
            log.error("菜单创建异常", e);
            throw new BuzException("菜单创建异常： UnknownHost");
        }

        // 获取或者创建服务
        Pair<String, String> service = fetchOrCreateService(menuDto.getServiceName(), updateService, protocol, host, port);;

        List<String> tags = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(menuDto.getTags())) {
            tags.addAll(menuDto.getTags());
        }

        RoutResponseVO existRoute = kongAdapterService.fetchRoute(menuDto.getName());
        try {
            if (existRoute != null) {
                // 更新路由
                kongAdapterService.updateRoute(service.getLeft(), menuDto.getName(), path, tags);
            } else {
                kongAdapterService.createRoute(service.getLeft(), menuDto.getName(), path, tags);
                if (needApiKey) {
                    kongAdapterService.addApiKey(menuDto.getName());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BuzException("menu.save.failed");
        }
    }

    /**
     * 创建菜单
     * @param menuDto
     */
    public void createMenu(MenuDto menuDto, boolean updateService) {

        String host, protocol, path;
        int port;
        try {
            URL url = new URL(menuDto.getBaseUrl());
            host = url.getHost();
            protocol = url.getProtocol();

            port = url.getPort();
            if (port < 0) {
                port = url.getDefaultPort();
            }
            path = StringUtils.isNotBlank(url.getFile()) ? url.getFile() : "/";

            // 校验host
            InetAddress inetAddress = InetAddress.getByName(host);
        } catch (MalformedURLException e) {
            log.error(e.getMessage(), e);
            throw new BuzException("menu.baseurl.invalid");
        } catch (UnknownHostException e) {
            log.error("菜单创建异常", e);
            throw new BuzException("菜单创建异常： UnknownHost");
        }

        // 获取或者创建服务
        Pair<String, String> service = fetchOrCreateService(menuDto.getServiceName(), updateService, protocol, host, port);;

        String iconName = null;
        if (menuDto.getIcon() != null) {
            String fileName = menuDto.getIcon().getOriginalFilename();
            String extName = FileUtil.extName(fileName);
            iconName = menuDto.getName() + "." + extName;
/*            if (!StringUtils.endsWith(fileName.toLowerCase(), ".svg")) {
                throw new BuzException("menu.icon.invalid");
            }*/
            try {
                FileUtil.writeFromStream(menuDto.getIcon().getInputStream(), iCON_ROOT_PATH + iconName);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new BuzException("menu.icon.save.failed");
            }
        }

        // 封装标签
        List<String> tags = new ArrayList<>();
        tags.add(String.format("showName:%s", menuDto.getShowName()));
        tags.add("menu");// 标识为菜单
        tags.add(String.format("description:%s", StringUtils.isNotBlank(menuDto.getDescription()) ? menuDto.getDescription() : " "));
        tags.add("parentName:menu.tag.appspace");
        tags.add(String.format("openType:%d", menuDto.getOpenType()));
        if (iconName != null) {
            tags.add(String.format("iconUrl:%s", iconName));
        }

        if (CollectionUtil.isNotEmpty(menuDto.getTags())) {
            tags.addAll(menuDto.getTags());
        }

        List<RoutResponseVO> existShowNames = kongAdapterService.searchRoute(List.of(String.format("showName:%s", menuDto.getShowName()), "menu"));
        if (CollectionUtil.isNotEmpty(existShowNames)) {
            for (RoutResponseVO existShowName : existShowNames) {
                if (!StringUtils.equals(existShowName.getName(), menuDto.getName())) {
                    throw new BuzException("menu.showname.exist");
                }
            }
        }

        boolean app = false;
        if (menuDto.getOpenType() != null) {
            if (menuDto.getOpenType() != 2) {
                path = "/third-apps/"+service.getRight() + path;
            } else {
                //app = true;
            }
        }

        RoutResponseVO existRoute = kongAdapterService.fetchRoute(menuDto.getName());
        try {
            if (existRoute != null) {
                // 更新路由
                kongAdapterService.updateRoute(service.getLeft(), menuDto.getName(), path, tags);
            } else {
                kongAdapterService.createRoute(service.getLeft(), menuDto.getName(), path, tags);
                if (app) {
                    kongAdapterService.addApiKey(menuDto.getName());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BuzException("menu.save.failed");
        }
    }

    /**
     * 根据baseUrl 获取或创建服务
     * @param serviceName
     * @param serviceProtocol
     * @param serviceHost
     * @param servicePort
     * @return
     */
    private Pair<String, String> fetchOrCreateService(String serviceName, boolean updateService, String serviceProtocol, String serviceHost, int servicePort) {
        String serviceId = null;
        if (StringUtils.isNotBlank(serviceName)) {
            JSONObject service = kongAdapterService.queryServiceJsonById(serviceName);
            if (service == null) {
                ServiceResponseVO newService = kongAdapterService.createService(serviceName, serviceProtocol, serviceHost, servicePort);
                serviceId = newService.getId();
            } else {
                if (updateService) {
                    if (!StringUtils.equalsIgnoreCase(serviceProtocol, service.getString("protocol"))
                            || !StringUtils.equalsIgnoreCase(serviceHost, service.getString("host"))
                            || servicePort != service.getIntValue("port")) {
                        // service变更
                        service.put("protocol", serviceProtocol);
                        service.put("host", serviceHost);
                        service.put("port", servicePort);
                        kongAdapterService.updateService(service.getString("id"), service);
                    }
                }

                serviceId = service.getString("id");
            }
        } else  {
            serviceName = String.format("%s-%s-%d", serviceProtocol, serviceHost, servicePort);
            JSONObject service = kongAdapterService.queryServiceJsonById(serviceName);
            if (service == null) {
                ServiceResponseVO newService = kongAdapterService.createService(serviceName, serviceProtocol, serviceHost, servicePort);
                serviceId = newService.getId();
            } else {
                serviceId = service.getString("id");
            }
        }

        return Pair.of(serviceId, serviceName);
    }

    public void deleteMenu(String name) {
        RoutResponseVO existRoute = kongAdapterService.fetchRoute(name);
        if (existRoute != null) {
            kongAdapterService.deleteRoute(name);
        }
    }
}

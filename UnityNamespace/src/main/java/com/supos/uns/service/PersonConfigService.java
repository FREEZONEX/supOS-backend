package com.supos.uns.service;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import com.supos.uns.dao.mapper.PersonConfigMapper;
import com.supos.uns.dao.po.PersonConfigPo;
import com.supos.uns.vo.PersonConfigVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author chenlizhong@supos.com
 * @description:
 * @date 2025/7/3 13:45
 */
@Service
public class PersonConfigService {

    @Value("${SYS_OS_LANG:zh-CN}")
    private String systemLocale;

    // 若集群部署，要改成分布式缓存
    private Cache<String, PersonConfigVo> personConfigCache = CacheUtil.newTimedCache(1000 * 60 * 60);

    @Autowired
    private PersonConfigMapper personConfigMapper;

    public int updateByUserId(String userId, String mainLanguage) {
        int ret = personConfigMapper.updateByUserId(userId, mainLanguage);

        if (ret > 0) {
            personConfigCache.remove(userId);
        }
        return ret;
    }

    public PersonConfigVo getByUserId(String userId) {
        PersonConfigVo personConfigVo = personConfigCache.get(userId);
        if (personConfigVo != null) {
            return personConfigVo;
        }

        PersonConfigPo personConfigPo = personConfigMapper.getByUserId(userId);
        if (personConfigPo == null) {
            personConfigPo = new PersonConfigPo();
            personConfigPo.setUserId(userId);
            // 默认值
            personConfigPo.setMainLanguage(systemLocale);
        }
        personConfigVo = new PersonConfigVo();
        BeanUtils.copyProperties(personConfigPo, personConfigVo);
        personConfigCache.put(userId, personConfigVo);
        return personConfigVo;
    }
}

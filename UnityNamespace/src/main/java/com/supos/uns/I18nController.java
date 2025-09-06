package com.supos.uns;

import com.supos.uns.service.I18nService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author chenlizhong@supos.com
 * @description:
 * @date 2025/7/4 14:53
 */
@Slf4j
@RestController
@RequestMapping("/inter-api/supos/uns/i18n")
public class I18nController {

    @Autowired
    private I18nService i18nService;

    @GetMapping(value = "/messages", produces = "text/plain;charset=UTF-8")
    public String readFile(@RequestParam("lang") String lang) {
        return i18nService.readMessages(lang);
    }

    @GetMapping(value = "/messages/plugin", produces = "text/plain;charset=UTF-8")
    public String readI18nForPlugin(@RequestParam("lang") String lang, @RequestParam("pluginId") List<String> pluginId) {
        return i18nService.readMessages4Plugin(lang, pluginId);
    }

    @GetMapping(value = "/messages/app", produces = "text/plain;charset=UTF-8")
    public String readI18nForApp(@RequestParam("lang") String lang, @RequestParam("appId") List<String> appId) {
        return i18nService.readMessages4App(lang, appId);
    }
}

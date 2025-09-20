package com.supos.i18n.service.excel;

import com.supos.common.RunningStatus;
import com.supos.i18n.dto.AddLanguageDto;
import com.supos.i18n.dto.AddModuleDto;
import com.supos.i18n.dto.ResourceDto;
import com.supos.i18n.service.excel.entity.LanguageData;
import com.supos.i18n.service.excel.entity.ModuleData;
import com.supos.i18n.service.excel.entity.ResourceData;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 导入上下文
 * @date 2025/9/4 15:39
 */
@Data
public class ExcelImportContext {

    private Consumer<RunningStatus> consumer;
    private Map<String, String> errorMap = new HashMap<>();

    private boolean shoutDown = false;

    /*************************待保存的数据**************************/
    private LanguageData language;
    private Map<String, ModuleData> moduleMap = new HashMap<>();
    private Map<Pair<String, String>, ResourceData> resourceMap = new HashMap<>();

    private Set<String> actuallyHandleModuleCodes = new HashSet<>();

    public ExcelImportContext(Consumer<RunningStatus> consumer) {
        this.consumer = consumer;
    }

    public boolean containsModuleCode(String moduleCode) {
        return moduleMap.containsKey(moduleCode);
    }

    public boolean containsKey(String moduleCode, String key) {
        return resourceMap.containsKey(Pair.of(moduleCode, key));
    }

    public void addError(String flagNo, String msg) {
        errorMap.put(flagNo, msg);
    }

    public void addAllError(Map<String, String> subErrorMap) {
        errorMap.putAll(subErrorMap);
    }

    public boolean hasError() {
        return !errorMap.isEmpty();
    }

    public void addModule(ModuleData moduleData) {
        moduleMap.put(moduleData.getModuleCode(), moduleData);
    }

    public void addResource(ResourceData resourceData) {
        resourceMap.put(Pair.of(resourceData.getModuleCode(), resourceData.getKey()), resourceData);
    }

    public void setShoutDown(boolean shoutDown) {
        this.shoutDown = shoutDown;
    }

    public boolean isShoutDown() {
        return shoutDown;
    }

    public void addHandleModuleCode(String moduleCode) {
        actuallyHandleModuleCodes.add(moduleCode);
    }
}

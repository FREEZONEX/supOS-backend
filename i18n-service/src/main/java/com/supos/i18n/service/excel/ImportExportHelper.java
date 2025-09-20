package com.supos.i18n.service.excel;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.poi.excel.ExcelReader;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.google.common.collect.Lists;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.I18nUtils;
import com.supos.i18n.service.excel.entity.LanguageData;
import com.supos.i18n.service.excel.entity.ModuleData;
import com.supos.i18n.service.excel.entity.ResourceData;
import org.springframework.util.StopWatch;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 导入导出辅助器
 * @date 2025/9/4 19:03
 */
public class ImportExportHelper {

    public final static String SHEET_EXPLANATION = "Explanation";
    public final static String SHEET_LANGUAGE = "Language";
    public final static String SHEET_MODULE = "Module";
    public final static String SHEET_RESOURCE = "Resource";

    public static Map<String, Integer> SHEET_INDEX = new HashMap<>();


    public static List<String> EXPLANATION = new LinkedList<>();
    private static List<String> LANGUAGE_INDEX = new LinkedList<>();
    private static List<String> MODULE_INDEX = new LinkedList<>();
    private static List<String> RESOURCE_INDEX = new LinkedList<>();

    static {
        SHEET_INDEX.put(SHEET_EXPLANATION, 0);
        SHEET_INDEX.put(SHEET_LANGUAGE, 1);
        SHEET_INDEX.put(SHEET_MODULE, 2);
        SHEET_INDEX.put(SHEET_RESOURCE, 3);

        EXPLANATION.add("i18n.import.explanation1");
        EXPLANATION.add("i18n.import.explanation2");
        EXPLANATION.add("i18n.import.explanation3");
        EXPLANATION.add("i18n.import.explanation4");

        LANGUAGE_INDEX.addAll(getFields(SHEET_LANGUAGE));

        MODULE_INDEX.addAll(getFields(SHEET_MODULE));

        RESOURCE_INDEX.addAll(getFields(SHEET_RESOURCE));
    }

    public static List<String> getSheet() {
        return Lists.newArrayList(SHEET_EXPLANATION, SHEET_LANGUAGE, SHEET_MODULE, SHEET_RESOURCE);
    }

    public static int errorIndex(String sheetName) {
        List<String> fields = getFields(sheetName);
        return fields.size();
    }

    private static List<String> getFields(String sheetName) {
        Class<?> clazz = null;
        switch (sheetName) {
            case SHEET_LANGUAGE:
                clazz = LanguageData.class;
                break;
            case SHEET_MODULE:
                clazz = ModuleData.class;
                break;
            case SHEET_RESOURCE:
                clazz = ResourceData.class;
                break;
        }
        return Arrays.stream(clazz.getDeclaredFields()).filter(field -> {
            return field.getAnnotation(ExcelProperty.class) != null;
        }).map(Field::getName).collect(Collectors.toList());
    }

    /**
     * 校验表头是否正确
     *
     * @param sheetName
     * @param heads
     * @return
     */

    public static boolean checkHead(String sheetName, List<Object> heads) {
        List<String> needHeads = new ArrayList<>();
        List<String> tempHeads = heads != null ? heads.stream().map(head -> head != null ? head.toString() : null).collect(Collectors.toList()) : new ArrayList<>();
        switch (sheetName) {
            case SHEET_LANGUAGE:
                needHeads = LANGUAGE_INDEX;
                break;
            case SHEET_MODULE:
                needHeads = MODULE_INDEX;
                break;
            case SHEET_RESOURCE:
                needHeads = RESOURCE_INDEX;
                break;
        }

        for (String needHead : needHeads) {
            if (!tempHeads.contains(needHead)) {
                return false;
            }
        }
        return true;
    }

    public static void checkExplanationSheet(ExcelReader reader) {
        reader.setSheet(SHEET_INDEX.get(SHEET_EXPLANATION));
        String explanationSheetName = reader.getSheet().getSheetName();
        if (!SHEET_EXPLANATION.equals(explanationSheetName)) {
            throw new BuzException("i18n.import.template.error");
        }
    }

    public static void checkLanguageSheet(ExcelReader reader) {
        reader.setSheet(SHEET_INDEX.get(SHEET_LANGUAGE));
        String languageSheetName = reader.getSheet().getSheetName();
        if (!SHEET_LANGUAGE.equals(languageSheetName)) {
            throw new BuzException("i18n.import.template.error");
        }

        List<Object> heads = reader.readRow(0);
        if (CollectionUtil.isEmpty(heads)) {
            throw new BuzException("i18n.import.template.error");
        }

        if (!checkHead(SHEET_LANGUAGE, heads)) {
            throw new BuzException("i18n.import.template.error");
        }
    }

    public static void checkModule(ExcelReader reader) {
        reader.setSheet(SHEET_INDEX.get(SHEET_MODULE));
        String moduleSheetName = reader.getSheet().getSheetName();
        if (!SHEET_MODULE.equals(moduleSheetName)) {
            throw new BuzException("i18n.import.template.error");
        }

        List<Object> heads = reader.readRow(0);
        if (CollectionUtil.isEmpty(heads)) {
            throw new BuzException("i18n.import.template.error");
        }

        if (!checkHead(SHEET_MODULE, heads)) {
            throw new BuzException("i18n.import.template.error");
        }
    }

    public static void checkResource(ExcelReader reader) {
        reader.setSheet(SHEET_INDEX.get(SHEET_RESOURCE));
        String resourceSheetName = reader.getSheet().getSheetName();
        if (!SHEET_RESOURCE.equals(resourceSheetName)) {
            throw new BuzException("i18n.import.template.error");
        }

        List<Object> heads = reader.readRow(0);
        if (CollectionUtil.isEmpty(heads)) {
            throw new BuzException("i18n.import.template.error");
        }

        if (!checkHead(SHEET_RESOURCE, heads)) {
            throw new BuzException("i18n.import.template.error");
        }
    }

    /**
     * 创建说明页
     * @param excelWriter
     */
    public static void writeExplanation(ExcelWriter excelWriter, StopWatch stopWatch) {
        if (stopWatch != null) {
            stopWatch.start("create Explanation sheet");
        }
        try {
            WriteSheet writeSheet = EasyExcel.writerSheet(SHEET_EXPLANATION).sheetNo(SHEET_INDEX.get(SHEET_EXPLANATION)).build();
            List<Map<Integer, Object>> dataList = Lists.newArrayList();
            for (int i = 0; i < EXPLANATION.size(); i++) {
                Map<Integer, Object> dataMap = new HashMap<>();
                dataMap.put(0, I18nUtils.getMessage(EXPLANATION.get(i)));

                dataList.add(dataMap);
            }
            excelWriter.write(dataList, writeSheet);
        } finally {
            if (stopWatch != null) {
                stopWatch.stop();
            }
        }
    }

    /**
     * 创建语言页
     * @param excelWriter
     * @param languageData
     * @param stopWatch
     */
    public static void writeLanguage(ExcelWriter excelWriter, LanguageData languageData, StopWatch stopWatch) {
        if (stopWatch != null) {
            stopWatch.start("create Language sheet");
        }
        try {
            WriteSheet writeSheet = EasyExcel.writerSheet(SHEET_LANGUAGE).sheetNo(SHEET_INDEX.get(SHEET_LANGUAGE)).build();
            List<LanguageData> dataList = Lists.newArrayList();

            // 表头
            LanguageData head = new LanguageData();
            head.setCode("code");
            head.setName("name");
            dataList.add(head);

            if (languageData != null) {
                dataList.add(languageData);
            }
            excelWriter.write(dataList, writeSheet);
        } finally {
            if (stopWatch != null) {
                stopWatch.stop();
            }
        }
    }

    /**
     * 创建模块页
     * @param excelWriter
     * @param modules
     * @param stopWatch
     */
    public static void writeModule(ExcelWriter excelWriter, List<ModuleData> modules, StopWatch stopWatch) {
        if (stopWatch != null) {
            stopWatch.start("create Module sheet");
        }
        try {
            WriteSheet writeSheet = EasyExcel.writerSheet(SHEET_MODULE).sheetNo(SHEET_INDEX.get(SHEET_MODULE)).build();
            List<ModuleData> dataList = Lists.newArrayList();

            // 表头
            ModuleData head = new ModuleData();
            head.setModuleCode("moduleCode");
            head.setModuleName("moduleName");
            dataList.add(head);

            if (CollectionUtil.isNotEmpty(modules)) {
                dataList.addAll(modules);
            }
            excelWriter.write(dataList, writeSheet);
        } finally {
            if (stopWatch != null) {
                stopWatch.stop();
            }
        }
    }

    /**
     * 创建资源页
     * @param excelWriter
     * @param resources
     * @param stopWatch
     */
    public static void writeResource(ExcelWriter excelWriter, List<ResourceData> resources, StopWatch stopWatch) {
        if (stopWatch != null) {
            stopWatch.start("create Resource sheet");
        }
        try {
            WriteSheet writeSheet = EasyExcel.writerSheet(SHEET_RESOURCE).sheetNo(SHEET_INDEX.get(SHEET_RESOURCE)).build();
            List<ResourceData> dataList = Lists.newArrayList();

            // 表头
            ResourceData head = new ResourceData();
            head.setModuleCode("moduleCode");
            head.setKey("key");
            head.setValue("value");
            dataList.add(head);

            if (CollectionUtil.isNotEmpty(resources)) {
                dataList.addAll(resources);
            }
            excelWriter.write(dataList, writeSheet);
        } finally {
            if (stopWatch != null) {
                stopWatch.stop();
            }
        }
    }
}

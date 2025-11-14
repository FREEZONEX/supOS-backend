package com.supos.uns.service.exportimport.cjson;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supos.common.Constants;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.I18nUtils;
import com.supos.common.RunningStatus;
import com.supos.uns.service.UnsAddService;
import com.supos.uns.service.UnsLabelService;
import com.supos.uns.service.UnsManagerService;
import com.supos.uns.service.UnsTemplateService;
import com.supos.uns.service.exportimport.core.DataImporter;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.parser.ParserAble;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO 后续性能优化和内存优化需考虑使用流式处理
 * @author sunlifang
 * @version 1.0
 * @description: JsonDataImporter
 * @date 2025/5/10 13:05
 */
@Slf4j
public class ComplexJsonDataImporter extends DataImporter {

    private long batch = 2000;

    private JsonNode jsonNode;

    public ComplexJsonDataImporter(ExcelImportContext context, UnsManagerService unsManagerService, UnsLabelService unsLabelService,
                                   UnsTemplateService unsTemplateService, UnsAddService unsAddService) {
        super(context, unsManagerService, unsLabelService, unsTemplateService, unsAddService);
    }

    @Override
    public void importData(File file) {
        try {
            JsonMapper jsonMapper = new JsonMapper();
            jsonNode = jsonMapper.readTree(file);

        } catch (Exception e) {
            log.error("解析json文件失败", e);
            throw new BuzException("uns.import.json.error");
        }
        try {
            handleTemplate();
            handleLabel();
            handleUns();

            log.info("import running time:{}s", getStopWatch().getTotalTimeSeconds());
            log.info(getStopWatch().prettyPrint());
        } catch (Exception e) {
            log.error("导入失败", e);
            throw new BuzException("uns.import.error");
        }
    }

    @Override
    public void writeError(File srcfile, File outFile) {
        try {
            JsonMapper jsonMapper = new JsonMapper();
            JsonFactory factory = jsonMapper.getFactory();
            JsonGenerator jsonGenerator = factory.createGenerator(outFile, JsonEncoding.UTF8);
            jsonGenerator.useDefaultPrettyPrinter();


            for (Map.Entry<String, String> e : getContext().getExcelCheckErrorMap().entrySet()) {
                String errorIndex[] = e.getKey().split("-");
                JsonNode errorNode = jsonNode;
                if (errorNode == null) {
                    continue;
                }
                for (String index : errorIndex) {
                    if (NumberUtils.isDigits(index)) {
                        errorNode = errorNode.get(Integer.valueOf(index));
                    } else {
                        errorNode = errorNode.get(index);
                    }
                }

                ((ObjectNode)errorNode).put("error", e.getValue());
            }

            jsonMapper.writeTree(jsonGenerator, jsonNode);
            jsonGenerator.close();
        } catch (Exception e) {
            log.error("导入失败", e);
            throw new BuzException(e.getMessage());
        }
    }

    /**
     * 解析处理模板
     */
    private void handleTemplate() {
        JsonNode templateDataListNode = jsonNode.get("Template");
        if (templateDataListNode != null && templateDataListNode.isArray()) {
            ParserAble parser = getParser(ExcelTypeEnum.Template);
            AtomicInteger index = new AtomicInteger(0);
            Iterator<JsonNode> iterator = templateDataListNode.iterator();
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                ((ObjectNode) node).remove("error");
                // 解析模板
                parser.parseComplexJson(String.format("%s-%d", ExcelTypeEnum.Template.getCode(), index.getAndIncrement()), node, getContext(), null);
                // 保存模板
                doImport(ExcelTypeEnum.Template, false);
            }
            // 保存模板
            doImport(ExcelTypeEnum.Template, true);
        }
    }

    /**
     * 解析处理标签
     */
    private void handleLabel() {
        JsonNode labelDataListNode = jsonNode.get("Label");
        if (labelDataListNode != null && labelDataListNode.isArray()) {
            ParserAble parser = getParser(ExcelTypeEnum.Label);
            AtomicInteger index = new AtomicInteger(0);
            Iterator<JsonNode> iterator = labelDataListNode.iterator();
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                ((ObjectNode) node).remove("error");
                parser.parseComplexJson(String.format("%s-%d", ExcelTypeEnum.Label.getCode(), index.getAndIncrement()), node, getContext(), null);
                doImport(ExcelTypeEnum.Label, false);
            }
            doImport(ExcelTypeEnum.Label, true);
        }
    }

    /**
     * 解析处理文件夹、文件
     */
    private void handleUns() {
        JsonNode unsDataListNode = jsonNode.get("UNS");
        if (unsDataListNode != null && unsDataListNode.isArray()) {
            ParserAble parser = getParser(ExcelTypeEnum.UNS);
            AtomicInteger index = new AtomicInteger(0);
            Iterator<JsonNode> iterator = unsDataListNode.iterator();
            while (iterator.hasNext()) {
                parser.parseComplexJson(String.format("%s-%d", ExcelTypeEnum.UNS.getCode(), index.getAndIncrement()), iterator.next(), getContext(), null);
            }
            doImport(ExcelTypeEnum.UNS, true);
        }
    }

    private void doImport(ExcelTypeEnum excelTypeEnum, boolean finish) {
        switch (excelTypeEnum) {
            case Template:
                if (finish || (getContext().templateSize() % batch == 0)) {
                    importTemplate(getContext());
                }
                getContext().getConsumer().accept(new RunningStatus()
                        .setTask(I18nUtils.getMessage("uns.create.task.name.template"))
                        .setFinished(false)
                        .setProgress(20.0));
                break;
            case Label:
                if (finish || (getContext().labelSize() % batch == 0)) {
                    importLabel(getContext());
                }
                getContext().getConsumer().accept(new RunningStatus()
                        .setTask(I18nUtils.getMessage("uns.create.task.name.label"))
                        .setFinished(false)
                        .setProgress(40.0));
                break;
            case UNS:
                // 保存文件夹
                importFolder(getContext());
                getContext().getConsumer().accept(new RunningStatus()
                        .setTask(I18nUtils.getMessage("uns.create.task.name.folder"))
                        .setFinished(false)
                        .setProgress(60.0));
                // 保存时序文件
                importFile(getContext(), Constants.TIME_SEQUENCE_TYPE);
                // 保存关系文件
                importFile(getContext(), Constants.RELATION_TYPE);

                // 因为计算、聚合、引用可能用到导入文件中的引用文件，需要确保这些引用文件先导入
                separationRefer(getContext().getFileCalculateMap());
                separationRefer(getContext().getFileAggregationMap());
                separationRefer(getContext().getFileReferenceMap());
                importFile(getContext(), ExcelImportContext.REFER_DATATYPE);

                // 保存计算文件
                importFile(getContext(), Constants.CALCULATION_REAL_TYPE);
                // 保存聚合文件
                importFile(getContext(), Constants.MERGE_TYPE);
                // 保存引用文件
                importFile(getContext(), Constants.CITING_TYPE);
                // 保存jsonb文件
                importFile(getContext(), Constants.JSONB_TYPE);
                getContext().getConsumer().accept(new RunningStatus()
                        .setTask(I18nUtils.getMessage("uns.create.task.name.file"))
                        .setFinished(false)
                        .setProgress(80.0));
                break;
        }
    }
}

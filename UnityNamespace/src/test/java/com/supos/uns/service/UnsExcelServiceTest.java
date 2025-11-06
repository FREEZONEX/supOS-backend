package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.ExcelNamespaceBaseDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.utils.I18nUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xinwangji@supos.com
 * @date 2024/10/30 9:13
 * @description
 */
public class UnsExcelServiceTest {

    @Test
    public void testReadExcelModbus() {
/*        ExcelReader reader = ExcelUtil.getReader(getClass().getClassLoader().getResourceAsStream("modbus-lch.xlsx"), 0);
        String sheetName = reader.getSheet().getSheetName();
        ExcelTypeEnum excelType = ExcelTypeEnum.valueOfCode(sheetName);
        if (ExcelTypeEnum.ERROR.equals(excelType)) {
            throw new BuzException("uns.import.template.error");
        }
        List<Map<String, Object>> dataList = reader.read(0, 4, Integer.MAX_VALUE);
        System.out.println(JSON.toJSONString(dataList));

        HashMap<String, String> excelCheckErrorMap = new HashMap<>();
        List<CreateTopicDto> topicDtos = new UnsExcelService.TopicParser().parseExcelDataList(0, 4, ExcelTypeEnum.TIMESERIES_MODBUS, dataList, new HashMap<>(), new HashMap<>(), excelCheckErrorMap, new HashSet<>());
        System.out.println("excelCheckErrorMap: " + excelCheckErrorMap);
        System.out.println("topicDtos.size: " + topicDtos.size());
        System.out.println(JsonUtil.toJson(topicDtos));*/

    }

    @Test
    public void testUa() {
/*        ExcelReader reader = ExcelUtil.getReader(getClass().getClassLoader().getResourceAsStream("opcua-zz.xlsx"), 0);
        String sheetName = reader.getSheet().getSheetName();
        ExcelTypeEnum excelType = ExcelTypeEnum.valueOfCode(sheetName);
        if (ExcelTypeEnum.ERROR.equals(excelType)) {
            throw new BuzException("uns.import.template.error");
        }
        List<Map<String, Object>> dataList = reader.read(0, 4, Integer.MAX_VALUE);
        System.out.println(JSON.toJSONString(dataList));

        HashMap<String, String> excelCheckErrorMap = new HashMap<>();
        List<CreateTopicDto> topicDtos = new UnsExcelService.TopicParser().parseExcelDataList(0, 4, ExcelTypeEnum.TIMESERIES_OPCUA, dataList, new HashMap<>(), new HashMap<>(), excelCheckErrorMap, new HashSet<>());
        System.out.println("excelCheckErrorMap: " + excelCheckErrorMap);
        System.out.println("topicDtos.size: " + topicDtos.size());
        System.out.println(JSON.toJSONString(topicDtos, true));*/

    }

    @Test
    public void testReadExcelRest() {
/*        ExcelReader reader = ExcelUtil.getReader(getClass().getClassLoader().getResourceAsStream("rest-demo.xlsx"), 0);
        String sheetName = reader.getSheet().getSheetName();
        ExcelTypeEnum excelType = ExcelTypeEnum.valueOfCode(sheetName);
        if (ExcelTypeEnum.ERROR.equals(excelType)) {
            throw new BuzException("uns.import.template.error");
        }
        List<Map<String, Object>> dataList = reader.read(0, 4, Integer.MAX_VALUE);
        System.out.println(JSON.toJSONString(dataList));

        HashMap<String, String> excelCheckErrorMap = new HashMap<>();
        List<CreateTopicDto> topicDtos = new UnsExcelService.TopicParser().parseExcelDataList(0, 4, ExcelTypeEnum.RELATION, dataList, new HashMap<>(), new HashMap<>(), excelCheckErrorMap, new HashSet<>());
        System.out.println("excelCheckErrorMap: " + excelCheckErrorMap);
        System.out.println("topicDtos.size: " + topicDtos.size());
        System.out.println(JSON.toJSONString(topicDtos, true));*/

    }

    @Test
    public void testReadExcelRelation() {
/*        File dest = new File("target", "uns4.xlsx");

//        ExcelReader reader = ExcelUtil.getReader(getClass().getClassLoader().getResourceAsStream("relation-wh.xlsx"), 0);
        ExcelReader reader = ExcelUtil.getReader(dest);
        String sheetName = reader.getSheet().getSheetName();
        ExcelTypeEnum excelType = ExcelTypeEnum.valueOfCode(sheetName);
        if (ExcelTypeEnum.ERROR.equals(excelType)) {
            throw new BuzException("uns.import.template.error");
        }
        List<Map<String, Object>> dataList = reader.read(0, 4, Integer.MAX_VALUE);
        System.out.println(JSON.toJSONString(dataList));

        HashMap<String, String> excelCheckErrorMap = new HashMap<>();
        List<CreateTopicDto> topicDtos = new UnsExcelService.TopicParser().parseExcelDataList(0, 4, ExcelTypeEnum.RELATION, dataList, new HashMap<>(), new HashMap<>(), excelCheckErrorMap, new HashSet<>());
        System.out.println("excelCheckErrorMap: " + excelCheckErrorMap);
        System.out.println("topicDtos.size: " + topicDtos.size());
        System.out.println(JSON.toJSONString(topicDtos, true));*/

    }

    @Test
    public void testReadExcelWriteErr() {
        ExcelReader reader = ExcelUtil.getReader(getClass().getClassLoader().getResourceAsStream("ren-double-5000.xlsx"), 0);
        ExcelWriter excelWriter = reader.getWriter();
        File destFile = new File("E:\\AppData\\err4.xlsx");
        excelWriter.setDestFile(destFile);

        HashMap<Integer, String> errorMap = new HashMap<>();
        errorMap.put(2, "testErr");
        //UnsExcelService.writeErrTipExcel(0, errorMap, excelWriter);
        excelWriter.close();
    }

    @Test
    public void testParseDataAndValid() {
/*        String json = "[{\"topic\":\"/rest\",\"fields\":\"[{\\\"name\\\":\\\"tag\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"score\\\",\\\"type\\\":\\\"double\\\"}]\"},{\"topic\":\"/rest/dev1\",\"fields\":\"\",\"dataPath\":\"data.list\",\"serverName\":\"demoRestServer\",\"method\":\"GET\",\"syncRate.unit\":\"s\",\"syncRate.value\":100,\"pageDef.start.key\":\"page\",\"pageDef.offset.key\":\"pageSize\",\"fullUrl\":\"http://demo.api/prod/list?page=1&pageSize=20&type=ok\"},{\"topic\":\"/rest/dev2\",\"fields\":\"[{\\\"name\\\":\\\"tag\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"score\\\",\\\"type\\\":\\\"double\\\"}]\",\"dataPath\":\"data.list\",\"serverName\":\"demoRestServer\",\"method\":\"GET\",\"syncRate.unit\":\"s\",\"syncRate.value\":200,\"pageDef.start.key\":\"page\",\"pageDef.offset.key\":\"pageSize\",\"fullUrl\":\"http://demo.api/prod/list?page=1&pageSize=20&type=ok\"},{\"topic\":\"/rest/dev3\",\"fields\":\"[{\\\"name\\\":\\\"tag\\\",\\\"type\\\":\\\"string\\\",\\\"index\\\":\\\"Tag\\\"},{\\\"name\\\":\\\"score\\\",\\\"type\\\":\\\"double\\\",\\\"index\\\":\\\"Sc\\\"}]\",\"dataPath\":\"data.arr\",\"serverName\":\"rServer3\",\"method\":\"GET\",\"syncRate.unit\":\"s\",\"syncRate.value\":30,\"pageDef.start.key\":\"pageNo\",\"pageDef.offset.key\":\"size\",\"fullUrl\":\"https://demo.api/prod/list?type=3\"},{\"topic\":\"/rest/dev4\",\"fields\":\"[{\\\"name\\\":\\\"tag\\\",\\\"type\\\":\\\"string\\\",\\\"index\\\":\\\"Tag\\\"},{\\\"name\\\":\\\"score\\\",\\\"type\\\":\\\"double\\\",\\\"index\\\":\\\"Sc\\\"}]\",\"dataPath\":\"data.arr\",\"serverName\":\"rServer4\",\"syncRate.unit\":\"s\",\"syncRate.value\":60},{\"topic\":\"/rest/dev5\",\"fields\":\"[{\\\"name\\\":\\\"tag\\\",\\\"type\\\":\\\"string\\\",\\\"index\\\":\\\"Tag\\\"},{\\\"name\\\":\\\"score\\\",\\\"type\\\":\\\"double\\\",\\\"index\\\":\\\"Sc\\\"}]\"}]\n";
        List dataList = JSON.parseArray(json);
        HashMap<String, String> excelCheckErrorMap = new HashMap<>();
        List<CreateTopicDto> topicDtos = new UnsExcelService.TopicParser().parseExcelDataList(0, 4, ExcelTypeEnum.RELATION, dataList, new HashMap<>(), new HashMap<>(), excelCheckErrorMap, new HashSet<>());
        System.out.println("excelCheckErrorMap: " + excelCheckErrorMap);
        System.out.println("topicDtos.size: " + topicDtos.size());
        System.out.println(JSON.toJSONString(topicDtos, true));

        Assert.assertEquals(3, topicDtos.size());
        Assert.assertEquals(3, excelCheckErrorMap.size());
        CreateTopicDto ins3 = topicDtos.get(1), ins5 = topicDtos.get(2);
        Assert.assertTrue(ins3.getProtocolBean() instanceof RestConfigDTO);
        Assert.assertTrue(ins5.getProtocolBean() == null);
        RestConfigDTO restCfg3 = (RestConfigDTO) ins3.getProtocolBean();
        Assert.assertTrue(restCfg3.isHttps());
        Assert.assertEquals("data.arr", ins3.getDataPath());*/
    }

    @Test
    public void testReadExcelDataList() {
/*        ExcelReader reader = ExcelUtil.getReader(getClass().getClassLoader().getResourceAsStream("namespace-20241204152432.xlsx"), 0);
        String sheetName = reader.getSheet().getSheetName();
        ExcelTypeEnum excelType = ExcelTypeEnum.valueOfCode(sheetName);
        if (ExcelTypeEnum.ERROR.equals(excelType)) {
            throw new BuzException("uns.import.template.error");
        }
        List<Map<String, Object>> dataList = reader.read(0, 4, Integer.MAX_VALUE);
//        for (int i = 0; i < dataList.size(); i++) {
//            Map<String, Object> dataMap = dataList.get(i);
//            Map<String, Object> protocolMap = JsonMapConvertUtils.convertMap(dataMap);
//            Object fs = protocolMap.remove("fields");
//            if (fs != null) {
//                protocolMap.put("fields", JSONUtil.parse(fs.toString()));
//            }
//            protocolMap.put("dataType", 1);
//            dataList.set(i, protocolMap);
//        }
        System.out.println(JSON.toJSONString(dataList));

        HashMap<String, String> excelCheckErrorMap = new HashMap<>();
        List<CreateTopicDto> topicDtos = new UnsExcelService.TopicParser().parseExcelDataList(0, 4, ExcelTypeEnum.TIMESERIES_OPCUA, dataList, new HashMap<>(), new HashMap<>(), excelCheckErrorMap, new HashSet<>());
        System.out.println("excelCheckErrorMap: " + excelCheckErrorMap);
        System.out.println("topicDtos.size: " + topicDtos.size());
        System.out.println(JSON.toJSONString(topicDtos, true));*/

    }

    @Test
    public void testExcel() {
        // 读取现有的Excel文件
        ExcelWriter excelWriter = ExcelUtil.getWriter(excel);
        Workbook workbook = excelWriter.getWorkbook();
        // 设置样式，例如设置字体加粗
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);

        // 操作第一个Sheet
        Sheet sheet = workbook.getSheetAt(0);
        for (Row row : sheet) {
            for (Cell cell : row) {
                // 应用样式到单元格
                cell.setCellStyle(style);
            }
        }

        //第2行红色
        CellStyle cellStyle = excelWriter.getWorkbook().createCellStyle();
        cellStyle.setFillBackgroundColor(IndexedColors.RED.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        excelWriter.setRowStyle(2, cellStyle);

        // 写入到新的Excel文件或直接写回原文件
        excelWriter.write(workbook, true);

        // 关闭writer
        excelWriter.close();
    }

    private String excel = "target/namespace.xlsx";

    @Test
    public void setRed() throws IOException {
        File dest = new File("target", "uns.xlsx");
        if (dest.exists()) {
            dest.delete();
        }
        if (!dest.exists()) {
            dest.createNewFile();
        }
        ExcelReader reader = ExcelUtil.getReader(getClass().getClassLoader().getResourceAsStream("relation-wh.xlsx"), 0);

        System.out.println(dest.getAbsolutePath());
        // 读取现有的Excel文件
        ExcelWriter writer = reader.getWriter();
        writer.setDestFile(dest);
        Workbook workbook = writer.getWorkbook();
        Sheet sheet = workbook.getSheetAt(0);
        writer.setSheet(0);
        //
        CellStyle cellStyle = writer.createCellStyle();
        cellStyle.setFillForegroundColor(IndexedColors.RED1.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row row = sheet.getRow(10);
        for (Cell cell : row) {
           cell.setCellStyle(cellStyle);
        }
        int lastCellNum = row.getLastCellNum();
        Cell lastCell = row.createCell(lastCellNum + 1);
        lastCell.setCellStyle(cellStyle);
        lastCell.setCellValue("test Err Msg4!");
        // 写入到新的Excel文件或直接写回原文件
//        writer.write(workbook, true);
        // 关闭writer
        writer.close();
    }


    @Test
    public void setRed2() {
        // 读取现有的Excel文件
        ExcelWriter excelWriter = ExcelUtil.getWriter(excel);
        Workbook workbook = excelWriter.getWorkbook();
        CellStyle cellStyle = setCellStyle(excelWriter, 10);
        // 操作第一个Sheet
        Sheet sheet = workbook.getSheetAt(0);
        for (Row row : sheet) {
            if (row.getRowNum() == 3) {
                for (Cell cell : row) {
                    // 应用样式到单元格
                    cell.setCellStyle(cellStyle);
                }
                int lastCellNum = row.getLastCellNum();
                Cell lastCell = row.createCell(lastCellNum);
                String msg = I18nUtils.getMessage("uns.excel.cellValue");
                lastCell.setCellValue(msg);
            }
        }
        // 写入到新的Excel文件或直接写回原文件
        excelWriter.write(workbook, true);
        // 关闭writer
        excelWriter.close();
    }

    @Test
    public void setRed3() {
        // 读取现有的Excel文件
        ExcelWriter excelWriter = ExcelUtil.getWriter(excel);
        Workbook workbook = excelWriter.getWorkbook();
        excelWriter.setSheet(0);
        CellStyle cellStyle = setCellStyle(excelWriter, 10);
        Cell cell = excelWriter.getOrCreateCell("A2");
        cell.setCellStyle(cellStyle);
        // 写入到新的Excel文件或直接写回原文件
        excelWriter.write(workbook, true);
        // 关闭writer
        excelWriter.close();
    }

    private CellStyle setCellStyle(ExcelWriter writer, int row) {
        CellStyle cellStyle = writer.getOrCreateRowStyle(row);
        // 顶边栏
//        cellStyle.setBorderTop(BorderStyle.THIN);
//        cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
//        // 右边栏
//        cellStyle.setBorderRight(BorderStyle.THIN);
//        cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
//        // 底边栏
//        cellStyle.setBorderBottom(BorderStyle.THIN);
//        cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
//        // 左边栏
//        cellStyle.setBorderLeft(BorderStyle.THIN);
//        cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        // 填充前景色(两个一起使用)
        cellStyle.setFillForegroundColor(IndexedColors.RED1.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cellStyle;
    }


    public void testImport() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("topic", "a");
        dataMap.put("fields", "[{\"name\":\"t0\",\"type\":\"int\"},{\"name\":\"t1\",\"type\":\"long\"},{\"name\":\"t2\",\"type\":\"float\"}]");
        dataMap.put("protocol", "modbus");
        dataMap.put("serverName", "serverName");

        ExcelNamespaceBaseDto data = BeanUtil.toBean(dataMap, ExcelNamespaceBaseDto.class);
        CreateTopicDto createTopicDto = BeanUtil.copyProperties(data, CreateTopicDto.class, "fields");
        if (StringUtils.isNotBlank(data.getFields())) {
            List<FieldDefine> defineList = JSONUtil.toList(data.getFields(), FieldDefine.class);
            FieldDefine[] fieldDefines = defineList.toArray(new FieldDefine[defineList.size()]);
            createTopicDto.setFields(fieldDefines);
        }
        dataMap.remove("topic");
        dataMap.remove("fields");
        createTopicDto.setProtocol(dataMap);
        System.out.println();
    }

    @Test
    public void testPride(){
//        String url = "http://192.168.236.55:8080/os/open-api/uns/pride/task/import";
        String url = "http://10.10.58.193:8080/os/open-api/uns/pride/task/import?rootAlias=_Awenjianjia_f875ed21281c4fb9ba9b";
//        String file = "E:\\工作\\supos2\\7b853055-0c83-4d1e-a21e-813c94306846_2supos.txt";
        String file = "E:\\工作\\supos2\\30M.txt";
        String body = FileUtil.readString(new File(file), StandardCharsets.UTF_8);

//        String ak = "ak-7383753428075283084";//100
        String ak = "ak-7383770872672158317";//pride


        HttpResponse res = HttpRequest.of(url).method(Method.POST).body(body).bearerAuth(ak).timeout(Integer.MAX_VALUE).execute();
        System.out.println(res);
        System.out.println(res.body());
    }
}

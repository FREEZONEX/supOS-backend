package com.supos.uns.service.exportimport.core.data;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileAggregation
 * @date 2025/5/8 17:26
 */
@Data
public class FileAggregationBase implements ExportImportData {

    @ExcelProperty(index = 0)
    private String path;
    @ExcelProperty(index = 1)
    private String alias;
    @ExcelProperty(index = 2)
    private String displayName;
    @ExcelProperty(index = 3)
    private String refers;
    @ExcelProperty(index = 4)
    private String frequency;
    @ExcelProperty(index = 5)
    private String description;
    @ExcelProperty(index = 6)
    private String autoDashboard;
    @ExcelProperty(index = 7)
    private String persistence;
    @ExcelProperty(index = 8)
    private String label;

    @Override
    public void handleRefers(String refers) {
        this.refers = refers;
    }

    public static List<String> getFields() {
        Field[] fields = FileAggregationBase.class.getDeclaredFields();
        List<String> fieldNames = new ArrayList<>();
        for (Field field : fields) {
            fieldNames.add(field.getName());
        }
        return fieldNames;
    }

    public static void main(String[] args) {

        System.out.println(getFields());
    }
}

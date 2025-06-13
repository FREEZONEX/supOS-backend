package com.supos.common.utils;

import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.FieldDefines;
import com.supos.common.enums.FieldType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;

import static com.supos.common.Constants.*;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/3/3 11:12
 */
@Slf4j
public class FieldUtils {

    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

    public static FieldDefine getTimestampField(FieldDefine[] fields) {
        for (FieldDefine define : fields) {
            if (define.getType() == FieldType.DATETIME) {
                return define;
            }
        }
        return null;
    }

    public static FieldDefine getQualityField(FieldDefine[] fields, int dataType) {
        if (dataType == Constants.TIME_SEQUENCE_TYPE && fields != null && fields.length > 2) {
            return fields[fields.length - 2];
        }
        return null;
    }

    public static TableFieldDefine processFieldDefines(final String ALIAS, final SrcJdbcType jdbcType, FieldDefine[] fields, String[] err, boolean checkSysField) {
        return processFieldDefines(ALIAS, jdbcType, fields, err, checkSysField, true);
    }

    public static class TableFieldDefine {
        public final String tableName;
        public final FieldDefine[] fields;

        TableFieldDefine(String tableName, FieldDefine[] fields) {
            this.tableName = tableName;
            this.fields = fields;
        }
    }

    public static TableFieldDefine processFieldDefines(final String ALIAS, final SrcJdbcType jdbcType, FieldDefine[] fields, String[] err, boolean checkSysField, boolean addSysField) {
        if (fields == null || fields.length == 0) {
            return null;
        }
        HashMap<String, FieldDefine> fieldMap = new HashMap<>();
        for (FieldDefine f : fields) {
            String name = f.getName();
            if (name == null || (name = name.trim()).isEmpty()) {
                err[0] = I18nUtils.getMessage("uns.invalid.emptyFieldName", name);
                return null;
            }
            if (name.length() > 63) {
                err[0] = I18nUtils.getMessage("uns.field.tooLong", name);
                return null;
            }
            if (fieldMap.put(name, f) != null) {
                err[0] = I18nUtils.getMessage("uns.field.duplicate", name);
                return null;
            } else if (name.startsWith(Constants.SYSTEM_FIELD_PREV)) {
                if (checkSysField && !systemFields.contains(name)) {
                    err[0] = I18nUtils.getMessage("uns.field.startWith.limit.underline", name);
                    return null;
                }
            } else if (Character.isDigit(name.charAt(0))) {
                err[0] = I18nUtils.getMessage("uns.field.startWith.limit.number", name);
                return null;
            }

            if (f.getType() == FieldType.STRING && f.getMaxLen() == null) {
                String low = name.toLowerCase();
                f.setMaxLen(low.contains("name") || low.contains("tag") ? 64 : FieldDefines.DEFAULT_MAX_STR_LEN);
            }
        }
        FieldDefine createTimeField = fieldMap.get(SYS_FIELD_CREATE_TIME);
        if (createTimeField != null && FieldType.DATETIME != createTimeField.getType()) {
            err[0] = I18nUtils.getMessage("uns.field.type.must.be.datetime");
            return null;
        }
        if (!addSysField) {
            return new TableFieldDefine(null, fields);
        }
        String tableName = null;
        if (jdbcType.typeCode == Constants.TIME_SEQUENCE_TYPE) {
            ArrayList<FieldDefine> fNews = new ArrayList<>(Math.max(fields.length + 4, 16));
            fNews.add(new FieldDefine(SYS_FIELD_CREATE_TIME, FieldType.DATETIME, true));
            int countNormal = 0;
            FieldDefine nf = null;
            for (FieldDefine f : fields) {
                String name = f.getName();
                if (!systemFields.contains(name)) {
                    countNormal++;
                    nf = f;
                    fNews.add(f);
                }
            }
            if (jdbcType == SrcJdbcType.TimeScaleDB && countNormal == 1 && Constants.SYSTEM_SEQ_VALUE.equals(nf.getName())) {
                FieldDefine tableValue = new FieldDefine(Constants.SYSTEM_SEQ_TAG, FieldType.STRING, true);
                tableValue.setTbValueName(nf.getName());
                tableValue.setMaxLen(200);
                fNews.get(1).setName(Constants.SYSTEM_SEQ_VALUE);
                fNews.add(tableValue);
                tableName = "supos_timeserial_" + nf.getType().getName().toLowerCase();
            } else {
                tableName = "";
            }
            fNews.add(new FieldDefine(Constants.QOS_FIELD, FieldType.LONG));
            fNews.add(new FieldDefine(Constants.SYS_SAVE_TIME, FieldType.DATETIME));
            fields = fNews.toArray(new FieldDefine[0]);
        } else {
            FieldDefine idField = fieldMap.get(SYS_FIELD_ID);
            boolean hasId = idField != null;
            if (!hasId) {
                for (FieldDefine f : fields) {
                    if (f.isUnique()) {
                        hasId = true;
                        break;
                    }
                }
            } else if (FieldType.LONG != idField.getType()) {
                err[0] = I18nUtils.getMessage("uns.field.type.must.be.long");
                return null;
            }
            if (!hasId || createTimeField == null) {
                FieldDefine[] newFs;
                if (!hasId && createTimeField == null) {
                    newFs = new FieldDefine[2 + fields.length];
                    newFs[0] = new FieldDefine(SYS_FIELD_CREATE_TIME, FieldType.DATETIME, false);
                    newFs[newFs.length - 1] = new FieldDefine(SYS_FIELD_ID, FieldType.LONG, true);//自动加上 id字段并作为主键
                    System.arraycopy(fields, 0, newFs, 1, fields.length);
                    log.debug("关系库类型自动加Id+ct: {}.{}", ALIAS, newFs[0].getName());
                } else if (!hasId) {
                    newFs = new FieldDefine[fields.length + 1];
                    newFs[newFs.length - 1] = new FieldDefine(SYS_FIELD_ID, FieldType.LONG, true);
                    System.arraycopy(fields, 0, newFs, 0, fields.length);
                } else {
                    newFs = new FieldDefine[1 + fields.length];
                    newFs[0] = new FieldDefine(SYS_FIELD_CREATE_TIME, FieldType.DATETIME, false);
                    System.arraycopy(fields, 0, newFs, 1, fields.length);
                }
                fields = newFs;
            }
        }
        return new TableFieldDefine(tableName, fields);
    }

    public static String validateFields(FieldDefine[] fields, boolean checkSysField) {
        HashMap<String, FieldDefine> fieldMap = new HashMap<>();
        for (FieldDefine f : fields) {
            final String name = f.getName();
            if (fieldMap.put(name, f) != null) {
                return I18nUtils.getMessage("uns.field.duplicate", name);
            }
            if (f.isSystemField()) {
                if (checkSysField && !systemFields.contains(name)) {
                    return I18nUtils.getMessage("uns.field.startWith.limit.underline", name);
                }
                continue;
            }

            if (!FIELD_NAME_PATTERN.matcher(name).matches()) {
                return I18nUtils.getMessage("uns.field.name.format.invalid", name);
            }
        }

        FieldDefine createTimeField = fieldMap.get(SYS_FIELD_CREATE_TIME);
        if (createTimeField != null && FieldType.DATETIME != createTimeField.getType()) {
            return I18nUtils.getMessage("uns.field.type.must.be.datetime");
        }

        return null;
    }

    public static int countNumericFields(FieldDefine[] fields) {
        int total = 0;
        for (FieldDefine f : fields) {
            if (f.getType().isNumber) {
                total++;
            }
        }
        return total;
    }
}

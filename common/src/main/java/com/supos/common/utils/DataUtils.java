package com.supos.common.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.enums.FieldType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/4/16 9:03
 */
@Slf4j
public class DataUtils {

    private static ExecutorService blobSaveExecutorService = ThreadUtil.newFixedExecutor(1, 1024, "Blob-save-", true);

    public static String getBlobTag(String fileName, FieldDefine blobField) {
        return getBlobTag(fileName, blobField.getName(), blobField.getType());
    }

    public static String getBlobTag(String fileName, String fieldName, FieldType fieldType) {
        return String.format("%s---%s-%s-%d", fieldType.name, fileName, fieldName, SuposIdUtil.nextId());
    }

    public static String saveBlobData(String fileAlias, FieldDefine fieldDefine, Object blobData) {
        return saveBlobData(fileAlias, fieldDefine.getName(), fieldDefine.getType(), blobData);
    }

    public static String getBlobData(String blobTag) {
        if (blobTag == null || blobTag.isEmpty()) {
            return null;
        }
        String filePath = String.format("%s/%s", Constants.BLOB_PATH, blobTag);
        File file = new File(filePath);
        if (file.exists()) {
            try {
                return Base64.encode(file);
            } catch (Exception ex) {
                return null;
            }
        } else {
            return "";
        }
    }

    /**
     * 异步保存blob类型的数据
     *
     * @param fileAlias
     * @param blobData
     * @return
     */
    public static String saveBlobData(String fileAlias, String fieldName, FieldType fieldType, Object blobData) {

        if (blobData == null) {
            return null;
        }
        byte[] bytes;
        if (blobData instanceof CharSequence) {
            if (Base64.isBase64((CharSequence) blobData)) {
                bytes = Base64.decode((CharSequence) blobData);
            } else {
                bytes = null;
                throw new RuntimeException("blobData is not base64");
            }
        } else if (blobData instanceof byte[]) {
            bytes = (byte[]) blobData;
        } else {
            bytes = null;
            throw new RuntimeException("blobData is not base64");
        }
            /* else if (blobData.getClass().isArray()){
            if (blobData.getClass().componentType() == byte.class) {
                try {
                    byteArrayOutputStream.write((byte[]) blobData);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("blobData is not byte[]");
            }
        }*/
        AtomicReference<String> blobFlag = new AtomicReference<>();
        if (bytes != null) {
            blobFlag.set(getBlobTag(fileAlias, fieldName, fieldType));
            String filePath = String.format("%s/%s", Constants.BLOB_PATH, blobFlag.get());
            //blobSaveExecutorService.execute(() -> {
                try {
                    log.info("save blob {},size:{}", blobFlag.get(), bytes.length);
                    FileUtil.writeBytes(bytes, filePath);
                } catch (Exception e) {
                    log.error("blob数据保存失败", e);
                }

            //});
        }
        return blobFlag.get();
    }

    public static String handleBolb(String msg, CreateTopicDto def) {
        if (StringUtils.isBlank(msg) || def == null || !def.isHasBlobField()) {
            return msg;
        }

        List<FieldDefine> blobFields = def.filterAllBlobField();
        if (blobFields.isEmpty()) {
            return msg;
        }

        JSONObject oldMsg = JSON.parseObject(msg);
        JSONObject data = oldMsg.getJSONObject("data");
        String payload = oldMsg.getString("payload");
        for (FieldDefine blobField : blobFields) {
            Object valueObj = data.get(blobField.getName());
            if (valueObj instanceof String valueStr) {
                if (StringUtils.startsWith(valueStr, FieldType.BLOB.getName() + "---")) {
                    String blobValue = getBlobData(valueStr);
                    data.put(blobField.getName(), blobValue);
                    payload = payload.replace(valueStr, blobValue);
                } else if (StringUtils.startsWith(valueStr, FieldType.LBLOB.getName() + "---")) {
                    data.put(blobField.getName(), "");
                    payload = payload.replace(valueStr, "");
                }
            }
        }

        JSONObject blobData = new JSONObject(oldMsg);
        blobData.put("data", data);
        blobData.put("payload", payload);
        return blobData.toJSONString();
    }

    public static Object getDefaultValue(FieldType fieldType) {
        if (fieldType == null) {
            return null;
        } else if (fieldType == FieldType.STRING || fieldType == FieldType.BLOB) {
            return "";
        } else if (fieldType == FieldType.LBLOB) {
            return "-";
        }
        return fieldType.defaultValue;
    }


    /**
     * 2025-06-05pride新加需求
     * 如果所订查询的文件当前无实时值，
     * 则status=通讯异常（0x80 00 00 00 00 00 00 00），
     * timeStampe=当前服务器时间戳，
     * value=各数据类型的初始值（详见文章节：支持数据类型）
     * @param strTypeQos true=字符串，false=long
     * 质量码类型：websokcet返回long类型的16进制质量码（0x8000000000000000L），open api接口返回String hex = Long.toHexString(0x8000000000000000L);
     */
    public static JSONObject transEmptyValue(CreateTopicDto uns ,boolean strTypeQos){
        JSONObject data = new JSONObject();
        if (uns == null || uns.getFields() == null){
            return data;
        }
        FieldDefine[] fields = uns.getFields();
        for (FieldDefine field : fields) {
            if (!Constants.QOS_FIELD.equals(field.getName())
                    && !Constants.SYS_FIELD_CREATE_TIME.equals(field.getName())
                    && !Constants.SYS_SAVE_TIME.equals(field.getName())){
                Object defValue = DataUtils.getDefaultValue(field.getType());
                data.put(field.getName(),defValue);
            }
        }
        //open api接口返回long类型的16进制质量码（0x8000000000000000L），websokcet返回String hex = Long.toHexString(0x8000000000000000L);
        if (strTypeQos){
            String qos = Long.toHexString(0x8000000000000000L);
            data.put(Constants.QOS_FIELD,qos);
        } else {
            long qos = 0x8000000000000000L;
            data.put(Constants.QOS_FIELD,qos);
        }
        data.put(Constants.SYS_FIELD_CREATE_TIME,System.currentTimeMillis());
        return data;
    }

}

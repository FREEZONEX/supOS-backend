package com.supos.common.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.HexUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.supos.common.Constants;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author sunlifang
 * @version 1.0
 * @description: topic、path、alias通用处理工具
 * @date 2025/2/14 9:01
 */
public class PathUtil {

    static final MessageDigest PATH_ID_DIGEST;

    static {
        try {
            PATH_ID_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    static final Pattern TOPIC_PATTERN = Pattern.compile(Constants.TOPIC_REG);

    static final Pattern ALIAS_PATTERN = Pattern.compile(Constants.ALIAS_REG);

    /**
     * 校验topic格式
     *
     * @param topic
     * @return
     */
    public static boolean validTopicFormate(String topic, Integer dataType) {
        if (dataType != null && Constants.ALARM_RULE_TYPE == dataType) {
            return true;
        }

        if (StringUtils.startsWith(topic, "/") || StringUtils.contains(topic, "//")) {
            return false;
        }
        if (!StringUtils.contains(topic, "/")) {
            return TOPIC_PATTERN.matcher(topic).matches();
        }

        String[] names = StringUtils.split(topic, '/');
        for (String name : names) {
            if (!TOPIC_PATTERN.matcher(name).matches()) {
                return false;
            }
        }
        return true;
    }

    public static final boolean isAliasFormatOk(String alias) {
        if (ALIAS_PATTERN.matcher(alias).matches()) {
            return true;
        }
        return false;
    }

    /**
     * 获取path的名称
     *
     * @param path
     * @return
     */
    public static String getName(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        int ed = path.length() - 1;
        if (path.charAt(ed) == '/') {
            ed--;
        }
        int x = path.lastIndexOf('/', ed);
        path = path.substring(x + 1, ed + 1);
        return path;
    }

    public static String cleanPath(String path) {
        int ed = path.length() - 1;
        if (ed > 0 && path.charAt(ed) == '/') {
            path = path.substring(0, ed);
        }
        return path;
    }

    public static boolean isRootPath(String path) {
        int st = 0, ed = path.length();
        if (path.charAt(0) == '/') {
            st++;
        }
        if (path.charAt(path.length() - 1) == '/') {
            ed--;
        }
        for (int i = st; i < ed; i++) {
            if (path.charAt(i) == '/') {
                return false;
            }
        }
        return true;
    }

    public static String generateFileAlias(String path) {
        return generateMd5Alias(path);
    }

    public static String generateMd5Alias(String path) {
        String aliasPath = path;
        final int LEN = path.length();
        if (LEN > 20) {
            int startPo = StringUtils.lastOrdinalIndexOf(path, "/", 2);
            aliasPath = startPo >= 0 ? path.substring(startPo + 1) : path.substring(LEN - 20, LEN);
        }
        aliasPath = aliasPath.replace("/", "_");
        aliasPath = aliasPath.replace("-", "_");
        aliasPath = PinyinUtil.getPinyin(aliasPath, "");
        if (!Character.isLetter(aliasPath.charAt(0))) {
            aliasPath = "_" + aliasPath;
        }

        if (LEN < 20) {
            return aliasPath;
        }
        HashFunction hf = Hashing.md5();
        HashCode hc = hf.newHasher().putBytes(path.getBytes(StandardCharsets.UTF_8)).hash();
        aliasPath = aliasPath.substring(0, Math.min(4, aliasPath.length())) + "_" + hc;
        return aliasPath;
    }

    public static String generateAlias(String path, int pathType) {
        if (pathType == 2) {
            return generateFileAlias(path);
        }
        String aliasPath = "";
        if (pathType == 0) {
            // folder:folder1/、folder1/folder2/
            if (StringUtils.countMatches(path, '/') > 1) {
                // folder:folder1/folder2/
                aliasPath = StringUtils.substring(path, StringUtils.lastOrdinalIndexOf(path, "/", 2));
            } else {
                // folder:folder1/
                aliasPath = path;
            }
        } else {
            aliasPath = path;
        }
        aliasPath = aliasPath.replace("/", "_");
        aliasPath = aliasPath.replace("-", "_");
        aliasPath = PinyinUtil.getPinyin(aliasPath, "");
        if (aliasPath.length() > 20) {
            aliasPath = aliasPath.substring(0, 20);
        }
        String uuid = UUID.randomUUID().toString(true).substring(0, 20);
        aliasPath = aliasPath + "_" + uuid;
        if (!Character.isLetter(aliasPath.charAt(0))) {
            aliasPath = "_" + aliasPath;
        }
        return aliasPath;
    }

    public static String subParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        int ed = path.length() - 1;
        if (path.charAt(ed) == '/') {
            ed--;
        }
        int x = path.lastIndexOf('/', ed);
        return x > 0 ? path.substring(0, x) : null;
    }

    public static final String genIdForPath(String path) {
        return HexUtil.encodeHexStr(PATH_ID_DIGEST.digest(path.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 以basePath作为父级节点，比对path找到basePath下一级的path
     * @param basePath
     * @param path
     * @return null 不存在下一级path
     */
    public static String getNextNodeAfterBasePath(String basePath, String path) {
        if (StringUtils.isBlank(basePath)) {
            // 如果 basePath 为空，直接返回 path 的第一个节点
            String[] pathParts = path.split("/");
            return pathParts.length > 0 ? pathParts[0] : null;
        } else {
            // 判断 path 是否以 basePath 开头
            if (path.startsWith(basePath)) {
                // 获取 basePath 后面的部分
                String remainingPath = path.substring(basePath.length());

                // 如果剩余部分以 / 开头，去掉它
                if (remainingPath.startsWith("/")) {
                    remainingPath = remainingPath.substring(1);
                }

                // 返回剩余部分的第一个节点（以 / 为分隔符）
                int nextSlashIndex = remainingPath.indexOf('/');
                if (nextSlashIndex != -1) {
                    return remainingPath.substring(0, nextSlashIndex);
                } else {
                    // 如果没有 /，说明这就是最后的节点
                    return remainingPath;
                }
            }
        }
        // 如果 path 不是以 basePath 开头，返回 null 或者处理异常
        return null;
    }



    /**
     * 判断path是否是basePath的下级路径
     * @param basePath
     * @param path
     * @return
     */
    public static boolean isNextLevelPath(String basePath, String path) {
        if (basePath.isEmpty()) {
            // 如果 basePath 为空，判断 path 是否是单层路径（没有 /）
            return !path.contains("/");
        }
        int baseDepth = basePath.split("/").length;
        // 判断是否以 basePath + "/" 开头，且层级是 baseDepth + 1
        return path.startsWith(basePath + "/") && path.split("/").length == baseDepth + 1;
    }

    public static String escapeName(String name) {
        char[] cs = name.toCharArray();
        boolean changed = false;
        for (int i = 0; i < cs.length; i++) {
            char c = cs[i];
            if (!Character.isJavaIdentifierPart(c) || c == '$') {
                changed = true;
                cs[i] = '_';
            }
        }
        return changed ? new String(cs) : name;
    }

    /**
     * 去掉 path 的最后一级路径
     * @param path 原始路径，如 "a/b/c/d/e"
     * @return 去掉最后一级后的路径，如 "a/b/c/d"
     */
    public static String removeLastLevel(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        String[] parts = path.split("/");

        // 如果只有一级或为空，直接返回空字符串
        if (parts.length <= 1) {
            return "";
        }

        // 用 StringBuilder 组装前面部分
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                result.append("/");
            }
            result.append(parts[i]);
        }

        return result.toString();
    }

    /**
     * 返回路径的最后一级
     * @param path 原始路径，如 "a/b/c/d/e"
     * @return 最后一级，如 "e"
     */
    public static String getLastLevel(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        String[] parts = path.split("/");

        // 如果路径为空或无有效层级，返回空字符串
        if (parts.length == 0) {
            return "";
        }

        return parts[parts.length - 1];
    }

    public static String generateUniqueName(String baseName, List<String> all) {
        int maxSuffix = -1;

        if (!all.contains(baseName)) {
            return baseName;
        }

        for (String name : all) {

            if (name.equals(baseName)) {
                // 说明原始名字已被占用
                maxSuffix = Math.max(maxSuffix, 0);
            } else if (name.startsWith(baseName + "-")) {
                // 提取后缀数字
                String suffixStr = name.substring(baseName.length() + 1);
                if (suffixStr.matches("\\d+")) {
                    int suffix = Integer.parseInt(suffixStr);
                    maxSuffix = Math.max(maxSuffix, suffix);
                }
            }
        }

        if (maxSuffix == -1) {
            // 没有重复
            return baseName;
        } else {
            return baseName + "-" + (maxSuffix + 1);
        }
    }
}

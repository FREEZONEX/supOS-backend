package com.supos.common.utils;

import com.google.common.primitives.Doubles;
import com.supos.common.annotation.DateTimeConstraint;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.FieldDefines;
import com.supos.common.enums.FieldType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ALL")
@Slf4j
public class FindDataListUtils {

    public static SearchResult findDataList(Object obj, int minMatchField, FieldDefines fieldDefines) {
        SearchResult sm = new SearchResult(minMatchField, false);
        if (fieldDefines == null) {
            fieldDefines = new FieldDefines();
        }
        findDataList(null, 0, obj, fieldDefines, sm, "");
        return sm;
    }

    public static SearchResult findMultiDataList(Object obj, FieldDefines fieldDefines) {
        SearchResult sm = new SearchResult(0, true);
        if (fieldDefines == null) {
            fieldDefines = new FieldDefines();
        }
        findDataList(null, 0, obj, fieldDefines, sm, "");
        return sm;
    }

    public static class ListResult {
        public final List<Map<String, Object>> list;
        public final String dataPath;
        public final int score;
        public final boolean dataInList;

        ListResult(List<Map<String, Object>> list, String dataPath, int score, boolean dataInList) {
            this.list = list;
            this.dataPath = dataPath;
            this.score = score;
            this.dataInList = dataInList;
        }
    }

    public static class SearchResult {
        final int minMatchField;
        final boolean multiFind;
        int maxMatch;
        public List<Map<String, Object>> list;
        public Collection<ListResult> multiResults;
        public String dataPath;
        public boolean dataInList;
        public String errorField;
        public String toLongField;

        SearchResult(int minMatchField, boolean multiFind) {
            this.minMatchField = minMatchField;
            this.multiFind = multiFind;
        }

        void setList(List<Map<String, Object>> list, int score, String dataPath, boolean dataInList) {
            if (score > maxMatch) {
                this.list = list;
                this.maxMatch = score;
                this.dataPath = dataPath;
                this.dataInList = dataInList;
            }
            if (multiFind) {
                if (multiResults == null) {
                    multiResults = new TreeSet<>((a, b) -> {
                        int rs = b.score - a.score;
                        return rs != 0 ? rs : a.hashCode() - b.hashCode();
                    });
                }
                multiResults.add(new ListResult(list, dataPath, score, dataInList));
            }
        }
    }

    public static boolean isSimpleType(Class clazz) {
        return (Number.class.isAssignableFrom(clazz) || clazz == String.class || clazz == Boolean.class || clazz.isPrimitive());
    }

    public static int typeMatchScore(AtomicReference<Object> obj, FieldDefine define) {
        if (define == null) {
            return -2;
        }
        FieldType fieldType = define.getType();
        Integer maxLen = define.getMaxLen();
        return typeMatchScore(obj, fieldType, maxLen != null ? maxLen.intValue() : FieldDefines.DEFAULT_MAX_STR_LEN);
    }

    static final int ERR_OUT_OF_RANGE = -101;
    private static BigDecimal maxInt = java.math.BigDecimal.valueOf(Integer.MAX_VALUE);
    private static BigDecimal minInt = java.math.BigDecimal.valueOf(Integer.MIN_VALUE);
    private static BigDecimal maxLong = java.math.BigDecimal.valueOf(Long.MAX_VALUE);
    private static BigDecimal minLong = java.math.BigDecimal.valueOf(Long.MIN_VALUE);
    private static BigDecimal maxFloat = java.math.BigDecimal.valueOf(Float.MAX_VALUE);
    private static BigDecimal minFloat = java.math.BigDecimal.valueOf(Float.MIN_VALUE);
    private static BigDecimal maxDouble = java.math.BigDecimal.valueOf(Double.MAX_VALUE);
    private static BigDecimal minDouble = java.math.BigDecimal.valueOf(Double.MIN_VALUE);

    public static int typeMatchScore(AtomicReference<Object> vHolder, FieldType fieldType, int maxStrLen) {
        if (fieldType == null) {
            return -1;
        }
        Object obj;
        if (vHolder == null || (obj = vHolder.get()) == null) {
            return 98;
        }
        Class clazz = obj.getClass();
        int score = 0;
        if (fieldType.isNumber) {
            BigDecimal vNum;
            try {
                vNum = new BigDecimal(obj.toString());
                score = 99;
            } catch (Exception ex) {
                return -1;
            }
            if (vNum.intValue() == 0) {
                switch (fieldType) {
                    case INT:
                        vHolder.set(vNum.intValue());
                        break;
                    case LONG:
                        vHolder.set(vNum.longValue());
                        break;
                    case FLOAT:
                        vHolder.set(vNum.floatValue());
                        break;
                    case DOUBLE:
                        vHolder.set(vNum.doubleValue());
                        break;
                }
                return score;
            }
            switch (fieldType) {
                case INT:
                    if (vNum.compareTo(minInt) < 0 || vNum.compareTo(maxInt) > 0) {
                        return ERR_OUT_OF_RANGE;
                    }
                    vHolder.set(vNum.intValue());
                    break;
                case LONG:
                    if (vNum.compareTo(minLong) < 0 || vNum.compareTo(maxLong) > 0) {
                        return ERR_OUT_OF_RANGE;
                    }
                    vHolder.set(vNum.longValue());
                    break;
                case FLOAT:
                    if (vNum.compareTo(minFloat) < 0 || vNum.compareTo(maxFloat) > 0) {
                        return ERR_OUT_OF_RANGE;
                    }
                    vHolder.set(vNum.floatValue());
                    break;
                case DOUBLE:
                    if (vNum.compareTo(minDouble) < 0 || vNum.compareTo(maxDouble) > 0) {
                        return ERR_OUT_OF_RANGE;
                    }
                    vHolder.set(vNum.doubleValue());
                    break;
            }
        } else if (fieldType == FieldType.STRING) {
            String s = obj.toString();
            if (maxStrLen > 0 && s.length() > maxStrLen) {
                score = ERR_OUT_OF_RANGE;
            } else if (clazz == String.class) {
                score = 100;
            } else {
                score = 97;
            }
        } else if (fieldType == FieldType.DATETIME) {
            String str = obj.toString();
            if (Number.class.isAssignableFrom(clazz)) {
                score = 97;
            } else if (DateTimeConstraint.parseDate(str) != null) {
                score = 98;
            }
        } else if (fieldType == FieldType.BOOLEAN) {
            if (clazz == Boolean.class || clazz == boolean.class) {
                score = 100;
            } else {
                String str = obj.toString();
                if ("true".equalsIgnoreCase(str)) {
                    score = 99;
                    vHolder.set(Boolean.TRUE);
                } else if ("false".equalsIgnoreCase(str)) {
                    score = 99;
                    vHolder.set(Boolean.FALSE);
                } else if ("0".equals(str)) {
                    score = 98;
                    vHolder.set(Boolean.FALSE);
                } else if ("1".equals(str)) {
                    score = 98;
                    vHolder.set(Boolean.TRUE);
                } else if (Doubles.tryParse(str) != null) {
                    score = 97;
                    vHolder.set(Boolean.TRUE);
                }
            }
        } else if (fieldType == FieldType.BLOB || fieldType == FieldType.LBLOB) {
            if (clazz == String.class) {
                score = 100;
            }
        }
        return score;
    }

    private static boolean findDataList(Object parent, int Size, Object obj, FieldDefines fieldDefines, SearchResult rs, String dataPath) {
        Map<String, FieldDefine> fieldTypes = fieldDefines.getFieldsMap();
        Map<String, String> fieldIndexes = fieldDefines.getFieldIndexMap();
        Class clazz = obj.getClass();
        if (isSimpleType(clazz)) {
            if (fieldTypes != null && fieldTypes.size() > 0) {
                int score = -1;
                String field = null;
                AtomicReference<Object> vh = new AtomicReference<>(obj);
                for (Map.Entry<String, FieldDefine> entry : fieldTypes.entrySet()) {
                    FieldDefine f = entry.getValue();
                    String fieldName = f.getName();
                    if (!f.isSystemField()) {
                        int matchScore = typeMatchScore(vh, f);
                        if (matchScore > score) {
                            score = matchScore;
                            field = fieldName;
                        }
                    }
                }
                if (field != null) {
                    HashMap map = new HashMap(2);
                    map.put(field, vh.get());
                    rs.setList(Collections.singletonList(map), score, "", false);
                }
            }
            return true;
        }
        if (List.class.isAssignableFrom(clazz)) {
            List list = (List) obj;
            int size = list.size();
            if (size > 0) {
                Object v = list.get(0);
                Class vc = v.getClass();
                if (isSimpleType(vc)) {
                    Map<String, Object> bean = new HashMap<>(fieldTypes.size());
                    for (Map.Entry<String, FieldDefine> entry : fieldTypes.entrySet()) {
                        FieldDefine fieldDefine = entry.getValue();
                        String indexStr = fieldDefine.getIndex();
                        if (indexStr != null && (indexStr = indexStr.trim()).length() > 0 && Character.isDigit(indexStr.charAt(0))) {
                            Integer index = IntegerUtils.parseInt(indexStr);
                            if (index != null && index.intValue() < size) {
                                String fieldName = entry.getKey();
                                bean.put(fieldName, list.get(index));
                            }
                        }
                    }
                    if (!bean.isEmpty()) {
                        rs.setList(Arrays.asList(bean), 1, dataPath, false);
                    } else {
                        log.warn("fieldIndexNotFound: {}", obj);
                    }
                } else {
                    for (Object o : list) {
                        if (o != null) {
                            return findDataList(obj, size, o, fieldDefines, rs, dataPath);
                        }
                    }
                }
            }
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection collection = (Collection) obj;
            int size = collection.size();
            if (size > 0) {
                for (Object o : collection) {
                    if (o != null) {
                        return findDataList(obj, size, o, fieldDefines, rs, "");
                    }
                }
            }
        } else if (clazz.isArray()) {
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                Object o = Array.get(obj, i);
                if (o != null) {
                    return findDataList(obj, len, o, fieldDefines, rs, "");
                }
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map<String, Object> m = (Map) obj;
            int countFieldMatch = procMap(parent, Size, fieldDefines, rs, dataPath, m, fieldTypes, fieldIndexes);
            if (countFieldMatch >= rs.minMatchField) {//至少匹配1个字段
                if (parent instanceof Collection) {
                    Collection collection = (Collection) parent;
                    Iterator<Map<String, Object>> itr = collection.iterator();
                    if (itr.hasNext()) {
                        itr.next();
                        while (itr.hasNext()) {
                            Map<String, Object> brother = itr.next();
                            int brotherMatch = procMap(parent, Size, fieldDefines, rs, dataPath, brother, fieldTypes, fieldIndexes);
                            if (brotherMatch < rs.minMatchField) {
                                itr.remove();
                            }
                        }
                    }
                }
                List<Map<String, Object>> list;
                boolean inList = false;
                if (parent != null) {
                    if (parent instanceof List) {
                        inList = true;
                        List ls = (List) parent;
                        list = ls;
                    } else if (parent instanceof Collection) {
                        inList = true;
                        list = new ArrayList<>((Collection) parent);
                    } else if (parent.getClass().isArray()) {
                        inList = true;
                        int len = Array.getLength(parent);
                        list = new ArrayList<>(len);
                        for (int i = 0; i < len; i++) {
                            Object o = Array.get(parent, i);
                            list.add((Map<String, Object>) o);
                        }
                    } else {
                        list = new ArrayList<>(1);
                        list.add(m);
                    }
                } else {
                    list = new ArrayList<>(1);
                    list.add(m);
                }
                rs.setList(list, Size * 2 + countFieldMatch, dataPath, inList);
                return true;
            }
        }
        return false;
    }

    private static int procMap(Object parent, int Size, FieldDefines fieldDefines, SearchResult rs, String dataPath, Map<String, Object> m, Map<String, FieldDefine> fieldTypes, Map<String, String> fieldIndexes) {
        int countFieldMatch = 0;
        java.util.Iterator<Map.Entry<String, Object>> itr = m.entrySet().iterator();
        LinkedList<Object[]> addkvList = null;
        while (itr.hasNext()) {
            Map.Entry<String, Object> entry = itr.next();
            String k = String.valueOf(entry.getKey());
            Object v = entry.getValue();
            Class vc;
            Map<String, Object> vm;
            if (v == null || isSimpleType(vc = v.getClass())) {
                String fieldName;
                FieldDefine fd1 = null, fdIdx = null;
                int ecA = 0, ecB = 0;
                AtomicReference<Object> vh = new AtomicReference<>(v);
                if (fieldTypes.isEmpty() || (ecA = typeMatchScore(vh, fd1 = fieldTypes.get(k))) > 0) {
                    countFieldMatch++;
                    Object newVal = vh.get();
                    if (newVal != v) {
                        if (addkvList == null) {
                            addkvList = new LinkedList<>();
                        }
                        addkvList.add(new Object[]{k, newVal});
                    }
                } else if ((fieldName = fieldIndexes.get(k)) != null && (ecB = typeMatchScore(vh, fdIdx = fieldTypes.get(fieldName))) > 0) {
                    countFieldMatch++;
                    itr.remove();
                    if (addkvList == null) {
                        addkvList = new LinkedList<>();
                    }
                    addkvList.add(new Object[]{fieldName, vh.get()});
                } else if (fd1 != null || fdIdx != null) {
                    FieldDefine fd = fd1 != null ? fd1 : fdIdx;
                    int errCode;
                    if (fd1 != null) {
                        fd = fd1;
                        errCode = ecA;
                    } else {
                        fd = fdIdx;
                        errCode = ecB;
                    }
                    if (errCode != ERR_OUT_OF_RANGE) {
                        rs.errorField = fd.getName();
                    } else {
                        rs.toLongField = fd.getName();
                    }
                    countFieldMatch = -1;
                    break;
                }
            } else {
                FieldDefine fd = fieldTypes.get(k);
                String fieldName;
                if (fd == null && (fieldName = fieldIndexes.get(k)) != null) {
                    fd = fieldTypes.get(fieldName);
                }
                if (fd != null && fd.getType() != FieldType.STRING) {
                    rs.errorField = fd.getName();
                    countFieldMatch = -1;
                    break;
                }
                int sc = rs.maxMatch;
                findDataList(m, 0, v, fieldDefines, rs, StringUtils.hasText(dataPath) ? dataPath + "." + k : k);
                if (fd != null && fd.getType() == FieldType.STRING && rs.maxMatch == sc) {
                    itr.remove();
                    String cvtStr = JsonUtil.toJson(v);
                    Integer maxLen = fd.getMaxLen();
                    int max = maxLen != null ? maxLen.intValue() : FieldDefines.DEFAULT_MAX_STR_LEN;
                    if (max < 0 || cvtStr.length() <= max) {
                        countFieldMatch++;
                        if (addkvList == null) {
                            addkvList = new LinkedList<>();
                        }
                        addkvList.add(new Object[]{fd.getName(), cvtStr});
                    } else {
                        rs.toLongField = fd.getName();
                    }
                }
            }
        }
        if (addkvList != null && countFieldMatch > 0) {
            for (Object[] kv : addkvList) {
                m.put(kv[0].toString(), kv[1]);
            }
        }
        return countFieldMatch;
    }
}

package com.supos.common.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.util.*;

public class FieldDefines {
    public static final int DEFAULT_MAX_STR_LEN = 512;
    @Getter
    private Map<String, FieldDefine> fieldsMap;
    @Getter @Setter
    private FieldDefine calcField;

    @Getter
    private Map<String, String> fieldIndexMap;
    @Getter
    Set<String> uniqueKeys = Collections.emptySet();

    public FieldDefines() {
        fieldsMap = Collections.emptyMap();
        fieldIndexMap = Collections.emptyMap();
    }

    public FieldDefines(Map<String, FieldDefine> fieldsMap) {
        init(fieldsMap != null && !fieldsMap.isEmpty() ? fieldsMap.values() : null);
    }

    public FieldDefines(FieldDefine[] fields) {
        init(fields != null && fields.length > 0 ? Arrays.asList(fields) : null);
    }

    private void init(Collection<FieldDefine> fs) {
        if (!CollectionUtils.isEmpty(fs)) {
            Set<String> ids = new LinkedHashSet<>(2);
            fieldsMap = new LinkedHashMap<>(fs.size());
            for (FieldDefine f : fs) {
                fieldsMap.put(f.getName(), f);
                if (f.isUnique()) {
                    ids.add(f.getName());
                }
                String index = f.getIndex();
                if (index != null) {
                    if (fieldIndexMap == null) {
                        fieldIndexMap = new HashMap<>(fs.size());
                    }
                    fieldIndexMap.put(index, f.getName());
                }
            }
            this.uniqueKeys = Collections.unmodifiableSet(ids) ;
            if (fieldIndexMap == null) {
                fieldIndexMap = Collections.emptyMap();
            }
        } else {
            fieldsMap = Collections.emptyMap();
            fieldIndexMap = Collections.emptyMap();
        }
    }


    public FieldDefine[] toFieldDefineArray() {
        if (fieldsMap.isEmpty()) {
            return new FieldDefine[0];
        }
        return fieldsMap.values().toArray(new FieldDefine[0]);
    }
}

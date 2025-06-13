package com.supos.common.dto;

import com.supos.common.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Valid
public class SaveDataDto implements Cloneable {
    Long id;

    String table;

    FieldDefines fieldDefines;
    CreateTopicDto createTopicDto;
    /**
     * 数据列表
     */
    @NotEmpty
    List<Map<String, Object>> list;

    Set<String> tables;

    Iterator<Map<String, Object>> listItr;

    public SaveDataDto(Long id, String table, FieldDefines fieldDefines, List<Map<String, Object>> list) {
        this.id = id;
        this.table = table;
        this.fieldDefines = fieldDefines;
        this.list = list;
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }

    @Override
    public SaveDataDto clone() {
        SaveDataDto rs;
        try {
            rs = (SaveDataDto) super.clone();
        } catch (CloneNotSupportedException e) {
            rs = new SaveDataDto(id, table, fieldDefines, list);
            rs.createTopicDto = this.createTopicDto;
        }
        rs.setList(list != null && !list.isEmpty() ? new LinkedList<>(list) : new LinkedList<>());
        return rs;
    }
}

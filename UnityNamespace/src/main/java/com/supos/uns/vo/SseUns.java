package com.supos.uns.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class SseUns {
    Long id;
    String alias;
    String path;
    String dataPath;
    FieldDefine[] fields;
    Map<Long, Integer> refUns;

    public SseUns(Long id) {
        this.id = id;
    }

    public SseUns(CreateTopicDto dto) {
        this.id = dto.getId();
        this.alias =  dto.getAlias();
        this.path = dto.getPath();
        this.dataPath = dto.getDataPath();
        this.fields = dto.getFields();
        this.refUns = dto.getRefUns();
    }
}

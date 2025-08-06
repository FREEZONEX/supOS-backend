package com.supos.uns.bo;

import com.supos.common.dto.CreateTopicDto;
import com.supos.uns.dao.po.UnsPo;
import lombok.Getter;

import java.util.TreeMap;

public class UnsPoLabels implements UnsLabels {
    @Getter
    final UnsPo unsPo;
    final String[] labels;
    @Getter
    final boolean resetLabels;
    CreateTopicDto dto;
    final TreeMap<Long, String> labelIds = new TreeMap<>();

    public UnsPoLabels(UnsPo unsPo, boolean resetLabels, String[] labels) {
        this.unsPo = unsPo;
        this.resetLabels = resetLabels;
        this.labels = labels;
        unsPo.setLabelIds(labelIds);
    }

    @Override
    public Long unsId() {
        return unsPo.getId();
    }

    public void setLabelId(String label, Long id) {
        labelIds.put(id, label);
    }

    public void setDto(CreateTopicDto dto) {
        this.dto = dto;
        dto.setLabelIds(labelIds);
    }

    @Override
    public String[] labelNames() {
        return labels;
    }
}

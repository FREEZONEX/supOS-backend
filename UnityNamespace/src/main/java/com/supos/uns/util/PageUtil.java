package com.supos.uns.util;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.common.dto.PageResultDTO;

import java.util.Collections;
import java.util.List;

public class PageUtil {

    public static <T> PageResultDTO<T> build(IPage<T> iPage) {
        PageResultDTO.PageResultDTOBuilder<T> pageBuilder = PageResultDTO.<T>builder()
                .code(0)
                .total(iPage.getTotal())
                .pageNo(iPage.getPages())
                .pageSize(iPage.getSize())
                .data(iPage.getRecords());
        return pageBuilder.build();
    }

    public static <K, T> PageResultDTO<K> build(IPage<T> iPage, List<K> records) {
        PageResultDTO.PageResultDTOBuilder<K> pageBuilder = PageResultDTO.<K>builder()
                .code(0)
                .total(iPage.getTotal())
                .pageNo(iPage.getCurrent())
                .pageSize(iPage.getSize())
                .data(records);
        return pageBuilder.build();
    }

    public static <K, T> PageResultDTO<K> empty(Page page) {
        PageResultDTO.PageResultDTOBuilder<K> pageBuilder = PageResultDTO.<K>builder()
                .code(0)
                .total(0)
                .pageNo(page.getCurrent())
                .pageSize(page.getSize())
                .data(Collections.emptyList());
        return pageBuilder.build();
    }
}

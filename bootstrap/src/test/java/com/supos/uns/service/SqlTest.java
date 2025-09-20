package com.supos.uns.service;

import com.supos.uns.dao.mapper.UnsMapper;
import org.junit.Test;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class SqlTest {

    @Test
    public void testSql() {
        Map<Long, Integer> idDataTypes = new LinkedHashMap<>();
        idDataTypes.put(3L, 1);
        idDataTypes.put(6L, 5);
        idDataTypes.put(9L, 7);
        String sql = UnsMapper.UnsRefUpdateProvider.updateRefUns(1L, idDataTypes, new Date());
        System.out.println(sql);
    }
}

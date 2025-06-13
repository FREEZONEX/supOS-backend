package com.supos.uns.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supos.common.dto.AlarmRuleDefine;
import com.supos.common.event.EventBus;
import com.supos.common.event.multicaster.StatusAwareApplicationEventMultiCaster;
import com.supos.conf.TestCaseInitConfiguration;
import com.supos.uns.dao.mapper.AlarmMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.AlarmPo;
import org.junit.Test;
import org.junit.jupiter.api.Order;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestCaseInitConfiguration.class)
@RunWith(SpringRunner.class)
@ActiveProfiles("local")
public class UnsManagerServiceTest {

    @Autowired
    UnsManagerService unsManagerService;
    @Autowired
    UnsQueryService unsQueryService;
    @Autowired
    UnsMapper unsMapper;
    @Autowired
    StatusAwareApplicationEventMultiCaster runningAwareApplicationEventMultiCaster;

    @Autowired
    AlarmMapper alarmMapper;

    @Test
    public void test_event() {
        DemoEvent event = new DemoEvent(this);
        Collection<ApplicationListener<?>> listeners = runningAwareApplicationEventMultiCaster.getApplicationListeners(event);
        System.out.println("listeners.size=" + listeners.size() + ", " + listeners);
        EventBus.publishEvent(event);

        EventBus.publishEvent(event);
    }

    @Test
    public void test_alarmCount() {
        List<AlarmPo> alarmPos = alarmMapper.selectList(new QueryWrapper<AlarmPo>().select(AlarmRuleDefine.FIELD_UNS_ID, "count(1) as currentValue").groupBy(AlarmRuleDefine.FIELD_UNS_ID)
                .in(AlarmRuleDefine.FIELD_UNS_ID, Arrays.asList("/$alarm/baojing01_df8ac4c6fdb443c99c70", "/$alarm/baojing01_abcdfef488e149ab9b4f")));
        for (AlarmPo po : alarmPos) {
            System.out.println(po.getUns() + " : " + po.getCurrentValue().longValue());
        }
    }

    /*@Test
    public void test_listTree() {
        List<UnsPo> list = unsMapper.listAllNamespaces();
        long t0 = System.currentTimeMillis();
        List<TopicTreeResult> treeResults = unsQueryService.getTopicTreeResults(null, list, false);
        System.out.printf("buildTree Spend: %d ms\n", System.currentTimeMillis() - t0);
        long t1 = System.currentTimeMillis();
        String json = JsonUtil.toJson(treeResults);
        long t2 = System.currentTimeMillis();
        System.out.println(json);
        System.out.printf("toJson Spend: %d ms\n", t2 - t1);
    }*/

    @EventListener(classes = DemoEvent.class)
    @Order(100)
    void onDemoEvent(DemoEvent event) {
        System.out.println("执行Test");
    }

}

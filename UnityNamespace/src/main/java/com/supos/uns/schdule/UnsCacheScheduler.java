package com.supos.uns.schdule;

import com.supos.uns.dto.ExternalTopicCacheDto;
import com.supos.uns.service.UnsQueryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

@Component
public class UnsCacheScheduler {

    /**
     * 淘汰更新时间超过12小时的topic
     */
    @Scheduled(cron = "0 0 22 * * ?")
    public void cleanupExpiredTopics() {
        Map<String, ExternalTopicCacheDto> externalTopics = UnsQueryService.EXTERNAL_TOPIC_CACHE;
        long twelveHoursMillis = 12 * 60 * 60 * 1000; // 12 小时的毫秒数
        long currentTimeMillis = System.currentTimeMillis(); // 当前时间

        Iterator<Map.Entry<String, ExternalTopicCacheDto>> iterator = externalTopics.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ExternalTopicCacheDto> entry = iterator.next();
            if (currentTimeMillis - entry.getValue().getLastTime().getTime() > twelveHoursMillis) {
                iterator.remove(); // 删除超过 12 小时的 key
            }
        }
    }

}

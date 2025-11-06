package com.supos.uns;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.event.BatchCreateTableEvent;
import com.supos.common.event.InitTopicsEvent;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.event.UpdateInstanceEvent;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.IntegerUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.util.SseJsonBuilder;
import com.supos.uns.vo.SseUns;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/service-api/supos/uns/sync")
public class SseController {
    @Autowired
    IUnsDefinitionService unsDefinitionService;
    private final Map<String, UnsSseEmitter> emitters = new ConcurrentHashMap<>();
    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    static class UnsSseEmitter extends SseEmitter {
        final String cid;
        boolean synced;

        UnsSseEmitter(String cid) {
            super(0L);// 0表示不超时
            this.cid = cid;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Setter
    public static class SseResponse {
        int code;
        String msg;
        String cid;
        Collection<SseUns> list;

        SseResponse(int code, String msg, String cid) {
            this.code = code;
            this.msg = msg;
            this.cid = cid;
        }

        SseResponse(int code, Collection<SseUns> list) {
            this.code = code;
            this.list = list;
        }
    }

    // SSE 事件流端点 - 供客户端订阅
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@RequestParam("cid") String cid) {
        String clientId = UUID.randomUUID().toString();
        UnsSseEmitter emitter = new UnsSseEmitter(cid); // 0表示不超时

        emitters.put(clientId, emitter);
        emitter.onCompletion(() -> emitters.remove(clientId));
        emitter.onTimeout(() -> emitters.remove(clientId));
        SseJsonBuilder jsonBuilder = new SseJsonBuilder(JsonUtil.jackToJson(new SseResponse(200, "ok", clientId)));
        // 发送欢迎消息
        boolean health = true;
        try {
            emitter.send(jsonBuilder);
        } catch (IOException e) {
            health = false;
            emitter.completeWithError(e);
        }
        if (health) {
            if (!Constants.readOnlyMode.get()) {
                emitter.synced = true;
                initSync(emitter);
            }
        }
        return emitter;
    }

    @EventListener(classes = InitTopicsEvent.class)
    @Order
    void init() {
        initSync(null);
    }

    private void initSync(UnsSseEmitter emitter) {
        if (emitter == null && emitters.values().stream().allMatch(e -> e.synced)) {
            return;
        }
        List<SseUns> allList = unsDefinitionService.getTopicDefinitionMap().values()
                .stream().filter(t -> filter(t.getCreateTopicDto()))
                .map(t -> new SseUns(t.getCreateTopicDto()))
                .sorted(Comparator.comparing(SseUns::getId).reversed())
                .collect(Collectors.toList());
        for (List<SseUns> files : Lists.partition(allList, 1000)) {
            SseJsonBuilder jsonBuilder = new SseJsonBuilder(JsonUtil.jackToJson(new SseResponse(200, files)));
            if (emitter != null) {
                log.info("直接推送>> {}", emitter.cid);
                try {
                    emitter.send(jsonBuilder);
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            } else {
                for (UnsSseEmitter em : emitters.values()) {
                    if (!em.synced) {
                        log.info("启动后推送>> {}", em.cid);
                        executorService.submit(() -> {
                            try {
                                em.send(jsonBuilder);
                            } catch (IOException e) {
                                em.completeWithError(e);
                            }
                        });
                    }
                }

            }
        }
    }

    @EventListener(classes = BatchCreateTableEvent.class)
    void onBatchCreateTableEvent(BatchCreateTableEvent event) {
        if (emitters.isEmpty()) {
            return;
        }
        ArrayList<SseUns> list = new ArrayList<>();
        for (CreateTopicDto[] dtos : event.topics.values()) {
            for (CreateTopicDto dto : dtos) {
                if (filter(dto)) {
                    list.add(new SseUns(dto));
                    if (list.size() >= 1000) {
                        broadcastUns(list);
                        list.clear();
                    }
                }
            }
        }
        if (!list.isEmpty()) {
            broadcastUns(list);
        }
    }

    @EventListener(classes = UpdateInstanceEvent.class)
    void onUpdateInstanceEvent(UpdateInstanceEvent event) {
        if (emitters.isEmpty()) {
            return;
        }
        broadcastUns(event.topics.stream().filter(SseController::filter).map(SseUns::new).toList());
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        if (emitters.isEmpty()) {
            return;
        }
        broadcastUns(event.topics.stream().filter(SseController::filter).map(t -> new SseUns(t.getId())).toList());
    }

    private void broadcastUns(Collection<SseUns> files) {
        if (CollectionUtils.isNotEmpty(files) && !emitters.isEmpty()) {
            files = files.stream().sorted(Comparator.comparing(SseUns::getId).reversed())
                    .collect(Collectors.toList());
            broadcastMessage(new MessageRequest(JsonUtil.jackToJson(new SseResponse(200, files))));
        }
    }

    // 普通 API 端点 - 目前仅自测用
    @PostMapping("/send-message")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody MessageRequest request) {
        // 向所有连接的客户端广播消息
        broadcastMessage(request);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "timestamp", System.currentTimeMillis()
        ));
    }

    private static boolean filter(CreateTopicDto dto) {
        int pt = IntegerUtils.getInt(dto.getPathType(), 0);
        int dt = IntegerUtils.getInt(dto.getDataType(), 0);
        return pt == Constants.PATH_TYPE_FILE && (dt == 1 || dt == 2 || dt == 7);
    }

    // 广播消息给所有客户端
    private void broadcastMessage(MessageRequest request) {
        executorService.submit(() -> {
            LinkedList<String> deadClients = new LinkedList<>();
            String srcCli = request.clientId;
            SseJsonBuilder jsonBuilder = new SseJsonBuilder(request.message);
            emitters.forEach((clientId, emitter) -> {
                if (!clientId.equals(srcCli)) {
                    try {
                        emitter.send(jsonBuilder);
                    } catch (IOException e) {
                        deadClients.add(clientId);
                    }
                }
            });

            // 清理失效的连接
            deadClients.forEach(emitters::remove);
        });
    }

    // 请求体类
    @Getter
    @Setter
    @NoArgsConstructor
    public static class MessageRequest {
        String clientId;
        String message;

        public MessageRequest(String message) {
            this.message = message;
        }
    }
}
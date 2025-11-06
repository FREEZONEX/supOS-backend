package com.supos.adpter.nodered.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EventflowAdapterService extends NodeRedAdapterService {

    public EventflowAdapterService() {
        super("eventflow", "1889");
    }
}

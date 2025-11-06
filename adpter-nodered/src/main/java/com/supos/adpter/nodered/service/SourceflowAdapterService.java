package com.supos.adpter.nodered.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SourceflowAdapterService extends NodeRedAdapterService {

    public SourceflowAdapterService() {
        super("nodered", "1880");
    }
}

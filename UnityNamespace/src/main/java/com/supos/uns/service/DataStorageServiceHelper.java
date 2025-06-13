package com.supos.uns.service;

import com.supos.common.adpater.DataStorageAdapter;
import com.supos.common.adpater.TimeSequenceDataStorageAdapter;
import lombok.Getter;

public class DataStorageServiceHelper {
    @Getter
    private final DataStorageAdapter relationDbEnabled;//启用的关系库
    @Getter
    private final TimeSequenceDataStorageAdapter sequenceDbEnabled;//启用的时序库

    public DataStorageServiceHelper(DataStorageAdapter relationDbEnabled, TimeSequenceDataStorageAdapter sequenceDbEnabled) {
        this.relationDbEnabled = relationDbEnabled;
        this.sequenceDbEnabled = sequenceDbEnabled;
    }

}

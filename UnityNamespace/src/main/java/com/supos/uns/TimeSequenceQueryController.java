package com.supos.uns;

import com.supos.common.adpater.historyquery.HistoryQueryParams;
import com.supos.common.adpater.historyquery.HistoryQueryResult;
import com.supos.uns.service.DataStorageServiceHelper;
import com.supos.uns.service.UnsDataService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

@RestController
public class TimeSequenceQueryController {

    private @Autowired DataStorageServiceHelper storageServiceHelper;
    @Autowired
    UnsDataService unsDataService;

    @PostMapping("/open-api/rest/sql")
    @ResponseBody
    @Hidden
    public String executeSql(@RequestParam(value = "tz", required = false) String tz, @RequestParam(value = "req_id", required = false) String reqId,
                             @RequestParam(value = "row_with_meta", required = false) String rowWithMeta, HttpEntity<String> requestEntity) throws Exception {
        return storageServiceHelper.getSequenceDbEnabled().execSQL(requestEntity.getBody());
    }


    @PostMapping("/inter-api/supos/rest/hist")
    @ResponseBody
    public HistoryQueryResult queryHistory(@RequestBody HistoryQueryParams params,
                                           @RequestParam(value = "setBlob", required = false) boolean setBlob) {
        HistoryQueryResult rs = storageServiceHelper.getSequenceDbEnabled().queryHistory(params);
        if (setBlob && !CollectionUtils.isEmpty(rs.getResults())) {
            unsDataService.filterBlobLBlobs(rs.getResults());
        }
        return rs;
    }

}

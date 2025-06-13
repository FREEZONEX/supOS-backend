package com.supos.uns.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.TimeSequenceDataStorageAdapter;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.JsonResult;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.PaginationDTO;
import com.supos.common.dto.grafana.DashboardDto;
import com.supos.common.dto.mock.MockDemoDTO;
import com.supos.common.dto.mock.MockWeatherDTO;
import com.supos.common.enums.IOTProtocol;
import com.supos.common.event.EventBus;
import com.supos.common.event.FlowInstallEvent;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.dao.mapper.ExampleMapper;
import com.supos.uns.dao.po.ExamplePo;
import com.supos.uns.util.FileUtils;
import com.supos.uns.vo.ModelDetail;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class ExampleService extends ServiceImpl<ExampleMapper, ExamplePo> {

    @Resource
    private DashboardService dashboardService;
    @Resource
    private UnsExcelService unsExcelService;
    @Resource
    private UnsLabelService unsLabelService;

    @Value("${node-red.host:nodered}")
    private String nodeRedHost;
    @Value("${node-red.port:1880}")
    private String nodeRedPort;

    JdbcTemplate jdbcTemplate;

    @Autowired
    private DataStorageServiceHelper storageServiceHelper;

    @Autowired
    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        DataSource dataSource = sqlSessionTemplate.getConfiguration().getEnvironment().getDataSource();
        jdbcTemplate = new JdbcTemplate(dataSource);
    }


    @Autowired
    UnsManagerService unsManagerService;

    @Autowired
    private UnsQueryService unsQueryService;
    @Autowired
    UnsRemoveService unsRemoveService;
    @Autowired
    UnsTemplateService unsTemplateService;
    @Resource
    private SystemConfig systemConfig;

    // 随机数生成器
    private static final Random RANDOM = new Random();


    public PageResultDTO<ExamplePo> pageList(PaginationDTO paginationDTO){
        Page<ExamplePo> page = new Page<>(paginationDTO.getPageNo(), paginationDTO.getPageSize());
        Page<ExamplePo> iPage = page(page);
        PageResultDTO.PageResultDTOBuilder<ExamplePo> pageBuilder = PageResultDTO.<ExamplePo>builder()
                .total(iPage.getTotal()).pageNo(paginationDTO.getPageNo()).pageSize(paginationDTO.getPageSize());
        iPage.getRecords().forEach(po ->{
            String code = po.getName();
            po.setName(I18nUtils.getMessage("uns.example.name." + code));
            po.setDescription(I18nUtils.getMessage("uns.example.description." + code));
        });
        return pageBuilder.code(0).data(iPage.getRecords()).build();
    }



    public ResultVO install(Long id){
        ExamplePo example = getById(id);
        if (null == example){
            return ResultVO.fail("example is not found");
        }

        if (1 != example.getStatus()){
            return ResultVO.fail("example is already installed");
        }

        try {
            String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
//            File zipFile = HttpUtil.downloadFileFromUrl(example.getPackagePath(),outFile);
            String demoName = example.getType() == 1 ? "ot.zip" : "it.zip";
            String lang = "zh-CN".equals(systemConfig.getLang()) ? "zh": "en";
            String packagePath = "/templates/example/" + lang + "/" + demoName;
            ClassPathResource resource = new ClassPathResource(packagePath);
            String targetPath = String.format("%s%s%s/%s", FileUtils.getFileRootPath(), Constants.EXAMPLE_ROOT, datePath,resource.getFilename());
            File zipFile = FileUtil.copyFile(resource.getInputStream(),new File(targetPath));
            if (!zipFile.exists()){
                return ResultVO.fail("example package is not found");
            }
            File zipPath = ZipUtil.unzip(zipFile);
            File[] packageFiles = zipPath.listFiles();
            if (ObjectUtil.isNull(packageFiles)){
                throw new BuzException(400,"package zip file is empty");
            }

            //UNS 实例导入
            File unsExcel = Arrays.stream(packageFiles).filter(file -> file.getName().endsWith(".xlsx")).findFirst().orElseThrow(() -> new BuzException("uns excel file not found"));
            unsExcelService.asyncImport(unsExcel,runningStatus -> {
                JSONObject json = JSON.parseObject(JsonUtil.toJson(runningStatus));
                log.info(">>>>>>>>>>>>>>>>example install excel import json:{}",json);
                if (json.getIntValue("code") == 500 ){
                    throw new BuzException(400,"example install failed. message:" + json.getString("msg"));
                }
            },false);

            // flow 导入
            String flowName = String.format("demo-%s-flows", FileUtil.getPrefix(demoName));
            EventBus.publishEvent(new FlowInstallEvent(this, flowName, FlowInstallEvent.INSTALL));

            SystemConfig systemConfig = SpringUtil.getBean(SystemConfig.class);
            if (null != systemConfig.getContainerMap().get("fuxa")){
                Arrays.stream(packageFiles).filter(file -> file.getName().startsWith(Constants.EXAMPLE_FUXA_FILE)).findFirst().ifPresent(file ->{
                    String json = FileUtil.readString(file,StandardCharsets.UTF_8);
                    DashboardDto dashboardDto = new DashboardDto();
                    dashboardDto.setType(2);
                    dashboardDto.setName(example.getName() + "-" + datePath);
                    String dashboardId = dashboardService.create(dashboardDto).getData().getId();
                    example.setDashboardType(2);
                    example.setDashboardId(dashboardId);
                    example.setDashboardName(dashboardDto.getName());
                    boolean fuxaCreated = createFuxaProject(json,dashboardId);
                    if (!fuxaCreated){
                        throw new BuzException(400,"fuxa project create failed.");
                    }
                });
            }

            //安装包元数据
//            Arrays.stream(packageFiles).filter(file -> file.getName().startsWith(Constants.EXAMPLE_METADATA)).findFirst().ifPresent(file ->{
//                String jsonStr = FileUtil.readString(file,StandardCharsets.UTF_8);
//                if (StringUtils.isNotBlank(jsonStr)){
//                    //TODO
//                }
//
//            });

            //协议文件
            Arrays.stream(packageFiles).filter(file -> file.getName().startsWith(Constants.EXAMPLE_PROTOCOL)).findFirst().ifPresent(file ->{
                String jsonStr = FileUtil.readString(file,StandardCharsets.UTF_8);
                if (StringUtils.isNotBlank(jsonStr)){
                    parserProtocolJson(jsonStr);
                }
            });
            example.setStatus(3);
            ThreadUtil.execute(() -> {
                ThreadUtil.sleep(15000);
                initDemoItData();
            });
            updateById(example);
        } catch (Exception e) {
            log.error("example install Exception",e);
            return ResultVO.fail("example install failed");
        }

        return ResultVO.success("ok");
    }


    public ResultVO uninstall(Long id){
        ExamplePo example = getById(id);
        if (null == example){
            return ResultVO.fail("example is not found");
        }
        String path = example.getType() == 1 ? "新能源光伏电站/" : "电气一厂/";
        if ("en-US".equals(System.getenv("SYS_OS_LANG"))){
            path = example.getType() == 1 ? "NewEnergyPVPowerStation/" : "ElectricalFactory1/";
            //TODO 订单 demo 约定 其他 id
//            unsTemplateService.deleteTemplate("a240fa27925a635b08dc28c9e4f9216d");//订单
//            unsTemplateService.deleteTemplate("eb951c9b55033e3207d4ec0bdf8c825b");//设备-光伏
        } else {//TODO 设备-光伏 demo 约定 其他 id
//            unsTemplateService.deleteTemplate("3b0efde0906bd41610c19a646902f132");//设备-光伏
//            unsTemplateService.deleteTemplate("4c117f2037181c74559db1829298e041");//订单
        }
        String folderAlias = null;
        if (example.getType() == 1) {
            folderAlias = "xinnengyuanguangfudi_6d499cc25b7349a684ae";
        } else {
            folderAlias = "dianqiyichang__a837e8f2f2c94ca091c4";
        }
        JsonResult<ModelDetail> modelDetailJson = unsQueryService.getModelDefinition(null, folderAlias);
        if (modelDetailJson.getData() != null &&  modelDetailJson.getData().getId() != null) {
            unsRemoveService.removeModelOrInstance(Long.valueOf(modelDetailJson.getData().getId()),true,true,true);
        }

        unsLabelService.deleteByName("modbus");
        unsLabelService.deleteByName("opcua");
        dashboardService.delete(example.getDashboardId());
        example.setStatus(1);
        updateById(example);


        // flow 删除
        String flowName = String.format("demo-%s-flows", FileUtil.getPrefix(new File(example.getPackagePath()).getName()));
        EventBus.publishEvent(new FlowInstallEvent(this, flowName, FlowInstallEvent.UNINSTALL));

        //fuxa 删除
        if (null != systemConfig.getContainerMap().get("fuxa")){
            String url = Constants.FUXA_API_URL + "/api/project/" + example.getDashboardId();
            HttpResponse response = HttpRequest.delete(url).execute();
            log.info(">>>>>>>>>>>>>>>dashboard fuxa delete response code:{}", response.getStatus());
        }
        return ResultVO.success("ok");
    }

    public ResultVO initDemoItData(){
        TimeSequenceDataStorageAdapter timeSequenceDataStorageAdapter = storageServiceHelper.getSequenceDbEnabled();
        SrcJdbcType srcJdbcType = timeSequenceDataStorageAdapter.getJdbcType();
        List<String> tableNames = new ArrayList<>();
        JdbcTemplate jdbcTemplate1 = null;
        if (srcJdbcType == SrcJdbcType.TdEngine) {
            jdbcTemplate1 = timeSequenceDataStorageAdapter.getJdbcTemplate();
            String sql = "show public.tables like '%_guang%'";
            tableNames.addAll(jdbcTemplate1.queryForList(sql, String.class));

        } else if (srcJdbcType == SrcJdbcType.TimeScaleDB) {
            jdbcTemplate1 = timeSequenceDataStorageAdapter.getJdbcTemplate();
            String sql = "SELECT tablename FROM pg_tables WHERE schemaname = 'public' and tablename like '%_guang%'";
            tableNames.addAll(jdbcTemplate1.queryForList(sql,String.class));
        }

        if (CollectionUtils.isNotEmpty(tableNames)){
            for (String tableName : tableNames) {
                try {
                    MockDemoDTO demoData = mockDemoData();
                    Object[] params = demoData.convertOrderToParams(srcJdbcType);
                    String insertSql = "INSERT INTO public.\"" + tableName + "\"  (\""+ Constants.SYS_FIELD_CREATE_TIME +"\", \"id\", \"name\",\"installedCapacity\",\"dailyPowerGeneration\",\"owner\") VALUES (?, ?, ?, ?, ?, ?)";
                    int c = jdbcTemplate1.update(insertSql,params);
                    log.info(">>>>>>>>>>>>>>>>>初始化Demo表数据 表：{}，是否成功：{}",tableName,c > 0);
                } catch (Exception e) {
                    log.error(">>>>>>>>>>>>>>>>>初始化Demo表数据 表：{}",tableName,e);
                }
            }
        }
        return ResultVO.successWithData(tableNames);
    }

    private boolean createFuxaProject(String json,String dashboardId){
        String url = Constants.FUXA_API_URL + "/api/project";
        JSONObject params = new JSONObject();
        params.put("projectData",json);
        params.put("layoutId",dashboardId);
        params.put("isClear",true);
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpRequest.post(url).body(params.toJSONString())
                    .setConnectionTimeout(30000)
                    .setReadTimeout(30000)
                    .execute();
        } catch (Exception e) {
            log.error("createFuxaProject Exception",e);
            return false;
        }
        log.warn(">>>>>>>>>>>调用创建fuxa工程，httpCode:{} body:{}",httpResponse.getStatus(),httpResponse.body());
        return 200 == httpResponse.getStatus();
    }

    public MockDemoDTO mockDemoData(){
        MockDemoDTO demo = new MockDemoDTO();
        return demo.mockDemoData();
    }


    public ResultVO<List<MockWeatherDTO>> mockRestApiData(){
        MockWeatherDTO bj = new MockWeatherDTO();
        bj.setCity("北京");
        bj.setTemperature(generateTemperature());
        bj.setHumidity(generateHumidity());

        MockWeatherDTO sh = new MockWeatherDTO();
        sh.setCity("上海");
        sh.setTemperature(generateTemperature());
        sh.setHumidity(generateHumidity());

        MockWeatherDTO hz = new MockWeatherDTO();
        hz.setCity("杭州");
        hz.setTemperature(generateTemperature());
        hz.setHumidity(generateHumidity());

        return ResultVO.successWithData(Arrays.asList(bj,sh,hz));
    }

    /**
     * 解析协议文件并模拟数据
     * @param protocolJson
     */
    private void parserProtocolJson(String protocolJson){
        JSONArray protocolArray = JSONArray.parseArray(protocolJson);
        for (int i = 0; i < protocolArray.size(); i++) {
            JSONObject protocol = protocolArray.getJSONObject(i);
            mockProtocolData(protocol);
        }
    }

    private void mockProtocolData(JSONObject protocol){
        String protocolType = protocol.getString("protocol");
        IOTProtocol iotProtocol = IOTProtocol.getByName(protocolType);
        switch (iotProtocol){
            case OPC_UA:
                String opcuaUrl = String.format("http://%s:%s/%s", nodeRedHost, nodeRedPort,"mock/opcua");
                log.info(">>>>>>请求opcua mock url:{},params:{}",opcuaUrl,protocol);
                HttpResponse response = HttpRequest.post(opcuaUrl).body(protocol.toJSONString()).execute();
                log.info(">>>>>>opcua mock 结果:{},{}",response.getStatus(),response.body());
                if (response.getStatus()!=200){
                    throw new BuzException(400,"opcua模拟数据失败:" + response.body());
                }
                break;
            case MODBUS:
                break;
            case MQTT:
                break;
            case REST:
                break;
            default:
                break;
        }
    }

    private static double generateTemperature() {
        double temp = -10 + (40 - (-10)) * RANDOM.nextDouble(); // 温度范围：-10到40
        return Math.round(temp * 100.0) / 100.0; // 保留两位小数
    }

    // 随机生成湿度（例如：30%到90%之间）
    private static double generateHumidity() {
        double humidity = 30 + (90 - 30) * RANDOM.nextDouble(); // 湿度范围：30%到90%
        return Math.round(humidity * 100.0) / 100.0; // 保留两位小数
    }


}

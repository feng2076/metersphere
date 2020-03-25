package io.metersphere.engine;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.metersphere.base.domain.FileMetadata;
import io.metersphere.base.domain.LoadTestWithBLOBs;
import io.metersphere.base.domain.TestResource;
import io.metersphere.base.domain.TestResourcePool;
import io.metersphere.commons.constants.FileType;
import io.metersphere.commons.constants.ResourcePoolTypeEnum;
import io.metersphere.commons.constants.TestStatus;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.CommonBeanFactory;
import io.metersphere.i18n.Translator;
import io.metersphere.service.FileService;
import io.metersphere.service.LoadTestService;
import io.metersphere.service.TestResourcePoolService;
import io.metersphere.service.TestResourceService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractEngine implements Engine {
    protected FileMetadata jmxFile;
    protected List<FileMetadata> csvFiles;
    protected LoadTestWithBLOBs loadTest;
    protected LoadTestService loadTestService;
    protected Integer threadNum;
    protected List<TestResource> resourceList;

    private TestResourcePoolService testResourcePoolService;
    private TestResourceService testResourceService;
    private FileService fileService;

    public AbstractEngine() {
        testResourcePoolService = CommonBeanFactory.getBean(TestResourcePoolService.class);
        testResourceService = CommonBeanFactory.getBean(TestResourceService.class);
        fileService = CommonBeanFactory.getBean(FileService.class);
    }

    @Override
    public boolean init(LoadTestWithBLOBs loadTest) {
        if (loadTest == null) {
            MSException.throwException("LoadTest is null.");
        }
        this.loadTest = loadTest;

        final List<FileMetadata> fileMetadataList = fileService.getFileMetadataByTestId(loadTest.getId());
        if (org.springframework.util.CollectionUtils.isEmpty(fileMetadataList)) {
            MSException.throwException(Translator.get("run_load_test_file_not_found") + loadTest.getId());
        }
        jmxFile = fileMetadataList.stream().filter(f -> StringUtils.equalsIgnoreCase(f.getType(), FileType.JMX.name()))
                .findFirst().orElseGet(() -> {
                    throw new RuntimeException(Translator.get("run_load_test_file_not_found") + loadTest.getId());
                });

        csvFiles = fileMetadataList.stream().filter(f -> StringUtils.equalsIgnoreCase(f.getType(), FileType.CSV.name())).collect(Collectors.toList());


        this.loadTestService = CommonBeanFactory.getBean(LoadTestService.class);

        threadNum = getThreadNum(loadTest);
        String resourcePoolId = loadTest.getTestResourcePoolId();
        if (StringUtils.isBlank(resourcePoolId)) {
            MSException.throwException("Resource Pool ID is empty");
        }
        TestResourcePool resourcePool = testResourcePoolService.getResourcePool(resourcePoolId);
        if (resourcePool == null) {
            MSException.throwException("Resource Pool is empty");
        }
        if (!ResourcePoolTypeEnum.K8S.name().equals(resourcePool.getType())) {
            MSException.throwException("Invalid Resource Pool type.");
        }
        this.resourceList = testResourceService.getResourcesByPoolId(resourcePool.getId());
        if (CollectionUtils.isEmpty(this.resourceList)) {
            MSException.throwException("Test Resource is empty");
        }
        return true;
    }

    protected Integer getRunningThreadNum() {
        List<LoadTestWithBLOBs> loadTests = loadTestService.selectByTestResourcePoolId(loadTest.getTestResourcePoolId());
        // 使用当前资源池正在运行的测试占用的并发数
        return loadTests.stream()
                .filter(t -> TestStatus.Running.name().equals(t.getStatus()))
                .map(this::getThreadNum)
                .reduce(Integer::sum)
                .orElse(0);
    }

    private Integer getThreadNum(LoadTestWithBLOBs t) {
        Integer s = 0;
        String loadConfiguration = t.getLoadConfiguration();
        JSONArray jsonArray = JSON.parseArray(loadConfiguration);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject o = jsonArray.getJSONObject(i);
            if (StringUtils.equals(o.getString("key"), "TargetLevel")) {
                s = o.getInteger("value");
            }
        }
        return s;
    }
}

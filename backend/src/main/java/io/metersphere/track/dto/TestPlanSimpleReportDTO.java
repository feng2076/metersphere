package io.metersphere.track.dto;

import io.metersphere.api.dto.automation.TestPlanFailureApiDTO;
import io.metersphere.api.dto.automation.TestPlanFailureScenarioDTO;
import io.metersphere.base.domain.IssuesDao;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class TestPlanSimpleReportDTO {
    private String name;
    private Long startTime;
    private Long endTime;
    private int caseCount;
    private int executeCount;
    private int passCount;
    private double executeRate;
    private double passRate;
    private String summary;
    private String config;
    private Boolean isThirdPartIssue;
    private TestPlanFunctionResultReportDTO functionResult;
    private TestPlanApiResultReportDTO apiResult;
    private TestPlanLoadResultReportDTO loadResult;

    List<TestPlanCaseDTO> functionFailureCases;
    List<TestPlanCaseDTO> functionAllCases;
    List<IssuesDao> issueList;
    List<TestPlanFailureApiDTO> apiFailureResult;
    List<TestPlanFailureApiDTO> apiAllCases;
    List<TestPlanFailureScenarioDTO> scenarioFailureResult;
    List<TestPlanFailureScenarioDTO> scenarioAllCases;
    List<TestPlanLoadCaseDTO> loadAllTestCases;
    List<TestPlanLoadCaseDTO> loadFailureTestCases;
}

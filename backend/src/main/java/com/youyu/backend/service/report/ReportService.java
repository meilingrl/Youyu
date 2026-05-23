package com.youyu.backend.service.report;

import java.util.Map;

public interface ReportService {

    Map<String, Object> moduleInfo();

    Map<String, Object> submitReport(Long reporterUserId, Map<String, Object> command);
}

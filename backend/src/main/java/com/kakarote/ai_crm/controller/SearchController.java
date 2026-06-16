package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.GlobalSearchQueryBO;
import com.kakarote.ai_crm.entity.VO.GlobalSearchResultVO;
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@Tag(name = "Global Search")
public class SearchController {

    @Autowired
    private IGlobalSearchIndexService globalSearchIndexService;

    /**
     * 处理globalSearch方法逻辑。
     */
    @PostMapping("/global")
    @Operation(summary = "Global search across CRM modules")
    public Result<BasePage<GlobalSearchResultVO>> globalSearch(@RequestBody GlobalSearchQueryBO queryBO) {
        return Result.ok(globalSearchIndexService.queryPageList(queryBO));
    }
}

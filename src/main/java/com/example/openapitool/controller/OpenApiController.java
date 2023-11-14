package com.example.openapitool.controller;

import com.example.openapitool.constant.ContentType;
import com.example.openapitool.constant.TemplateType;
import com.example.openapitool.model.RefOpenApiDTO;
import com.example.openapitool.service.SheetService;
import com.example.openapitool.util.OkHttpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
@Tag(name = "open api")
@RestController
@RequestMapping("api/open-api/excel")
public class OpenApiController {

    private final SheetService sheetService;

    @SneakyThrows
    @Operation(summary = "Convert Open Api To Excel (file)")
    @PostMapping(value = "file" ,name = "convert open api file to excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void convertOpenApiToExcel(
        @Parameter(description = "Template Type")
        @RequestParam(required = false) TemplateType templateType,
        @Parameter(description = "Open Api Spec (support: json, yml)")
        @RequestPart MultipartFile openApi,
        HttpServletResponse response) {
        response.setContentType(ContentType.XLSX);
        response.setHeader("Content-Disposition", "attachment;filename=export.xlsx");
        sheetService.convertOpenApiToSheet(templateType, openApi.getBytes(), response.getOutputStream());
    }

    @SneakyThrows
    @Operation(summary = "Convert Open Api To Excel (url)")
    @PostMapping(value = "url" ,name = "convert open api url to excel")
    public void convertOpenApiToExcel(
        @Parameter(description = "Template Type")
        @RequestParam(required = false) TemplateType templateType,
        @Parameter(description = "Open Api Spec (support: json, yml)")
        @RequestBody RefOpenApiDTO refOpenApiDTO,
        HttpServletResponse response) {
        var content = OkHttpUtils.getRequest(refOpenApiDTO.getUrl());
        response.setContentType(ContentType.XLSX);
        response.setHeader("Content-Disposition", "attachment;filename=export.xlsx");
        sheetService.convertOpenApiToSheet(templateType, content, response.getOutputStream());
    }

}
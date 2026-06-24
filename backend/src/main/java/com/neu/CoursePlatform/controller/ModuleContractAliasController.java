package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.KnowledgeExtractionCandidate;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Exact aliases from the shared specification; legacy routes remain compatible. */
@RestController
@RequestMapping("/api")
public class ModuleContractAliasController {

    private final CourseResourceController resourceController;
    private final KnowledgeExtractionController extractionController;
    private final AbilityMapController abilityMapController;

    public ModuleContractAliasController(CourseResourceController resourceController,
                                         KnowledgeExtractionController extractionController,
                                         AbilityMapController abilityMapController) {
        this.resourceController = resourceController;
        this.extractionController = extractionController;
        this.abilityMapController = abilityMapController;
    }

    @PostMapping("/courses/{courseCode}/resources")
    public Result<String> uploadResource(@PathVariable String courseCode,
                                         @RequestParam(required = false) String title,
                                         @RequestParam(required = false) String chapter,
                                         @RequestParam(required = false) String knowledgePointId,
                                         @RequestParam(required = false) String resourceType,
                                         @RequestParam MultipartFile file,
                                         HttpSession session) {
        return resourceController.upload(courseCode, title, chapter, knowledgePointId, resourceType, file, session);
    }

    @PostMapping("/courses/{courseCode}/knowledge-points/extract")
    public Result<List<KnowledgeExtractionCandidate>> extractKnowledgePoints(@PathVariable String courseCode,
                                                                               @RequestParam String resourceId,
                                                                               HttpSession session) {
        return extractionController.extract(courseCode, resourceId, session);
    }

    @PutMapping("/ability-points/{abilityPointId}")
    public Result<Void> updateAbilityPoint(@PathVariable String abilityPointId,
                                           @RequestBody AbilityPoint request,
                                           HttpSession session) {
        return abilityMapController.update(abilityPointId, request, session);
    }
}

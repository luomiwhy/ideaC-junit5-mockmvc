package indi.luo.idea.plugin.junit5mockmvc.constant;

import java.util.Set;

public interface WebAnnotation {

    String Controller = "Controller";
    String RequestMapping = "@RequestMapping";
    String RequestParam = "@RequestParam";
    String GetMapping = "@GetMapping";
    String PutMapping = "@PutMapping";
    String DeleteMapping = "@DeleteMapping";
    String PatchMapping = "@PatchMapping";
    String RequestBody = "@RequestBody";
    String requestBodyFull = "org.springframework.web.bind.annotation.RequestBody";
    String ResponseBody = "@ResponseBody";
    String PathVariable = "@PathVariable";

    Set<String> mappingSet = Set.of("org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.GetMapping","org.springframework.web.bind.annotation.RequestMapping"
            );
}

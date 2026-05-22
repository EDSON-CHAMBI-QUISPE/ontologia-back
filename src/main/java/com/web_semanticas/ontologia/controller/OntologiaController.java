package com.web_semanticas.ontologia.controller;

import com.web_semanticas.ontologia.dto.QueryRequest;
import com.web_semanticas.ontologia.service.FusekiService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class OntologiaController {

    private final FusekiService service;

    public OntologiaController(FusekiService service) {
        this.service = service;
    }

    @PostMapping("/query")
    public List<String> ejecutarQuery(
            @RequestBody QueryRequest request
    ) {

        return service.ejecutarConsulta(request.getSparql());
    }
}
package com.web_semanticas.ontologia.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.web_semanticas.ontologia.dto.QueryRequest;
import com.web_semanticas.ontologia.service.FusekiService;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class OntologiaController {

    private final FusekiService service;

    public OntologiaController(FusekiService service) {
        this.service = service;
    }

    @PostMapping("/query")
    public List<Map<String, String>> ejecutarQuery(@RequestBody QueryRequest request) {
        String modo = request.getModo() != null ? request.getModo().toUpperCase() : "FUSEKI";

        switch (modo) {
            case "DBPEDIA_ONLINE":
                return service.ejecutarConsultaDBpediaOnline(request.getSparql());
                
            case "DBPEDIA_OFFLINE":
                return service.ejecutarConsultaDBpediaOffline(request.getSparql());
                
            case "FUSEKI":
            default:
                return service.ejecutarConsulta(request.getSparql());
        }
    }
}
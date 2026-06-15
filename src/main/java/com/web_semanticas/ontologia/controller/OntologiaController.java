package com.web_semanticas.ontologia.controller;

import java.util.Collections;
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

    // --- NUEVO ENDPOINT PARA DISPARAR EL ENLAZAMIENTO MASIVO SEGURO ---
    @PostMapping("/enlazar")
public Map<String, String> ejecutarEnlazadoMasivo(@RequestBody Map<String, String> params) {
    String claseLocal = params.get("claseLocal");
    String claseDbpedia = params.get("claseDbpedia"); // Requerido si es offline para validación semántica
    String tipoMapeo = params.getOrDefault("tipoMapeo", "ONLINE").toUpperCase(); 

    if (claseLocal == null) {
        return Collections.singletonMap("resultado", "Error: Falta el campo requerido 'claseLocal'.");
    }

    String mensajeResultado;
    if ("OFFLINE".equals(tipoMapeo)) {
        mensajeResultado = service.autoEnlazarMasivoOffline(claseLocal, claseDbpedia);
    } else {
        mensajeResultado = service.autoEnlazarMasivoSinDuplicados(claseLocal);
    }
    
    return Collections.singletonMap("resultado", mensajeResultado);
}

@PostMapping("/importar-series")
public Map<String,String> importarSeries(
        @RequestBody Map<String,Integer> body) {

    int limite =
            body.getOrDefault("limite",100);

    String resultado =
            service.poblarTodoDBpedia(limite);

    return Collections.singletonMap(
            "resultado",
            resultado
    );
}


}
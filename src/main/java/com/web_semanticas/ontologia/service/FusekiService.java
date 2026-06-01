package com.web_semanticas.ontologia.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.springframework.stereotype.Service;

@Service
public class FusekiService {

    private static final String FUSEKI_URL = "http://localhost:3030/series/query";
    private static final String DBPEDIA_ENDPOINT_URL = "https://dbpedia.org/sparql";

    // Helper para transformar una fila de SPARQL (QuerySolution) en un Mapa JSON estructurado
    private Map<String, String> transformarFilaAMapa(QuerySolution solucion) {
        Map<String, String> filaMapa = new HashMap<>();
        Iterator<String> nombresVariables = solucion.varNames();
        
        while (nombresVariables.hasNext()) {
            String variable = nombresVariables.next();
            RDFNode nodo = solucion.get(variable);
            if (nodo != null) {
                if (nodo.isLiteral()) {
                    filaMapa.put(variable, nodo.asLiteral().getLexicalForm());
                } else if (nodo.isResource()) {
                    // Si es una URI, extraemos solo el fragmento final legible para la UI si es posible
                    String uri = nodo.asResource().getURI();
                    if (uri.contains("#")) {
                        filaMapa.put(variable, uri.substring(uri.lastIndexOf("#") + 1));
                    } else if (uri.contains("/resource/")) {
                        filaMapa.put(variable, uri.substring(uri.lastIndexOf("/") + 1).replace("_", " "));
                    } else {
                        filaMapa.put(variable, uri);
                    }
                }
            }
        }
        return filaMapa;
    }

    // --- 1. MODO FUSEKI LOCAL (Limpio, sin conexiones externas ni enlaces ontológicos) ---
    public List<Map<String, String>> ejecutarConsulta(String sparql) {
        List<Map<String, String>> resultados = new ArrayList<>();
        Query query = QueryFactory.create(sparql);

        try (QueryExecution qexec = QueryExecutionHTTP.service(FUSEKI_URL).query(query).build()) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                resultados.add(transformarFilaAMapa(rs.next()));
            }
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Error en Fuseki Local: " + e.getMessage());
            resultados.add(errorMap);
        }
        return resultados;
    }

    // --- 2. MODO ONLINE: CONSULTA DIRECTA A DBPEDIA INTERNET ---
    public List<Map<String, String>> ejecutarConsultaDBpediaOnline(String sparql) {
        List<Map<String, String>> resultados = new ArrayList<>();
        Query query = QueryFactory.create(sparql);

        try (QueryExecution qexec = QueryExecutionHTTP.service(DBPEDIA_ENDPOINT_URL).query(query).build()) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                resultados.add(transformarFilaAMapa(rs.next()));
            }
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Error al conectar con DBpedia Online: " + e.getMessage());
            resultados.add(errorMap);
        }
        return resultados;
    }

    // --- 3. MODO OFFLINE: CONSULTA A ARCHIVO LOCAL EN MEMORIA ---
    public List<Map<String, String>> ejecutarConsultaDBpediaOffline(String sparql) {
        List<Map<String, String>> resultados = new ArrayList<>();
        Query query = QueryFactory.create(sparql);

        try {
            Model model = ModelFactory.createDefaultModel();
            InputStream in = getClass().getClassLoader().getResourceAsStream("dbpedia_subconjunto.rdf");                
            if (in == null) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("error", "No se encontró el archivo 'dbpedia_subconjunto.rdf' en resources.");
                resultados.add(errorMap);
                return resultados;
            }
            
            model.read(in, null, "RDF/XML");

            try (QueryExecution qexec = QueryExecution.create(query, model)) {
                ResultSet rs = qexec.execSelect();
                while (rs.hasNext()) {
                    resultados.add(transformarFilaAMapa(rs.next()));
                }
            }
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Error en consulta Offline: " + e.getMessage());
            resultados.add(errorMap);
        }
        return resultados;
    }
}
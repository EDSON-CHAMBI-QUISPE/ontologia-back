package com.web_semanticas.ontologia.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.springframework.stereotype.Service;

@Service
public class FusekiService {

    private static final String FUSEKI_URL = "http://localhost:3030/series/query";
    
    // Endpoint público oficial de DBpedia en internet
    private static final String DBPEDIA_ENDPOINT_URL = "https://dbpedia.org/sparql";

    private static final String PREFIX = """
        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        PREFIX ont: <http://www.semanticweb.org/dell/ontologies/2026/2#>
        """;

    // --- 1. TU CONSULTA A FUSEKI LOCAL (YA EXISTENTE) ---
    public List<String> ejecutarConsulta(String sparql) {
        List<String> resultados = new ArrayList<>();
        Query query = QueryFactory.create(sparql);

        try (QueryExecution qexec = QueryExecutionHTTP.service(FUSEKI_URL).query(query).build()) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                resultados.add(rs.next().toString());
            }
        }
        return resultados;
    }

    // --- 2. MODO ONLINE: CONSULTA DIRECTA A DBPEDIA ---
    public List<String> ejecutarConsultaDBpediaOnline(String sparql) {
        List<String> resultados = new ArrayList<>();
        Query query = QueryFactory.create(sparql);

        // Apuntamos el QueryExecutionHTTP directamente al servidor remoto de DBpedia
        try (QueryExecution qexec = QueryExecutionHTTP.service(DBPEDIA_ENDPOINT_URL).query(query).build()) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                resultados.add(rs.next().toString());
            }
        } catch (Exception e) {
            resultados.add("Error al conectar con DBpedia Online: " + e.getMessage());
        }
        return resultados;
    }

    // --- 3. MODO OFFLINE: CONSULTA A ARCHIVO LOCAL ---
    public List<String> ejecutarConsultaDBpediaOffline(String sparql) {
        List<String> resultados = new ArrayList<>();
        Query query = QueryFactory.create(sparql);

        try {
            // Creamos un modelo vacío en memoria
            Model model = ModelFactory.createDefaultModel();
            
            // Leemos el archivo RDF/Turtle desde la carpeta resources
            InputStream in = getClass().getClassLoader().getResourceAsStream("dbpedia_subconjunto.ttl");
            
            if (in == null) {
                resultados.add("Error: No se encontró el archivo 'dbpedia_subconjunto.ttl' en resources.");
                return resultados;
            }
            
            // Cargamos los datos en el modelo (cambiar "TTL" por "RDF/XML" si tu archivo es .rdf)
            model.read(in, null, "TTL");

            // Ejecutamos la query directamente sobre el modelo en memoria (Local)
            try (QueryExecution qexec = QueryExecution.create(query, model)) {
                ResultSet rs = qexec.execSelect();
                while (rs.hasNext()) {
                    resultados.add(rs.next().toString());
                }
            }
        } catch (Exception e) {
            resultados.add("Error en consulta Offline: " + e.getMessage());
        }
        return resultados;
    }

    // Tu método obtenerDirectores() permanece igual abajo...
}
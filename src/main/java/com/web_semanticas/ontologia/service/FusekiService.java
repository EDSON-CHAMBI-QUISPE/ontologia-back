package com.web_semanticas.ontologia.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.springframework.stereotype.Service;

@Service
public class FusekiService {

    private static final String FUSEKI_URL =
            "http://localhost:3030/series/query";

    private static final String PREFIX = """
        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        PREFIX ont: <http://www.semanticweb.org/dell/ontologies/2026/2#>
        """;

    public List<String> obtenerDirectores() {

        String sparql = PREFIX + """
            SELECT ?nombreDirector ?tituloEpisodio
            WHERE {

              ?episodio ont:episodioDirigidoPor ?director .

              ?episodio ont:nombreEpisodio ?tituloEpisodio .

              ?director ont:nombreDirector ?nombreDirector .
              ?director ont:reconocido ?esRenombrado .

              FILTER (STR(?esRenombrado) = "si")
            }
            """;

        List<String> resultados = new ArrayList<>();

        Query query = QueryFactory.create(sparql);

        try (QueryExecution qexec =
                     QueryExecutionHTTP.service(FUSEKI_URL)
                             .query(query)
                             .build()) {

            ResultSet rs = qexec.execSelect();

            while (rs.hasNext()) {
                resultados.add(rs.next().toString());
            }
        }

        return resultados;
    }

    public List<String> ejecutarConsulta(String sparql) {

    List<String> resultados = new ArrayList<>();

    Query query = QueryFactory.create(sparql);

    try (QueryExecution qexec =
                 QueryExecutionHTTP.service(FUSEKI_URL)
                         .query(query)
                         .build()) {

        ResultSet rs = qexec.execSelect();

        while (rs.hasNext()) {
            resultados.add(rs.next().toString());
        }
    }

    return resultados;
}
}
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
import org.apache.jena.update.UpdateExecution;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.springframework.stereotype.Service;

@Service
public class FusekiService {

    // Cambiado al nuevo dataset seguro para no alterar tus datos originales
    private static final String FUSEKI_QUERY_URL = "http://localhost:3030/series_2/query";
    private static final String FUSEKI_UPDATE_URL = "http://localhost:3030/series_2/update";
    private static final String DBPEDIA_ENDPOINT_URL = "https://dbpedia.org/sparql";

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

    // --- 1. MODO FUSEKI LOCAL ---
    public List<Map<String, String>> ejecutarConsulta(String sparql) {
        List<Map<String, String>> resultados = new ArrayList<>();
        Query query = QueryFactory.create(sparql);

        try (QueryExecution qexec = QueryExecutionHTTP.service(FUSEKI_QUERY_URL).query(query).build()) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                resultados.add(transformarFilaAMapa(rs.next()));
            }
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Error en Fuseki Local (series_2): " + e.getMessage());
            resultados.add(errorMap);
        }
        return resultados;
    }

    // --- 2. MODO ONLINE ---
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

    // --- 3. MODO OFFLINE ---
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

    // --- SCRIPT DIRECTO: ALGORITMO GENÉRICO DE ENLAZADO AUTOMÁTICO ---
    // --- SCRIPT CORREGIDO PARA ADMITIR CARACTERES ESPECIALES COMO '/' ---
// --- SCRIPT DE INSERCIÓN MASIVA SEMÁNTICA DIRECTA Y SEGURA ---
public String autoEnlazarMasivoSinDuplicados(String claseLocal) {
    // Si la clase local contiene caracteres especiales (como /), la encerramos en < >
    String targetClaseLocal = "ontologies:" + claseLocal;
    if (claseLocal.contains("/")) {
        targetClaseLocal = "<http://www.semanticweb.org/dell/ontologies/2026/2#" + claseLocal + ">";
    }

    // Esta consulta única en la base de datos asocia tu URI local con DBpedia 
    // extrayendo de forma segura el identificador real del recurso (ej: Breaking_Bad)
    String updateQuery = 
        "PREFIX ontologies: <http://www.semanticweb.org/dell/ontologies/2026/2#>\n" +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
        "INSERT {\n" +
        "  ?individuoLocal owl:sameAs ?dbpediaUri .\n" +
        "}\n" +
        "WHERE {\n" +
        "  # 1. Selecciona tus individuos locales de la clase indicada\n" +
        "  ?individuoLocal rdf:type " + targetClaseLocal + " .\n" +
        "  \n" +
        "  # 2. SEGURO: Filtra y descarta los individuos que ya tengan un enlace owl:sameAs\n" +
        "  FILTER NOT EXISTS { ?individuoLocal owl:sameAs ?cualquiera }\n" +
        "  \n" +
        "  # 3. Extrae de forma limpia el nombre identificador final de tu recurso local\n" +
        "  BIND(STRAFTER(STR(?individuoLocal), \"#\") AS ?nombreRecurso)\n" +
        "  \n" +
        "  # 4. Construye dinámicamente la URI estructural oficial de DBpedia\n" +
        "  BIND(URI(CONCAT(\"http://dbpedia.org/resource/\", ?nombreRecurso)) AS ?dbpediaUri)\n" +
        "}";

    try {
        org.apache.jena.update.UpdateExecution.service(FUSEKI_UPDATE_URL)
            .update(org.apache.jena.update.UpdateFactory.create(updateQuery))
            .execute();
            
        return "Inserción masiva completada con éxito para la clase: " + claseLocal + " en series_2 (Omitiendo duplicados).";
    } catch (Exception e) {
        return "Error durante la inserción masiva de " + claseLocal + ": " + e.getMessage();
    }
}

private void ejecutarSparqlUpdate(String uriLocal, String uriDbpedia) {
        String updateQuery = 
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "INSERT DATA {\n" +
            "  <" + uriLocal + "> owl:sameAs <" + uriDbpedia + "> .\n" +
            "}";
            
        UpdateExecution.service(FUSEKI_UPDATE_URL)
            .update(UpdateFactory.create(updateQuery))
            .execute();
    }

    // --- SCRIPT DE ENLAZAMIENTO MASIVO USANDO EL ARCHIVO LOCAL ENRIQUECIDO (OFFLINE) ---
// --- SCRIPT DE ENLAZAMIENTO OFFLINE CORREGIDO Y OPTIMIZADO ---
// --- SCRIPT OFFLINE TOTALMENTE CORREGIDO Y ROBUSTO ---
public String autoEnlazarMasivoOffline(String claseLocal, String claseDbpedia) {
    int contadorEnlazados = 0;
    
    // 1. Cargar tu subconjunto de DBpedia local en memoria
    Model dbpediaLocalModel = ModelFactory.createDefaultModel();
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("dbpedia_subconjunto.rdf")) {
        if (in == null) {
            return "Error: No se encontró el archivo 'dbpedia_subconjunto.rdf' en resources.";
        }
        dbpediaLocalModel.read(in, null, "RDF/XML");
    } catch (Exception e) {
        return "Error al leer el archivo RDF local: " + e.getMessage();
    }

    // 2. Obtener tus individuos de Fuseki series_2 que no tengan owl:sameAs
    String targetClaseLocal = claseLocal.contains("/") ? 
        "<http://www.semanticweb.org/dell/ontologies/2026/2#" + claseLocal + ">" : "ontologies:" + claseLocal;

    String consultaLocales = 
        "PREFIX ontologies: <http://www.semanticweb.org/dell/ontologies/2026/2#>\n" +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
        "SELECT ?individuoLocal \n" +
        "WHERE {\n" +
        "  ?individuoLocal rdf:type " + targetClaseLocal + " .\n" +
        "  FILTER NOT EXISTS { ?individuoLocal owl:sameAs ?cualquiera }\n" +
        "}";

    Query queryLocal = QueryFactory.create(consultaLocales);
    
    try (QueryExecution qexecLocal = QueryExecutionHTTP.service(FUSEKI_QUERY_URL).query(queryLocal).build()) {
        ResultSet rsLocal = qexecLocal.execSelect();
        
        while (rsLocal.hasNext()) {
            QuerySolution solLocal = rsLocal.next();
            String uriLocal = solLocal.getResource("individuoLocal").getURI();
            
            // Extraemos el fragmento final (ej: "Breaking_Bad" o "Breaking Bad")
            String nombreRecurso = uriLocal.substring(uriLocal.lastIndexOf("#") + 1);
            
            // Formateamos al estilo estándar de DBpedia (Primera letra mayúscula, espacios por guiones bajos)
            String nombreNormalizado = nombreRecurso.trim().replace(" ", "_");
            String posibleUriDbpedia = "http://dbpedia.org/resource/" + nombreNormalizado;

            // 3. Crear el recurso Jena para validar contra el archivo en memoria
            org.apache.jena.rdf.model.Resource recursoDbpedia = dbpediaLocalModel.getResource(posibleUriDbpedia);
            
            // Verificamos si existe alguna propiedad relacionada a este recurso en el archivo descargado
            if (dbpediaLocalModel.contains(recursoDbpedia, null, (org.apache.jena.rdf.model.RDFNode) null)) {
                ejecutarSparqlUpdate(uriLocal, posibleUriDbpedia);
                contadorEnlazados++;
            } else {
                // Alternativa secundaria: buscar si coincide ignorando estrictamente mayúsculas/minúsculas
                boolean encontradoProcesado = false;
                org.apache.jena.rdf.model.ResIterator it = dbpediaLocalModel.listSubjects();
                while(it.hasNext()) {
                    org.apache.jena.rdf.model.Resource sujeto = it.nextResource();
                    if(sujeto.getURI() != null && sujeto.getURI().equalsIgnoreCase(posibleUriDbpedia)) {
                        ejecutarSparqlUpdate(uriLocal, sujeto.getURI());
                        contadorEnlazados++;
                        encontradoProcesado = true;
                        break;
                    }
                }
            }
        }
        return "Enlazado Offline completado para " + claseLocal + ". Se generaron con éxito " + contadorEnlazados + " enlaces usando el archivo local.";
    } catch (Exception e) {
        return "Error durante el enlazamiento masivo offline: " + e.getMessage();
    }
}

public String importarSeriesDesdeDBpedia(int limite) {

    String consultaDBpedia = """
        PREFIX dbo: <http://dbpedia.org/ontology/>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

        SELECT DISTINCT
            ?serie
            ?label
            ?abstract
            ?episodes
            ?seasons
        WHERE {

            ?serie a dbo:TelevisionShow .

            OPTIONAL { ?serie dbo:numberOfEpisodes ?episodes }
            OPTIONAL { ?serie dbo:numberOfSeasons ?seasons }
            OPTIONAL { ?serie dbo:abstract ?abstract }

            ?serie rdfs:label ?label .

            FILTER(lang(?label)="en")

            OPTIONAL {
                FILTER(lang(?abstract)="en")
            }
        }
        LIMIT """ + limite;

    int insertadas = 0;

    try {

        Query query = QueryFactory.create(consultaDBpedia);

        try (QueryExecution qexec =
                QueryExecutionHTTP.service(DBPEDIA_ENDPOINT_URL)
                        .query(query)
                        .build()) {

            ResultSet rs = qexec.execSelect();

            while (rs.hasNext()) {

                QuerySolution sol = rs.next();

                String uriDbpedia =
                        sol.getResource("serie").getURI();

                String titulo =
                        sol.getLiteral("label").getString();

                String descripcion = "";

                if (sol.contains("abstract")) {
                    descripcion =
                            sol.getLiteral("abstract").getString()
                                    .replace("\"", "'");
                }

                String temporadas = "0";

                if (sol.contains("seasons")) {
                    temporadas =
                            sol.get("seasons").toString();
                }

                String episodios = "0";

                if (sol.contains("episodes")) {
                    episodios =
                            sol.get("episodes").toString();
                }

                String nombreIndividual =
                        generarNombreSeguro(titulo);

                String insert = """
                    PREFIX ont: <http://www.semanticweb.org/dell/ontologies/2026/2#>
                    PREFIX owl: <http://www.w3.org/2002/07/owl#>
                    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                    INSERT DATA {

                        ont:%s rdf:type ont:Series_televisivas ;

                            ont:tituloSerie "%s" ;

                            ont:descripcion "%s" ;

                            ont:numeroTemporadas %s ;

                            ont:numeroEpisodios %s ;

                            owl:sameAs <%s> .
                    }
                    """.formatted(
                        nombreIndividual,
                        titulo.replace("\"", "'"),
                        descripcion,
                        temporadas,
                        episodios,
                        uriDbpedia
                );

                try {

                    UpdateRequest request =
                            UpdateFactory.create(insert);

                    UpdateExecution.service(FUSEKI_UPDATE_URL)
                            .update(request)
                            .execute();

                    insertadas++;

                } catch (Exception e) {
                        System.out.println("Error insertando: " + titulo +
                                        " -> " + e.getMessage());
                    }
            }
        }

        return "Series importadas: " + insertadas;

    } catch (Exception e) {

        return "Error: " + e.getMessage();
    }
}

private String generarNombreSeguro(String texto) {

    String nombre = texto
            .replaceAll("[^a-zA-Z0-9]", "_")
            .replaceAll("_+", "_");

    if (Character.isDigit(nombre.charAt(0))) {
        nombre = "Serie_" + nombre;
    }

    return nombre;
}


public String importarGenerosDesdeDBpedia(int limite) {

    String consultaDBpedia = """
    PREFIX dbo: <http://dbpedia.org/ontology/>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

    SELECT DISTINCT ?genre ?label
    WHERE {

        ?serie a dbo:TelevisionShow .
        ?serie dbo:genre ?genre .

        ?genre rdfs:label ?label .

        FILTER(lang(?label)="en")
    }
    LIMIT """ + limite;

    int insertados = 0;

    try {

        Query query = QueryFactory.create(consultaDBpedia);

        try (QueryExecution qexec =
                QueryExecutionHTTP.service(DBPEDIA_ENDPOINT_URL)
                        .query(query)
                        .build()) {

            ResultSet rs = qexec.execSelect();

            while (rs.hasNext()) {

                QuerySolution sol = rs.next();

                String uriDbpedia =
                        sol.getResource("genre").getURI();

                String nombreGenero =
                        sol.getLiteral("label").getString();

                String individual =
                        generarNombreSeguro(nombreGenero);

                String insert = """
                PREFIX ont:<http://www.semanticweb.org/dell/ontologies/2026/2#>
                PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX owl:<http://www.w3.org/2002/07/owl#>

                INSERT DATA {

                    ont:%s rdf:type ont:Genero ;

                    ont:nombreGenero "%s" ;

                    owl:sameAs <%s> .
                }
                """.formatted(
                        individual,
                        nombreGenero.replace("\"","'"),
                        uriDbpedia
                );

                try {

                    UpdateExecution.service(FUSEKI_UPDATE_URL)
                            .update(UpdateFactory.create(insert))
                            .execute();

                    insertados++;

                } catch(Exception ex) {
                    System.out.println(
                        "Error insertando género "
                        + nombreGenero + " -> "
                        + ex.getMessage()
                    );
                }
            }
        }

    } catch(Exception e) {
        return "Error: " + e.getMessage();
    }

    return "Generos: " + insertados;
}


public String importarDirectoresDesdeDBpedia(int limite) {

    String consulta = """
        PREFIX dbo:<http://dbpedia.org/ontology/>
        PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>

        SELECT DISTINCT ?director ?nombre
        WHERE{

            ?serie a dbo:TelevisionShow .
            ?serie dbo:creator ?director .

            ?director rdfs:label ?nombre .

            FILTER(lang(?nombre)="en")
        }
        LIMIT """ + limite;

    return importarPersonasGenericas(
            consulta,
            "Director",
            "nombreDirector"
    );
}

public String importarActoresDesdeDBpedia(int limite) {

    String consulta = """
    PREFIX dbo:<http://dbpedia.org/ontology/>
    PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>

    SELECT DISTINCT ?persona ?nombre
    WHERE{
        ?persona a dbo:Actor .
        ?persona rdfs:label ?nombre .
        FILTER(lang(?nombre)="en")
    }
    LIMIT """ + limite;

    return importarPersonasGenericas(
            consulta,
            "Actor/Actriz",
            "actorInterpretadoPor"
    );
}

public String importarProductorasDesdeDBpedia(int limite) {

    String consulta = """
    PREFIX dbo:<http://dbpedia.org/ontology/>
    PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>

    SELECT DISTINCT ?empresa ?nombre
    WHERE{
        ?empresa a dbo:Company .
        ?empresa rdfs:label ?nombre .
        FILTER(lang(?nombre)="en")
    }
    LIMIT """ + limite;

    int insertados = 0;

    try {

        Query query = QueryFactory.create(consulta);

        try(QueryExecution qexec =
                QueryExecutionHTTP.service(DBPEDIA_ENDPOINT_URL)
                        .query(query)
                        .build()) {

            ResultSet rs = qexec.execSelect();

            while(rs.hasNext()) {

                QuerySolution sol = rs.next();

                String uri = sol.getResource("empresa").getURI();

                String nombre =
                        sol.getLiteral("nombre").getString();

                String individual =
                        generarNombreSeguro(nombre);

                String insert = """
                PREFIX ont:<http://www.semanticweb.org/dell/ontologies/2026/2#>
                PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX owl:<http://www.w3.org/2002/07/owl#>

                INSERT DATA {

                    ont:%s rdf:type ont:Productora ;

                    ont:nombreProduc "%s" ;

                    owl:sameAs <%s> .
                }
                """.formatted(
                        individual,
                        nombre.replace("\"","'"),
                        uri
                );

                UpdateExecution.service(FUSEKI_UPDATE_URL)
                        .update(UpdateFactory.create(insert))
                        .execute();

                insertados++;
            }
        }

    } catch(Exception e) {
        return e.getMessage();
    }

    return "Productoras: " + insertados;
}

public String importarPlataformasDesdeDBpedia(int limite) {

    String consulta = """
    PREFIX dbo:<http://dbpedia.org/ontology/>
    PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>

    SELECT DISTINCT ?canal ?nombre
    WHERE{
        ?canal a dbo:TelevisionStation .
        ?canal rdfs:label ?nombre .
        FILTER(lang(?nombre)="en")
    }
    LIMIT """ + limite;

    int insertados = 0;

    try {

        Query query = QueryFactory.create(consulta);

        try(QueryExecution qexec =
                QueryExecutionHTTP.service(DBPEDIA_ENDPOINT_URL)
                        .query(query)
                        .build()) {

            ResultSet rs = qexec.execSelect();

            while(rs.hasNext()) {

                QuerySolution sol = rs.next();

                String uri =
                        sol.getResource("canal").getURI();

                String nombre =
                        sol.getLiteral("nombre").getString();

                String individual =
                        generarNombreSeguro(nombre);

                String insert = """
                PREFIX ont:<http://www.semanticweb.org/dell/ontologies/2026/2#>
                PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX owl:<http://www.w3.org/2002/07/owl#>

                INSERT DATA {

                    ont:%s rdf:type ont:PlataformaEmision ;

                    ont:nombrePlataforma "%s" ;

                    owl:sameAs <%s> .
                }
                """.formatted(
                        individual,
                        nombre.replace("\"","'"),
                        uri
                );

                UpdateExecution.service(FUSEKI_UPDATE_URL)
                        .update(UpdateFactory.create(insert))
                        .execute();

                insertados++;
            }
        }

    } catch(Exception e) {
        return e.getMessage();
    }

    return "Plataformas: " + insertados;
}

private String importarPersonasGenericas(
        String consulta,
        String claseOntologia,
        String propiedadNombre) {

    int insertados = 0;

    try {

        Query query = QueryFactory.create(consulta);

        try(QueryExecution qexec =
                QueryExecutionHTTP.service(DBPEDIA_ENDPOINT_URL)
                        .query(query)
                        .build()) {

            ResultSet rs = qexec.execSelect();

            while(rs.hasNext()) {

                QuerySolution sol = rs.next();
                
                String claseURI = claseOntologia.contains("/")
                        ? "<http://www.semanticweb.org/dell/ontologies/2026/2#" + claseOntologia + ">"
                        : "ont:" + claseOntologia;

                String uri;

                        if(sol.contains("director"))
                            uri = sol.getResource("director").getURI();
                        else if(sol.contains("persona"))
                            uri = sol.getResource("persona").getURI();
                        else
                            continue;

                String nombre =
                        sol.getLiteral("nombre").getString();

                String individual =
                        generarNombreSeguro(nombre);

                String insert = String.format("""
                    PREFIX ont:<http://www.semanticweb.org/dell/ontologies/2026/2#>
                    PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                    PREFIX owl:<http://www.w3.org/2002/07/owl#>

                    INSERT DATA {

                        ont:%s rdf:type %s ;

                        ont:%s "%s" ;

                        owl:sameAs <%s> .
                    }
                    """,
                            individual,
                            claseURI,
                            propiedadNombre,
                            nombre.replace("\"","'"),
                            uri
                    );

                UpdateExecution.service(FUSEKI_UPDATE_URL)
                        .update(UpdateFactory.create(insert))
                        .execute();

                insertados++;
            }
        }

    } catch(Exception e) {
        return e.getMessage();
    }

    return claseOntologia + ": " + insertados;
}

public String poblarTodoDBpedia(int limite) {

    StringBuilder sb = new StringBuilder();

    sb.append(importarSeriesDesdeDBpedia(limite)).append("\n");

    sb.append(importarGenerosDesdeDBpedia(10)).append("\n");

    sb.append(importarDirectoresDesdeDBpedia(113)).append("\n");

    sb.append(importarActoresDesdeDBpedia(1000)).append("\n");

    sb.append(importarProductorasDesdeDBpedia(20)).append("\n");

    sb.append(importarPlataformasDesdeDBpedia(15)).append("\n");

    return sb.toString();
}

}
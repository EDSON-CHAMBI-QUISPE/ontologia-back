package com.web_semanticas.ontologia.dto;

public class QueryRequest {

    private String sparql;
    private String modo; // Valores posibles: "FUSEKI", "DBPEDIA_ONLINE", "DBPEDIA_OFFLINE"

    public String getSparql() {
        return sparql;
    }

    public void setSparql(String sparql) {
        this.sparql=sparql;
    }

    public String getModo() {
        return modo;
    }

    public void setModo(String modo) {
        this.modo = modo;
    }
}
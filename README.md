# OWL2SPARQL - "Yet another OWL To SPARQL Query rewriter"

This project provides a simple converter from OWL axioms and OWL class expressions to SPARQL queries.

## From OWL axiom to SPARQL query

### Usage
```Java
// create the converter
OWLAxiomToSPARQLConverter converter = new OWLAxiomToSPARQLConverter("?s","?o");

// provide some OWL axiom using OWL API datastructures
OWLAxiom axiom = ...;

// convert the axiom into a SPARQLquery
String queryString = converter.convert(axiom);
```

### Example
OWL axiom (in Manchester OWL Syntax)
```
PREFIX: : <http://example.org#>
ObjectProperty: r
  Domain: A
```
SPARQL query
```
PREFIX : <http://example.org#>
SELECT DISTINCT  ?s
WHERE
  { ?s :r ?s0 .
    ?s a :A
  }
```
## From OWL class expression to SPARQL query

### Usage
```Java
// create the converter
OWLClassExpressionToSPARQLConverter converter = new OWLClassExpressionToSPARQLConverter();

// provide some OWL class expression using OWL API datastructures
OWLClassExpression ce = ...;

// convert the class expression into a SPARQL query
String queryString = converter.convert(ce);
```

### Example
OWL class expression (in Manchester OWL Syntax)
```
PREFIX: : <http://example.org#>
A and ( B or not (r some B))
```
SPARQL query
```
PREFIX : <http://example.org#>
SELECT DISTINCT  ?x
WHERE
  { ?x a :A
      { ?x a :B }
    UNION
      { ?x ?p ?o
        FILTER NOT EXISTS {
          ?x :r ?s0 .
          ?s0 a :B
        }
      }  
  }
```

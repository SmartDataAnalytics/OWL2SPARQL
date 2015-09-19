# OWL2SPARQL - "Yet another OWL To SPARQL Query rewriter?!"

[![Build Status](http://ci.aksw.org/jenkins/job/jena-sparql-api/badge/icon)](http://ci.aksw.org/jenkins/job/owl2sparql/)

This project provides a simple converter from OWL axioms and OWL class expressions to SPARQL queries.

## Maven Settings
```XML
<repositories>
    <repository>
        <id>maven.aksw.internal</id>
        <name>University Leipzig, AKSW Maven2 Repository</name>
        <url>http://maven.aksw.org/archiva/repository/internal</url>
    </repository>

    <repository>
        <id>maven.aksw.snapshots</id>
        <name>University Leipzig, AKSW Maven2 Repository</name>
        <url>http://maven.aksw.org/archiva/repository/snapshots</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>org.aksw.owl2sparql</groupId>
        <artifactId>owl2sparql-core</artifactId>
        <version>0.1</version>
    </dependency>

    ...
</dependencies>
```

## From OWL axiom to SPARQL query

### Usage
```Java
// create the converter
OWLAxiomToSPARQLConverter converter = new OWLAxiomToSPARQLConverter("?s","?o");

// provide some OWL axiom using OWL API datastructures
OWLAxiom axiom = ...;

// convert the axiom into a SPARQL query
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

## License
The source code of this repo is published under the [Apache License Version 2.0](https://github.com/AKSW/owl2sparql/blob/master/LICENSE).

This project makes use of several dependencies: When in doubt, please cross-check with the respective projects:
* [Apache Jena](https://jena.apache.org/) (Apache License 2.0)
* [Guava](http://code.google.com/p/guava-libraries/) (Apache License 2.0)

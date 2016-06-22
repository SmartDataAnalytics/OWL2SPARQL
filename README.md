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
* 

## More Examples
<table style='border:1; border-collapse: separate; border-spacing: 0 1em;'>
<tr><th>Class Expression</th>
<th>SPARQL Query</th>
</tr>
<tr><td>A</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;A&gt;}
</pre></td></tr>
<tr><td>A
 and (B or (not (r some Thing)))</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;A&gt;
      { ?x rdf:type &lt;B&gt;}
    <b>UNION</b>
      { ?x ?p ?o
        <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?x &lt;r&gt; ?s0}
      }
  }
</pre></td></tr>
<tr><td>A
 and (not (B))</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;A&gt;
    <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?x rdf:type &lt;B&gt;}
  }
</pre></td></tr>
<tr><td>A
 and (not (r some B))</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;A&gt;
    <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?x &lt;r&gt; ?s0 .
      ?s0 rdf:type &lt;B&gt;
    }
  }
</pre></td></tr>
<tr><td>A
 and (r some  Self )</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;A&gt; .
    ?x &lt;r&gt; ?x
  }
</pre></td></tr>
<tr><td>A
 and (t some boolean)</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  xsd:  &lt;http://www.w3.org/2001/XMLSchema#&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;A&gt; .
    ?x &lt;t&gt; ?s0
    <b>FILTER</b> ( datatype(?s0) = xsd:boolean )
  }
</pre></td></tr>
<tr><td>A
 and (t some  not boolean)</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  xsd:  &lt;http://www.w3.org/2001/XMLSchema#&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;A&gt; .
    ?x &lt;t&gt; ?s0
    <b>FILTER</b> ( datatype(?s0) != xsd:boolean )
  }
</pre></td></tr>
<tr><td>A
 and (t value 1)</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;A&gt; .
    ?x &lt;t&gt; 1
  }
</pre></td></tr>
<tr><td>A
 and (t min 2 boolean)</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  xsd:  &lt;http://www.w3.org/2001/XMLSchema#&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;A&gt;
    { <b>SELECT</b>  ?x
      <b>WHERE</b>
        { ?x &lt;t&gt; ?s0
          <b>FILTER</b> ( datatype(?s0) = xsd:boolean )
        }
      <b>GROUP BY</b> ?x
      <b>HAVING</b> ( <b>COUNT</b>(?s0) &gt;= 2 )
    }
  }
</pre></td></tr>
<tr><td>B
 and (r some B)</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;B&gt; .
    ?x &lt;r&gt; ?s0 .
    ?s0 rdf:type &lt;B&gt;
  }
</pre></td></tr>
<tr><td>B
 and (r some B)
 and (s some A)</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;B&gt; .
    ?x &lt;r&gt; ?s0 .
    ?s0 rdf:type &lt;B&gt; .
    ?x &lt;s&gt; ?s1 .
    ?s1 rdf:type &lt;A&gt;
  }
</pre></td></tr>
<tr><td>B
 and (r some 
    (C
     and (s some A)))</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;B&gt; .
    ?x &lt;r&gt; ?s0 .
    ?s0 rdf:type &lt;C&gt; .
    ?s0 &lt;s&gt; ?s1 .
    ?s1 rdf:type &lt;A&gt;
  }
</pre></td></tr>
<tr><td>Place
 and (language min 2 Language)</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x rdf:type &lt;Place&gt;
    { <b>SELECT</b>  ?x
      <b>WHERE</b>
        { ?x &lt;language&gt; ?s0 .
          ?s0 rdf:type &lt;Language&gt;
        }
      <b>GROUP BY</b> ?x
      <b>HAVING</b> ( <b>COUNT</b>(?s0) &gt;= 2 )
    }
  }
</pre></td></tr>
<tr><td>(not (A))
 and (r some (s some (not (B))))</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?x rdf:type &lt;A&gt;}
    ?x &lt;r&gt; ?s0 .
    ?s0 &lt;s&gt; ?s1
    <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?s1 rdf:type &lt;B&gt;}
  }
</pre></td></tr>
<tr><td>A or B</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  {   { ?x rdf:type &lt;A&gt;}
    <b>UNION</b>
      { ?x rdf:type &lt;B&gt;}
  }
</pre></td></tr>
<tr><td>(not (A)) or (not (B))</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  {   { ?x ?p ?o
        <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?x rdf:type &lt;A&gt;}
      }
    <b>UNION</b>
      { ?x ?p ?o
        <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?x rdf:type &lt;B&gt;}
      }
  }
</pre></td></tr>
<tr><td>not (B)</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?x rdf:type &lt;B&gt;}}
</pre></td></tr>
<tr><td>{a , b}</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x ?p ?o
    <b>FILTER</b> ( ?x<b> IN</b> (&lt;a&gt;, &lt;b&gt;) )
  }
</pre></td></tr>
<tr><td>r some B</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x &lt;r&gt; ?s0 .
    ?s0 rdf:type &lt;B&gt;
  }
</pre></td></tr>
<tr><td>r some 
    (A
     and (not (B)))</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x &lt;r&gt; ?s0 .
    ?s0 rdf:type &lt;A&gt;
    <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?s0 rdf:type &lt;B&gt;}
  }
</pre></td></tr>
<tr><td>r some ({a})</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x &lt;r&gt; ?s0
    <b>FILTER</b> ( ?s0<b> IN</b> (&lt;a&gt;) )
  }
</pre></td></tr>
<tr><td>r some ({a , b})</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x &lt;r&gt; ?s0
    <b>FILTER</b> ( ?s0<b> IN</b> (&lt;a&gt;, &lt;b&gt;) )
  }
</pre></td></tr>
<tr><td>language only Language</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x ?p0 ?s0
    <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?x &lt;language&gt; ?s1
      <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?s1 rdf:type &lt;Language&gt;}
    }
  }
</pre></td></tr>
<tr><td>r only B</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x ?p0 ?s0
    <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?x &lt;r&gt; ?s1
      <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?s1 rdf:type &lt;B&gt;}
    }
  }
</pre></td></tr>
<tr><td>r only Thing</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x ?p ?o}
</pre></td></tr>
<tr><td>r only 
    (A
     and (s only Thing))</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x ?p0 ?s0
    <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?x &lt;r&gt; ?s1
      <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?s1 rdf:type &lt;A&gt;}
    }
  }
</pre></td></tr>
<tr><td>r only 
    (A or (s only Thing))</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x ?p ?o}
</pre></td></tr>
<tr><td>r only (s only Thing)</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x ?p ?o}
</pre></td></tr>
<tr><td>r value a</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x &lt;r&gt; &lt;a&gt;}
</pre></td></tr>
<tr><td>language min 2 Language</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  rdf:  &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x &lt;language&gt; ?s0 .
    ?s0 rdf:type &lt;Language&gt;
  }
<b>GROUP BY</b> ?x
<b>HAVING</b> ( <b>COUNT</b>(?s0) &gt;= 2 )
</pre></td></tr>
<tr><td>t some (integer or (boolean and {1 , 2}))</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  xsd:  &lt;http://www.w3.org/2001/XMLSchema#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x &lt;t&gt; ?s0
    <b>FILTER</b> ( ( datatype(?s0) = xsd:integer ) || ( ( datatype(?s0) = xsd:boolean ) && ( ?s0<b> IN</b> (1, 2) ) ) )
  }
</pre></td></tr>
<tr><td>t some  not ({1 , 2})</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x &lt;t&gt; ?s0
    <b>FILTER</b> ( ?s0 <b>NOT</b><b> IN</b> (1, 2) )
  }
</pre></td></tr>
<tr><td>t some {1 , 2}</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x &lt;t&gt; ?s0
    <b>FILTER</b> ( ?s0<b> IN</b> (1, 2) )
  }
</pre></td></tr>
<tr><td>t some (boolean and {1 , 2})</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  xsd:  &lt;http://www.w3.org/2001/XMLSchema#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x &lt;t&gt; ?s0
    <b>FILTER</b> ( ( datatype(?s0) = xsd:boolean ) && ( ?s0<b> IN</b> (1, 2) ) )
  }
</pre></td></tr>
<tr><td>t some PlainLiteral[length 10]</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x &lt;t&gt; ?s0
    <b>FILTER</b> strlen(( str(?s0) = 10 ))
  }
</pre></td></tr>
<tr><td>t some integer[>= 3 , < 10]</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x &lt;t&gt; ?s0
    <b>FILTER</b> ( ( ?s0 &gt;= 3 ) && ( ?s0 &lt; 10 ) )
  }
</pre></td></tr>
<tr><td>t only boolean</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;
<b>PREFIX</b>  xsd:  &lt;http://www.w3.org/2001/XMLSchema#&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x ?p0 ?s0
    <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?x &lt;t&gt; ?s1
      <b>FILTER</b> ( datatype(?s1) != xsd:boolean )
    }
  }
</pre></td></tr>
<tr><td>t only {1}</td><td><pre><b>BASE</b>    &lt;http://example.org/ontology/&gt;

<b>SELECT</b> <b>DISTINCT</b>  ?x
<b>WHERE</b>
  { ?x ?p0 ?s0
    <b>FILTER</b> <b>NOT</b> <b>EXISTS</b> {?x &lt;t&gt; ?s1
      <b>FILTER</b> ( ?s1 <b>NOT</b><b> IN</b> (1) )
    }
  }
</pre></td></tr>
</table>

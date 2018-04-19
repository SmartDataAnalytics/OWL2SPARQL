/*
 * #%L
 * owl2sparql-core
 * %%
 * Copyright (C) 2015 - 2018 AKSW
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.aksw.owl2sparql;

import org.aksw.owl2sparql.util.OWLClassExpressionMinimizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;


public class OWLClassExpressionToSPARQLConverterTest {
    private static OWLDataFactory df = OWLManager.getOWLDataFactory();

    @Test
    public void testIntegerDatatypeRestriction() {
        OWLLiteral literal = df.getOWLLiteral(23);
        String expectedLiteral = "\"23\"^^<http://www.w3.org/2001/XMLSchema#integer>";
//        String expectedLiteral = "23";

        /* ---------- MIN_INCLUSIVE ---------- */
        OWLClassExpressionToSPARQLConverter converter =
                new OWLClassExpressionToSPARQLConverter();

        OWLDatatypeRestriction restriction =
                df.getOWLDatatypeRestriction(
                        df.getIntegerOWLDatatype(),
                        OWLFacet.MIN_INCLUSIVE,
                        literal);

        OWLClassExpression ce = df.getOWLDataSomeValuesFrom(
                df.getOWLDataProperty("http://ex.com/dataProp"),
                restriction);

        Query expectedQuery = QueryFactory.create(
                "SELECT DISTINCT ?s\n" +
                "WHERE\n" +
                "  { ?s <http://ex.com/dataProp> ?s0\n" +
                "    FILTER( ?s0>=" + expectedLiteral + ")\n" +
                "  }\n");

        Query query = converter.asQuery(ce, "?s");

        assertTrue("Conversion of class expression " + ce +
                    " failed.\n" + query.toString() + " does not match\n" +
                        expectedQuery,
                query.equals(expectedQuery));


        /* ---------- MIN_EXCLUSIVE ---------- */
        converter = new OWLClassExpressionToSPARQLConverter();

        restriction =
                df.getOWLDatatypeRestriction(
                        df.getIntegerOWLDatatype(),
                        OWLFacet.MIN_EXCLUSIVE,
                        literal);

        ce = df.getOWLDataSomeValuesFrom(
                df.getOWLDataProperty("http://ex.com/dataProp"),
                restriction);

        expectedQuery = QueryFactory.create(
                "SELECT DISTINCT ?s\n" +
                "WHERE\n" +
                "  { ?s <http://ex.com/dataProp> ?s0\n" +
                "    FILTER(?s0 > " + expectedLiteral + ")\n" +
                "  }");

        query = converter.asQuery(ce, "?s");

        assertTrue("Conversion of class expression " + ce +
                        " failed.\n" + query + " does not match\n" +
                        expectedQuery,
                query.equals(expectedQuery));

        /* ---------- MAX_INCLUSIVE ---------- */
        converter = new OWLClassExpressionToSPARQLConverter();

        restriction = df.getOWLDatatypeRestriction(
                df.getIntegerOWLDatatype(),
                OWLFacet.MAX_INCLUSIVE,
                literal);

        ce = df.getOWLDataSomeValuesFrom(
                df.getOWLDataProperty("http://ex.com/dataProp"),
                restriction);

        expectedQuery = QueryFactory.create(
                "SELECT DISTINCT ?s\n" +
                "WHERE\n" +
                "  { ?s <http://ex.com/dataProp> ?s0\n" +
                "    FILTER(?s0 <= " + expectedLiteral + ")\n" +
                "  }");

        query = converter.asQuery(ce, "?s");

        assertTrue("Conversion of class expression " + ce +
                        " failed.\n" + query + " does not match\n" + expectedQuery,
                query.equals(expectedQuery));

        /* ---------- MAX_EXCLUSIVE ---------- */
        converter = new OWLClassExpressionToSPARQLConverter();

        restriction = df.getOWLDatatypeRestriction(
                df.getIntegerOWLDatatype(),
                OWLFacet.MIN_INCLUSIVE,
                literal);

        ce = df.getOWLDataSomeValuesFrom(
                df.getOWLDataProperty("http://ex.com/dataProp"),
                restriction);

        expectedQuery = QueryFactory.create(
                "SELECT DISTINCT ?s\n" +
                "WHERE\n" +
                "  { ?s <http://ex.com/dataProp> ?s0\n" +
                "    FILTER(?s0 >= " + expectedLiteral + ")\n" +
                "  }");

        query = converter.asQuery(ce, "?s");

        assertTrue("Conversion of class expression " + ce +
                        " failed.\n" + query + " does not match\n" + expectedQuery,
                query.equals(expectedQuery));
    }

    @Test
    public void testNestedIntegerDatatypeRestriction() {
        OWLClassExpressionToSPARQLConverter converter =
                new OWLClassExpressionToSPARQLConverter();

        OWLDatatypeRestriction restriction = df.getOWLDatatypeRestriction(
                df.getIntegerOWLDatatype(),
                OWLFacet.MIN_INCLUSIVE,
                df.getOWLLiteral(42));
        OWLDataSomeValuesFrom ce = df.getOWLDataSomeValuesFrom(
                df.getOWLDataProperty("http://ex.com/datatypeProp"),
                restriction);

        /* ---------- OWLObjectIntersectionOf ---------- */
        OWLObjectIntersectionOf intersection = df.getOWLObjectIntersectionOf(
                df.getOWLClass("http://ex.com/SomeCls"),
                ce);

        Query query = converter.asQuery(intersection, "?s");

        Query expectedQuery = QueryFactory.create(
                "SELECT DISTINCT ?s\n" +
                "WHERE\n" +
                "  { ?s a <http://ex.com/SomeCls> ;\n" +
                "      <http://ex.com/datatypeProp> ?s0\n" +
                "      FILTER(?s0 >= 42)\n" +
                "  }");

        assertTrue("Conversion of class expression " + intersection +
                        " failed.\n" + query + " does not match\n" + expectedQuery,
                query.equals(expectedQuery));

        /* ---------- OWLObjectUnionOf ---------- */
        OWLObjectUnionOf union = df.getOWLObjectUnionOf(
                df.getOWLClass("http://ex.com/SomeCls"),
                ce);

        query = converter.asQuery(union, "?s");

        expectedQuery = QueryFactory.create(
                "SELECT DISTINCT ?s\n" +
                "WHERE\n" +
                "{\n" +
                "  { ?s a <http://ex.com/SomeCls> }\n" +
                "  UNION\n" +
                "  { ?s <http://ex.com/datatypeProp> ?s0\n" +
                "    FILTER(?s0 >= 42) }\n" +
                "}");

        assertTrue("Conversion of class expression " + union +
                        " failed.\n" + query + " does not match\n" + expectedQuery,
                query.equals(expectedQuery));

        /* -------- OWLObjectSomeValuesFrom OWLObjectIntersectionOf -------- */
        OWLObjectSomeValuesFrom someValuesFrom = df.getOWLObjectSomeValuesFrom(
                df.getOWLObjectProperty("http://ex.com/objProp"),
                intersection);

        query = converter.asQuery(someValuesFrom, "?s");

        expectedQuery = QueryFactory.create(
                "SELECT DISTINCT ?s\n" +
                "WHERE\n" +
                "  {\n" +
                "    ?s <http://ex.com/objProp> ?s0 .\n" +
                "    ?s0 a <http://ex.com/SomeCls> ;\n" +
                "        <http://ex.com/datatypeProp> ?s1\n" +
                "    FILTER(?s1 >= 42)\n" +
                "  }");

        assertTrue("Conversion of class expression " + someValuesFrom +
                        " failed.\n" + query + " does not match\n" + expectedQuery,
                query.equals(expectedQuery));

        /* OWLObj.IntersectionOf OWLObj.SomeValuesFrom OWLObj.IntersectionOf */
        OWLObjectIntersectionOf outerIntersectionOf =
                df.getOWLObjectIntersectionOf(
                        df.getOWLClass("http://ex.com/AnotherCls"),
                        someValuesFrom);

        query = converter.asQuery(outerIntersectionOf, "?s");

        expectedQuery = QueryFactory.create(
                "SELECT DISTINCT ?s\n" +
                "WHERE\n" +
                "  {\n" +
                "    ?s a <http://ex.com/AnotherCls> ;\n" +
                "        <http://ex.com/objProp> ?s0 .\n" +
                "    ?s0 a <http://ex.com/SomeCls> ;\n" +
                "        <http://ex.com/datatypeProp> ?s1\n" +
                "    FILTER(?s1 >= 42)\n" +
                "  }"
        );

        assertTrue("Conversion of class expression " + outerIntersectionOf +
                        " failed.\n" + query + " does not match\n" + expectedQuery,
                query.equals(expectedQuery));

        /* SPECIAL example (didn't use the original namespaces, but ex.com) */
        OWLClassExpression specialExample = df.getOWLObjectIntersectionOf(
                // ObjectSomeValueFrom(
                df.getOWLObjectSomeValuesFrom(
                    // hasData
                    df.getOWLObjectProperty("http://ex.com/hasData"),
                    // ObjectUnionOf(
                    df.getOWLObjectUnionOf(
                        // Demographic Navigation TV Location ))
                        df.getOWLClass("http://ex.com/Demographic"),
                        df.getOWLClass("http://ex.com/Navigation"),
                        df.getOWLClass("http://ex.com/TV"),
                        df.getOWLClass("http://ex.com/Location")
                    )
                ),
                // ObjectSomeValueFrom(
                df.getOWLObjectSomeValuesFrom(
                    // hasProcessing
                    df.getOWLObjectProperty("http://ex.com/hasProcessing"),
                    // px:Profiling )
                    df.getOWLClass("http://ex.com/Profiling")
                ),
                // ObjectSomeValueFrom(
                df.getOWLObjectSomeValuesFrom(
                    // hasPurpose
                    df.getOWLObjectProperty("http://ex.com/hasPurpose"),
                    // px:Recommendation )
                    df.getOWLClass("http://ex.com/Recommendation")
                ),
                // ObjectSomeValueFrom(
                df.getOWLObjectSomeValuesFrom(
                    // spl:hasStorage
                    df.getOWLObjectProperty("http://ex.com/hasStorage"),
                        // ObjectIntersectionOf(
                        df.getOWLObjectIntersectionOf(
                            // ObjectSomeValuesFrom(
                            df.getOWLObjectSomeValuesFrom(
                                // hasLocation
                                df.getOWLObjectProperty("http://ex.com/hasLocation"),
                                // ObjectIntersectionOf(
                                df.getOWLObjectIntersectionOf(
                                    // OurServers EU ))
                                    df.getOWLClass("http://ex.com/OurServers"),
                                    df.getOWLClass("http://ex.com/EU")
                                )
                            ),
                            // DataSomeValuesFrom(
                            df.getOWLDataSomeValuesFrom(
                                // durationInDays
                                df.getOWLDataProperty("http://ex.com/durationInDays"),
                                // DatatypeRestriction( xsd:integer xsd:mininclusive "0"^^xsd:integer ))))
                                df.getOWLDatatypeRestriction(
                                    df.getIntegerOWLDatatype(),
                                    OWLFacet.MIN_INCLUSIVE,
                                    df.getOWLLiteral(0)
                                )
                            )
                        )
                ),
                // ObjectSomeValueFrom(
                df.getOWLObjectSomeValuesFrom(
                    // hasRecipient
                    df.getOWLObjectProperty("http://ex.com/hasRecipient"),
                    // Ours )
                    df.getOWLClass("http://ex.com/Ours")
                )
        );
        OWLClassExpressionMinimizer minimizer = new OWLClassExpressionMinimizer(OWLManager.getOWLDataFactory());
        query = converter.asQuery(minimizer.minimize(specialExample), "?s");

        expectedQuery = QueryFactory.create(
                "SELECT DISTINCT  ?s\n" +
                "WHERE\n" +
                "  {" +
                "    ?s  <http://ex.com/hasData>  ?s0\n" +
                "    { ?s0  a  <http://ex.com/Demographic> }\n" +
                "    UNION\n" +
                "    { ?s0  a  <http://ex.com/Location> }\n" +
                "    UNION\n" +
                "    { ?s0  a  <http://ex.com/Navigation> }\n" +
                "    UNION\n" +
                "    { ?s0  a  <http://ex.com/TV> }\n" +
                "    ?s   <http://ex.com/hasProcessing>  ?s1 .\n" +
                "    ?s1  a  <http://ex.com/Profiling> .\n" +
                "    ?s   <http://ex.com/hasPurpose>  ?s2 .\n" +
                "    ?s2  a  <http://ex.com/Recommendation> .\n" +
                "    ?s   <http://ex.com/hasRecipient>  ?s3 .\n" +
                "    ?s3  a  <http://ex.com/Ours> .\n" +
                "    ?s   <http://ex.com/hasStorage>  ?s4 .\n" +
                "    ?s4  <http://ex.com/hasLocation>  ?s5 .\n" +
                "    ?s5  a  <http://ex.com/EU> ;\n" +
                "        a  <http://ex.com/OurServers> .\n" +
                "    ?s4  <http://ex.com/durationInDays>  ?s6\n" +
                "    FILTER ( ?s6 >= 0 )\n" +
                "  }");

        assertTrue("Conversion of class expression " + specialExample +
                        " failed.\n" + query + " does not match\n" + expectedQuery,
                query.equals(expectedQuery));
    }
}

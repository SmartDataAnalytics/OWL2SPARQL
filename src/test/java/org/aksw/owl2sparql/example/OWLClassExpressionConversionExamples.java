/*
 * #%L
 * owl2sparql-core
 * %%
 * Copyright (C) 2015 - 2016 AKSW
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
package org.aksw.owl2sparql.example;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import org.aksw.owl2sparql.OWLClassExpressionToSPARQLConverter;
import org.aksw.owl2sparql.style.AllQuantorTranslation;
import org.aksw.owl2sparql.util.OWLClassExpressionMinimizer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntax;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWLFacet;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * @author Lorenz Buehmann
 */
public class OWLClassExpressionConversionExamples {

	public static void main(String[] args) throws Exception {
		ToStringRenderer.getInstance().setRenderer(new ManchesterOWLSyntaxOWLObjectRendererImpl());

		OWLClassExpressionToSPARQLConverter converter = new OWLClassExpressionToSPARQLConverter();
		converter.setAllQuantorTranslation(AllQuantorTranslation.DOUBLE_NEGATION);

		String NS = "http://example.org/ontology/";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();
		PrefixManager pm = new DefaultPrefixManager(NS);
		OWLClassExpressionMinimizer minimizer = new OWLClassExpressionMinimizer(df);

		OWLClass clsA = df.getOWLClass("A", pm);
		OWLClass clsB = df.getOWLClass("B", pm);
		OWLClass clsC = df.getOWLClass("C", pm);

		OWLObjectProperty propR = df.getOWLObjectProperty("r", pm);
		OWLObjectProperty propS = df.getOWLObjectProperty("s", pm);

		OWLDataProperty dpT = df.getOWLDataProperty("t", pm);
		OWLDataRange booleanRange = df.getBooleanOWLDatatype();
		OWLLiteral lit = df.getOWLLiteral(1);

		OWLIndividual indA = df.getOWLNamedIndividual("a", pm);
		OWLIndividual  indB = df.getOWLNamedIndividual("b", pm);

		String rootVar = "?x";

		List<OWLClassExpression> classExpressions = Lists.newArrayList(
				clsA,
				df.getOWLObjectSomeValuesFrom(propR, clsB),
				df.getOWLObjectIntersectionOf(
						df.getOWLObjectSomeValuesFrom(propR, clsB),
						clsB),
				df.getOWLObjectUnionOf(
						clsA,
						clsB),
				df.getOWLObjectHasValue(propR,
										indA),
				df.getOWLObjectAllValuesFrom(propR,
											 df.getOWLThing()),
				df.getOWLObjectAllValuesFrom(propR,
											 df.getOWLObjectAllValuesFrom(propS, df.getOWLThing())),
				df.getOWLObjectAllValuesFrom(
						propR,
						df.getOWLObjectIntersectionOf(
								clsA,
								df.getOWLObjectAllValuesFrom(propS, df.getOWLThing()))),
				df.getOWLObjectAllValuesFrom(
						propR,
						df.getOWLObjectUnionOf(
								clsA,
								df.getOWLObjectAllValuesFrom(propS, df.getOWLThing()))),
				df.getOWLObjectAllValuesFrom(propR, clsB),
				df.getOWLObjectAllValuesFrom(df.getOWLObjectProperty("language", pm), df.getOWLClass("Language", pm)),
				df.getOWLObjectMinCardinality(2, df.getOWLObjectProperty("language", pm), df.getOWLClass("Language", pm)),
				df.getOWLObjectIntersectionOf(
						df.getOWLClass("Place", pm),
						df.getOWLObjectMinCardinality(
								2,
								df.getOWLObjectProperty("language", pm),
								df.getOWLClass("Language", pm))),
				df.getOWLObjectOneOf(indA, indB),
				df.getOWLObjectSomeValuesFrom(
						propR,
						df.getOWLObjectOneOf(indA)),
				df.getOWLObjectSomeValuesFrom(propR, df.getOWLObjectOneOf(indA, indB)),
				df.getOWLObjectIntersectionOf(
						clsA,
						df.getOWLObjectHasSelf(propR)),
				df.getOWLObjectIntersectionOf(
						clsA,
						df.getOWLDataSomeValuesFrom(dpT, booleanRange)),
				df.getOWLObjectIntersectionOf(
						clsA,
						df.getOWLDataHasValue(dpT, lit)),
				df.getOWLObjectIntersectionOf(
						clsA,
						df.getOWLDataMinCardinality(2, dpT, booleanRange)),
				df.getOWLObjectIntersectionOf(
						clsA,
						df.getOWLObjectComplementOf(clsB)),
				df.getOWLObjectSomeValuesFrom(propR,
											  df.getOWLObjectIntersectionOf(
													  clsA,
													  df.getOWLObjectComplementOf(clsB))),
				df.getOWLDataAllValuesFrom(dpT, booleanRange),
				df.getOWLDataAllValuesFrom(dpT,df.getOWLDataOneOf(lit)),
				df.getOWLObjectIntersectionOf(
						df.getOWLObjectSomeValuesFrom(
								propR,
								clsB),
						clsB,
						df.getOWLObjectSomeValuesFrom(propS, clsA)),
				df.getOWLObjectComplementOf(clsB),
				df.getOWLObjectIntersectionOf(
						df.getOWLObjectSomeValuesFrom(
								propR,
								df.getOWLObjectIntersectionOf(
										df.getOWLObjectSomeValuesFrom(propS, clsA),
										clsC)),
						clsB),
				df.getOWLObjectIntersectionOf(
						df.getOWLObjectComplementOf(clsA),
						df.getOWLObjectSomeValuesFrom(
								propR,
								df.getOWLObjectSomeValuesFrom(
										propS,
										df.getOWLObjectComplementOf(clsB)
								)
						)
				),
				df.getOWLObjectUnionOf(
						df.getOWLObjectComplementOf(clsA),
						df.getOWLObjectComplementOf(clsB)
				),
				df.getOWLObjectIntersectionOf(
						clsA,
						df.getOWLObjectUnionOf(
								clsB,
								df.getOWLObjectComplementOf(
										df.getOWLObjectSomeValuesFrom(
												propR,
												df.getOWLThing()
										)
								)
						)
				),
				df.getOWLObjectIntersectionOf(
						clsA,
						df.getOWLObjectComplementOf(
								df.getOWLObjectSomeValuesFrom(
										propR,
										clsB
								)
						)
				),
				df.getOWLObjectIntersectionOf(
						clsA,
						df.getOWLDataSomeValuesFrom(dpT, df.getOWLDataComplementOf(booleanRange))
				),
				df.getOWLDataSomeValuesFrom(dpT,df.getOWLDataOneOf(df.getOWLLiteral(1), df.getOWLLiteral(2))),
				df.getOWLDataSomeValuesFrom(
						dpT,
						df.getOWLDataComplementOf(
								df.getOWLDataOneOf(df.getOWLLiteral(1), df.getOWLLiteral(2)))),
				df.getOWLDataSomeValuesFrom(
						dpT,
						df.getOWLDataIntersectionOf(
								df.getBooleanOWLDatatype(),
								df.getOWLDataOneOf(df.getOWLLiteral(1), df.getOWLLiteral(2)))),
				df.getOWLDataSomeValuesFrom(
						dpT,
						df.getOWLDataUnionOf(
								df.getIntegerOWLDatatype(),
								df.getOWLDataIntersectionOf(
										df.getBooleanOWLDatatype(),
										df.getOWLDataOneOf(df.getOWLLiteral(1), df.getOWLLiteral(2))))),
				df.getOWLDataSomeValuesFrom(
						dpT,
						df.getOWLDatatypeRestriction(
								df.getIntegerOWLDatatype(),
								df.getOWLFacetRestriction(
										OWLFacet.MAX_EXCLUSIVE, df.getOWLLiteral(10)),
								df.getOWLFacetRestriction(
										OWLFacet.MIN_INCLUSIVE, df.getOWLLiteral(3)))),
				df.getOWLDataSomeValuesFrom(
						dpT,
						df.getOWLDatatypeRestriction(
								df.getRDFPlainLiteral(),
								df.getOWLFacetRestriction(
										OWLFacet.LENGTH, df.getOWLLiteral(10))))

		);


		HTMLTableBuilder html = new HTMLTableBuilder("OWL To SPARQL Converter", true, classExpressions.size(), 2);
		html.addTableHeader("Class Expression", "SPARQL Query");
		String[] keywords = {"WHERE", "SELECT", "DISTINCT", "FILTER", "NOT", "EXISTS", "UNION", "OPTIONAL", "GROUP BY", "HAVING", "COUNT", " IN", "PREFIX", "BASE"};

		for (OWLClassExpression ce : new TreeSet<>(classExpressions)) {
			Query query = converter.asQuery(ce, rootVar);
			PrefixMapping pm2 = new PrefixMappingImpl();
			if(query.toString().contains("rdf-schema#")) {
				pm2.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
			}
			if(query.toString().contains("rdf-syntax-ns#")) {
				pm2.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
			}
			if(query.toString().contains("XMLSchema#")) {
				pm2.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
			}
			query.setPrefixMapping(pm2);
			query.setBaseURI(NS);
			System.out.println(ce + "\n" + query);

			String ceString = ce.toString();
			String queryString = query.toString();

			// add comment if CE was rewritten
			OWLClassExpression minCE = minimizer.minimize(ce);
			if(!ce.equals(minCE)) {
				ceString += "\n(logically equivalent to\n" + minCE.toString() + ")";
			}
			// format OWL class expression
			for(ManchesterOWLSyntax keyword : ManchesterOWLSyntax.values()) {
				if(keyword.isClassExpressionConnectiveKeyword() || keyword.isClassExpressionQuantiferKeyword())
				ceString = ceString.replace(keyword.keyword(), "<b>" + keyword + "</b>");
			}

			// format SPARQL query
			queryString = queryString.replace("<", "&lt;").replace(">", "&gt;");
			for (String keyword : keywords) {
				queryString = queryString.replace(keyword , "<b>" + keyword + "</b>");
			}
			// add to HTML table
			html.addRowValues("<pre>" + ceString + "</pre>", "<pre>" + queryString + "</pre>");
		}
		com.google.common.io.Files.write(html.build(), new File("examples/owl2sparql-examples.html"), Charsets.UTF_8);
//		Files.write(Paths.get("/tmp/owl2sparql.html"), Arrays.asList(html.build().split("\n")), Charset.forName("UTF-8"));
	}
}

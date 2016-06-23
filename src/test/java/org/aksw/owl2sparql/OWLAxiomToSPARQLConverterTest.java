/*
 * #%L
 * owl2sparql-core
 * %%
 * Copyright (C) 2015 AKSW
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import static org.junit.Assert.assertTrue;

/**
 * @author Lorenz Buehmann
 *
 */
public class OWLAxiomToSPARQLConverterTest {
	
	OWLAxiomToSPARQLConverter converter = new OWLAxiomToSPARQLConverter("?s","?o");
	
	OWLOntologyManager man = OWLManager.createOWLOntologyManager();
	OWLDataFactory df = man.getOWLDataFactory();
	PrefixManager pm = new DefaultPrefixManager("http://foo.bar/");
	
	OWLClass clsA = df.getOWLClass("A", pm);
	OWLClass clsB = df.getOWLClass("B", pm);
	OWLClass clsC = df.getOWLClass("C", pm);
	OWLClass clsD = df.getOWLClass("D", pm);
	
	OWLObjectProperty propR = df.getOWLObjectProperty("r", pm);
	OWLObjectProperty propS = df.getOWLObjectProperty("s", pm);
	OWLObjectProperty propT = df.getOWLObjectProperty("t", pm);
	
	OWLDataProperty dpT = df.getOWLDataProperty("dpT", pm);
	OWLDataRange booleanRange = df.getBooleanOWLDatatype();
	OWLLiteral lit = df.getOWLLiteral(1);
	
	OWLIndividual indA = df.getOWLNamedIndividual("a", pm);
	OWLIndividual  indB = df.getOWLNamedIndividual("b", pm);
	
	String rootVariable = "?s";

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ToStringRenderer.getInstance().setRenderer(new DLSyntaxObjectRenderer());
	}

	@Test
	public void testSubClassOfAxiom() {
		OWLClassExpression subClass = clsA;
		OWLClassExpression superClass = clsB;
		OWLAxiom axiom = df.getOWLSubClassOfAxiom(subClass, superClass);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"WHERE\n" + 
				"  { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/A> .\n" + 
				"    ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/B>\n" + 
				"  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testEquivalentClassesAxiom() {
		OWLAxiom axiom = df.getOWLEquivalentClassesAxiom(clsA, clsB, clsC);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"WHERE\n" + 
				"  { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/A> .\n" + 
				"    ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/B> .\n" + 
				"    ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/C> \n" + 
				"  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testDisjointClassesAxiom() {
		OWLAxiom axiom = df.getOWLDisjointClassesAxiom(clsA, clsB, clsC);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"WHERE\n" + 
				"  {   { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/A>\n" + 
				"        FILTER NOT EXISTS {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/B> }\n" + 
				"        FILTER NOT EXISTS {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/C> }\n" + 
				"      }\n" + 
				"    UNION\n" + 
				"      { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/B>\n" + 
				"        FILTER NOT EXISTS {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/A> }\n" + 
				"        FILTER NOT EXISTS {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/C> }\n" + 
				"      }\n" + 
				"    UNION\n" + 
				"      { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/C>\n" + 
				"        FILTER NOT EXISTS {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/A> }\n" + 
				"        FILTER NOT EXISTS {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/B> }\n" + 
				"      }\n" + 
				"  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testDisjointUnionAxiom() {
		OWLAxiom axiom = df.getOWLDisjointUnionAxiom(clsA, Sets.newHashSet(clsB, clsC, clsD));
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"			WHERE\n" + 
				"			  { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/A>\n" + 
				"			      { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/B>\n" + 
				"			        FILTER NOT EXISTS {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/C> }\n" + 
				"			        FILTER NOT EXISTS {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/D> }\n" + 
				"			      }\n" + 
				"			    UNION\n" + 
				"			      { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/C>\n" + 
				"			        FILTER NOT EXISTS {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/B> }\n" + 
				"			        FILTER NOT EXISTS {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/D> }\n" + 
				"			      }\n" + 
				"			    UNION\n" + 
				"			      { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/D>\n" + 
				"			        FILTER NOT EXISTS {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/B> }\n" + 
				"			        FILTER NOT EXISTS {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/C> }\n" + 
				"			      }\n" + 
				"			  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testSubPropertyAxiom() {
		OWLAxiom axiom = df.getOWLSubObjectPropertyOfAxiom(propR, propS);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"			WHERE\n" + 
				"			  { ?s <http://foo.bar/r> ?o .\n" + 
				"			    ?s <http://foo.bar/s> ?o\n" + 
				"			  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testEquivalentPropertiesAxiom() {
		OWLAxiom axiom = df.getOWLEquivalentObjectPropertiesAxiom(propR, propS, propT);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"			WHERE\n" + 
				"			  { ?s <http://foo.bar/r> ?o .\n" + 
				"			    ?s <http://foo.bar/s> ?o .\n" + 
				"			    ?s <http://foo.bar/t> ?o\n" + 
				"			  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testDisjointPropertiesAxiom() {
		OWLAxiom axiom = df.getOWLDisjointObjectPropertiesAxiom(propR, propS, propT);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"WHERE\n" + 
				"  {   { ?s <http://foo.bar/r> ?o\n" + 
				"        FILTER NOT EXISTS {?s <http://foo.bar/s> ?o }\n" + 
				"        FILTER NOT EXISTS {?s <http://foo.bar/t> ?o }\n" + 
				"      }\n" + 
				"    UNION\n" + 
				"      { ?s <http://foo.bar/s> ?o\n" + 
				"        FILTER NOT EXISTS {?s <http://foo.bar/r> ?o }\n" + 
				"        FILTER NOT EXISTS {?s <http://foo.bar/t> ?o }\n" + 
				"      }\n" + 
				"    UNION\n" + 
				"      { ?s <http://foo.bar/t> ?o\n" + 
				"        FILTER NOT EXISTS {?s <http://foo.bar/r> ?o }\n" + 
				"        FILTER NOT EXISTS {?s <http://foo.bar/s> ?o }\n" + 
				"      }\n" + 
				"  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testPropertyDomainAxiom() {
		OWLAxiom axiom = df.getOWLObjectPropertyDomainAxiom(propR, clsA);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"			WHERE\n" + 
				"			  { ?s <http://foo.bar/r> ?s0 .\n" + 
				"			    ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/A>\n" + 
				"			  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testObjectPropertyRangeAxiom() {
		OWLAxiom axiom = df.getOWLObjectPropertyRangeAxiom(propR, clsA);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"	WHERE\n" + 
				"	  { ?s ?p ?o\n" +
				"	    FILTER NOT EXISTS {?s <http://foo.bar/r> ?s1\n" + 
				"	      FILTER NOT EXISTS {?s1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://foo.bar/A> }\n" + 
				"	    }\n" + 
				"	  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testDataPropertyRangeAxiom() {
		OWLAxiom axiom = df.getOWLDataPropertyRangeAxiom(dpT, booleanRange);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"			WHERE\n" + 
				"			  { ?s <http://foo.bar/dpT> ?o\n" + 
				"			    FILTER ( datatype(?o) = <http://www.w3.org/2001/XMLSchema#boolean> )\n" + 
				"			  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testAsymmetricObjectPropertyAxiom() {
		OWLAxiom axiom = df.getOWLAsymmetricObjectPropertyAxiom(propR);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"			WHERE\n" + 
				"			  { ?s <http://foo.bar/r> ?o\n" + 
				"			    FILTER NOT EXISTS {?o <http://foo.bar/r> ?s }\n" + 
				"			  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testSymmetricObjectPropertyAxiom() {
		OWLAxiom axiom = df.getOWLSymmetricObjectPropertyAxiom(propR);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"			WHERE\n" + 
				"			  { ?s <http://foo.bar/r> ?o .\n" + 
				"			    ?o <http://foo.bar/r> ?s \n" + 
				"			  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testReflexiveObjectPropertyAxiom() {
		OWLAxiom axiom = df.getOWLReflexiveObjectPropertyAxiom(propR);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"			WHERE\n" + 
				"			  { ?s <http://foo.bar/r> ?s\n" + 
				"			  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testIrreflexiveObjectPropertyAxiom() {
		OWLAxiom axiom = df.getOWLIrreflexiveObjectPropertyAxiom(propR);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"			WHERE\n" + 
				"			  { ?s ?p ?o\n" + 
				"			    FILTER NOT EXISTS {?s <http://foo.bar/r> ?s }\n" + 
				"			  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testFunctionalObjectPropertyAxiom() {
		OWLAxiom axiom = df.getOWLFunctionalObjectPropertyAxiom(propR);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"	WHERE\n" + 
				"	  { ?s <http://foo.bar/r> ?s0\n" + 
				"	    FILTER NOT EXISTS {?s <http://foo.bar/r> ?s1\n" + 
				"	      FILTER ( ! sameTerm(?s0, ?s1) )\n" + 
				"	    }\n" + 
				"	  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testInverseFunctionalObjectPropertyAxiom() {
		OWLAxiom axiom = df.getOWLInverseFunctionalObjectPropertyAxiom(propR);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"	WHERE\n" + 
				"	  { ?s0 <http://foo.bar/r> ?s\n" + 
				"	    FILTER NOT EXISTS {?s1 <http://foo.bar/r> ?s\n" + 
				"	      FILTER ( ! sameTerm(?s0, ?s1) )\n" + 
				"	    }\n" + 
				"	  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testTransitiveObjectPropertyAxiom() {
		OWLAxiom axiom = df.getOWLTransitiveObjectPropertyAxiom(propR);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s\n" + 
				"	WHERE\n" + 
				"	  { ?s <http://foo.bar/r> ?o1 .\n" + 
				"	    ?o1 <http://foo.bar/r> ?o .\n" + 
				"	    ?s <http://foo.bar/r> ?o\n" + 
				"	  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}
	
	@Test
	public void testSubPropertyChainAxiom() {
		OWLAxiom axiom = df.getOWLSubPropertyChainOfAxiom(Lists.newArrayList(propR, propS, propT), propT);
		
		Query targetQuery = QueryFactory.create("SELECT DISTINCT  ?s \n" + 
				"	WHERE\n" + 
				"	  { ?s <http://foo.bar/r> ?s0 .\n" + 
				"	    ?s0 <http://foo.bar/s> ?s1 .\n" + 
				"	    ?s1 <http://foo.bar/t> ?o .\n" + 
				"	    ?s <http://foo.bar/t> ?o\n" + 
				"	  }");
		Query query = converter.asQuery(axiom);
		
		assertTrue("Conversion of axiom " + axiom + " failed.\n" + query + " does not match " + targetQuery, query.equals(targetQuery));
	}

}

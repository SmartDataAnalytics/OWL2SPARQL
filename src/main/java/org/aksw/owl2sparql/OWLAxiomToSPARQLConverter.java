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


import java.util.LinkedList;
import java.util.List;

import org.aksw.owl2sparql.util.VarGenerator;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.DataRangeType;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;

/**
 * A converter from <a href="http://www.w3.org/TR/owl2-syntax/#Axioms">OWL 2
 * axioms</a> to SPARQL queries.
 * 
 * @author Lorenz Buehmann
 *
 */
public class OWLAxiomToSPARQLConverter implements OWLAxiomVisitor{
	
	private String subjectVar = "?x";
	private String objectVar = "?o";
	
	private boolean useDistinct = true;
	
	private OWLClassExpressionToSPARQLConverter expressionConverter;
	
	private String sparql;
	
	public OWLAxiomToSPARQLConverter() {}
	
	public OWLAxiomToSPARQLConverter(String targetSubjectVariable, String targetObjectVariable) {
		this.subjectVar = targetSubjectVariable;
		this.objectVar = targetObjectVariable;
	}
	
	/**
	 * Converts an OWL axiom into a SPARQL query.
	 * 
	 * @param axiom the OWL axiom to convert
	 * @return the SPARQL query
	 */
	public String convert(OWLAxiom axiom){
		return convert(axiom, subjectVar, objectVar);
	}
	
	/**
	 * Converts an OWL axiom into a SPARQL query with <code>targetSubjectVariable</code>
	 * as single projection variable.
	 * @param axiom the OWL axiom to convert
	 * @param targetSubjectVariable the name of the projection variable in the SPARQL
	 *            query
	 * 
	 * @return the SPARQL query
	 */
	public String convert(OWLAxiom axiom, String targetSubjectVariable){
		return convert(axiom, targetSubjectVariable, objectVar);
	}
	
	/**
	 * Converts an OWL axiom into a SPARQL query with <code>targetSubjectVariable</code>
	 * as single projection variable.
	 * @param axiom the OWL axiom to convert
	 * @param targetSubjectVariable the name of the subject projection variable in the SPARQL
	 *            query
	 * @param targetObjectVariable the name of the object projection variable in the SPARQL
	 *            query
	 * @return the SPARQL query
	 */
	public String convert(OWLAxiom axiom, String targetSubjectVariable, String targetObjectVariable) {
		this.subjectVar = targetSubjectVariable;
		this.objectVar = targetObjectVariable;
		
		sparql = "";

		String queryString = createSelectClause() + createWhereClause(axiom);

		return queryString;
	}
	
	/**
	 * Converts an OWL axiom into a SPARQL query using the default variable as
	 * projection variable.
	 * 
	 * @param axiom the OWL axiom
	 * @return the SPARQL query
	 */
	public Query asQuery(OWLAxiom axiom){
		return asQuery(axiom, subjectVar, objectVar);
	}
	
	/**
	 * Converts an OWL axiom into a SPARQL query with <code>targetSubjectVariable</code>
	 * as single projection variable.
	 * @param axiom the OWL axiom to convert
	 * @param targetSubjectVariable the name of the projection variable in the SPARQL
	 *            query
	 * 
	 * @return the SPARQL query
	 */
	public Query asQuery(OWLAxiom axiom, String targetSubjectVariable){
		return asQuery(axiom, targetSubjectVariable, objectVar);
	}
	
	/**
	 * Converts an OWL axiom into a SPARQL query with <code>targetSubjectVariable</code>
	 * as single projection variable.
	 * @param axiom the OWL axiom to convert
	 * @param targetSubjectVariable the name of the subject projection variable in the SPARQL
	 *            query
	 * @param targetObjectVariable the name of the object projection variable in the SPARQL
	 *            query
	 * @return the SPARQL query
	 */
	public Query asQuery(OWLAxiom axiom, String targetSubjectVariable, String targetObjectVariable){
		
		String queryString = convert(axiom, targetSubjectVariable, targetObjectVariable);
		
		return QueryFactory.create(queryString, Syntax.syntaxARQ);
	}
	
	/**
	 * Whether to return SPARQL queries with DISTINCT keyword.
	 * @param useDistinct <code>true</code> if use DISTINCT, otherwise <code>false</code>
	 */
	public void setUseDistinct(boolean useDistinct) {
		this.useDistinct = useDistinct;
	}
	
	private String createSelectClause() {
		return "SELECT " + (useDistinct ? " DISTINCT " : "") + subjectVar;
	}
	
	private String createWhereClause(OWLAxiom axiom){
		return " WHERE " + createGroupGraphPattern(axiom);
	}
	
	private String createGroupGraphPattern(OWLAxiom axiom) {
		expressionConverter = new OWLClassExpressionToSPARQLConverter();
		axiom.accept(this);
		return "{" + sparql + "}";
	}
	
	private String notExists(String pattern){
		return "FILTER NOT EXISTS{" + pattern + "}";
	}
	
	private String notExists(String targetVar, List<OWLClassExpression> classExpressions, boolean useUnion){
		String pattern = "";
		if(useUnion){
			String unionPattern = "";
			if(classExpressions.size() > 1){
				for (int i = 0; i < classExpressions.size() - 1; i++) {
					unionPattern += "{" + expressionConverter.asGroupGraphPattern(classExpressions.get(i), subjectVar) + "}";
					unionPattern += " UNION ";
				}
				unionPattern += "{" + expressionConverter.asGroupGraphPattern(classExpressions.get(classExpressions.size() - 1), subjectVar) + "}";
			} else {
				unionPattern = expressionConverter.asGroupGraphPattern(classExpressions.get(0), subjectVar);
			}
			pattern = notExists(unionPattern);
		} else {
			for (OWLClassExpression ce : classExpressions) {
				pattern += notExists(expressionConverter.asGroupGraphPattern(ce, subjectVar));
			}
		}
		return pattern;
	}
	
	///////////////////////////////////////////////////////////////////////////
	//                                                                       //
	//       Class axioms                                                    //
	//                                                                       //
	///////////////////////////////////////////////////////////////////////////

	@Override
	public void visit(OWLSubClassOfAxiom axiom) {
		OWLClassExpression subClass = axiom.getSubClass();
		if(!subClass.isOWLThing()){// we do not need to convert owl:Thing
			String subClassPattern = expressionConverter.asGroupGraphPattern(subClass, subjectVar);
			sparql += subClassPattern;
		}
		
		OWLClassExpression superClass = axiom.getSuperClass();
		String superClassPattern = expressionConverter.asGroupGraphPattern(superClass, subjectVar, subClass.isOWLThing() && superClass.getClassExpressionType() == ClassExpressionType.OBJECT_COMPLEMENT_OF);
		sparql += superClassPattern;
	}
	
	@Override
	public void visit(OWLEquivalentClassesAxiom axiom) {
		List<OWLClassExpression> classExpressions = axiom.getClassExpressionsAsList();
		
		for (OWLClassExpression ce : classExpressions) {
			sparql += expressionConverter.asGroupGraphPattern(ce, subjectVar);
		}
	}
	
	@Override
	public void visit(OWLDisjointClassesAxiom axiom) {
		List<OWLClassExpression> disjointClasses = axiom.getClassExpressionsAsList();
		
		for(int i = 0; i < disjointClasses.size(); i++){
			sparql += "{";
			OWLClassExpression ce = disjointClasses.remove(i);
			sparql += expressionConverter.asGroupGraphPattern(ce, subjectVar);
			for (OWLClassExpression ce2 : disjointClasses) {
				sparql += notExists(expressionConverter.asGroupGraphPattern(ce2, subjectVar));
			}
			disjointClasses.add(i, ce);
			sparql += "}";
			if(i < disjointClasses.size()-1){
				sparql += " UNION ";
			}
		}
	}
	
	@Override
	public void visit(OWLDisjointUnionAxiom axiom) {
		OWLClass cls = axiom.getOWLClass();
		sparql += expressionConverter.asGroupGraphPattern(cls, subjectVar);
		
		List<OWLClassExpression> classExpressions = new LinkedList<>(axiom.getClassExpressions());
		
		for(int i = 0; i < classExpressions.size(); i++){
			sparql += "{";
			OWLClassExpression ce = classExpressions.remove(i);
			
			// add triple pattern for class to be
			sparql += expressionConverter.asGroupGraphPattern(ce, subjectVar);
			
			// add NOT EXISTS for classes not to be
			sparql += notExists(subjectVar, classExpressions, false);
			
			classExpressions.add(i, ce);
			sparql += "}";
			if(i < classExpressions.size()-1){
				sparql += " UNION ";
			}
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	//                                                                       //
	//       Property axioms                                                 //
	//                                                                       //
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public void visit(OWLSubObjectPropertyOfAxiom axiom) {
		OWLObjectPropertyExpression subPropertyExpression = axiom.getSubProperty();
		if(subPropertyExpression.isAnonymous()){
			sparql += objectVar + "<" + subPropertyExpression.getInverseProperty().asOWLObjectProperty().toStringID() + "> " + subjectVar + " .";
		} else {
			sparql += subjectVar + "<" + subPropertyExpression.asOWLObjectProperty().toStringID() + "> " + objectVar + " .";
		}
		
		OWLObjectPropertyExpression superPropertyExpression = axiom.getSuperProperty();
		if(superPropertyExpression.isAnonymous()){
			sparql += objectVar + "<" + superPropertyExpression.getInverseProperty().asOWLObjectProperty().toStringID() + "> " + subjectVar + " .";
		} else {
			sparql += subjectVar + "<" + superPropertyExpression.asOWLObjectProperty().toStringID() + "> " + objectVar + " .";
		}
	}
	
	@Override
	public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
		for (OWLObjectPropertyExpression propertyExpression : axiom.getProperties()) {
			if(propertyExpression.isAnonymous()){
				sparql += objectVar + "<" + propertyExpression.getInverseProperty().asOWLObjectProperty().toStringID() + "> " + subjectVar + " .";
			} else {
				sparql += subjectVar + "<" + propertyExpression.asOWLObjectProperty().toStringID() + "> " + objectVar + " .";
			}
		}
	}
	
	@Override
	public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
		List<OWLObjectPropertyExpression> propertyExpressions = new LinkedList<>(axiom.getProperties());
		
		for(int i = 0; i < propertyExpressions.size(); i++){
			sparql += "{";
			OWLObjectPropertyExpression pe = propertyExpressions.remove(i);
			if(pe.isAnonymous()){
				sparql += objectVar + "<" + pe.getInverseProperty().asOWLObjectProperty().toStringID() + "> " + subjectVar + " .";
			} else {
				sparql += subjectVar + "<" + pe.asOWLObjectProperty().toStringID() + "> " + objectVar + " .";
			}
			for (OWLObjectPropertyExpression pe2 : propertyExpressions) {
				String pattern;
				if(pe2.isAnonymous()){
					pattern = objectVar + "<" + pe2.getInverseProperty().asOWLObjectProperty().toStringID() + "> " + subjectVar + " .";
				} else {
					pattern = subjectVar + "<" + pe2.asOWLObjectProperty().toStringID() + "> " + objectVar + " .";
				}
				sparql += notExists(pattern);
			}
			propertyExpressions.add(i, pe);
			sparql += "}";
			if(i < propertyExpressions.size()-1){
				sparql += " UNION ";
			}
		}
	}
	
	@Override
	public void visit(OWLSubDataPropertyOfAxiom axiom) {
		OWLDataPropertyExpression subPropertyExpression = axiom.getSubProperty();
		sparql += subjectVar + "<" + subPropertyExpression.asOWLDataProperty().toStringID() + "> " + objectVar + " .";
		
		OWLDataPropertyExpression superPropertyExpression = axiom.getSuperProperty();
		sparql += subjectVar + "<" + superPropertyExpression.asOWLDataProperty().toStringID() + "> " + objectVar + " .";
	}
	
	@Override
	public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
		for (OWLDataPropertyExpression propertyExpression : axiom.getProperties()) {
			sparql += subjectVar + "<" + propertyExpression.asOWLDataProperty().toStringID() + "> " + objectVar + " .";
		}
	}
	
	@Override
	public void visit(OWLDisjointDataPropertiesAxiom axiom) {
		List<OWLDataPropertyExpression> propertyExpressions = new LinkedList<>(axiom.getProperties());
		
		for(int i = 0; i < propertyExpressions.size(); i++){
			sparql += "{";
			OWLDataPropertyExpression pe = propertyExpressions.remove(i);
			sparql += subjectVar + "<" + pe.asOWLDataProperty().toStringID() + "> " + objectVar + " .";
			for (OWLDataPropertyExpression pe2 : propertyExpressions) {
				String pattern = subjectVar + "<" + pe2.asOWLDataProperty().toStringID() + "> " + objectVar + " .";
				sparql += notExists(pattern);
			}
			propertyExpressions.add(i, pe);
			sparql += "}";
			if(i < propertyExpressions.size()-1){
				sparql += " UNION ";
			}
		}
	}

	@Override
	public void visit(OWLObjectPropertyDomainAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	@Override
	public void visit(OWLObjectPropertyRangeAxiom axiom) {
		OWLSubClassOfAxiom subClassOfAxiom = axiom.asOWLSubClassOfAxiom();
		subClassOfAxiom.accept(this);
		String tmp = subjectVar;
		subjectVar = objectVar;
		objectVar = tmp;
	}
	
	@Override
	public void visit(OWLDataPropertyDomainAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	@Override
	public void visit(OWLDataPropertyRangeAxiom axiom) {
		OWLDataRange range = axiom.getRange();
		if (range.getDataRangeType() == DataRangeType.DATATYPE) {
			OWLDataProperty property = axiom.getProperty().asOWLDataProperty();
			sparql += subjectVar + " <" + property.toStringID() + "> ?o." + "FILTER (DATATYPE(?o) = <"
					+ range.asOWLDatatype().toStringID() + ">)";
			String tmp = subjectVar;
			subjectVar = objectVar;
			objectVar = tmp;
		} else {
			throw new IllegalArgumentException("Datarange " + range + " not supported yet.");
		}
	}

	@Override
	public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
		String propertyURI = axiom.getProperty().asOWLObjectProperty().toStringID();
		ParameterizedSparqlString query = new ParameterizedSparqlString("?s ?p ?o . FILTER NOT EXISTS{?o ?p ?s}");
		query.setIri("p", propertyURI);
		sparql += query.toString();
	}
	
	@Override
	public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
		String propertyURI = axiom.getProperty().asOWLObjectProperty().toStringID();
		ParameterizedSparqlString query = new ParameterizedSparqlString("?s ?p ?o . ?o ?p ?s .");
		query.setIri("p", propertyURI);
		sparql += query.toString();
	}

	@Override
	public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	@Override
	public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}

	@Override
	public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	@Override
	public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	@Override
	public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
		ParameterizedSparqlString template = new ParameterizedSparqlString(
				subjectVar + " ?p ?o1 . ?o1 ?p ?o . " + subjectVar + " ?p " + objectVar);
		template.setIri("p", axiom.getProperty().asOWLObjectProperty().toStringID());
		sparql += template.toString();
	}

	@Override
	public void visit(OWLFunctionalDataPropertyAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}

	@Override
	public void visit(OWLSubPropertyChainOfAxiom axiom) {
		VarGenerator varGenerator = new VarGenerator();
		
		List<OWLObjectPropertyExpression> propertyChain = axiom.getPropertyChain();
		String subjectVar = this.subjectVar;
		for (int i = 0; i < propertyChain.size() - 1; i++) {
			OWLObjectPropertyExpression propertyExpression = propertyChain.get(i);
			
			// new object var will be created
			String objectVar = varGenerator.newVar();
			
			sparql += subjectVar + render(propertyExpression) + objectVar + " .";
			
			// subject var becomes old object var
			subjectVar = objectVar;
		}
		sparql += subjectVar + render(propertyChain.get(propertyChain.size()-1)) + this.objectVar + " .";
		
		OWLObjectPropertyExpression superProperty = axiom.getSuperProperty();
		sparql += this.subjectVar + render(superProperty) + objectVar; 
	}

	@Override
	public void visit(OWLInverseObjectPropertiesAxiom axiom) {
	}
	
	@Override
	public void visit(SWRLRule rule) {
	}
	
	@Override
	public void visit(OWLHasKeyAxiom axiom) {
	}
	
	private String render(OWLObjectPropertyExpression propertyExpression){
		if(propertyExpression.isAnonymous()){
			return "^" + render(propertyExpression.getInverseProperty());
		} else {
			return "<" + propertyExpression.asOWLObjectProperty().toStringID() + ">";
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	//                                                                       //
	//       ABox axioms                                                     //
	//                                                                       //
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLDifferentIndividualsAxiom axiom) {
	}

	@Override
	public void visit(OWLObjectPropertyAssertionAxiom axiom) {
	}
	
	@Override
	public void visit(OWLClassAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLDataPropertyAssertionAxiom axiom) {
	}
	
	@Override
	public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
	}
	
	@Override
	public void visit(OWLSameIndividualAxiom axiom) {
	}

	@Override
	public void visit(OWLDatatypeDefinitionAxiom axiom) {
	}
	
	///////////////////////////////////////////////////////////////////////////
	//                                                                       //
	//       Annotation axioms                                               //
	//                                                                       //
	///////////////////////////////////////////////////////////////////////////

	@Override
	public void visit(OWLAnnotationAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {
	}

	@Override
	public void visit(OWLAnnotationPropertyDomainAxiom axiom) {
	}

	@Override
	public void visit(OWLAnnotationPropertyRangeAxiom axiom) {
	}

	@Override
	public void visit(OWLDeclarationAxiom axiom) {
	}
}

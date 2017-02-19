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

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import org.aksw.owl2sparql.style.AllQuantorTranslation;
import org.aksw.owl2sparql.style.EqualityRendering;
import org.aksw.owl2sparql.style.NominalTranslation;
import org.aksw.owl2sparql.style.OWLThingRendering;
import org.aksw.owl2sparql.util.OWLClassExpressionMinimizer;
import org.aksw.owl2sparql.util.VariablesMapping;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.util.*;
import java.util.stream.Collectors;

import static org.semanticweb.owlapi.model.ClassExpressionType.OBJECT_MAX_CARDINALITY;

/**
 * A converter from
 * <a href="http://www.w3.org/TR/owl2-syntax/#Class_Expressions">OWL 2 class
 * expressions</a> to SPARQL queries.
 * 
 * @author Lorenz Buehmann
 *
 */
public class OWLClassExpressionToSPARQLConverter implements OWLClassExpressionVisitor, OWLDataRangeVisitor{
	
	private static final Logger logger = LoggerFactory.getLogger(OWLClassExpressionToSPARQLConverter.class);
	
	private static final String TRIPLE_PATTERN = "%s %s %s .\n";
	
	private OWLDataFactory df = new OWLDataFactoryImpl();
	
	private Multimap<Integer, OWLEntity> properties = HashMultimap.create();
	
	private Map<Integer, Boolean> intersection;
	private Set<? extends OWLEntity> variableEntities = new HashSet<>();
	
	private VariablesMapping mapping = new VariablesMapping();
	
	private OWLClassExpression expr;
	
	private Deque<Integer> naryExpressions = new ArrayDeque<>();
	
	private OWLClassExpressionMinimizer minimizer = new OWLClassExpressionMinimizer(df);
	
	private boolean ignoreGenericTypeStatements = true;

	private boolean needOuterTriplePattern = true;
	
	private boolean useDistinct = true;
	
	private boolean useReasoning = false;
	
	private EqualityRendering equalityRendering = EqualityRendering.TERM_EQUALITY;
	
	private AllQuantorTranslation allQuantorTranslation = AllQuantorTranslation.DOUBLE_NEGATION;
	
	private OWLThingRendering owlThingRendering = OWLThingRendering.GENERIC_TRIPLE_PATTERN;

	private NominalTranslation nominalTranslation = NominalTranslation.VALUES;
	
	private String countVar = "?cnt";
	
	private String sparql = "";
	private String appendix = "";
	private Stack<String> variables = new Stack<>();
	
	private boolean negatedDataRange = false;

	private Deque<OWLClassExpression> existentialFillers = new ArrayDeque<>();
	
	public OWLClassExpressionToSPARQLConverter() {}
	
	public OWLClassExpressionToSPARQLConverter(VariablesMapping mapping) {
		this.mapping = mapping;
	}

	/**
	 * Converts an OWL class expression into a SPARQL query with
	 * <code>rootVariable</code> as projection variable. It's possible to
	 * return it as COUNT query if wanted.
	 *
	 * @param ce the OWL class expression to convert
	 * @param rootVariable the name of the projection variable in the SPARQL
	 *            query
	 * @param countQuery whether to return a SELECT (COUNT(?var) as ?cnt) query
	 * @return the SPARQL query
	 */
	public String convert(OWLClassExpression ce, String rootVariable, boolean countQuery){
		return createSelectClause(countQuery) + createWhereClause(ce) + createSolutionModifier();
	}
	
	/**
	 * Converts an OWL class expression into a SPARQL query with
	 * <code>rootVariable</code> as projection variable.
	 *
	 * @param ce the OWL class expression to convert
	 * @param rootVariable the name of the projection variable in the SPARQL
	 *            query
	 * @return the SPARQL query
	 */
	public Query asQuery(OWLClassExpression ce, String rootVariable){
		return asQuery(rootVariable, ce, Collections.<OWLEntity>emptySet());
	}
	
	/**
	 * Converts an OWL class expression into a SPARQL query with
	 * <code>rootVariable</code> as projection variable. It's possible to
	 * return it as COUNT query if wanted.
	 *
	 * @param ce the OWL class expression to convert
	 * @param rootVariable the name of the projection variable in the SPARQL
	 *            query
	 * @param countQuery whether to return a SELECT (COUNT(?var) as ?cnt) query
	 * @return the SPARQL query
	 */
	public Query asQuery(String rootVariable, OWLClassExpression ce, boolean countQuery){
		String queryString = convert(ce, rootVariable, countQuery);
		
		return QueryFactory.create(queryString, Syntax.syntaxARQ);
	}
	
	/**
	 * Converts an OWL class expression into a SPARQL query with
	 * <code>rootVariable</code> as projection variable. It's possible to
	 * declare a set of OWL entities which will be replaced by variables
	 * in the query, that are then additionally used as projection variables
	 * and grouping variables.
	 *
	 * @param ce the OWL class expression to convert
	 * @param rootVariable the name of the projection variable in the SPARQL
	 *            query
	 * @param variableEntities a set of entities that are replaced by variables
	 * @return the SPARQL query
	 */
	public Query asQuery(String rootVariable, OWLClassExpression ce, Set<? extends OWLEntity> variableEntities){
		return asQuery(rootVariable, ce, variableEntities, false);
	}
	
	/**
	 * Converts an OWL class expression into a SPARQL query with
	 * <code>rootVariable</code> as projection variable. It's possible to
	 * declare a set of OWL entities which will be replaced by variables
	 * in the query, that are then additionally used as projection variables
	 * and grouping variables. Moreover, it's possible to
	 * return a COUNT query if wanted.
	 *
	 * @param ce the OWL class expression to convert
	 * @param rootVariable the name of the projection variable in the SPARQL
	 *            query
	 * @param variableEntities a set of entities that are replaced by variables
	 * @param countQuery whether to return a SELECT (COUNT(?var) as ?cnt) query
	 * @return the SPARQL query
	 */
	public Query asQuery(String rootVariable, OWLClassExpression ce, Set<? extends OWLEntity> variableEntities, boolean countQuery){
		this.variableEntities = variableEntities;
		
		String queryString = "SELECT DISTINCT ";
		
		String triplePattern = asGroupGraphPattern(ce, rootVariable, true);
		
		if(variableEntities.isEmpty()){
			queryString += rootVariable + " WHERE {";
		} else {
			for (OWLEntity owlEntity : variableEntities) {
				String var = mapping.get(owlEntity);
				queryString += var + " ";
			}
			if(countQuery){
				queryString += "(COUNT(DISTINCT " + rootVariable + ") AS ?cnt)"; 
			} else {
				queryString += rootVariable;
			}
			queryString += " WHERE {";
		}
		
		queryString += triplePattern;
		queryString += "}";

		if (countQuery && !variableEntities.isEmpty()) {
			queryString += "GROUP BY ";
			for (OWLEntity owlEntity : variableEntities) {
				String var = mapping.get(owlEntity);
				queryString += var;
			}
			queryString += " ORDER BY DESC(?cnt)";
		}
		queryString += appendix;
		logger.debug("Query: {}", queryString);
		return QueryFactory.create(queryString, Syntax.syntaxSPARQL_11);
	}
	
	/**
	 * Converts an OWL class expression into a GroupGraphPattern, which can be described 
	 * as the outer-most graph pattern in a query, sometimes also called the query pattern.
	 * @param ce the OWL class expression
	 * @param rootVariable  the name of the projection variable
	 * @return a SPARQL graph pattern
	 */
	public String asGroupGraphPattern(OWLClassExpression ce, String rootVariable){
		return asGroupGraphPattern(ce, rootVariable, false);
	}
	
	/**
	 * Converts an OWL class expression into a GroupGraphPattern, which can be described 
	 * as the outer-most graph pattern in a query, sometimes also called the query pattern.
	 * @param ce the OWL class expression
	 * @param rootVariable the name of the projection variable
	 * @param needOuterTriplePattern whether 
	 * @return a SPARQL graph pattern
	 */
	public String asGroupGraphPattern(OWLClassExpression ce, String rootVariable, boolean needOuterTriplePattern){
		this.needOuterTriplePattern = needOuterTriplePattern;
		reset();
		variables.push(rootVariable);
		
		// minimize the class expression
		this.expr = minimizer.minimizeClone(ce);
		if(!expr.equals(ce)) {
			logger.info("CE rewritten to " + expr);
		}

		if(expr.equals(df.getOWLThing())) {
			logger.warn("Expression is logically equivalent to owl:Thing, thus, the SPARQL query returns all triples.");
		}
		
		// convert
		expr.accept(this);
		
		return sparql;
	}
	
	/**
	 * Converts an OWL data range into a GroupGraphPattern.
	 * @param dr the OWL data range
	 * @param rootVariable the name of the projection variable
	 * @return a SPARQL graph pattern
	 */
	public String asGroupGraphPattern(OWLDataRange dr, String rootVariable) 
	{
		reset();
		variables.push(rootVariable);
		
		// convert
		dr.accept(this);
		
		return sparql;
	}

	/**
	 * Whether to return SPARQL queries with DISTINCT keyword.
	 * @param useDistinct <code>true</code> if use DISTINCT, otherwise <code>false</code>
	 */
	public void setUseDistinct(boolean useDistinct) {
		this.useDistinct = useDistinct;
	}
	
	/**
	 * Since SPARQL 1.1 there is a mechanism called property
	 * paths (see <a href="http://www.w3.org/TR/sparql11-query/#propertypaths">W3C rec.</a>),
	 * which allows to add some kind of light-weight inferencing to a SPARQL query.
	 * 
	 * Currently, we do the following if enabled
	 * 
	 * <ul>
	 * <li>?s rdf:type :A . -&gt; ?s rdf:type/(rdfs:subClassOf|owl:equivalentClass)* :A . </li>
	 * </ul>
	 * 
	 * Note, this feature only works on query engines that support SPARQL 1.1 . 
	 *
	 * @param useReasoning use inferencing
	 */
	public void setUseReasoning(boolean useReasoning) {
		this.useReasoning = useReasoning;
	}
	
	/**
	 * How to express equality in SPARQL.
	 * 
	 * @param equalityRendering the equalityRendering to set
	 */
	public void setEqualityRendering(EqualityRendering equalityRendering) {
		this.equalityRendering = equalityRendering;
	}
	
	/**
	 * How to translate <code>owl:allValuesFrom</code> into SPARQL.
	 * 
	 * @param allQuantorTranslation the allQuantorTranslation to set
	 */
	public void setAllQuantorTranslation(AllQuantorTranslation allQuantorTranslation) {
		this.allQuantorTranslation = allQuantorTranslation;
	}
	
	/**
	 * How to express <code>owl:Thing</code> in SPARQL.
	 * @param owlThingRendering the owlThingRendering to set
	 */
	public void setOwlThingRendering(OWLThingRendering owlThingRendering) {
		this.owlThingRendering = owlThingRendering;
	}
	
	public VariablesMapping getVariablesMapping() {
		return mapping;
	}
	
	private String createSelectClause(boolean countQuery) {
		return "SELECT " + (countQuery ? "(COUNT(" : "") + (useDistinct ? " DISTINCT " : "")  + variables.firstElement() + (countQuery ? " AS " + countVar + ")" : "");
	}
	
	private String createWhereClause(OWLClassExpression ce){
		return " WHERE " + createGroupGraphPattern(ce);
	}
	
	private String createGroupGraphPattern(OWLClassExpression ce) {
		ce.accept(this);
		return "{" + sparql + "}";
	}

//	private OWLClassExpression rewrite(OWLClassExpression ce) {
//
//	}
	
	private String createSolutionModifier() {
		return appendix;
	}
	
	private void reset(){
		variables.clear();
		properties.clear();
		sparql = "";
		appendix = "";
		intersection = new HashMap<>();
		mapping.reset();
	}
	
	private int modalDepth(){
		return variables.size();
	}
	
	/**
	 * Checks whether the intersection contains at least one operand that
	 * is not a negation.
	 * @param intersection the intersection
	 * @return whether the intersection contains at least one operand that
	 * is not a negation.
	 */
	private boolean containsNonNegationOperand(OWLObjectIntersectionOf intersection){
		for (OWLClassExpression op : intersection.getOperands()) {
			if(op.getClassExpressionType() != ClassExpressionType.OBJECT_COMPLEMENT_OF){
				return true;
			}
		}
		return false;
	}
	
	private boolean inIntersection(){
		return intersection.containsKey(modalDepth()) ? intersection.get(modalDepth()) : false;
	}
	
	private void enterIntersection(){
		naryExpressions.push(1);
		intersection.put(modalDepth(), true);
	}
	
	private void leaveIntersection(){
		naryExpressions.pop();
		intersection.remove(modalDepth());
	}
	
	private String asTriplePattern(String subject, String predicate, String object){
		return String.format(TRIPLE_PATTERN, subject, predicate, object);
	}
	
	private String asTriplePattern(String subject, OWLEntity predicate, OWLEntity object){
		return asTriplePattern(subject, render(predicate), render(object));
	}
	
	private String asTriplePattern(String subject, OWLEntity predicate, String object){
		return asTriplePattern(subject, render(predicate), object);
	}
	
	private String asTriplePattern(String subject, OWLEntity predicate, OWLLiteral object){
		return asTriplePattern(subject, render(predicate), render(object));
	}
	
	private String genericTriplePattern(){
//		BasicPattern bgp = new BasicPattern();
//		bgp.add(Triple.create(NodeFactory.createVariable("s"), NodeFactory.createVariable("s"), NodeFactory.createVariable("s")));
//		System.out.println(FormatterElement.asString(new ElementTriplesBlock(bgp)));
		return variables.peek() + " ?p ?o .";
	}
	
	private String typeTriplePattern(String var, String type){
		return var + (useReasoning ? " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>/<http://www.w3.org/2000/01/rdf-schema#subClassOf>* " : " a ") + type + " .\n";
	}
	
	private String equalExpressions(String expr1, String expr2, boolean negated){
		return (equalityRendering == EqualityRendering.TERM_EQUALITY) ?
				(negated ? "!" : "") + "sameTerm(" + expr1 + ", " + expr2 + ")" :
					expr1 + (negated ? " != " : " = ") + expr2;
	}
	
	private String filter(String expr){
		return "FILTER(" + expr + ")";
	}
	
	private String render(OWLEntity entity){
		String s;
		if(variableEntities.contains(entity)){
			s = mapping.getVariable(entity);
		} else {
			s = "<" + entity.toStringID() + ">";
		}
		if(entity.isOWLObjectProperty()){
			properties.put(modalDepth(), entity);
		}
		return s;
	}
	
	private String render(OWLLiteral literal){
		return "\"" + literal.getLiteral() + "\"^^<" + literal.getDatatype().toStringID() + ">";
	}

	@Override
	public void visit(OWLClass ce) {
		if(ce.equals(expr) || (ignoreGenericTypeStatements && !ce.isOWLThing())){
			if(!ce.isOWLThing() || owlThingRendering == OWLThingRendering.EXPLICIT){
				sparql += typeTriplePattern(variables.peek(), render(ce));
			} else {
				sparql += asTriplePattern(variables.peek(), "?p", "?o");
			}
		}
	}

	@Override
	public void visit(OWLObjectIntersectionOf ce) {
		// if all operands are negated, we have to add a generic triple
		if(!containsNonNegationOperand(ce) && needOuterTriplePattern){
			sparql += genericTriplePattern();
		}
		
		enterIntersection();
		List<OWLClassExpression> operands = ce.getOperandsAsList();
		for (OWLClassExpression operand : operands) {
			operand.accept(this);
		}
		Collection<OWLEntity> props = properties.get(modalDepth());
		if(props.size() > 1){
			Collection<String> vars = new TreeSet<>();
			for (OWLEntity p : props) {
				if(mapping.containsKey(p)){
					vars.add(mapping.get(p));
				}
			}
			if(vars.size() == 2){
				List<String> varList = new ArrayList<>(vars);
				sparql += filter(equalExpressions(varList.get(0), varList.get(1), true));
			}
		}
		leaveIntersection();
	}

	@Override
	public void visit(OWLObjectUnionOf ce) {
		naryExpressions.push(0);
		List<OWLClassExpression> operands = ce.getOperandsAsList();
		for (int i = 0; i < operands.size()-1; i++) {
			sparql += "{";
			operands.get(i).accept(this);
			sparql += "}";
			sparql += " UNION ";
		}
		sparql += "{";
		operands.get(operands.size()-1).accept(this);
		sparql += "}";
		naryExpressions.pop();
	}
	
	private boolean inUnion() {
		return !naryExpressions.isEmpty() && naryExpressions.peek().equals(0);
	}

	@Override
	public void visit(OWLObjectComplementOf ce) {
		if((!inIntersection() &&
//				modalDepth() == 1 &&
				needOuterTriplePattern && (existentialFillers.isEmpty() || !existentialFillers.peek().equals(ce))) || inUnion()){
			sparql += genericTriplePattern();
		}

		sparql += "FILTER NOT EXISTS {";
		ce.getOperand().accept(this);
		sparql += "}";
	}

	@Override
	public void visit(OWLObjectSomeValuesFrom ce) {
		String objectVariable = mapping.newIndividualVariable();
		OWLObjectPropertyExpression propertyExpression = ce.getProperty();
		if(propertyExpression.isAnonymous()){
			//property expression is inverse of a property
			sparql += asTriplePattern(objectVariable, propertyExpression.getNamedProperty(), variables.peek());
		} else {
			sparql += asTriplePattern(variables.peek(), propertyExpression.getNamedProperty(), objectVariable);
		}
		OWLClassExpression filler = ce.getFiller();
		existentialFillers.push(filler);
		variables.push(objectVariable);
		filler.accept(this);
		variables.pop();
		existentialFillers.pop();
//		if(filler.isAnonymous()){
//			variables.push(objectVariable);
//			filler.accept(this);
//			variables.pop();
//		} else {
//			sparql += triple(objectVariable, "a", filler.asOWLClass());
//		}
		
	}
	
	private boolean isTrivialConcept(OWLClassExpression ce) {
		return ce.isOWLThing()
				|| (ce.getClassExpressionType() == ClassExpressionType.OBJECT_ALL_VALUES_FROM && isTrivialConcept(((OWLObjectAllValuesFrom) ce)
						.getFiller()));
	}

	@Override
	public void visit(OWLObjectAllValuesFrom ce) {
		OWLClassExpression filler = ce.getFiller();
		
		String subject = variables.peek();
		String objectVariable = mapping.newIndividualVariable();
		
		if(isTrivialConcept(filler)) { 
			// \forall r.\top is trivial, as everything belongs to that concept
			// thus, we can omit it if it's used in a conjunction or as complex filler
			if(!inIntersection()) {
				sparql += asTriplePattern(subject, mapping.newPropertyVariable(), objectVariable);
			}
		} else {
			// we can either use double negation on \forall r.A such that we have a logically
			// equivalent expression \neg \exists r.\neg A
			// or we use subselects get the individuals whose r successors are only of type A
			if(allQuantorTranslation == AllQuantorTranslation.DOUBLE_NEGATION){
				OWLObjectComplementOf doubleNegatedExpression = df.getOWLObjectComplementOf(
						df.getOWLObjectSomeValuesFrom(
								ce.getProperty(), 
								df.getOWLObjectComplementOf(ce.getFiller())));
				doubleNegatedExpression.accept(this);
			} else {
				if(!inIntersection()) {
					sparql += asTriplePattern(subject, mapping.newPropertyVariable(), objectVariable);
				}

				OWLObjectPropertyExpression propertyExpression = ce.getProperty();
				OWLObjectProperty predicate = propertyExpression.getNamedProperty();
				if(propertyExpression.isAnonymous()){
					//property expression is inverse of a property
					sparql += asTriplePattern(objectVariable, predicate, variables.peek());
				} else {
					sparql += asTriplePattern(variables.peek(), predicate, objectVariable);
				}
				
				String var = mapping.newIndividualVariable();
				sparql += "{SELECT " + subject + " (COUNT(" + var + ") AS ?cnt1) WHERE {";
				sparql += asTriplePattern(subject, predicate, var);
				variables.push(var);
				filler.accept(this);
				variables.pop();
				sparql += "} GROUP BY " + subject + "}";
				
				var = mapping.newIndividualVariable();
				sparql += "{SELECT " + subject + " (COUNT(" + var + ") AS ?cnt2) WHERE {";
				sparql += asTriplePattern(subject, predicate, var);
				sparql += "} GROUP BY " + subject + "}";
				
				sparql += filter("?cnt1=?cnt2");
			}
		}
	}

	@Override
	public void visit(OWLObjectHasValue ce) {
		OWLObjectPropertyExpression propertyExpression = ce.getProperty();
		OWLNamedIndividual value = ce.getFiller().asOWLNamedIndividual();
		if(propertyExpression.isAnonymous()){
			//property expression is inverse of a property
			sparql += asTriplePattern(value.toStringID(), propertyExpression.getNamedProperty(), variables.peek());
		} else {
			sparql += asTriplePattern(variables.peek(), propertyExpression.getNamedProperty(), value);
		}
	}

	@Override
	public void visit(OWLObjectMinCardinality ce) {
		processObjectCardinalityRestriction(ce, ">=");
	}

	@Override
	public void visit(OWLObjectExactCardinality ce) {
		processObjectCardinalityRestriction(ce, "=");
	}

	@Override
	public void visit(OWLObjectMaxCardinality ce) {
		processObjectCardinalityRestriction(ce, "<=");
	}

	private void processObjectCardinalityRestriction(OWLObjectCardinalityRestriction ce, String operator) {
		String subjectVariable = variables.peek();
		String objectVariable = mapping.newIndividualVariable();

		int cardinality = ce.getCardinality();

		boolean maxOneCardinalityAsFilterNotExists = true;
		boolean useMaxCardOptimization = ce.getClassExpressionType() ==  OBJECT_MAX_CARDINALITY && cardinality == 1 && maxOneCardinalityAsFilterNotExists;

		if(useMaxCardOptimization){
			// process the restriction once with the filler variable ?o1
			processPropertyRestriction(ce, subjectVariable, objectVariable);

			sparql += "FILTER NOT EXISTS {";

			// we need a second filler variable ?o2
			String objectVariable2 = mapping.newIndividualVariable();

			// process the restriction again with the new filler variable
			processPropertyRestriction(ce, subjectVariable, objectVariable2);

			// add FILTER(?o1 != ?o2)
			sparql += filter(equalExpressions(objectVariable, objectVariable2, true));
			sparql += "}";
		} else {
			if(!useMaxCardOptimization && (inIntersection() || modalDepth() > 1)){
				sparql += "{SELECT " + subjectVariable + " WHERE {";
			}

			processPropertyRestriction(ce, subjectVariable, objectVariable);

			String grouping = " GROUP BY " + subjectVariable + " HAVING(COUNT(" + objectVariable + ")" + operator + cardinality + ")";
			if(inIntersection() || modalDepth() > 1){
				sparql += "}" + grouping + "}";
			} else {
				appendix += grouping;
			}
		}
	}

	private void processPropertyRestrictionFiller(String objectVariable, OWLClassExpression filler) {
		variables.push(objectVariable);
		filler.accept(this);
		variables.pop();
	}

	private <R extends OWLObjectRestriction & OWLQuantifiedRestriction<OWLClassExpression>> void processPropertyRestriction(
			R ce, String subjectVariable, String objectVariable) {
		OWLObjectPropertyExpression pe = ce.getProperty();

		if (pe.isAnonymous()) {// property expression is inverse of a property
			sparql += asTriplePattern(objectVariable, pe.getNamedProperty(), subjectVariable);
		} else {
			sparql += asTriplePattern(subjectVariable, pe.getNamedProperty(), objectVariable);
		}

		// process the filler
		processPropertyRestrictionFiller(objectVariable, ce.getFiller());
	}

	@Override
	public void visit(OWLObjectHasSelf ce) {
		String subject = variables.peek();
		OWLObjectPropertyExpression property = ce.getProperty();
		sparql += asTriplePattern(subject, property.getNamedProperty(), subject);
	}

	@Override
	public void visit(OWLObjectOneOf ce) {
		String subject = variables.peek();
		String valuesString = Joiner
				.on(nominalTranslation == NominalTranslation.FILTER_IN ? "," : " ")
				.join(ce.getIndividuals().stream()
							  .map(ind -> "<" + ind.toStringID() + ">")
							  .collect(Collectors.toList()));
		if(nominalTranslation == NominalTranslation.FILTER_IN) {
			if(modalDepth() == 1){
				sparql += genericTriplePattern();
			}
			sparql += "FILTER(" + subject + " IN (" + valuesString + "))";
		} else if(nominalTranslation == NominalTranslation.VALUES){
			sparql += "VALUES " + subject + " {" + valuesString + "}";
		} else {
			throw new RuntimeException(nominalTranslation + " not implemented yet");
		}
	}

	@Override
	public void visit(OWLDataSomeValuesFrom ce) {
		String objectVariable = mapping.newIndividualVariable();
		OWLDataPropertyExpression propertyExpression = ce.getProperty();
		sparql += asTriplePattern(variables.peek(), propertyExpression.asOWLDataProperty(), objectVariable);
		OWLDataRange filler = ce.getFiller();
		variables.push(objectVariable);
		processDataRange(filler);
		variables.pop();
	}

	@Override
	public void visit(OWLDataAllValuesFrom ce) {
		OWLDataRange filler = ce.getFiller();

		String subject = variables.peek();
		String objectVariable = mapping.newIndividualVariable();

		// we can either use double negation on \forall r.A such that we have a logically
		// equivalent expression \neg \exists r.\neg A
		// or we use subselects get the individuals whose r successors are only of type A
		if(allQuantorTranslation == AllQuantorTranslation.DOUBLE_NEGATION){
			OWLObjectComplementOf doubleNegatedExpression = df.getOWLObjectComplementOf(
					df.getOWLDataSomeValuesFrom(
							ce.getProperty(),
							df.getOWLDataComplementOf(ce.getFiller())));
			doubleNegatedExpression.accept(this);
		} else {
			if(!inIntersection()) {
				sparql += asTriplePattern(subject, mapping.newPropertyVariable(), objectVariable);
			}

			OWLDataPropertyExpression propertyExpression = ce.getProperty();
			OWLDataProperty predicate = propertyExpression.asOWLDataProperty();
			if(propertyExpression.isAnonymous()){
				//property expression is inverse of a property
				sparql += asTriplePattern(objectVariable, predicate, variables.peek());
			} else {
				sparql += asTriplePattern(variables.peek(), predicate, objectVariable);
			}

			String var = mapping.newIndividualVariable();
			sparql += "{SELECT " + subject + " (COUNT(" + var + ") AS ?cnt1) WHERE {";
			sparql += asTriplePattern(subject, predicate, var);
			variables.push(var);
			filler.accept(this);
			variables.pop();
			sparql += "} GROUP BY " + subject + "}";

			var = mapping.newIndividualVariable();
			sparql += "{SELECT " + subject + " (COUNT(" + var + ") AS ?cnt2) WHERE {";
			sparql += asTriplePattern(subject, predicate, var);
			sparql += "} GROUP BY " + subject + "}";

			sparql += filter("?cnt1=?cnt2");
		}
	}

	@Override
	public void visit(OWLDataHasValue ce) {
		OWLDataPropertyExpression propertyExpression = ce.getProperty();
		OWLLiteral value = ce.getFiller();
		sparql += asTriplePattern(variables.peek(), propertyExpression.asOWLDataProperty(), value);
	}

	@Override
	public void visit(OWLDataMinCardinality ce) {
		processDataCardinality(ce, ">=");
	}

	@Override
	public void visit(OWLDataExactCardinality ce) {
		processDataCardinality(ce, "=");
	}

	@Override
	public void visit(OWLDataMaxCardinality ce) {
		processDataCardinality(ce, "<=");
	}

	private void processDataCardinality(OWLDataCardinalityRestriction ce, String operator) {
		String subjectVariable = variables.peek();
		String objectVariable = mapping.newIndividualVariable();
		OWLDataPropertyExpression propertyExpression = ce.getProperty();
		int cardinality = ce.getCardinality();
		sparql += "{SELECT " + subjectVariable + " WHERE {";
		sparql += asTriplePattern(subjectVariable, propertyExpression.asOWLDataProperty(), objectVariable);
		OWLDataRange filler = ce.getFiller();
		variables.push(objectVariable);
		processDataRange(filler);
		variables.pop();

		sparql += "} GROUP BY " + subjectVariable + " HAVING(COUNT(" + objectVariable + ")" + operator + cardinality + ")}";
	}
	
	private void processDataRange(OWLDataRange dataRange) {
		sparql += "FILTER(";
		dataRange.accept(this);
		sparql += ")";
	}
	
	@Override
	public void visit(OWLDatatype node) {
		if (ignoreGenericTypeStatements && !node.isRDFPlainLiteral() && !node.isTopDatatype()) {
			sparql += 
					String.format("DATATYPE(%s) %s %s", 
						variables.peek(),
						(negatedDataRange ? "!=" : "="),
						node.getIRI().toQuotedString());
		}
	}

	@Override
	public void visit(OWLDataOneOf node) {
		String subject = variables.peek();
		if(modalDepth() == 1){
			sparql += genericTriplePattern();
		}
		sparql += subject + (negatedDataRange ? " NOT " : "") + " IN (";
		String values = "";
		for (OWLLiteral value : node.getValues()) {
			if(!values.isEmpty()){
				values += ",";
			}
			values += render(value);
		}
		sparql += values;
		sparql +=  ")";
	}

	@Override
	public void visit(OWLDataComplementOf node) {
		negatedDataRange = true;
		node.getDataRange().accept(this);
		negatedDataRange = false;
	}

	@Override
	public void visit(OWLDataIntersectionOf node) {
		Iterator<OWLDataRange> iterator = node.getOperands().iterator();
		while(iterator.hasNext()) {
			iterator.next().accept(this);
			if(iterator.hasNext()) {
				sparql += " && ";
			}
		}
	}

	@Override
	public void visit(OWLDataUnionOf node) {
		Iterator<OWLDataRange> iterator = node.getOperands().iterator();
		while(iterator.hasNext()) {
			iterator.next().accept(this);
			if(iterator.hasNext()) {
				sparql += " || ";
			}
		}
	}

	@Override
	public void visit(OWLDatatypeRestriction node) {
		String subject = variables.peek();

		for (Iterator<OWLFacetRestriction> iterator = node.getFacetRestrictions().iterator(); iterator.hasNext();) {
			OWLFacetRestriction fr = iterator.next();
			
			OWLFacet facet = fr.getFacet();
			OWLLiteral value = fr.getFacetValue();
			String valueString = value.getLiteral();
			
			switch(facet) {
				case LENGTH: sparql += String.format("STRLEN(STR(%s) = %d)", subject, value.parseInteger());
					break;
				case MIN_LENGTH: sparql += String.format("STRLEN(STR(%s) >= %d)", subject, value.parseInteger());
					break;
				case MAX_LENGTH: sparql += String.format("STRLEN(STR(%s) <= %d)", subject, value.parseInteger());
					break;
				case PATTERN: sparql += String.format("REGEX(STR(%s), %d)", subject, value.parseInteger());
					break;
				case LANG_RANGE:
					break;
				case MAX_EXCLUSIVE: sparql += subject + "<" + valueString;
					break;
				case MAX_INCLUSIVE: sparql += subject + "<=" + valueString;
					break;
				case MIN_EXCLUSIVE: sparql += subject + ">" + valueString;
					break;
				case MIN_INCLUSIVE: sparql += subject + ">=" + valueString;
					break;
				case FRACTION_DIGITS:
					break;
				case TOTAL_DIGITS:
					break;
				default:
					break;
			
			}
			
			if(iterator.hasNext()) {
				sparql += " && ";
			}
		}
		
		sparql += " && datatype(" + subject + ")=<" + node.getDatatype().toStringID() + ">";
	}
	
	public static void main(String[] args) throws Exception {
		ToStringRenderer.getInstance().setRenderer(new DLSyntaxObjectRenderer());
		OWLClassExpressionToSPARQLConverter converter = new OWLClassExpressionToSPARQLConverter();
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();
		PrefixManager pm = new DefaultPrefixManager();
		pm.setDefaultPrefix("http://dbpedia.org/ontology/");
		
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
		
		OWLClassExpression expr = clsA;
		String query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectSomeValuesFrom(propR, clsB);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				df.getOWLObjectSomeValuesFrom(propR, clsB),
				clsB);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectUnionOf(
				clsA,
				clsB);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectHasValue(propR, indA);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectAllValuesFrom(propR, df.getOWLThing());
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectAllValuesFrom(propR, df.getOWLObjectAllValuesFrom(propS, df.getOWLThing()));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectAllValuesFrom(
				propR, 
				df.getOWLObjectIntersectionOf(
						clsA,
						df.getOWLObjectAllValuesFrom(propS, df.getOWLThing())));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectAllValuesFrom(
				propR, 
				df.getOWLObjectUnionOf(
						clsA,
						df.getOWLObjectAllValuesFrom(propS, df.getOWLThing())));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectAllValuesFrom(propR, clsB);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectAllValuesFrom(df.getOWLObjectProperty("language", pm), df.getOWLClass("Language", pm));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectMinCardinality(2, df.getOWLObjectProperty("language", pm), df.getOWLClass("Language", pm));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				df.getOWLClass("Place", pm),
				df.getOWLObjectMinCardinality(
						2, 
						df.getOWLObjectProperty("language", pm), 
						df.getOWLClass("Language", pm)));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectOneOf(indA, indB);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectSomeValuesFrom(propR, df.getOWLObjectOneOf(indA, indB));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				clsA, 
				df.getOWLObjectHasSelf(propR));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				clsA, 
				df.getOWLDataSomeValuesFrom(dpT, booleanRange));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				clsA, 
				df.getOWLDataHasValue(dpT, lit));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				clsA, 
				df.getOWLDataMinCardinality(2, dpT, booleanRange));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectComplementOf(clsB);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				clsA, 
				df.getOWLObjectComplementOf(clsB));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectSomeValuesFrom(propR, 
				df.getOWLObjectIntersectionOf(
						clsA, 
						df.getOWLObjectComplementOf(clsB)));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLDataAllValuesFrom(dpT, booleanRange);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLDataAllValuesFrom(dpT,df.getOWLDataOneOf(lit));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		//variable entity
		expr = df.getOWLObjectIntersectionOf(
				df.getOWLObjectSomeValuesFrom(propR, clsB),
				clsB, df.getOWLObjectSomeValuesFrom(propS, clsA));
		query = converter.asQuery(rootVar, expr, Sets.newHashSet(propR, propS)).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				df.getOWLObjectSomeValuesFrom(
						propR, 
						df.getOWLObjectIntersectionOf(
								df.getOWLObjectSomeValuesFrom(propS, clsA),
								clsC)),
				clsB);
		query = converter.asQuery(rootVar, expr, Sets.newHashSet(propR, propS)).toString();
		System.out.println(expr + "\n" + query);
		
		
		expr = df.getOWLObjectIntersectionOf(
					df.getOWLObjectComplementOf(clsA),
					df.getOWLObjectSomeValuesFrom(
							propR, 
							df.getOWLObjectSomeValuesFrom(
									propS,
									df.getOWLObjectComplementOf(clsB)
							)
					)
				);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectUnionOf(
				df.getOWLObjectComplementOf(clsA),
				df.getOWLObjectComplementOf(clsB)
			);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
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
			);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		Op op = Algebra.compile(converter.asQuery(expr, rootVar));
		System.out.println(op);
		
		expr = df.getOWLObjectIntersectionOf(
				clsA,
				df.getOWLObjectComplementOf(
					df.getOWLObjectSomeValuesFrom(
						propR, 
						clsB
					)
				)
			);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		expr = df.getOWLObjectIntersectionOf(
				clsA,
				df.getOWLDataSomeValuesFrom(dpT, df.getOWLDataComplementOf(booleanRange))
			);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		// data one of
		expr = df.getOWLDataSomeValuesFrom(dpT,df.getOWLDataOneOf(df.getOWLLiteral(1), df.getOWLLiteral(2)));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		// data not one of
		expr = df.getOWLDataSomeValuesFrom(
				dpT,
				df.getOWLDataComplementOf(
						df.getOWLDataOneOf(df.getOWLLiteral(1), df.getOWLLiteral(2))));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		// data intersection
		expr = df.getOWLDataSomeValuesFrom(
				dpT,
				df.getOWLDataIntersectionOf(
						df.getBooleanOWLDatatype(), 
						df.getOWLDataOneOf(df.getOWLLiteral(1), df.getOWLLiteral(2))));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		// data union
		expr = df.getOWLDataSomeValuesFrom(
				dpT, 
				df.getOWLDataUnionOf(
						df.getIntegerOWLDatatype(),
						df.getOWLDataIntersectionOf(
								df.getBooleanOWLDatatype(),
								df.getOWLDataOneOf(df.getOWLLiteral(1), df.getOWLLiteral(2)))));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		// data restriction
		expr = df.getOWLDataSomeValuesFrom(
				dpT, 
				df.getOWLDatatypeRestriction(
						df.getIntegerOWLDatatype(), 
						df.getOWLFacetRestriction(
								OWLFacet.MAX_EXCLUSIVE, df.getOWLLiteral(10)),
						df.getOWLFacetRestriction(
								OWLFacet.MIN_INCLUSIVE, df.getOWLLiteral(3))));
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
		
		// data restriction
				expr = df.getOWLDataSomeValuesFrom(
						dpT, 
						df.getOWLDatatypeRestriction(
								df.getRDFPlainLiteral(),
								df.getOWLFacetRestriction(
										OWLFacet.LENGTH, df.getOWLLiteral(10))));
				query = converter.asQuery(expr, rootVar).toString();
				System.out.println(expr + "\n" + query);

		expr = df.getOWLObjectMaxCardinality(1, propR, clsA);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);

		expr = df.getOWLObjectMaxCardinality(0, propR, clsA);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);

		expr = df.getOWLObjectComplementOf(clsA);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);

		expr = df.getOWLObjectAllValuesFrom(propR, clsA);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);

		expr = df.getOWLObjectExactCardinality(2, propR, clsA);
		query = converter.asQuery(expr, rootVar).toString();
		System.out.println(expr + "\n" + query);
	}
}
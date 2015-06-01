package org.aksw.owl2sparql.util;

import java.util.HashMap;

import org.semanticweb.owlapi.model.OWLEntity;

public class VariablesMapping extends HashMap<OWLEntity, String>{

	private VarGenerator classVarGenerator = new VarGenerator("cls");
	private VarGenerator propertyVarGenerator = new VarGenerator("p");
	private VarGenerator individualVarGenerator = new VarGenerator("s");
	
	/**
	 * @param entity
	 * @return the already used variable for the given entity or creates a new one
	 */
	public String getVariable(OWLEntity entity){
		String var = get(entity);
		
		// create fresh variable
		if(var == null){
			if(entity.isOWLClass()){
				var = classVarGenerator.newVar();
			} else if(entity.isOWLObjectProperty() || entity.isOWLDataProperty()){
				var = propertyVarGenerator.newVar();
			} else if(entity.isOWLNamedIndividual()){
				var = individualVarGenerator.newVar();
			} 
			put(entity, var);
		}
		return var;
	}
	
	/**
	 * @return a fresh variable used in subject/object position.
	 */
	public String newIndividualVariable(){
		return individualVarGenerator.newVar();
	}
	
	/**
	 * @return a fresh variable used in predicate position.
	 */
	public String newPropertyVariable(){
		return propertyVarGenerator.newVar();
	}
	
	public void reset(){
		clear();
		classVarGenerator.reset();
		propertyVarGenerator.reset();
		individualVarGenerator.reset();
	}
}

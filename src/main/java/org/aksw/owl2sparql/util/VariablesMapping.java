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
package org.aksw.owl2sparql.util;

import java.util.HashMap;

import org.semanticweb.owlapi.model.OWLEntity;

public class VariablesMapping extends HashMap<OWLEntity, String>{

	private VarGenerator classVarGenerator = new VarGenerator("cls");
	private VarGenerator propertyVarGenerator = new VarGenerator("p");
	private VarGenerator individualVarGenerator = new VarGenerator("s");
	
	/**
	 * Returns the already used variable for the given entity or creates a new one.
	 * @param entity the OWL entity
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
	
	/**
	 * Reset all settings.
	 */
	public void reset(){
		clear();
		classVarGenerator.reset();
		propertyVarGenerator.reset();
		individualVarGenerator.reset();
	}
}

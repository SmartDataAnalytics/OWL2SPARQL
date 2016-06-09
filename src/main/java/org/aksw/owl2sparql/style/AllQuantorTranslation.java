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
package org.aksw.owl2sparql.style;

/**
 * How to express a universal restriction, i.e. <code>owl:allValuesFrom</code> in SPARQL.
 * 
 * @author Lorenz Buehmann
 *
 */
public enum AllQuantorTranslation {
	/**
	 * Use double negation, e.g.
	 * <div>
	 * <pre>
	 * FILTER NOT EXISTS { 
	 * 	?s :p ?o . 
	 * 	FILTER NOT EXISTS { ?o a :Class }
	 * }
	 * </pre> 
	 * </div>
	 */
	DOUBLE_NEGATION, 
	/**
	 * Use two sub-selects and compare its values, e.g.
	 * <div>
	 * <pre>
	 * ?s ?p ?o .
	 * {SELECT (COUNT(*) AS ?cnt1) WHERE { ?s :p ?o } }
	 * {SELECT (COUNT(*) AS ?cnt1) WHERE { ?s :p ?o . ?o a :Class } }
	 * FILTER (?cnt1 = ?cnt2)
	 * </pre> 
	 * </div>
	 */
	SUBSELECT_COUNT_EQUALS
}
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
 * How to render equality in SPARQL.
 * <ul>
 * <li>{@link #VALUE_EQUALTIY}</li>
 * <li>{@link #TERM_EQUALITY}</li>
 * </ul>
 * @author Lorenz Buehmann
 *
 */
public enum EqualityRendering {
	
	/**
	 * Value equality uses the = operator, e.g.
	 * <code>FILTER(?x = 1)</code> .
	 */
	VALUE_EQUALTIY, 
	/**
	 * Term equality uses the SAMETERM function, e.g. 
	 * <code>FILTER(SAMETERM(?x, 1))</code> .
	 * <p>
	 * Term equality only returns true if the RDF terms are identical. So if 
	 * the RDF term in the database was encoded as <code>“001”^^xsd:integer</code> 
	 * term equality would give false whereas value equality would return true 
	 * because the value of the terms is equivalent.
	 */
	TERM_EQUALITY
}
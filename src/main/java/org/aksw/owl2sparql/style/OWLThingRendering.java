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
 * The way how <code>owl:Thing</code> in OWL class expressions is mapped to
 * SPARQL.
 * <ul>
 * <li>{@link #EXPLICIT}</li>
 * <li>{@link #GENERIC_TRIPLE_PATTERN}</li>
 * </ul>
 * @author Lorenz Buehmann
 *
 */
public enum OWLThingRendering {
	/**
	 * Use a triple pattern <code>?s a owl:Thing .</code> 
	 */
	EXPLICIT,
	/**
	 * Use a generic triple pattern <code>?s ?p ?o .</code> 
	 */
	GENERIC_TRIPLE_PATTERN
}
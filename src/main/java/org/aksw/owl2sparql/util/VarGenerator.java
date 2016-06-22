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
/**
 * 
 */
package org.aksw.owl2sparql.util;

/**
 * @author Lorenz Buehmann
 *
 */
public class VarGenerator {
	
	private static final String header = "?";
	private final String base;
	private int cnt = 0;
	
	public VarGenerator(String base) {
		this.base = base;
	}
	
	public VarGenerator() {
		this("s");
	}
	
	public String newVar(){
		return header + base + cnt++;
	}
	
	public void reset(){
		cnt = 0;
	}
}

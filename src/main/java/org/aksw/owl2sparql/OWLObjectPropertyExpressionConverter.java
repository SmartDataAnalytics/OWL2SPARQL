/**
 * 
 */
package org.aksw.owl2sparql;

import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyExpressionVisitor;
import org.semanticweb.owlapi.model.OWLPropertyExpressionVisitorEx;

/**
 * @author Lorenz Buehmann
 *
 */
public class OWLObjectPropertyExpressionConverter implements OWLPropertyExpressionVisitorEx<String>{
	
	String propertyVar = "?p";
	
	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLPropertyExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectProperty)
	 */
	@Override
	public String visit(OWLObjectProperty property) {
		return property.toStringID();
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLPropertyExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLObjectInverseOf)
	 */
	@Override
	public String visit(OWLObjectInverseOf property) {
		return propertyVar + "<http://www.w3.org/2002/07/owl#inverseOf>" + "";
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLPropertyExpressionVisitor#visit(org.semanticweb.owlapi.model.OWLDataProperty)
	 */
	@Override
	public String visit(OWLDataProperty property) {
		return property.toStringID();
	}

}

package ca.ubc.ece.salt.gumtree.gen.js;

import org.apache.commons.lang3.tuple.Pair;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.ThrowStatement;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;

/**
 * Visits a statement to find short circuits.
 */
class ShortCircuitExpand implements NodeVisitor {
	
	private AstNode condition;
	private boolean expanded;
	private boolean isTrueBranch;
	
	/**
	 * Expands the first short circuit operator in the statement.
	 * @param statement The statement to expand.
	 * @param condition The current branching condition for the statement.
	 * @param isTrueBranch Set to true to expand the true branch of the ternary operator.
	 * @return The expanded statement and the condition for which it executes.
	 */
	public static Pair<AstNode, AstNode> expand(AstNode statement, AstNode condition, boolean isTrueBranch) {

		ShortCircuitExpand expander = new ShortCircuitExpand(condition, isTrueBranch);
		
		/* For now, we only want to visit non-branching statements. Branching
		 * statements already contain control flow conditions. */
		if(statement instanceof ExpressionStatement)
            statement.visit(expander);
		else if(statement instanceof VariableDeclaration)
			statement.visit(expander);
		else if(statement instanceof ReturnStatement)
			statement.visit(expander);
		else if(statement instanceof ThrowStatement)
			statement.visit(expander);
		else return null;
		
		if(expander.expanded){
			return Pair.of(statement, expander.condition);
		}
		
		return null;

	}
	
	private ShortCircuitExpand(AstNode condition, boolean isTrueBranch) {
		this.expanded = false;
		this.condition = condition;
		this.isTrueBranch = isTrueBranch;
	}
	
	private void setCondition(AstNode testExpression) {
		
        /* Make the condition false if this is a false path. */
        if(!this.isTrueBranch) {
        	UnaryExpression not = new UnaryExpression(Token.NOT, 0, testExpression);
        	testExpression.setParent(not);
        	testExpression = not;
        }
        
        /* Add the condition. */
        if(this.condition == null) {
            this.condition = testExpression;
        }
        else {
        	InfixExpression ie = new InfixExpression(Token.AND, this.condition, testExpression, 0);
        	this.condition.setParent(ie);
        	testExpression.setParent(ie);

            this.condition = ie;
        }

		//System.out.println(this.condition.toSource());
        //if(testExpression == null) System.out.println("Parent is null!");
		
	}
	
	@Override
	public boolean visit(AstNode node) {

		if(this.expanded) return false; 

        /* TODO: Object assignments are too bulky. We need to expand them like
         * we expand variable initializers from variable declarations. For now
         * we set a hard limit of five elements in the object. */ 
        if(node instanceof ObjectLiteral) {
        	ObjectLiteral ol = (ObjectLiteral)node;
        	if(ol.getElements().size() > 5) return false;
        }

		/* We need to handle each node type on a case-by-case basis. */
		if(node instanceof InfixExpression) {
			
			InfixExpression ie = (InfixExpression) node;
			
			if(ie.getLeft() instanceof InfixExpression) {
				
				InfixExpression shortCircuit = (InfixExpression) ie.getLeft();
				int operator = shortCircuit.getOperator();
				
				if(operator == Token.AND || operator == Token.OR) {
				
                    /* Expand the statement by pulling up. */
                    if((this.isTrueBranch && operator == Token.AND) || 
                       (!this.isTrueBranch && operator == Token.OR)) ie.setLeft(shortCircuit.getRight());
                    else ie.setLeft(shortCircuit.getLeft());
                    ie.getLeft().setParent(ie);
                    
                    /* Add the test condition. */
                    this.setCondition(shortCircuit.getLeft().clone(shortCircuit.getLeft().getParent()));
                    
                    /* We have expanded the statement. */
                    this.expanded = true;

				}
				
			}
			else if(ie.getRight() instanceof InfixExpression) {

				InfixExpression shortCircuit = (InfixExpression) ie.getRight();
				int operator = shortCircuit.getOperator();
				
				if(operator == Token.AND || operator == Token.OR) {

                    /* Expand the statement by pulling up. */
                    if((this.isTrueBranch && operator == Token.AND) || 
                       (!this.isTrueBranch && operator == Token.OR)) ie.setRight(shortCircuit.getRight());
                    else ie.setRight(shortCircuit.getLeft());
                    ie.getRight().setParent(ie);
                    
                    /* Add the test condition. */
                    this.setCondition(shortCircuit.getLeft().clone(shortCircuit.getLeft().getParent()));

                    /* We have expanded the statement. */
                    this.expanded = true;

				}
				
			}
			
		}

		else if(node instanceof VariableInitializer) {
			
			VariableInitializer ie = (VariableInitializer) node;
			
			if(ie.getInitializer() instanceof InfixExpression) {

				InfixExpression shortCircuit = (InfixExpression) ie.getInitializer();
				int operator = shortCircuit.getOperator();
				
				if(operator == Token.AND || operator == Token.OR) {

                    /* ExpshortCircuit the statement by pulling up. */
                    if((this.isTrueBranch && operator == Token.AND) || 
                       (!this.isTrueBranch && operator == Token.OR)) ie.setInitializer(shortCircuit.getRight());
                    else ie.setInitializer(shortCircuit.getLeft());
                    ie.getInitializer().setParent(ie);
                    
                    /* Add the test condition. */
                    this.setCondition(shortCircuit.getLeft().clone(shortCircuit.getLeft().getParent()));
                    
                    /* We have expanded the statement. */
                    this.expanded = true;
				
				}
				
			}
			
		}

		else if(node instanceof ParenthesizedExpression) {
			
			ParenthesizedExpression pe = (ParenthesizedExpression) node;
			
			if(pe.getExpression() instanceof InfixExpression) {

				InfixExpression shortCircuit = (InfixExpression) pe.getExpression();
				int operator = shortCircuit.getOperator();
				
				if(operator == Token.AND || operator == Token.OR) {

                    /* ExpshortCircuit the statement by pulling up. */
                    if((this.isTrueBranch && operator == Token.AND) || 
                       (!this.isTrueBranch && operator == Token.OR)) pe.setExpression(shortCircuit.getRight());
                    else pe.setExpression(shortCircuit.getLeft());
                    pe.getExpression().setParent(pe);
                    
                    /* Add the test condition. */
                    this.setCondition(shortCircuit.getLeft().clone(shortCircuit.getLeft().getParent()));
                    
                    /* We have expanded the statement. */
                    this.expanded = true;

				}
				
			}
			
		}

		else if(node instanceof ExpressionStatement) {
			
			ExpressionStatement es = (ExpressionStatement) node;
			
			if(es.getExpression() instanceof InfixExpression) {

				InfixExpression shortCircuit = (InfixExpression) es.getExpression();
				int operator = shortCircuit.getOperator();
				
				if(operator == Token.AND || operator == Token.OR) {

                    /* ExpshortCircuit the statement by pulling up. */
                    if((this.isTrueBranch && operator == Token.AND) || 
                       (!this.isTrueBranch && operator == Token.OR)) es.setExpression(shortCircuit.getRight());
                    else es.setExpression(shortCircuit.getLeft());
                    es.getExpression().setParent(es);
                    
                    /* Add the test condition. */
                    this.setCondition(shortCircuit.getLeft().clone(shortCircuit.getLeft().getParent()));
                    
                    /* We have expanded the statement. */
                    this.expanded = true;
                   
				}
				
			}
			
		}

		else if(node instanceof ReturnStatement) {
			
			ReturnStatement is = (ReturnStatement) node;
			
			if(is.getReturnValue() instanceof InfixExpression) {

				InfixExpression shortCircuit = (InfixExpression) is.getReturnValue();
				int operator = shortCircuit.getOperator();
				
				if(operator == Token.AND || operator == Token.OR) {

                    /* ExpshortCircuit the statement by pulling up. */
                    if((this.isTrueBranch && operator == Token.AND) || 
                       (!this.isTrueBranch && operator == Token.OR)) is.setReturnValue(shortCircuit.getRight());
                    else is.setReturnValue(shortCircuit.getLeft());
                    is.getReturnValue().setParent(is);
                    
                    /* Add the test condition. */
                    this.setCondition(shortCircuit.getLeft().clone(shortCircuit.getLeft().getParent()));
                    
                    /* We have expanded the statement. */
                    this.expanded = true;
                    
				}
				
			}
			
		}

		else if(node instanceof ThrowStatement) {
			
			ThrowStatement is = (ThrowStatement) node;
			
			if(is.getExpression() instanceof InfixExpression) {

				InfixExpression shortCircuit = (InfixExpression) is.getExpression();
				int operator = shortCircuit.getOperator();
				
				if(operator == Token.AND || operator == Token.OR) {

                    /* Expand the statement by pulling up. */
                    if((this.isTrueBranch && operator == Token.AND) || 
                       (!this.isTrueBranch && operator == Token.OR)) is.setExpression(shortCircuit.getRight());
                    else is.setExpression(shortCircuit.getLeft());
                    is.getExpression().setParent(is);
                    
                    /* Add the test condition. */
                    this.setCondition(shortCircuit.getLeft().clone(shortCircuit.getLeft().getParent()));
                    
                    /* We have expanded the statement. */
                    this.expanded = true;
                    
				}
				
			}
			
		}
		
		else if(node instanceof FunctionNode) {
			
			/* Don't get into the function body. */
			return false;

		}
		
		if(this.expanded) return false; 
		return true;
	}
	
}
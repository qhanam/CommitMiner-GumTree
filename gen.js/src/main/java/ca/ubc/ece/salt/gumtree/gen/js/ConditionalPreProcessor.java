package ca.ubc.ece.salt.gumtree.gen.js;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang3.tuple.Pair;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.Scope;

public class ConditionalPreProcessor extends StatementPreProcessor {
	
	@Override
	protected AstNode processStatement(AstNode node) {
		
		if(node == null) return null;
		
		try {
			return createIfStatements(node);
		} catch (CloneNotSupportedException e) {
			return null;
		}
		
	}
	
	/** 
	 * Create the if/else statements for the conditional. 
	 * returns null if there are no conditional expressions in the statement.
	 */
	private IfStatement createIfStatements(AstNode node) throws CloneNotSupportedException{
		
		IfStatement ifStatement = null;
		IfStatement current = ifStatement;
		
		/* Get the list of expanded statements and their conditions. */
		List<Pair<AstNode, AstNode>> pairs =  expand(node);
		
		/* If the statement did not need to be expanded, there is nothing to be
		 * done. */
		if(pairs.size() == 1) return null;
		
		/* We have expanded statements, so create the if statements. */
		Iterator<Pair<AstNode, AstNode>> iterator = pairs.iterator();
		while(iterator.hasNext()) {
			Pair<AstNode, AstNode> pair = iterator.next();

			if(iterator.hasNext()) {
                IfStatement newIfStatement = new IfStatement();
                Scope thenScope = new Scope();
                thenScope.addChild(pair.getLeft());
                newIfStatement.setThenPart(thenScope);
                thenScope.setParent(newIfStatement);
                newIfStatement.setCondition(pair.getRight());
                pair.getRight().setParent(newIfStatement);
                
                if(current == null) {
                	ifStatement = newIfStatement;
                	ifStatement.setParent(node.getParent());
                }
                else {
                	current.setElsePart(newIfStatement);
                	newIfStatement.setParent(current);
                }

                current = newIfStatement;
			}
			else {
				Scope elseScope = new Scope();
				elseScope.addChild(pair.getLeft());
				current.setElsePart(elseScope);
				elseScope.setParent(current);
			}
			
		}

		return ifStatement;
		
	}
	
	/**
	 * Expand the ternary operators in a statement. The resulting statement
	 * will either be the original statement (if there are no ternary operators)
	 * or a series of if/else statements.
	 * @param statement The statement to expand.
	 * @throws CloneNotSupportedException 
	 */
	private List<Pair<AstNode, AstNode>> expand(AstNode node) throws CloneNotSupportedException {
		
		Queue<Pair<AstNode, AstNode>> toExpand = new LinkedList<Pair<AstNode, AstNode>>();
		List<Pair<AstNode, AstNode>> expanded = new LinkedList<Pair<AstNode, AstNode>>();
		
		toExpand.add(Pair.of(node, (AstNode)null));
		
		while(!toExpand.isEmpty()) {
			
			Pair<AstNode, AstNode> pair = toExpand.remove();
			AstNode statement = pair.getLeft();
			AstNode condition = pair.getRight();
			
			/* Make two clones of the statement: one for the true branch and
			 * one for the false branch. */
			AstNode trueClone = statement.clone(statement.getParent());
			AstNode trueCondition = null;
			if(condition != null) trueCondition = condition.clone(condition.getParent());
			AstNode falseClone = statement.clone(statement.getParent());
			AstNode falseCondition = null;
			if(condition != null) falseCondition = condition.clone(condition.getParent());
			
			/* Expand the first conditional. */
			Pair<AstNode, AstNode> trueResult = ConditionalExpand.expand(trueClone, trueCondition, true);
			Pair<AstNode, AstNode> falseResult = ConditionalExpand.expand(falseClone, falseCondition, false);
			
			/* If the statements were expanded, add them to the toExpand set to
			 * be expanded again. Otherwise add them to the completed list. */
			if(trueResult != null && falseResult != null) {
				toExpand.add(trueResult);
				toExpand.add(falseResult);
			}
			else {
                assert(trueResult == null && falseResult == null);
                if(!(trueResult == null && falseResult == null)) throw new Error("Assertion failed");
                expanded.add(pair);
			}

		}
		
		return expanded;
		
	}
	
}
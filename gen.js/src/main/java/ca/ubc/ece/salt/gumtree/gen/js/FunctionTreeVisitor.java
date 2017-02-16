package ca.ubc.ece.salt.gumtree.gen.js;

import java.util.LinkedList;
import java.util.List;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ScriptNode;

/**
 * Finds all the child functions in an AstNode. 
 * 
 * Note that only first-level children are returned. Functions inside of
 * functions are not visited.
 * 
 * @author qhanam
 */
public class FunctionTreeVisitor implements NodeVisitor {
	
	private List<FunctionNode> functions;
	
	private FunctionTreeVisitor() {
		this.functions = new LinkedList<FunctionNode>();
	}
	
	public static List<FunctionNode> getFunctions(ScriptNode node) {
		FunctionTreeVisitor visitor = new FunctionTreeVisitor();
		node.visit(visitor);
		return visitor.functions;
	}

	@Override
	public boolean visit(AstNode node) {
		if(node instanceof FunctionNode) {
			this.functions.add((FunctionNode) node);
		}
		return true;
	}

}
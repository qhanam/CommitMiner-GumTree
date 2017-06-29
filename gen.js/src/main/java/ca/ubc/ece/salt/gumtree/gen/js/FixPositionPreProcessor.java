package ca.ubc.ece.salt.gumtree.gen.js;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.NodeVisitor;

/**
 * Visits all nodes in the AST and fixes their absolute positions so that they
 * can be retrieved after the AST has been manipulated.
 */
public class FixPositionPreProcessor implements PreProcessor {

	public FixPositionPreProcessor() { }

	@Override
	public void process(AstRoot root) {
		FixPositionASTVisitor visitor = new FixPositionASTVisitor();
		root.visit(visitor);
	}
	
	private class FixPositionASTVisitor implements NodeVisitor {

		@Override
		public boolean visit(AstNode node) {
			node.setFixedPosition(node.getAbsolutePosition());
			return true;
		}
		
	}

}

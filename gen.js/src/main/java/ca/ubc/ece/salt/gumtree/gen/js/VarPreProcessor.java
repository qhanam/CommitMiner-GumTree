package ca.ubc.ece.salt.gumtree.gen.js;

import java.util.LinkedList;
import java.util.List;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;

/**
 * Converts multiple initializations with one declaration into multiple declarations.
 */
public class VarPreProcessor extends StatementPreProcessor {
	
	@Override
	protected AstNode processStatement(AstNode node) {
		
		if(node == null) return null;
        return createVarDeclarations(node);
		
	}
	
	/** 
	 * Create the if/else statements for the short circuit. 
	 * returns null if there are no short circuit expressions in the statement.
	 */
	private Block createVarDeclarations(AstNode node){
		
		if(node instanceof VariableDeclaration && ((VariableDeclaration)node).getVariables().size() > 1) {
			
			/* Split all the variable initializations into variable declarations. */
			VariableDeclaration vd = (VariableDeclaration) node;
			Block block = new Block();
			block.setParent(node.getParent());
			
			for(VariableInitializer vi : vd.getVariables()) {
				VariableDeclaration nvd = new VariableDeclaration();
				List<VariableInitializer> nvi = new LinkedList<VariableInitializer>();
				nvi.add(vi);
				nvd.setVariables(nvi);
				nvd.setParent(block);
				nvd.setIsStatement(true);
				vi.setParent(nvd);
				block.addChild(nvd);
			}
			
			return block;
			
		}
		
		return null;

	}
	
}
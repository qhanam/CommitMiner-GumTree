package ca.ubc.ece.salt.gumtree.ast;

/**
 * Provides a class to link Tree nodes back to the original ASTNode objects
 * produced by the parser. This is useful for performing a more detailed
 * analysis of a specific language without having to add data and functionality
 * to Tree.
 * @author qhanam
 *
 * @param <T> The type of ASTNode that this class stores.
 */
public class ParserASTNode<T> {
	
	private T node;
	
	public ParserASTNode (T node) {
		this.node = node;
	}
	
	public T getASTNode() {
		return node;
	}

}
package ca.ubc.ece.salt.gumtree.ast;

import java.io.InvalidClassException;

/**
 * {@link ClassifiedASTNode} is an interface for applying AST differencing
 * classifications (i.e. inserted, removed, moved and updated) to the ASTNode
 * of the base language.
 *
 * <p>Because GumTree uses many languages, it produces a common Tree node for
 * each AST node parsed from a language. The tree nodes contain all the
 * information and functionality needed for GumTree, but a lot of language
 * and parser specific features of the AST nodes are lost in the conversion
 * from AST node to Tree node. It is therefore unsuitable to use Tree nodes
 * for CFG production.</p>
 *
 * <p>The solution to this problem is to map the change classifications from
 * the Tree nodes back to the AST nodes. ClassifiedASTNode enables this by
 * providing a language-independent interface. To make a specific parser's
 * AST node's classifiable, the AST node class should be modified to implement
 * this interface. For example, the class {@code AstNode} in the Mozilla Rhino
 * parser for JavaScript is modified to support this interface.</p>
 *
 * @author qhanam
 *
 */
public interface ClassifiedASTNode {

	/**
	 * @param id A unique ID for the node. Should be the same as the mapped
	 * 			 node's ID. Used for identifying statements in declarative SA.
	 */
	void setID(Integer id);

	/**
	 * @return The unique ID for the node. Should be the same ID as the mapped
	 * 		   node's ID. Used for identifying statements in declarative SA.
	 */
	Integer getID();

	/**
	 * @param version The version of the node relative to the commit. Can be
	 * 				  source or destination node.
	 */
	void setVersion(Version version);

	/**
	 * @return The version of the node relative to the commit. Can be
	 * 		   source or destination node.
	 */
	Version getVersion();
	
    /**
     * Indicate that this node provided a dummy value during analysis.
     */
	void setDummy();
	
	/**
	 * @return {@code true} if this node provided a dummy value during analysis.
	 */
	boolean isDummy();
	
    /**
     * @param moved If a move was applied to this node from AST differencing.
     */
	void setMoved(boolean moved);

	/**
	 * @return {@code true} if the node has been moved in the AST (from AST differencing).
	 */
	boolean getIsMoved();

    /**
     * @param changeType The change applied to this node from AST differencing.
     */
    void setChangeType(ChangeType changeType);

    /**
     * @return The edit operation applied to this node from AST differencing.
     */
    ChangeType getChangeType();

    /**
     * @return The non-propagated edit operation applied to this node from AST differencing.
     */
	void setChangeTypeNoProp(ChangeType changeTypeNoProp);

    /**
     * @return The non-propagated edit operation applied to this node from AST differencing.
     */
	ChangeType getChangeTypeNoProp();
	
    /**
     * @param node The source or destination node to map this node to.
     */
    void map(ClassifiedASTNode node) throws InvalidClassException;

    /**
     * @return The source or destination node that maps to this node or null
     * 		   if this node does not have a mapping (which is the case for
     * 		   inserted and removed nodes).
     */
    ClassifiedASTNode getMapping();

    /**
     * @return true if the AST node is an empty statement.
     */
    boolean isEmpty();

    /**
     * @return the type of AST node as a string.
     */
    String getASTNodeType();

    /**
     * @return the CFG node or edge label (the source code).
     */
    String getCFGLabel();

    /**
     * Returns true if this node represents a statement.
     */
    boolean isStatement();
    
    /**
     * Returns true if this node represents a function.
     */
    boolean isFunction();
    
    /**
     * Set the absolute position of the node in the original (not pre-processed) AST.
     */
    void setFixedPosition(int fixedPosition);
    
    /**
     * @return the absolute position of the node in the original (not pre-processed) AST.
     */
    int getFixedPosition();

    /** The change type from AST differencing. **/
    public enum ChangeType {
    	INSERTED,
    	REMOVED,
    	UPDATED,
    	MOVED,
    	UNCHANGED,
    	INHERITED,
    	UNKNOWN
    }

    /**
     * Specifies which version of the change this AstNode is on - the source
     * (e.g., buggy) or the destination (e.g., repaired).
     */
    public enum Version {
    	SOURCE,
    	DESTINATION
    }

}
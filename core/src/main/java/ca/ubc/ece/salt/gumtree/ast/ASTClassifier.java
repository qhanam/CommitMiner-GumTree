package ca.ubc.ece.salt.gumtree.ast;

import java.io.InvalidClassException;
import java.util.Set;

import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.Version;

public class ASTClassifier {

	/** Assigns uniqueIDs to each node. **/
	private int uniqueID;

	private ITree srcTree;
	private ITree dstTree;

	private TreeClassifier treeClassifier;
	private MappingStore mappings;

	public ASTClassifier(TreeContext src, TreeContext dst, TreeClassifier treeClassifier, MappingStore mappings) {

		this.uniqueID = 0;
		this.srcTree = src.getRoot();
		this.dstTree = dst.getRoot();
		this.treeClassifier = treeClassifier;
		this.mappings = mappings;

	}

	/**
	 * Maps Tree node classifications to AST node classifications.
	 * @throws InvalidClassException
	 */
	public void classifyASTNodes() throws InvalidClassException {
		/* Classify the AST nodes from the Tree nodes. */
		classifyAs(treeClassifier.getSrcDelTrees(), ChangeType.REMOVED);
		classifyAs(treeClassifier.getSrcMvTrees(), ChangeType.MOVED);
		classifyAs(treeClassifier.getSrcUpdTrees(), ChangeType.UPDATED);

		classifyAs(treeClassifier.getDstMvTrees(), ChangeType.MOVED);
		classifyAs(treeClassifier.getDstUpdTrees(), ChangeType.UPDATED);
		classifyAs(treeClassifier.getDstAddTrees(), ChangeType.INSERTED);

		/* Classify the children of the classified  AST nodes and assign
		 * node mappings for MOVED, UPDATED and UNCHANGED nodes. */
		propagateChangesToChildren(srcTree, ChangeType.UNCHANGED, false, true);
		propagateChangesToChildren(dstTree, ChangeType.UNCHANGED, false, false);
		
		/* Push changes to ancestors (up to the function level) as UPDATED */
		propagateChangesToAncestors(srcTree);
		propagateChangesToAncestors(dstTree);
	}

	/**
	 * @return A unique ID for the node.
	 */
	private int getUniqueID() {
		this.uniqueID += 16; // Allow for offsetting the unique ID. Yields 2^28 addresses (270M).
		return this.uniqueID;
	}
	
	private void classifyAs(Set<ITree> set, ChangeType changeType) throws InvalidClassException {

		/* Iterate through the changed Tree nodes and classify the
		 * corresponding AST node with the Tree node's class. */
		for(ITree tree : set) {
			ClassifiedASTNode astNode = tree.getClassifiedASTNode();
			switch(changeType) {
			case INSERTED:
			case REMOVED:
			case UPDATED:
			case UNCHANGED:
				astNode.setChangeType(changeType);
				astNode.setChangeTypeNoProp(changeType);
				astNode.setMoved(false);
				break;
			case MOVED:
				astNode.setChangeType(ChangeType.UNCHANGED);
				astNode.setChangeTypeNoProp(ChangeType.UNCHANGED);
				astNode.setMoved(true);
				break;
			case UNKNOWN:
			case INHERITED:
			default:
				throw new Error("Unsupported change type in AST change type classification.");
			}
		}

	}
	
	/**
	 * Propagate the label up to the statement or function level (depending on needs)
	 */
	private void propagateChangesToAncestors(ITree subtree) throws InvalidClassException {
		/* Propagate UPDATED label upwards if this subtree was inserted, removed or updated. */
		switch(subtree.getClassifiedASTNode().getChangeTypeNoProp()) {
		case INSERTED:
		case REMOVED:
		case UPDATED:
			labelAncestorsUpdated(subtree);
			break;
		default:
		}
		
		/* Recursively visit children. */
		for(ITree childOfSubtree : subtree.getChildren()) 
			propagateChangesToAncestors(childOfSubtree);
	}

	/**
	 * Propagate change labels down to children.
	 * @param subtree The subtree to label.
	 * @param ancenstorChangeType The change type of the current node's ancestors.
	 * @param isSrc {@code true} if the current tree is the original AST; {@code false} if the current tree is the new AST.
	 * @throws InvalidClassException 
	 */
	private void propagateChangesToChildren(ITree subtree, ChangeType ancestorChangeType, boolean ancestorMoved, boolean isSrc) throws InvalidClassException {
		ClassifiedASTNode classifiedNode = subtree.getClassifiedASTNode();

		/* This node was moved if its parent was moved. */
		if(ancestorMoved) classifiedNode.setMoved(true);

		/* This node assumes the change type of its parent. */
		if(classifiedNode.getChangeType() == ChangeType.UNKNOWN) {
			if(ancestorChangeType != ChangeType.UNCHANGED) {
				classifiedNode.setChangeType(ancestorChangeType);
				classifiedNode.setChangeTypeNoProp(ChangeType.INHERITED);
			} else {
				classifiedNode.setChangeType(ChangeType.UNCHANGED);
				classifiedNode.setChangeTypeNoProp(ChangeType.UNCHANGED);
			}
		}

		/* Assign the node mapping if this is an UPDATED or UNCHANGED node. */
		if(classifiedNode.getChangeType() == ChangeType.UNCHANGED 
				|| classifiedNode.getChangeType() == ChangeType.UPDATED) {
			if(isSrc) {
				ITree dst = this.mappings.getDst(subtree);
				if(dst != null) classifiedNode.map(dst.getClassifiedASTNode());
			}
			else {
				ITree src = this.mappings.getSrc(subtree);
				if(src != null) classifiedNode.map(src.getClassifiedASTNode());
			}
		}
		
		/* Assign the node a unique id if it does not yet have one. */
		if(classifiedNode.getID() == null) {
			Integer id = getUniqueID();
			classifiedNode.setID(id);
			if(classifiedNode.getMapping() != null)
				classifiedNode.getMapping().setID(id);
		}

		/* Assign the node a version label. */
		if(isSrc) classifiedNode.setVersion(Version.SOURCE);
		else classifiedNode.setVersion(Version.DESTINATION);

		/* Classify this node's children with the new change type. */
		for(ITree childOfSubtree : subtree.getChildren()) {
			propagateChangesToChildren(childOfSubtree, classifiedNode.getChangeType(), classifiedNode.getIsMoved(), isSrc);
		}

	}

	/**
	 * Visits all the ancestors of the node and labels them as {@code UPDATED}
	 * if they have the label {@code UNCHANGED}.
	 * @param child2 The updated/inserted/removed node who's ancestors should be
	 * 			   labeled.
	 */
	private static void labelAncestorsUpdated(ITree child2) throws InvalidClassException {

		/* If this is a function, there is no ancestor to label. */
		if(child2.getClassifiedASTNode().isFunction()) return;

		/* Climb the tree and label all unchanged nodes until we get to the
		 * statement. */
		ITree ancestor = child2.getParent();
		while(ancestor != null && ancestor.getClassifiedASTNode() != null &&
			  ancestor.getClassifiedASTNode().getChangeType() == ChangeType.UNCHANGED) {

			/* Label the ancestor, and it's mapped node in the other version, as
			 * updated, since one of its descendants was inserted, removed or
			 * updated. */
			ancestor.getClassifiedASTNode().setChangeType(ChangeType.UPDATED);
			ancestor.getClassifiedASTNode().getMapping().setChangeType(ChangeType.UPDATED);

			/* If we've reached the statement level, stop. */
			if(ancestor.getClassifiedASTNode().isFunction()) {
				break;
			}

			ancestor = ancestor.getParent();

		}

	}

}
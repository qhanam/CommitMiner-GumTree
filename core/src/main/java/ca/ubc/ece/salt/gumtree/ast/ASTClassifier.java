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

		this.classifyAs(this.treeClassifier.getSrcDelTrees(), ChangeType.REMOVED);
		this.classifyAs(this.treeClassifier.getSrcMvTrees(), ChangeType.MOVED);
		this.classifyAs(this.treeClassifier.getSrcUpdTrees(), ChangeType.UPDATED);

		this.classifyAs(this.treeClassifier.getDstMvTrees(), ChangeType.MOVED);
		this.classifyAs(this.treeClassifier.getDstUpdTrees(), ChangeType.UPDATED);
		this.classifyAs(this.treeClassifier.getDstAddTrees(), ChangeType.INSERTED);

		/* Classify the children of the classified  AST nodes and assign
		 * node mappings for MOVED and UPDATED nodes. */

		this.classifyASTNode(this.srcTree, ChangeType.UNCHANGED, true);
		this.classifyASTNode(this.dstTree, ChangeType.UNCHANGED, false);

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
            astNode.setChangeType(changeType);
		}

	}

	/**
	 * Recursively classifies the Tree node's AST nodes with the change type
	 * of their parent. The classification only occurs if the current node's
	 * class is {@code UNCHANGED}. If a node is updated, inserted or removed,
	 * we also label that node's {@code UNCHANGED} or {@code MOVED} ancestors
	 * as {@code UPDATED} up to the statement level.
	 * @param child2 The node to classify.
	 * @param changeType The change type to assign the AST node.
	 * @throws InvalidClassException
	 */
	private void classifyASTNode(ITree child2, ChangeType changeType, boolean isSrc) throws InvalidClassException {

		/* Set the change class for the AST node if it is currently UNCHANGED. */

		ClassifiedASTNode classifiedNode = child2.getClassifiedASTNode();
		ChangeType nodeChangeType = classifiedNode.getChangeType();

		/* We may need to assign a change type if it is null (unchanged). */
		if(nodeChangeType == null) {
			classifiedNode.setChangeType(ChangeType.UNCHANGED);
			nodeChangeType = ChangeType.UNCHANGED;
		}

		if(nodeChangeType == ChangeType.UNCHANGED) {
			classifiedNode.setChangeType(changeType);
		}
		else {
			/* The node has been changed. */
			changeType = nodeChangeType;

			/* Since this is a new change type (updated, inserted or removed),
			 * label the ancestors as 'updated' up to the statement level. */
			labelAncestorsUpdated(child2);
		}


		if(changeType == ChangeType.MOVED || changeType == ChangeType.UPDATED || changeType == ChangeType.UNCHANGED) {

			/* Assign the node mapping if this is an UPDATED or MOVED node. */

			if(isSrc) {
				ITree dst = this.mappings.getDst(child2);
				if(dst != null) classifiedNode.map(dst.getClassifiedASTNode());
			}
			else {
				ITree src = this.mappings.getSrc(child2);
				if(src != null) classifiedNode.map(src.getClassifiedASTNode());
			}

		}

		/* Assign the node a unique id if it does not yet have one. */

		if(classifiedNode.getID() == null) {
			Integer id = this.getUniqueID();
			classifiedNode.setID(id);
			if(classifiedNode.getMapping() != null)
				classifiedNode.getMapping().setID(id);
		}

		/* Assign the node a version label. */
		if(isSrc) classifiedNode.setVersion(Version.SOURCE);
		else classifiedNode.setVersion(Version.DESTINATION);

		/* Classify this node's children with the new change type. */

		for(ITree child : child2.getChildren()) {
			classifyASTNode(child, changeType, isSrc);
		}

	}

	/**
	 * Visits all the ancestors of the node and labels them as {@code UPDATED}
	 * if they have the label {@code UNCHANGED}.
	 * @param child2 The updated/inserted/removed node who's ancestors should be
	 * 			   labeled.
	 */
	private static void labelAncestorsUpdated(ITree child2) throws InvalidClassException {

		/* If this is a statement, there is no ancestor to label. */
		if(child2.getClassifiedASTNode().isStatement()) return;

		/* Climb the tree and label all unchanged nodes until we get to the
		 * statement. */
		ITree ancestor = child2.getParent();
		while(ancestor != null && ancestor.getClassifiedASTNode() != null &&
			  (ancestor.getClassifiedASTNode().getChangeType() == ChangeType.UNCHANGED ||
			  ancestor.getClassifiedASTNode().getChangeType() == ChangeType.MOVED)) {

			/* Label the ancestor as updated, since one of its descendants was
			 * inserted, removed or updated. */
			ancestor.getClassifiedASTNode().setChangeType(ChangeType.UPDATED);

			/* If we've reached the statement level, stop. */
			if(ancestor.getClassifiedASTNode().isStatement()) {
				break;
			}

			ancestor = ancestor.getParent();

		}

	}

}
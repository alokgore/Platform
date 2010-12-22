package com.tejas.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

class BranchNode
{
	/**
	 * Maps key component to BranchNode
	 */
	private final Map<String, BranchNode> branchNodes_ = new HashMap<String, BranchNode>();

	/**
	 * The value that lives at this spot in the config tree. May be null.
	 */
	private ValueNode value_ = null;

	/**
	 * Find all the ValueNodes in the subtree below this node.
	 */
	public List<ValueNode> findAllValueNodes()
	{
		List<ValueNode> result = new ArrayList<ValueNode>();
		ValueNode value = getValue();
		if (value != null)
		{
			result.add(value);
		}
		for (BranchNode node : branchNodes_.values())
		{
			result.addAll(node.findAllValueNodes());
		}
		return result;
	}

	/**
	 * Find the branch node that is at a given relative path to this node.
	 * 
	 * @param relativePathToChild
	 *            '.' delimited path to the child we are searching <br>
	 *            E.g. We are at the root node and searching for the key 'xyz.foo.bar', this value
	 *            of the relativePathToChild parameter would be 'xyz.foo.bar' <br>
	 *            But if we are at the xyz child of root and searching for the same node, this
	 *            parameter would be 'foo.bar'
	 * @param createNodes
	 *            indicates whether to create the nodes for the requested key should they not exist.
	 * @return the BranchNode matching the requested key.
	 */
	public BranchNode findNode(String relativePathToChild, boolean createNodes)
	{
		Queue<String> components = new LinkedList<String>(Arrays.asList(relativePathToChild.split("\\.")));
		return findNode(components, createNodes);
	}

	public ValueNode getValue()
	{
		return value_;
	}

	public void printTo(StringBuffer out)
	{
		if (value_ != null)
		{
			value_.printTo(out);
		}

		for (BranchNode node : branchNodes_.values())
		{
			node.printTo(out);
		}
	}

	public void setValue(ValueNode value)
	{
		value_ = value;
	}

	private BranchNode findNode(Queue<String> keyComponents, boolean createNodes)
	{
		// strip the next token and find it in the tree.
		String component = keyComponents.remove();

		BranchNode child = branchNodes_.get(component);

		// if not found and create requested, make the node and put it into our map
		if ((child == null))
		{
			if (createNodes)
			{
				child = new BranchNode();
				branchNodes_.put(component, child);
			}
			else
			{
				// child is null and we shouldn't create -> return null
				return null;
			}
		}

		if (keyComponents.isEmpty() == false)
		{
			return child.findNode(keyComponents, createNodes);
		}
		return child;
	}
}

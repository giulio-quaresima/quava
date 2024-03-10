/*
 * Copyright (C) 2024 Giulio Quaresima
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package eu.giulioquaresima.quava.trie;

import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

/**
 * 
 * 
 * @author Giulio Quaresima (giulio.quaresima--at--gmail.com, giulio.quaresima--at--unipg.it, giulio.quaresima--at--studenti.unicam.it)
 */
public class TrieMap<V> extends AbstractMap<String, V>
{
	@SuppressWarnings("unchecked")
	private final Node[] EMPTY = new TrieMap.Node[0];

	private final Node root;
	private final CharUnaryOperator charTranslator;
	int size = 0;
	
	public TrieMap(CharUnaryOperator charTranslator)
	{
		this.root = new Node();
		this.charTranslator = Optional.ofNullable(charTranslator).orElseGet(CharUnaryOperator::identity);
	}
	public TrieMap()
	{
		this(null);
	}
	
	public int findAll(CharSequence charSequence, Visitor<V> visitor)
	{
		int count = 0;

		class Match
		{
			int start, end;
			Node node;
			boolean found()
			{
				return node != null;
			}
		}
		for (int textIndex = 0; textIndex < charSequence.length(); textIndex++)
		{
			Node currentNode = root;
			Match match = new Match();
			match.start = textIndex;
			for (int depth = 0; (textIndex + depth) < charSequence.length() && currentNode != null; depth++)
			{
				int index = charTranslator.applyAsChar(charSequence.charAt(textIndex + depth)) - currentNode.offset;
				if (index < 0 || index >= currentNode.children.length || currentNode.children[index] == null)
				{
					break;
				}
				currentNode = currentNode.children[index];
				if (currentNode.key != null)
				{
					match.end = textIndex + depth + 1;
					match.node = currentNode;
				}
			}
			if (match.found())
			{
				visitor.matched(match.node, match.start, match.end);
				textIndex = match.end;
				count++;
			}
		}
		
		return count;
	}
	
	@Override
	public V get(Object keyObj)
	{
		Node node = getNode(keyObj);
		if (node != null)
		{
			return node.value;
		}
		return null;
	}
	
	protected Node getNode(Object keyObj)
	{
		if (keyObj instanceof String)
		{
			String key = (String) keyObj;
			Node currentNode = root;
			for (int depth = 0; depth < key.length() && currentNode != null; depth++)
			{
				int index = charTranslator.applyAsChar(key.charAt(depth)) - currentNode.offset;
				if (index >= 0 && index < currentNode.children.length)
				{
					currentNode = currentNode.children[index];
				}
				else
				{
					currentNode = null;
				}
			}
			if (currentNode != null && currentNode.key != null)
			{
				assert currentNode.key.equals(key) : "Implementation error";
				return currentNode;
			}
		}
		return null;
	}

	@Override
	public boolean containsKey(Object key)
	{
		return get(key) != null;
	}
	
	@Override
	public V put(String key, V value)
	{
		if (key == null)
		{
			throw new IllegalArgumentException("key", new NullPointerException());
		}
		/*
		if (value == null)
		{
			throw new IllegalArgumentException("value", new NullPointerException());
		}
		*/
		
		boolean increaseSize = false;
		
		Node currentNode = root;
		for (int depth = 0; depth < key.length(); depth++)
		{
			char currentChar = charTranslator.applyAsChar(key.charAt(depth));
			int index = currentChar - currentNode.offset;
			if (currentNode.children.length == 0)
			{
				currentNode.offset = currentChar;
				currentNode.children = newNodes(1);
				index = 0;
			}
			
			if (index < 0)
			{
				TrieMap<V>.Node[] newChildren = newNodes(currentNode.children.length + (currentNode.offset - currentChar));
				System.arraycopy(currentNode.children, 0, newChildren, -index, currentNode.children.length);
				currentNode.children = newChildren;
				newChildren = null;
				currentNode.offset = currentChar;
				index = 0;
			}
			else if (index >= currentNode.children.length)
			{
				TrieMap<V>.Node[] newChildren = newNodes(index + 1);
				System.arraycopy(currentNode.children, 0, newChildren, 0, currentNode.children.length);
				currentNode.children = newChildren;
				newChildren = null;				
			}
			
			if (currentNode.children[index] == null)
			{
				currentNode.children[index] = new Node();
				increaseSize = true;
			}
			currentNode = currentNode.children[index];
		}
		increaseSize = increaseSize || currentNode.key == null;
		V previous = currentNode.value;
		currentNode.key = key;
		currentNode.value = value;
		
		if (increaseSize)
		{
			size++;
		}

		return previous;
	}

	@Override
	public Set<Entry<String, V>> entrySet()
	{
		Set<Entry<String, V>> entries = new LinkedHashSet<>();
		
		// pre-order traversal
		Stack<Node> stack = new Stack<>();
		stack.push(root);
		while ( ! stack.isEmpty() )
		{
			Node current = stack.pop();
			if (current.key != null)
			{
				entries.add(current);
			}
			for (int index = current.children.length - 1; index >= 0; index--)
			{
				Node child = current.children[index];
				if (child != null)
				{
					stack.push(current.children[index]);
				}
			}
		}
		
		return entries;
	}

	@Override
	public int size()
	{
		return size;
	}
	
	public void printTree(PrintStream printStream)
	{
		Queue<Node> queue = new LinkedList<>();
		queue.offer(root);
		while ( ! queue.isEmpty() )
		{
			List<Node> level = new LinkedList<>();
			printStream.println(queue.stream().map(this::toStringChildrenAsChars).collect(Collectors.joining(",")));
			Node node;
			while ((node = queue.poll()) != null)
			{
				for (Node child : node.children)
				{
					if (child != null)
					{
						level.add(child);
					}
				}
			}
			queue.addAll(level);
		}
	}
	
	private String toStringChildrenAsChars(Node node)
	{
		List<Character> chars = new LinkedList<>();
		for (int index = 0; index < node.children.length; index++)
		{
			if (node.children[index] != null)
			{
				chars.add((char) (node.offset + index));
			}
		}
		return String.format("[%s]", chars.stream().map(c -> c.toString()).collect(Collectors.joining(",")));
	}

	@SuppressWarnings("unchecked")
	private Node[] newNodes(int size)
	{
		return new TrieMap.Node[size];
	}

	class Node implements Entry<String, V>
	{
		private Node[] children = EMPTY;
		private char offset = 0;
		private String key;
		private V value;
		
		@Override
		public String getKey()
		{
			return key;
		}
		
		@Override
		public V getValue()
		{
			return value;
		}
		
		@Override
		public V setValue(V value)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int hashCode()
		{
			if (key != null)
			{
				return key.hashCode();
			}
			return super.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if ( ! Node.class.isAssignableFrom(obj.getClass()) )
				return false;
			@SuppressWarnings("unchecked")
			Node other = (Node) obj;
			return Objects.equals(key, other.key);
		}
	}
	
	@FunctionalInterface
	public interface Visitor<V>
	{
		void matched(Map.Entry<String, V> entry, int start, int end);
	}
	
	/**
	 * All copied from {@link IntUnaryOperator}, JavaDocs included.
	 * 
	 * @author Giulio Quaresima (giulio.quaresima--at--unipg.it, giulio.quaresima--at--gmail.com)
	 */
	public interface CharUnaryOperator
	{
	    /**
	     * Applies this operator to the given operand.
	     *
	     * @param operand the operand
	     * @return the operator result
	     */
	    char applyAsChar(char operand);

	    /**
	     * Returns a composed operator that first applies the {@code before}
	     * operator to its input, and then applies this operator to the result.
	     * If evaluation of either operator throws an exception, it is relayed to
	     * the caller of the composed operator.
	     *
	     * @param before the operator to apply before this operator is applied
	     * @return a composed operator that first applies the {@code before}
	     * operator and then applies this operator
	     * @throws NullPointerException if before is null
	     *
	     * @see #andThen(IntUnaryOperator)
	     */
	    default CharUnaryOperator compose(CharUnaryOperator before) {
	        Objects.requireNonNull(before);
	        return (char v) -> applyAsChar(before.applyAsChar(v));
	    }

	    /**
	     * Returns a composed operator that first applies this operator to
	     * its input, and then applies the {@code after} operator to the result.
	     * If evaluation of either operator throws an exception, it is relayed to
	     * the caller of the composed operator.
	     *
	     * @param after the operator to apply after this operator is applied
	     * @return a composed operator that first applies this operator and then
	     * applies the {@code after} operator
	     * @throws NullPointerException if after is null
	     *
	     * @see #compose(CharUnaryOperator)
	     */
	    default CharUnaryOperator andThen(CharUnaryOperator after) {
	        Objects.requireNonNull(after);
	        return (char t) -> after.applyAsChar(applyAsChar(t));
	    }

	    /**
	     * Returns a unary operator that always returns its input argument.
	     *
	     * @return a unary operator that always returns its input argument
	     */
	    static CharUnaryOperator identity() {
	        return t -> t;
	    }

	}
}

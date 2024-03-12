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

package eu.giulioquaresima.quava.collections.trie;

import java.io.PrintStream;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import eu.giulioquaresima.quava.functions.CharUnaryOperator;

/**
 * 
 * 
 * @author Giulio Quaresima (giulio.quaresima--at--gmail.com, giulio.quaresima--at--unipg.it, giulio.quaresima--at--studenti.unicam.it)
 */
public class TrieMap<V> implements Map<String, V>
{
	@SuppressWarnings("unchecked")
	private final Node[] EMPTY = new TrieMap.Node[0];

	private Node root;
	private final CharUnaryOperator charTranslator;
	int size = 0;
	
	public TrieMap(CharUnaryOperator charTranslator)
	{
		this.root = new Node(null);
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
				currentNode.children[index] = new Node(currentNode);
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
	public int size()
	{
		return size;
	}
	
	@Override
	public boolean isEmpty()
	{
		return size == 0;
	}
	
	@Override
	public V remove(Object key)
	{
		Node node = getNode(key);
		if (node != null)
		{
			node.remove();
			return node.getValue();
		}
		return null;
	}
	@Override
	public boolean remove(Object key, Object value)
	{
		Node node = getNode(key);
		if (node != null)
		{
			if (Objects.equals(value, node.getValue()))
			{
				node.remove();
				return true;				
			}
		}
		return false;
	}

	@Override
	public void putAll(Map<? extends String, ? extends V> m)
	{
		m.forEach(this::put);
	}
	
	@Override
	public void clear()
	{
		root = new Node(null);
		size = 0;
	}
	
	@Override
	public V getOrDefault(Object key, V defaultValue)
	{
		Node node = getNode(key);
		if (node != null)
		{
			return Optional.ofNullable(node.getValue()).orElse(defaultValue);
		}
		return defaultValue;
	}
	
	@Override
	public void replaceAll(BiFunction<? super String, ? super V, ? extends V> function)
	{
		TrieIterator trieIterator = new TrieIterator();
		while (trieIterator.hasNext())
		{
			Entry<String, V> entry = trieIterator.next();
			function.apply(entry.getKey(), entry.getValue());
		}
	}
	
	@Override
	public boolean replace(String key, V oldValue, V newValue)
	{
		Node node = getNode(key);
		if (node != null)
		{
			if (Objects.equals(oldValue, node.getValue()))
			{
				node.setValue(newValue);
				return true;				
			}
		}
		return false;
	}
	
	@Override
	public V replace(String key, V newValue)
	{
		Node node = getNode(key);
		if (node != null)
		{
			V oldValue = node.getValue();
			node.setValue(newValue);
			return oldValue;
		}
		return null;
	}
	
	@Override
	public boolean containsValue(Object value)
	{
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public Set<String> keySet()
	{
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Collection<V> values()
	{
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Set<Entry<String, V>> entrySet()
	{
		return new EntrySet();
	}

	/**
	 * @param printStream
	 * 
	 * @deprecated For debug purposes only
	 */
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
		private final Node parent;
		private Node[] children = EMPTY;
		private char offset = 0;
		private String key;
		private V value;

		public Node(TrieMap<V>.Node parent)
		{
			super();
			this.parent = parent;
		}

		private boolean isElement()
		{
			return key != null;
		}
		
		private void remove()
		{
			if (isElement())
			{
				key = null;
				value = null;
				size--;
			}
			if (children == EMPTY)
			{
				Node current = this;
				while (current.parent != null && removeChildAndCountRemaining(current.parent, current) == 0)
				{
					current = current.parent;
				}
			}
		}
		
		/**
		 * Search {@code child} through {@code parent.children}, set it to <code>null</code>,
		 * and return the count of nonnull remaining children.
		 * 
		 * @param parent
		 * @param child
		 * @return
		 */
		private int removeChildAndCountRemaining(Node parent, Node child)
		{
			int count = 0, left = -1, right = -1;
			
			for (int i = 0; i < parent.children.length; i++)
			{
				if (parent.children[i] != null)
				{
					if (parent.children[i] == child)
					{
						parent.children[i] = null;
					}
					else
					{
						if (left == -1)
						{
							left = i;
						}
						right = i;
						count++;
					}
				}
			}
			
			if (count > 0)
			{
				int newLength = ( right - left ) + 1 ;
				Node[] newChildren = newNodes(newLength);
				System.arraycopy(parent.children, left, newChildren, 0, newLength);
				parent.children = newChildren;
			}
			else
			{
				parent.children = EMPTY;
				parent.offset = 0;
			}
			
			return count;
		}
		
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
	
	class CharArrayComparator implements Comparator<CharSequence>
	{
		@Override
		public int compare(CharSequence o1, CharSequence o2)
		{
			if (o1 != null && o2 != null)
			{
				int compare = 0;
				int length = Math.min(o1.length(), o2.length());
				for (int index = 0; compare == 0 && index < length; index++)
				{
					compare = charTranslator.applyAsChar(o1.charAt(index)) - charTranslator.applyAsChar(o2.charAt(index));
				}
				return compare;
			}
			throw new NullPointerException("TrieMap does not permit null keys, nor do this comparator!");
		}
	}
	
	class TrieIterator implements Spliterator<Map.Entry<String, V>>, Iterator<Map.Entry<String, V>>
	{
		// pre-order traversal
		Stack<Node> stack = new Stack<>();
		private Node next = null, savedNext = null;
		TrieIterator()
		{
			stack.push(root);
		}
		
		@Override
		public boolean hasNext()
		{
			while ( this.next == null && ! stack.isEmpty() )
			{
				Node current = stack.pop();
				if (current.isElement())
				{
					this.next = current;
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

			return this.next != null;
		}

		@Override
		public Entry<String, V> next()
		{
			if (next != null)
			{
				savedNext = next;
				next = null;
				return savedNext;
			}
			throw new NoSuchElementException("The iteration has no more elements");
		}
		
		@Override
		public void remove()
		{
			if (savedNext != null)
			{
				if (savedNext.isElement())
				{
					savedNext.remove();
					savedNext = null;
				}
			}
			throw new IllegalStateException("The next method has not yet been called, or the remove method has already been called after the last call to the next method");
		}

		@Override
		public boolean tryAdvance(Consumer<? super Entry<String, V>> action)
		{
			if (hasNext())
			{
				action.accept(next());
				return true;
			}
			return false;
		}

		@Override
		public Spliterator<Entry<String, V>> trySplit()
		{
			return null;
		}

		@Override
		public long estimateSize()
		{
			return size();
		}

		@Override
		public int characteristics()
		{
			return DISTINCT | NONNULL | ORDERED | SORTED | SIZED | SUBSIZED;
		}

		@Override
		public void forEachRemaining(Consumer<? super Entry<String, V>> action)
		{
			// The default implementation is fine for me
			Spliterator.super.forEachRemaining(action);
		}

		@Override
		public long getExactSizeIfKnown()
		{
			return estimateSize();
		}

		@Override
		public boolean hasCharacteristics(int characteristics)
		{
			// The default implementation is fine for me
			return Spliterator.super.hasCharacteristics(characteristics);
		}

		@Override
		public Comparator<? super Entry<String, V>> getComparator()
		{
			// TODO Auto-generated method stub
			return Spliterator.super.getComparator();
		}

	}
	
	class EntrySet extends AbstractSet<Entry<String, V>>
	{
		@Override
		public int size()
		{
			return TrieMap.this.size();
		}

		@Override
		public Iterator<Entry<String, V>> iterator()
		{
			return new TrieIterator();
		}
		
	}
}

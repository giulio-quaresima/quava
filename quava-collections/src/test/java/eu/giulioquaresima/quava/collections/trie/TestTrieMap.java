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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import eu.giulioquaresima.quava.collections.trie.TrieMap;
import eu.giulioquaresima.quava.collections.trie.TrieMap.Visitor;


public class TestTrieMap
{
	@Test
	public void testPutGet()
	{
		Map<String, Integer> italianNumbers = new LinkedHashMap<>();
		int count = 0;
		italianNumbers.put("Zero", count++);
		italianNumbers.put("Uno", count++);
		italianNumbers.put("Due", count++);
		italianNumbers.put("Tre", count++);
		italianNumbers.put("Quattro", count++);
		italianNumbers.put("Cinque", count++);
		italianNumbers.put("Sei", count++);
		italianNumbers.put("Settecento", 700);
		italianNumbers.put("Sette", count++);
		italianNumbers.put("Otto", count++);
		italianNumbers.put("Nove", count++);
		
		Map<String, Integer> trieMap = new TrieMap<>();
		assertEquals(0, trieMap.size());
		for (Map.Entry<String, Integer> entry : italianNumbers.entrySet())
		{
			trieMap.put(entry.getKey(), entry.getValue());
		}
		assertEquals(italianNumbers.size(), trieMap.size());
		for (Map.Entry<String, Integer> entry : italianNumbers.entrySet())
		{
			assertTrue(trieMap.containsKey(entry.getKey()));
			assertEquals(entry.getValue(), trieMap.get(entry.getKey()));
		}
		
		Iterator<String> iterator = trieMap.keySet().iterator();
		assertTrue(iterator.hasNext());
		assertEquals("Cinque", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("Due", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("Nove", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("Otto", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("Quattro", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("Sei", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("Sette", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("Settecento", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("Tre", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("Uno", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("Zero", iterator.next());
		assertFalse(iterator.hasNext());
	}
	
	@Test
	public void testOrder() throws URISyntaxException, IOException
	{
		Map<String, String> trie = new TrieMap<>();
		Path unordered = Paths.get(TestTrieMap.class.getResource("unordered.txt").toURI());
		Path ordered = Paths.get(TestTrieMap.class.getResource("ordered.txt").toURI());
		Files.lines(unordered).forEach(line -> trie.put(line, line));
		Iterator<String> expectedValues = Files.lines(ordered).iterator();
		Iterator<String> actualValues = trie.values().iterator();
		while (expectedValues.hasNext())
		{
			assertTrue(actualValues.hasNext());
			assertEquals(expectedValues.next(), actualValues.next());
		}
		assertFalse(actualValues.hasNext());
	}
	
	@Test
	public void testCase()
	{
		Map<String, String> trie;
		
		trie = new TrieMap<>();
		trie.put("Hi!", "Hi!");
		trie.put("hi!", "hi!");
		assertTrue(trie.containsKey("Hi!"));
		assertTrue(trie.containsKey("hi!"));
		assertEquals("Hi!", trie.get("Hi!"));
		assertEquals("hi!", trie.get("hi!"));
		assertEquals(2, trie.size());
		
		trie = new TrieMap<>(Character::toLowerCase);
		trie.put("Hi!", "Hi!");
		trie.put("hi!", "hi!");
		assertTrue(trie.containsKey("Hi!"));
		assertTrue(trie.containsKey("hi!"));
		assertEquals("hi!" /* note the lower 'h': the last put value overwrite any previous value */, trie.get("Hi!"));
		assertEquals("hi!", trie.get("hi!"));
		assertEquals(1, trie.size());
	}
	
	@Test
	public void testFindAll()
	{
		String text = "It is fundamental and a fundament of this data structure to be greedy. Greetings!";
		// INDEXES:    012345678901234567890123456789012345678901234567890123456789012345678901234567890
		// INDEXES:    0         10        20        30        40        50        60        70        80 
		Map<TrieMap<String>, Integer> mapOfMaps = new IdentityHashMap<>();
		mapOfMaps.put(new TrieMap<>(), 4);
		mapOfMaps.put(new TrieMap<>(Character::toLowerCase), 6);
		for (Map.Entry<TrieMap<String>, Integer> tuple : mapOfMaps.entrySet())
		{
			TrieMap<String> trieMap = tuple.getKey();
			trieMap.put("Fundament", "FUNDAMENT");
			trieMap.put("fundamental", "FUNDAMENTAL");
			trieMap.put("data", "DATA");
			trieMap.put("structure", "STRUCTURE");
			trieMap.put("GREEDY", "greedy");
			trieMap.put("ings", "INGS");
			AtomicInteger counter = new AtomicInteger(0);
			Visitor<String> visitor = (entry, start, end) -> {
				counter.incrementAndGet();
				switch (start)
				{
				case 6:
					assertEquals(17, end);
					assertEquals("FUNDAMENTAL", entry.getValue());
					break;
				case 24:
					assertEquals(33, end);
					assertEquals("FUNDAMENT", entry.getValue());
					break;
				case 42:
					assertEquals(46, end);
					assertEquals("DATA", entry.getValue());
					break;
				case 47:
					assertEquals(56, end);
					assertEquals("STRUCTURE", entry.getValue());
					break;
				case 63:
					assertEquals(69, end);
					assertEquals("greedy", entry.getValue());
					break;
				case 76:
					assertEquals(80, end);
					assertEquals("INGS", entry.getValue());
					break;
				default:
					assertFalse(true, String.format("Unexpected match at index %d!", start));	
				}
			};
			trieMap.findAll(text, visitor);
			assertEquals(tuple.getValue(), counter.get());
		}
	}
	
	public static void main(String[] args) throws URISyntaxException, IOException
	{
		TestTrieMap _self = new TestTrieMap();
		_self.testPutGet();
		_self.testOrder();
		_self.testCase();
		_self.testFindAll();
	}
}

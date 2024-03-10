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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.text.RandomStringGenerator;

public class TestDataProducer
{

	public static void main(String[] args) throws IOException
	{
		RandomStringGenerator randomStringGenerator = new RandomStringGenerator
				.Builder()
				.withinRange('a', 'z')
				.build()
				;
		Supplier<String> randomStringProducer = () -> randomStringGenerator.generate(1, 4);
		List<String> strings = Stream
				.generate(randomStringProducer)
				.limit(4096)
				.collect(Collectors.toList())
				;
		Path unordered = Paths.get(System.getProperty("user.home"), "unordered.txt");
		Path ordered = Paths.get(System.getProperty("user.home"), "ordered.txt");
		Files.write(unordered, strings);
		Files.write(ordered, new TreeSet<>(strings));
	}

}

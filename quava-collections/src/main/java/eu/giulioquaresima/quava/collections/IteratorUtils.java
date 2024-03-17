package eu.giulioquaresima.quava.collections;

import java.util.Iterator;
import java.util.function.Function;

public class IteratorUtils
{
	public static <T, R> Iterator<R> map(Iterator<T> iterator, Function<T, R> mapper)
	{
		return new Iterator<R>() {

			@Override
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			@Override
			public R next()
			{
				return mapper.apply(iterator.next());
			}

			@Override
			public void remove()
			{
				iterator.remove();
			}
			
		};
	}
}

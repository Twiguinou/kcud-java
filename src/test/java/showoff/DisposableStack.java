package showoff;

import java.util.LinkedList;

// ahah
public class DisposableStack extends LinkedList<Disposable> implements AutoCloseable
{
    public DisposableStack()
    {
        super();
    }

    public <E extends Disposable> E addAndGet(E e)
    {
        this.add(e);
        return e;
    }

    public void dump()
    {
        while (!this.isEmpty())
        {
            this.removeLast().dispose();
        }
    }

    @Override
    public void close()
    {
        this.dump();
    }
}

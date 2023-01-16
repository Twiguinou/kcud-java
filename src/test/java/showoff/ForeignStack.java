package showoff;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collection;

public class ForeignStack extends MemoryStack
{
    private final MemorySession m_session;

    protected ForeignStack(ByteBuffer container, long address, int size, MemorySession session)
    {
        super(container, address, size);
        this.m_session = session;
    }

    public static ForeignStack pushConfined(long bytes)
    {
        MemorySession session = MemorySession.openConfined();
        MemorySegment stack_segment = session.allocate(bytes);
        ForeignStack wrapper = new ForeignStack(stack_segment.asByteBuffer(), stack_segment.address().toRawLongValue(), (int)stack_segment.byteSize(), session);
        wrapper.push();
        return wrapper;
    }

    private static final int DEFAULT_STACK_SIZE;
    public static ForeignStack pushConfined()
    {
        return pushConfined(DEFAULT_STACK_SIZE);
    }

    @Override
    public void close()
    {
        super.close();
        if (this.m_session.isCloseable())
        {
            this.m_session.close();
        }
    }

    public <T extends CharSequence> PointerBuffer UTF8_list(final Collection<T> strings)
    {
        if (strings.isEmpty()) return null;
        PointerBuffer array = this.mallocPointer(strings.size());
        for (T cs : strings)
        {
            array.put(this.UTF8(cs, false));
        }
        return array.rewind();
    }

    static
    {
        try
        {
            Field field$DEFAULT_STACK_SIZE = MemoryStack.class.getDeclaredField("DEFAULT_STACK_SIZE");
            field$DEFAULT_STACK_SIZE.setAccessible(true);
            DEFAULT_STACK_SIZE = (int)field$DEFAULT_STACK_SIZE.get(null);
        }
        catch (IllegalAccessException | NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }
    }

}

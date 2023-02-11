package showoff;

import org.lwjgl.system.Checks;
import org.lwjgl.system.MemoryStack;
import sun.misc.Unsafe;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public class FrameAllocator extends MemoryStack implements SegmentAllocator, AutoCloseable
{
    private static final ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();
    protected static class Frame
    {
        MemorySegment segment;
        long next_ptr;
        Frame previous;
        Frame next;
        protected Frame(MemorySegment segment, Frame previous)
        {
            this.segment = segment;
            this.next_ptr = this.segment.address().toRawLongValue();
            this.previous = previous;
            this.next = null;
        }
    }

    private final MemorySession m_session;
    protected final long m_frameBytesSize;
    protected Frame m_currentFrame;
    protected boolean m_shadowFrame;

    protected FrameAllocator(MemorySession session, long frameSize)
    {
        super(null, Long.MAX_VALUE, 0);
        this.address = 0L;
        this.m_session = session;
        this.m_frameBytesSize = frameSize;
        this.m_currentFrame = null;
        this.m_shadowFrame = false;
    }

    public static FrameAllocator openConfined(long size)
    {
        FrameAllocator allocator = new FrameAllocator(MemorySession.openConfined(), size);
        allocator.push();
        return allocator;
    }

    private static final long _gDefaultStackAllocSize;
    public static FrameAllocator openConfined()
    {
        return openConfined(_gDefaultStackAllocSize);
    }

    private static final ThreadLocal<FrameAllocator> _gPerThreadAllocator;
    static
    {
        long t;
        try
        {
            t = Long.parseLong(System.getProperty("frame-allocator-stdsize"));
        }
        catch (Exception ignored)
        {
            t = 65536;
        }
        _gDefaultStackAllocSize = t;
        _gPerThreadAllocator = ThreadLocal.withInitial(() -> new FrameAllocator(MemorySession.openImplicit(), _gDefaultStackAllocSize));
    }
    public static FrameAllocator take()
    {
        return _gPerThreadAllocator.get();
    }
    public static FrameAllocator takeAndPush()
    {
        return take().push();
    }
    public static FrameAllocator takeAndPushIfEmpty()
    {
        FrameAllocator e = take();
        if (e.m_currentFrame == null || e.m_shadowFrame)
        {
            e.push();
        }
        return e;
    }

    @Override
    public MemorySegment allocate(long bytesSize, long bytesAlignment)
    {
        if (this.m_shadowFrame) return null;
        final long new_ptr = (this.m_currentFrame.next_ptr + bytesAlignment - 1) & -bytesAlignment;
        final long offset = new_ptr - this.m_currentFrame.segment.address().toRawLongValue();
        if ((offset + bytesSize) > this.m_currentFrame.segment.byteSize()) return null;
        this.m_currentFrame.next_ptr = new_ptr + bytesSize;
        return this.m_currentFrame.segment.asSlice(offset, bytesSize);
    }

    public MemorySession getSession()
    {
        return this.m_session.asNonCloseable();
    }

    @Override
    public long address() {throw new UnsupportedOperationException();}
    @Override public long getAddress() {throw new UnsupportedOperationException();}
    @Override public int getSize() {throw new UnsupportedOperationException();}
    @Override public int getFrameIndex() {throw new UnsupportedOperationException();}
    @Override public long getPointerAddress() {throw new UnsupportedOperationException();}

    @Override
    public int getPointer()
    {
        return this.m_currentFrame != null ? (int)this.m_currentFrame.next_ptr : 0;
    }

    @Override public void setPointer(int pointer) {throw new UnsupportedOperationException();}

    protected MemorySegment lwjglAllocate(int alignment, int size)
    {
        MemorySegment allocated = this.allocate(size, alignment);
        if (allocated == null) throw new OutOfMemoryError("Out of stack space.");
        return allocated;
    }

    @Override
    public long nmalloc(int alignment, int size)
    {
        return this.lwjglAllocate(alignment, size).address().toRawLongValue();
    }
    @Override
    public long ncalloc(int alignment, int num, int size)
    {
        return this.lwjglAllocate(alignment, num * size).fill((byte)0).address().toRawLongValue();
    }

    private static void checkAlignment(int alignment)
    {
        if (Integer.bitCount(alignment) != 1)
        {
            throw new IllegalArgumentException("Alignment must be a power-of-two value.");
        }
    }

    @Override
    public ByteBuffer malloc(int alignment, int size)
    {
        if (Checks.DEBUG) checkAlignment(alignment);
        return this.lwjglAllocate(alignment, size).asByteBuffer().order(NATIVE_ORDER);
    }
    @Override
    public ByteBuffer calloc(int alignment, int size)
    {
        if (Checks.DEBUG) checkAlignment(alignment);
        return this.lwjglAllocate(alignment, size).fill((byte)0).asByteBuffer().order(NATIVE_ORDER);
    }
    @Override
    public ByteBuffer malloc(int size)
    {
        return this.malloc(POINTER_SIZE, size);
    }
    @Override
    public ByteBuffer calloc(int size)
    {
        return this.calloc(POINTER_SIZE, size);
    }

    @Override
    public ShortBuffer mallocShort(int size)
    {
        return this.malloc(2, size << 1).asShortBuffer();
    }
    @Override
    public ShortBuffer callocShort(int size)
    {
        return this.calloc(2, size << 1).asShortBuffer();
    }

    @Override
    public IntBuffer mallocInt(int size)
    {
        return this.malloc(4, size << 2).asIntBuffer();
    }
    @Override
    public IntBuffer callocInt(int size)
    {
        return this.calloc(4, size << 2).asIntBuffer();
    }

    @Override
    public LongBuffer mallocLong(int size)
    {
        return this.malloc(8, size << 3).asLongBuffer();
    }
    @Override
    public LongBuffer callocLong(int size)
    {
        return this.calloc(8, size << 3).asLongBuffer();
    }

    @Override
    public FloatBuffer mallocFloat(int size)
    {
        return this.malloc(4, size << 2).asFloatBuffer();
    }
    @Override
    public FloatBuffer callocFloat(int size)
    {
        return this.calloc(4, size << 2).asFloatBuffer();
    }

    @Override
    public DoubleBuffer mallocDouble(int size)
    {
        return this.malloc(4, size << 3).asDoubleBuffer();
    }
    @Override
    public DoubleBuffer callocDouble(int size)
    {
        return this.calloc(8, size << 3).asDoubleBuffer();
    }

    @Override
    public void close()
    {
        if (this.m_session.isCloseable())
        {
            this.m_session.close();
        }
        else
        {
            this.pop();
        }
    }

    @Override
    public FrameAllocator push()
    {
        if (this.m_currentFrame != null)
        {
            if (!this.m_shadowFrame)
            {
                if (this.m_currentFrame.next == null)
                {
                    this.m_currentFrame.next = new Frame(this.m_session.allocate(this.m_frameBytesSize), this.m_currentFrame);
                }
                this.m_currentFrame = this.m_currentFrame.next;
            }
            else
            {
                this.m_shadowFrame = false;
            }
        }
        else
        {
            this.m_currentFrame = new Frame(this.m_session.allocate(this.m_frameBytesSize), null);
        }
        return this;
    }

    @Override
    public FrameAllocator pop()
    {
        if (this.m_currentFrame != null && !this.m_shadowFrame)
        {
            this.m_currentFrame.next_ptr = this.m_currentFrame.segment.address().toRawLongValue();
            if (this.m_currentFrame.previous == null)
            {
                this.m_shadowFrame = true;
            }
            else
            {
                this.m_currentFrame = this.m_currentFrame.previous;
            }
        }
        return this;
    }

    static
    {
        try
        {
            final Unsafe unsafe;
            {
                Field unsafe_field = Unsafe.class.getDeclaredField("theUnsafe");
                unsafe_field.setAccessible(true);
                unsafe = (Unsafe)unsafe_field.get(null);
            }

            Field field = MemoryStack.class.getDeclaredField("DEFAULT_STACK_FRAMES");
            unsafe.putInt(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), 0);

            field = MemoryStack.class.getDeclaredField("DEFAULT_STACK_SIZE");
            unsafe.putInt(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), 0);
        }
        catch (IllegalAccessException | NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }
    }
}

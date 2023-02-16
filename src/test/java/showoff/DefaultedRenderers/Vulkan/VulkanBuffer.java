package showoff.DefaultedRenderers.Vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import showoff.FrameAllocator;

public interface VulkanBuffer
{
    long get();
    long size();
    void free();

    void map(PointerBuffer ppData) throws VulkanException;
    void unmap();

    default void put(MemoryStack stack, long data, long size) throws VulkanException
    {
        PointerBuffer ppData = stack.mallocPointer(1);
        this.map(ppData);
        long pData = ppData.get(0);
        MemoryUtil.memCopy(data, pData, size);
        this.unmap();
    }
    default void put(long data, long size) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            this.put(allocator, data, size);
        }
    }
}

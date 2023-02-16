package showoff.DefaultedRenderers.Vulkan;

import org.lwjgl.system.MemoryStack;
import showoff.FrameAllocator;

public abstract class VulkanAllocator
{
    protected final LogicalDevice m_device;

    public VulkanAllocator(LogicalDevice device)
    {
        this.m_device = device;
    }

    public abstract VulkanBuffer createBuffer(MemoryStack stack, long size, int usage, int[] queueFamilies, int mem_usage) throws VulkanException;
    public VulkanBuffer createBuffer(long size, int usage, int[] queueFamilies, int mem_usage) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            return this.createBuffer(allocator, size, usage, queueFamilies, mem_usage);
        }
    }

    public abstract VulkanImage.MemoryBound createImage(MemoryStack stack, int width, int height, int format, int tiling, int usage, int memoryProperties, boolean cubemap, int mip_levels, int sample_count, int aspectFlags,
                                                        int componentSwizzleR, int componentSwizzleG, int componentSwizzleB, int componentSwizzleA, int mem_usage) throws VulkanException;
    public VulkanImage.MemoryBound createImage(int width, int height, int format, int tiling, int usage, int memoryProperties, boolean cubemap, int mip_levels, int sample_count, int aspectFlags,
                                               int componentSwizzleR, int componentSwizzleG, int componentSwizzleB, int componentSwizzleA, int mem_usage) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            return this.createImage(allocator, width, height, format, tiling, usage, memoryProperties, cubemap, mip_levels, sample_count, aspectFlags, componentSwizzleR, componentSwizzleG, componentSwizzleB, componentSwizzleA, mem_usage);
        }
    }
}

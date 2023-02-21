package showoff.DefaultedRenderers.Vulkan;

import org.lwjgl.system.MemoryStack;

import static org.lwjgl.vulkan.VK13.*;

public abstract class VulkanAllocator
{
    protected final LogicalDevice m_device;

    public VulkanAllocator(LogicalDevice device)
    {
        this.m_device = device;
    }

    public abstract VulkanBuffer createBuffer(MemoryStack stack, long size, int usage, int[] queueFamilies, int mem_properties) throws VulkanException;
    public abstract VulkanImage.MemoryBound createImage(MemoryStack stack, int width, int height, int format, int tiling, int usage, boolean cubemap, int mip_levels, int sample_count, int aspectFlags,
                                                        int componentSwizzleR, int componentSwizzleG, int componentSwizzleB, int componentSwizzleA, int mem_properties) throws VulkanException;
    public VulkanImage.MemoryBound createTexture2D(MemoryStack stack, int width, int height, int format) throws VulkanException
    {
        return createImage(stack, width, height, format, VK_IMAGE_TILING_OPTIMAL,
                VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, false, 1, VK_SAMPLE_COUNT_1_BIT, VK_IMAGE_ASPECT_COLOR_BIT,
                VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
    }
}

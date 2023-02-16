package showoff.DefaultedRenderers.Vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK13.*;

public class VmaAllocator extends VulkanAllocator
{
    private record VmaBuffer(long allocator, long handle, long allocations, long size) implements VulkanBuffer
    {
        @Override public long get() {return this.handle;}
        @Override public void free()
        {
            vmaDestroyBuffer(this.allocator, this.handle, this.allocations);
        }
        @Override public void map(PointerBuffer ppData) throws VulkanException
        {
            VulkanException.check(vmaMapMemory(this.allocator, this.allocations, ppData), "Failed to map vma buffer memory.");
        }
        @Override public void unmap()
        {
            vmaUnmapMemory(this.allocator, this.allocations);
        }
    }
    private record VmaImage(VkDevice device, long allocator, long handle, long allocations, long imageView) implements VulkanImage.MemoryBound
    {
        @Override
        public long get() {return this.handle;}
        @Override public void free()
        {
            vkDestroyImageView(device, this.imageView, null);
            vmaDestroyImage(this.allocator, this.handle, this.allocations);
        }
        @Override public long memory()
        {
            return 0;
        }
    }

    public VmaAllocator(LogicalDevice device)
    {
        super(device);
    }

    @Override
    public VulkanBuffer createBuffer(MemoryStack stack, long size, int usage, int[] queueFamilies, int mem_usage) throws VulkanException
    {
        VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(size)
                .usage(usage);
        if (queueFamilies.length > 1)
        {
            bufferCreateInfo.sharingMode(VK_SHARING_MODE_CONCURRENT)
                    .queueFamilyIndexCount(queueFamilies.length)
                    .pQueueFamilyIndices(stack.ints(queueFamilies));
        }
        else
        {
            bufferCreateInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
        }

        VmaAllocationCreateInfo vmaAllocCreateInfo = VmaAllocationCreateInfo.calloc(stack)
                .usage(mem_usage);

        LongBuffer pBuffer = stack.mallocLong(1);
        PointerBuffer pAllocations = stack.mallocPointer(1);
        VulkanException.check(vmaCreateBuffer(this.m_device.getVmaAllocator(), bufferCreateInfo, vmaAllocCreateInfo, pBuffer, pAllocations, null), "Vma buffer allocation failed.");
        return new VmaBuffer(this.m_device.getVmaAllocator(), pBuffer.get(0), pAllocations.get(0), size);
    }

    @Override
    public VulkanImage.MemoryBound createImage(MemoryStack stack, int width, int height, int format, int tiling, int usage, int memoryProperties, boolean cubemap, int mip_levels, int sample_count, int aspectFlags,
                                               int componentSwizzleR, int componentSwizzleG, int componentSwizzleB, int componentSwizzleA, int mem_usage) throws VulkanException
    {
        VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .imageType(VK_IMAGE_TYPE_2D)
                .extent(e -> e.set(width, height, 1))
                .mipLevels(mip_levels)
                .format(format)
                .tiling(tiling)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .usage(usage)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .samples(sample_count);
        if (cubemap)
        {
            imageCreateInfo.flags(VK_IMAGE_CREATE_CUBE_COMPATIBLE_BIT)
                    .arrayLayers(6);
        }
        else
        {
            imageCreateInfo.arrayLayers(1);
        }

        VmaAllocationCreateInfo vmaAllocCreateInfo = VmaAllocationCreateInfo.calloc(stack)
                .usage(mem_usage);

        LongBuffer pImage = stack.mallocLong(1);
        PointerBuffer pAllocations = stack.mallocPointer(1);
        VulkanException.check(vmaCreateImage(this.m_device.getVmaAllocator(), imageCreateInfo, vmaAllocCreateInfo, pImage, pAllocations, null), "Vma image creation/allocation failed.");

        VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(pImage.get(0))
                .format(format)
                .components(c -> c.set(componentSwizzleR, componentSwizzleG, componentSwizzleB, componentSwizzleA))
                .subresourceRange(r -> r.aspectMask(aspectFlags).baseMipLevel(0).levelCount(mip_levels).baseArrayLayer(0));
        if (cubemap)
        {
            imageViewCreateInfo.viewType(VK_IMAGE_VIEW_TYPE_CUBE)
                    .subresourceRange().layerCount(6);
        }
        else
        {
            imageViewCreateInfo.viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .subresourceRange().layerCount(1);
        }
        LongBuffer pImageView = stack.mallocLong(1);
        VulkanException.orElse(vkCreateImageView(this.m_device.get(), imageViewCreateInfo, null, pImageView), "Image view creation failed.", () -> vmaDestroyImage(this.m_device.getVmaAllocator(), pImage.get(0), pAllocations.get(0)));

        return new VmaImage(this.m_device.get(), this.m_device.getVmaAllocator(), pImage.get(0), pAllocations.get(0), pImageView.get(0));
    }
}

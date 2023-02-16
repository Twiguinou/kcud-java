package showoff.DefaultedRenderers.Vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK13.*;

public class DefaultAllocator extends VulkanAllocator
{
    private record DefaultBuffer(VkDevice device, long handle, long memory, long size) implements VulkanBuffer
    {
        @Override public long get() {return this.handle;}
        @Override public void free()
        {
            vkFreeMemory(this.device, this.memory, null);
            vkDestroyBuffer(this.device, this.handle, null);
        }

        @Override public void map(PointerBuffer ppData) throws VulkanException
        {
            VulkanException.check(vkMapMemory(this.device, this.memory, 0L, VK_WHOLE_SIZE, 0, ppData), "Failed to map buffer memory.");
        }
        @Override public void unmap()
        {
            vkUnmapMemory(this.device, this.memory);
        }
    }
    private record DefaultImage(VkDevice device, long handle, long memory, long imageView) implements VulkanImage.MemoryBound
    {
        @Override public long get() {return this.handle;}
        @Override public void free()
        {
            vkDestroyImageView(this.device, this.imageView, null);
            vkDestroyImage(this.device, this.handle, null);
            vkFreeMemory(this.device, this.memory, null);
        }
    }

    public DefaultAllocator(LogicalDevice device)
    {
        super(device);
    }

    private int selectMemoryType(int filter, int properties) throws VulkanException
    {
        final VkPhysicalDeviceMemoryProperties memoryProperties = this.m_device.getBoundPhysicalDevice().memoryProperties();
        for (int i = 0; i < memoryProperties.memoryTypeCount(); i++)
        {
            if ((filter & (1 << i)) != 0 && (memoryProperties.memoryTypes(i).propertyFlags() & properties) == properties)
            {
                return i;
            }
        }
        throw new VulkanException("Could not find matching memory type.");
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

        LongBuffer pVkDest = stack.mallocLong(1);
        VulkanException.check(vkCreateBuffer(this.m_device.get(), bufferCreateInfo, null, pVkDest), "Vulkan buffer creation failed.");

        VkMemoryRequirements memoryRequirements = VkMemoryRequirements.malloc(stack);
        vkGetBufferMemoryRequirements(this.m_device.get(), pVkDest.get(0), memoryRequirements);
        if (memoryRequirements.size() > size)
        {
            bufferCreateInfo.size(memoryRequirements.size());
            vkDestroyBuffer(this.m_device.get(), pVkDest.get(0), null);
            VulkanException.check(vkCreateBuffer(this.m_device.get(), bufferCreateInfo, null, pVkDest), "Could not recreate buffer to satisfy memory requirements.");
        }
        final long buffer = pVkDest.get(0);

        VkMemoryAllocateInfo memoryAllocateInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memoryRequirements.size())
                .memoryTypeIndex(selectMemoryType(memoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT));
        VulkanException.orElse(vkAllocateMemory(this.m_device.get(), memoryAllocateInfo, null, pVkDest), "Failed to allocate memory for buffer.", () -> vkDestroyBuffer(this.m_device.get(), buffer, null));

        DefaultBuffer ret = new DefaultBuffer(this.m_device.get(), buffer, pVkDest.get(0), memoryRequirements.size());
        VulkanException.orElse(vkBindBufferMemory(this.m_device.get(), buffer, pVkDest.get(0), 0L), "Failed to bind buffer to allocated memory.", ret::free);
        return ret;
    }

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

        LongBuffer pVkDest = stack.mallocLong(1);
        VulkanException.check(vkCreateImage(this.m_device.get(), imageCreateInfo, null, pVkDest), "Vulkan image creation failed.");
        final long image = pVkDest.get(0);

        VkMemoryRequirements memoryRequirements = VkMemoryRequirements.malloc(stack);
        vkGetImageMemoryRequirements(this.m_device.get(), image, memoryRequirements);

        VkMemoryAllocateInfo memoryAllocateInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memoryRequirements.size())
                .memoryTypeIndex(selectMemoryType(memoryRequirements.memoryTypeBits(), memoryProperties));
        VulkanException.orElse(vkAllocateMemory(this.m_device.get(), memoryAllocateInfo, null, pVkDest), "Failed to allocate memory for image.", () -> vkDestroyImage(this.m_device.get(), image, null));
        final long memory = pVkDest.get(0);

        VulkanException.orElse(vkBindImageMemory(this.m_device.get(), image, memory, 0L), "Failed to bind memory to image.", () ->
        {
            vkDestroyImage(this.m_device.get(), image, null);
            vkFreeMemory(this.m_device.get(), memory, null);
        });

        VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(image)
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
        VulkanException.orElse(vkCreateImageView(this.m_device.get(), imageViewCreateInfo, null, pVkDest), "Failed to create image view.", () ->
        {
            vkDestroyImage(this.m_device.get(), image, null);
            vkFreeMemory(this.m_device.get(), memory, null);
        });

        return new DefaultImage(this.m_device.get(), image, memory, pVkDest.get(0));
    }
}

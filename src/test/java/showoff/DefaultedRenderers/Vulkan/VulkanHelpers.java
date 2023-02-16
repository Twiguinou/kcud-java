package showoff.DefaultedRenderers.Vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import showoff.FrameAllocator;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK13.*;

public final class VulkanHelpers
{private VulkanHelpers() {}

    public static void beginCommandBuffer(MemoryStack stack, VkCommandBuffer commandBuffer, int flags) throws VulkanException
    {
        VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(flags);
        VulkanException.check(vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo));
    }
    public static void beginCommandBuffer(VkCommandBuffer commandBuffer, int flags) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            beginCommandBuffer(allocator, commandBuffer, flags);
        }
    }

    public static void submitCommandBuffer(MemoryStack stack, VkCommandBuffer commandBuffer, long fence, LogicalDevice.Queue queue) throws VulkanException
    {
        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(stack.pointers(commandBuffer));
        synchronized (queue.handle())
        {
            VulkanException.check(vkQueueSubmit(queue.handle(), submitInfo, fence));
        }
    }
    public static void submitCommandBuffer(VkCommandBuffer commandBuffer, long fence, LogicalDevice.Queue queue) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            submitCommandBuffer(allocator, commandBuffer, fence, queue);
        }
    }

    public static long createFence(MemoryStack stack, VkDevice device, int flags) throws VulkanException
    {
        VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                .flags(flags);
        LongBuffer pFence = stack.mallocLong(1);
        VulkanException.check(vkCreateFence(device, fenceCreateInfo, null, pFence));
        return pFence.get(0);
    }
    public static long createFence(VkDevice device, int flags) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            return createFence(allocator, device, flags);
        }
    }

    public static long createSemaphore(MemoryStack stack, VkDevice device, int type, int initialValue) throws VulkanException
    {
        VkSemaphoreTypeCreateInfo semaphoreTypeCreateInfo = VkSemaphoreTypeCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO)
                .semaphoreType(type)
                .initialValue(initialValue);
        VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                .pNext(semaphoreTypeCreateInfo);
        LongBuffer pSemaphore = stack.mallocLong(1);
        VulkanException.check(vkCreateSemaphore(device, semaphoreCreateInfo, null, pSemaphore));
        return pSemaphore.get(0);
    }
    public static long createSemaphore(VkDevice device, int type, int initialValue) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            return createSemaphore(allocator, device, type, initialValue);
        }
    }

    public static long createImageView(MemoryStack stack, VkDevice device, int format, long image, int aspectFlags, boolean cubemap, int mip_levels,
                                       int componentSwizzleR, int componentSwizzleG, int componentSwizzleB, int componentSwizzleA) throws VulkanException
    {
        VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(image)
                .format(format)
                .components(c -> c.set(componentSwizzleR, componentSwizzleG, componentSwizzleB, componentSwizzleA))
                .subresourceRange(sr -> sr
                        .aspectMask(aspectFlags)
                        .baseMipLevel(0)
                        .levelCount(mip_levels)
                        .baseArrayLayer(0));
        if (cubemap)
        {
            imageViewCreateInfo.viewType(VK_IMAGE_VIEW_TYPE_CUBE)
                    .subresourceRange().layerCount(6);
        }
        else
        {
            imageViewCreateInfo.subresourceRange().layerCount(1);
        }

        LongBuffer pImageView = stack.mallocLong(1);
        VulkanException.check(vkCreateImageView(device, imageViewCreateInfo, null, pImageView));
        return pImageView.get(0);
    }
    public static long createImageView(VkDevice device, int format, long image, int aspectFlags, boolean cubemap, int mip_levels,
                                       int componentSwizzleR, int componentSwizzleG, int componentSwizzleB, int componentSwizzleA) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            return createImageView(allocator, device, format, image, aspectFlags, cubemap, mip_levels, componentSwizzleR, componentSwizzleG, componentSwizzleB, componentSwizzleA);
        }
    }

    public static int getMaxSampleCount(final VkPhysicalDeviceProperties properties)
    {
        final VkPhysicalDeviceLimits limits = properties.limits();
        final int counts = limits.framebufferColorSampleCounts() & limits.framebufferDepthSampleCounts();
        if ((counts & VK_SAMPLE_COUNT_64_BIT) != 0) return VK_SAMPLE_COUNT_64_BIT;
        if ((counts & VK_SAMPLE_COUNT_32_BIT) != 0) return VK_SAMPLE_COUNT_32_BIT;
        if ((counts & VK_SAMPLE_COUNT_16_BIT) != 0) return VK_SAMPLE_COUNT_16_BIT;
        if ((counts & VK_SAMPLE_COUNT_8_BIT) != 0) return VK_SAMPLE_COUNT_8_BIT;
        if ((counts & VK_SAMPLE_COUNT_4_BIT) != 0) return VK_SAMPLE_COUNT_4_BIT;
        if ((counts & VK_SAMPLE_COUNT_2_BIT) != 0) return VK_SAMPLE_COUNT_2_BIT;
        return VK_SAMPLE_COUNT_1_BIT;
    }

    public static int findSupportedFormat(VkPhysicalDevice physicalDevice, int[] candidates, int tiling, int features) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkFormatProperties properties = VkFormatProperties.malloc(allocator);
            for (int candidate : candidates)
            {
                vkGetPhysicalDeviceFormatProperties(physicalDevice, candidate, properties);
                if ((tiling == VK_IMAGE_TILING_LINEAR && (properties.linearTilingFeatures() & features) == features) ||
                        (tiling == VK_IMAGE_TILING_OPTIMAL && (properties.optimalTilingFeatures() & features) == features))
                {
                    return candidate;
                }
            }
            throw new VulkanException("No compatible format found");
        }
    }

    public record DescriptorInfo(int binding, int descriptorType, int shaderStage) {}
    public static long createDescriptorSetLayout(VkDevice device, DescriptorInfo[] uboInfo, DescriptorInfo[] samplersInfo) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(uboInfo.length + samplersInfo.length, allocator);
            for (int i = 0; i < uboInfo.length; i++)
            {
                bindings.get(i)
                        .binding(uboInfo[i].binding)
                        .descriptorType(uboInfo[i].descriptorType)
                        .descriptorCount(1)
                        .stageFlags(uboInfo[i].shaderStage);
            }

            for (int i = 0; i < samplersInfo.length; i++)
            {
                bindings.get(uboInfo.length + i)
                        .binding(samplersInfo[i].binding)
                        .descriptorType(samplersInfo[i].descriptorType)
                        .descriptorCount(1)
                        .stageFlags(samplersInfo[i].shaderStage);
            }

            VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(bindings);
            LongBuffer pLayout = allocator.mallocLong(1);
            VulkanException.check(vkCreateDescriptorSetLayout(device, descriptorSetLayoutCreateInfo, null, pLayout));
            return pLayout.get(0);
        }
    }
}

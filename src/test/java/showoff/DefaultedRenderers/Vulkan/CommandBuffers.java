package showoff.DefaultedRenderers.Vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;
import showoff.FrameAllocator;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK12.*;

public class CommandBuffers
{
    public static long createPool(VkDevice device, int flags, int queue_index) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPushIfEmpty())
        {
            VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(flags)
                    .queueFamilyIndex(queue_index);
            LongBuffer pCommandPool = allocator.mallocLong(1);
            VulkanException.check(vkCreateCommandPool(device, commandPoolCreateInfo, null, pCommandPool));
            return pCommandPool.get(0);
        }
    }

    public static long createPool(VkDevice device, int queue_index) throws VulkanException
    {
        return createPool(device, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT | VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT, queue_index);
    }

    public static void destroyPool(VkDevice device, long commandPool)
    {
        vkDestroyCommandPool(device, commandPool, null);
    }

    public static VkCommandBuffer allocate(VkDevice device, long commandPool, int level) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPushIfEmpty())
        {
            VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool)
                    .level(level)
                    .commandBufferCount(1);
            PointerBuffer pCommandBuffer = allocator.mallocPointer(1);
            VulkanException.check(vkAllocateCommandBuffers(device, commandBufferAllocateInfo, pCommandBuffer));
            return new VkCommandBuffer(pCommandBuffer.get(0), device);
        }
    }

    public static VkCommandBuffer[] allocate(VkDevice device, long commandPool, int count, int level) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool)
                    .level(level)
                    .commandBufferCount(count);
            PointerBuffer pCommandBuffers = allocator.mallocPointer(count);
            VulkanException.check(vkAllocateCommandBuffers(device, commandBufferAllocateInfo, pCommandBuffers));
            VkCommandBuffer[] result = new VkCommandBuffer[count];
            for (int i = 0; i < count; i++) result[i] = new VkCommandBuffer(pCommandBuffers.get(i), device);
            return result;
        }
    }

    public static void submit(VkCommandBuffer commandBuffer, VkQueue queue) throws VulkanException
    {
        VulkanException.check(vkEndCommandBuffer(commandBuffer));
        try (FrameAllocator allocator = FrameAllocator.takeAndPushIfEmpty())
        {
            VkSubmitInfo submitInfo = VkSubmitInfo.malloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(allocator.pointers(commandBuffer));
            VulkanException.check(vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE));
            VulkanException.check(vkQueueWaitIdle(queue));
        }
    }

    public static void free(VkDevice device, long commandPool, VkCommandBuffer commandBuffer)
    {
        vkFreeCommandBuffers(device, commandPool, commandBuffer);
    }
}

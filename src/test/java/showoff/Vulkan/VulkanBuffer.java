package showoff.Vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkSubmitInfo;
import showoff.DisposableStack;

import static org.lwjgl.vulkan.VK13.*;

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

    default void upload(MemoryStack stack, CommandPool commandPool, LogicalDevice.Queue queue, VulkanAllocator allocator, long data, long size) throws VulkanException
    {
        VulkanBuffer stagingBuffer = allocator.createBuffer(stack, size, VK_IMAGE_USAGE_TRANSFER_SRC_BIT, new int[]{queue.family()}, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        try (DisposableStack disposables = new DisposableStack())
        {
            disposables.add(stagingBuffer::free);
            stagingBuffer.put(stack, data, size);
            VkBufferCopy.Buffer regions = VkBufferCopy.calloc(1, stack)
                    .size(size);

            VkCommandBuffer commandBuffer = commandPool.allocate(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            disposables.add(() -> commandPool.free(commandBuffer));
            VulkanHelpers.beginCommandBuffer(stack, commandBuffer, VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            vkCmdCopyBuffer(commandBuffer, stagingBuffer.get(), this.get(), regions);
            VulkanException.check(vkEndCommandBuffer(commandBuffer));

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(commandBuffer));
            VulkanException.check(vkQueueSubmit(queue.handle(), submitInfo, VK_NULL_HANDLE));
            VulkanException.check(vkQueueWaitIdle(queue.handle()));
        }
    }
}

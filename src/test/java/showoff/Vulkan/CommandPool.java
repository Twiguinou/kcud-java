package showoff.Vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import showoff.Disposable;
import showoff.FrameAllocator;

import java.nio.LongBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.vulkan.VK13.*;

public class CommandPool implements Disposable
{
    private final long m_internalHandle;
    private final VkDevice device;
    private final Set<Long> m_allocatedBuffers;

    public CommandPool(VkDevice device, int flags, int queueFamily) throws VulkanException
    {
        this.device = device;
        this.m_allocatedBuffers = new HashSet<>();
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(flags)
                    .queueFamilyIndex(queueFamily);
            LongBuffer pCommandPool = allocator.mallocLong(1);
            VulkanException.check(vkCreateCommandPool(this.device, commandPoolCreateInfo, null, pCommandPool), "Vulkan command pool creation failed.");
            this.m_internalHandle = pCommandPool.get(0);
        }
    }

    public long get()
    {
        return this.m_internalHandle;
    }

    public void reset() throws VulkanException
    {
        VulkanException.check(vkResetCommandPool(this.device, this.m_internalHandle, 0), "Could not reset command pool.");
    }

    public VkCommandBuffer allocate(int level) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(this.m_internalHandle)
                    .level(level)
                    .commandBufferCount(1);
            PointerBuffer pCommandBuffer = allocator.mallocPointer(1);
            VulkanException.check(vkAllocateCommandBuffers(this.device, commandBufferAllocateInfo, pCommandBuffer), "Failed to allocate command buffer.");
            VkCommandBuffer ret = new VkCommandBuffer(pCommandBuffer.get(0), this.device);
            this.m_allocatedBuffers.add(ret.address());
            return ret;
        }
    }

    public VkCommandBuffer[] allocate(int count, int level) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(this.m_internalHandle)
                    .level(level)
                    .commandBufferCount(count);
            PointerBuffer pCommandBuffers = allocator.mallocPointer(count);
            VulkanException.check(vkAllocateCommandBuffers(device, commandBufferAllocateInfo, pCommandBuffers), "Failed to allocate command buffers.");
            VkCommandBuffer[] result = new VkCommandBuffer[count];
            for (int i = 0; i < count; i++)
            {
                result[i] = new VkCommandBuffer(pCommandBuffers.get(i), device);
                this.m_allocatedBuffers.add(result[i].address());
            }
            return result;
        }
    }

    public void free(VkCommandBuffer commandBuffer)
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            vkFreeCommandBuffers(this.device, this.m_internalHandle, allocator.pointers(commandBuffer));
            this.m_allocatedBuffers.remove(commandBuffer.address());
        }
    }

    public void free(VkCommandBuffer[] commandBuffers)
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            vkFreeCommandBuffers(this.device, this.m_internalHandle, allocator.pointers(commandBuffers));
            for (final VkCommandBuffer commandBuffer : commandBuffers)
            {
                this.m_allocatedBuffers.remove(commandBuffer.address());
            }
        }
    }

    @Override
    public void dispose()
    {
        // This step is unnecessary as every command buffer allocated will be freed anyway.
        /*if (!this.m_allocatedBuffers.isEmpty())
        {
            try (FrameAllocator allocator = FrameAllocator.takeAndPush())
            {
                PointerBuffer pCommandBuffers = allocator.mallocPointer(this.m_allocatedBuffers.size());
                this.m_allocatedBuffers.forEach(pCommandBuffers::put);
                vkFreeCommandBuffers(this.device, this.m_internalHandle, pCommandBuffers.rewind());
            }
        }*/
        vkDestroyCommandPool(this.device, this.m_internalHandle, null);
    }
}

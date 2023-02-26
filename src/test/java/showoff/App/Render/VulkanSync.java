package showoff.App.Render;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;
import showoff.Vulkan.VulkanException;
import showoff.FrameAllocator;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK13.*;

public class VulkanSync
{
    public final long[] m_fences;
    public final long[] m_imageAcquiredSemaphores;
    public final long[] m_renderCompleteSemaphores;
    private final VkDevice device;

    public VulkanSync(VkDevice device, int frame_count)
    {
        this.device = device;
        this.m_imageAcquiredSemaphores = new long[frame_count];
        this.m_renderCompleteSemaphores = new long[frame_count];
        this.m_fences = new long[frame_count];
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                    .pNext(VkSemaphoreTypeCreateInfo.calloc(allocator)
                            .sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO)
                            .semaphoreType(VK_SEMAPHORE_TYPE_BINARY)
                            .initialValue(0));
            VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK_FENCE_CREATE_SIGNALED_BIT);
            LongBuffer pVkDest = allocator.mallocLong(1);
            for (int i = 0; i < frame_count; i++)
            {
                VulkanException.check(vkCreateFence(this.device, fenceCreateInfo, null, pVkDest));
                this.m_fences[i] = pVkDest.get(0);
                VulkanException.check(vkCreateSemaphore(this.device, semaphoreCreateInfo, null, pVkDest));
                this.m_imageAcquiredSemaphores[i] = pVkDest.get(0);
                VulkanException.check(vkCreateSemaphore(this.device, semaphoreCreateInfo, null, pVkDest));
                this.m_renderCompleteSemaphores[i] = pVkDest.get(0);
            }
        }
    }

    public void dispose()
    {
        for (long fence : this.m_fences) vkDestroyFence(this.device, fence, null);
        for (long semaphore : this.m_imageAcquiredSemaphores) vkDestroySemaphore(this.device, semaphore, null);
        for (long semaphore : this.m_renderCompleteSemaphores) vkDestroySemaphore(this.device, semaphore, null);
    }
}

package showoff.Vulkan;

import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDevice;
import showoff.Disposable;
import showoff.FrameAllocator;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK13.*;

public class DescriptorPool implements Disposable
{
    private final long m_internalHandle;
    private final VkDevice device;

    public DescriptorPool(VkDevice device, VkDescriptorPoolSize.Buffer pPoolSizes, int capacity) throws VulkanException
    {
        this.device = device;
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(pPoolSizes)
                    .maxSets(capacity);
            LongBuffer pDescriptorPool = allocator.mallocLong(1);
            VulkanException.check(vkCreateDescriptorPool(this.device, descriptorPoolCreateInfo, null, pDescriptorPool), "Vulkan descriptor pool creation failed.");
            this.m_internalHandle = pDescriptorPool.get(0);
        }
    }

    public long get()
    {
        return this.m_internalHandle;
    }

    public void allocateDescriptorSets(LongBuffer pLayouts, LongBuffer pDescriptorSets) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(this.m_internalHandle)
                    .pSetLayouts(pLayouts);
            VulkanException.check(vkAllocateDescriptorSets(this.device, descriptorSetAllocateInfo, pDescriptorSets));
        }
    }

    @Override
    public void dispose()
    {
        vkDestroyDescriptorPool(this.device, this.m_internalHandle, null);
    }
}

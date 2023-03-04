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
    public record DescriptorPoolSize(int type, int count) {}

    private final long m_internalHandle;
    private final VkDevice device;
    private final boolean m_isAllocOnly;

    public DescriptorPool(VkDevice device, DescriptorPoolSize[] poolSizes, int capacity, boolean allocOnly) throws VulkanException
    {
        this.device = device;
        this.m_isAllocOnly = allocOnly;
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkDescriptorPoolSize.Buffer pPoolSizes = VkDescriptorPoolSize.calloc(poolSizes.length, allocator);
            for (int i = 0; i < poolSizes.length; i++)
            {
                pPoolSizes.get(i)
                        .type(poolSizes[i].type)
                        .descriptorCount(poolSizes[i].count);
            }
            VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .flags(this.m_isAllocOnly ? VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT : 0)
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

    public boolean isAllocOnly()
    {
        return this.m_isAllocOnly;
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

package showoff.Vulkan;

import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkWriteDescriptorSet;
import showoff.Disposable;
import showoff.FrameAllocator;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK13.*;

public class DescriptorSet implements Disposable
{
    private final long m_internalHandle;
    private final VkDevice device;
    private final DescriptorPool descriptorPool;

    public DescriptorSet(VkDevice device, DescriptorPool descriptorPool, long layout) throws VulkanException
    {
        this.device = device;
        this.descriptorPool = descriptorPool;
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            LongBuffer pDescriptorSet = allocator.mallocLong(1);
            this.descriptorPool.allocateDescriptorSets(allocator.longs(layout), pDescriptorSet);
            this.m_internalHandle = pDescriptorSet.get(0);
        }
    }

    public long get()
    {
        return this.m_internalHandle;
    }

    public void updateUniformBuffer(long buffer, long size, long offset)
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkDescriptorBufferInfo.Buffer descriptorBufferInfos = VkDescriptorBufferInfo.calloc(1, allocator)
                    .apply(0, info -> info
                            .buffer(buffer)
                            .offset(offset)
                            .range(size));
            VkWriteDescriptorSet.Buffer writeDescriptorSets = VkWriteDescriptorSet.calloc(1, allocator)
                    .apply(0, writeDesc -> writeDesc
                            .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                            .dstSet(this.m_internalHandle)
                            .dstBinding(0)
                            .dstArrayElement(0)
                            .descriptorCount(1)
                            .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                            .pBufferInfo(descriptorBufferInfos));
            vkUpdateDescriptorSets(this.device, writeDescriptorSets, null);
        }
    }

    @Override
    public void dispose()
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            vkFreeDescriptorSets(this.device, this.descriptorPool.get(), allocator.longs(this.m_internalHandle));
        }
    }
}

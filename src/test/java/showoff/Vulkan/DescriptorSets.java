package showoff.Vulkan;

import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkWriteDescriptorSet;
import showoff.Disposable;
import showoff.FrameAllocator;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK13.*;

public class DescriptorSets implements Disposable
{
    private final long[] m_internalHandles;
    private final VkDevice device;
    private final DescriptorPool descriptorPool;

    public DescriptorSets(VkDevice device, DescriptorPool descriptorPool, long[] layouts) throws VulkanException
    {
        this.device = device;
        this.descriptorPool = descriptorPool;
        this.m_internalHandles = new long[layouts.length];
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            LongBuffer pDescriptorSets = allocator.mallocLong(this.m_internalHandles.length);
            this.descriptorPool.allocateDescriptorSets(allocator.longs(layouts), pDescriptorSets);
            pDescriptorSets.get(0, this.m_internalHandles);
        }
    }

    public long[] get()
    {
        return this.m_internalHandles;
    }

    public void updateUniformBuffer(long descriptorSet, long buffer, long size, long offset)
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
                            .dstSet(descriptorSet)
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
        if (this.descriptorPool.isAllocOnly()) return;
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            vkFreeDescriptorSets(this.device, this.descriptorPool.get(), allocator.longs(this.m_internalHandles));
        }
    }

    public static class Layout implements Disposable
    {
        public record Uniform(int binding, int type, int count, int stages) {}
        private final long m_internalHandle;
        private final VkDevice device;

        public Layout(VkDevice device, Uniform[] objects) throws VulkanException
        {
            this.device = device;
            try (FrameAllocator allocator = FrameAllocator.takeAndPush())
            {
                VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(objects.length, allocator);
                for (int i = 0; i < objects.length; i++)
                {
                    bindings.get(i)
                            .binding(objects[i].binding)
                            .stageFlags(objects[i].stages)
                            .descriptorCount(objects[i].count)
                            .descriptorType(objects[i].type);
                }
                VkDescriptorSetLayoutCreateInfo layoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(allocator)
                        .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                        .pBindings(bindings);
                LongBuffer pLayout = allocator.mallocLong(1);
                VulkanException.check(vkCreateDescriptorSetLayout(this.device, layoutCreateInfo, null, pLayout));
                this.m_internalHandle = pLayout.get(0);
            }
        }

        public long get()
        {
            return this.m_internalHandle;
        }

        @Override
        public void dispose()
        {
            vkDestroyDescriptorSetLayout(this.device, this.m_internalHandle, null);
        }
    }
}

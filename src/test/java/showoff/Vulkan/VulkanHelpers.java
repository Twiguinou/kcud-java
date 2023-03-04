package showoff.Vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import java.io.IOException;
import java.io.InputStream;

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

    public static int getMaxUsableSampleCount(final VkPhysicalDeviceProperties properties)
    {
        final VkPhysicalDeviceLimits limits = properties.limits();
        int counts = limits.framebufferColorSampleCounts() & limits.framebufferDepthSampleCounts();
        if ((counts & VK_SAMPLE_COUNT_64_BIT) != 0) {return VK_SAMPLE_COUNT_64_BIT;}
        if ((counts & VK_SAMPLE_COUNT_32_BIT) != 0) {return VK_SAMPLE_COUNT_32_BIT;}
        if ((counts & VK_SAMPLE_COUNT_16_BIT) != 0) {return VK_SAMPLE_COUNT_16_BIT;}
        if ((counts & VK_SAMPLE_COUNT_8_BIT) != 0) {return VK_SAMPLE_COUNT_8_BIT;}
        if ((counts & VK_SAMPLE_COUNT_4_BIT) != 0) {return VK_SAMPLE_COUNT_4_BIT;}
        if ((counts & VK_SAMPLE_COUNT_2_BIT) != 0) {return VK_SAMPLE_COUNT_2_BIT;}
        return VK_SAMPLE_COUNT_1_BIT;
    }

    public static ShaderModule loadShaderModule(VkDevice device, String filepath, int stage) throws VulkanException
    {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("shaders/" + filepath))
        {
            if (stream == null) throw new VulkanException("Failed to load resource: " + filepath);
            return new ShaderModule(device, stage, stream, "main", new ShaderModule.CompilingDescription(filepath, true));
        }
        catch (IOException e)
        {
            throw new VulkanException(e.toString());
        }
    }
}

package showoff.DefaultedRenderers.Vulkan.ext;

import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import showoff.ForeignStack;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.KHRSurface.*;
import static showoff.DefaultedRenderers.Vulkan.VulkanToolbox.*;

public record SurfaceHandle(long handle, VkSurfaceFormatKHR.Buffer formats, int[] presentModes)
{
    public static SurfaceHandle create(VkPhysicalDevice physicalDevice, long surface)
    {
        try (ForeignStack stack = ForeignStack.pushConfined(Integer.BYTES * 32))
        {
            IntBuffer pCount = stack.mallocInt(1);
            kdvkAssertThrow(vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pCount, null));
            VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.create(pCount.get(0));
            kdvkAssertThrow(vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pCount, formats));

            kdvkAssertThrow(vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pCount, null));
            IntBuffer presentModes = stack.mallocInt(pCount.get(0));
            kdvkAssertThrow(vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pCount, presentModes));
            int[] jp = new int[presentModes.capacity()];
            presentModes.get(jp);
            return new SurfaceHandle(surface, formats, jp);
        }
    }

    public static void capabilities(long handle, VkPhysicalDevice physicalDevice, VkSurfaceCapabilitiesKHR capabilities)
    {
        kdvkAssertThrow(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, handle, capabilities));
    }

}

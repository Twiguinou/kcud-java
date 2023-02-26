package showoff.Vulkan.ext;

import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXTI;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import showoff.Vulkan.VulkanContext;
import showoff.Vulkan.VulkanException;
import showoff.FrameAllocator;
import showoff.WindowContext.WindowProcessor;

import javax.annotation.Nullable;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.function.BinaryOperator;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK13.VK_NULL_HANDLE;

public class VulkanRenderContext extends VulkanContext
{
    public record PhysicalDeviceSurfaceProperties(VkSurfaceCapabilitiesKHR capabilities, VkSurfaceFormatKHR.Buffer formats, int[] presentModes)
    {
        private void free()
        {
            this.capabilities.free();
        }
    }

    private final long m_surfaceHandle;
    private PhysicalDeviceSurfaceProperties m_physicalDeviceSurfaceProperties;
    private final WindowProcessor windowProc;

    public VulkanRenderContext(String app_name, int app_version, String engine_name, int engine_version, int vk_version, String[] requiredExtensions, @Nullable VkDebugUtilsMessengerCallbackEXTI debug_callback,
                               WindowProcessor windowProc) throws VulkanException
    {
        super(app_name, app_version, engine_name, engine_version, vk_version, requiredExtensions, debug_callback);
        this.windowProc = windowProc;
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            LongBuffer pSurface = allocator.mallocLong(1);
            VulkanException.check(this.windowProc.createVulkanSurface(this.getInstance(), null, pSurface), "Failed to create vulkan surface.");
            this.m_surfaceHandle = pSurface.get(0);
        }
        this.m_physicalDeviceSurfaceProperties = null;
    }

    public long getSurface()
    {
        return this.m_surfaceHandle;
    }

    public PhysicalDeviceSurfaceProperties getSurfaceProperties()
    {
        return this.m_physicalDeviceSurfaceProperties;
    }

    public WindowProcessor getWindowProcessor()
    {
        return this.windowProc;
    }

    @Override
    public void findSuitableDevice(BinaryOperator<PhysicalDevice> comparator) throws VulkanException
    {
        super.findSuitableDevice(comparator);
        if (this.m_physicalDeviceSurfaceProperties != null)
        {
            this.m_physicalDeviceSurfaceProperties.free();
            this.m_physicalDeviceSurfaceProperties = null;
        }
        if (this.getPhysicalDevice() != null && this.m_surfaceHandle != VK_NULL_HANDLE)
        {
            VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.malloc();
            VkSurfaceFormatKHR.Buffer formats = null;
            int[] presentModes = new int[0];
            try (FrameAllocator allocator = FrameAllocator.takeAndPush())
            {
                VulkanException.check(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(this.getPhysicalDevice().handle(), this.m_surfaceHandle, capabilities));
                IntBuffer pCount = allocator.mallocInt(1);
                VulkanException.check(vkGetPhysicalDeviceSurfaceFormatsKHR(this.getPhysicalDevice().handle(), this.m_surfaceHandle, pCount, null));
                if (pCount.get(0) > 0)
                {
                    formats = VkSurfaceFormatKHR.malloc(pCount.get(0), allocator);
                    VulkanException.check(vkGetPhysicalDeviceSurfaceFormatsKHR(this.getPhysicalDevice().handle(), this.m_surfaceHandle, pCount, formats));
                }
                VulkanException.check(vkGetPhysicalDeviceSurfacePresentModesKHR(this.getPhysicalDevice().handle(), this.m_surfaceHandle, pCount, null));
                presentModes = new int[pCount.get(0)];
                if (pCount.get(0) > 0)
                {
                    IntBuffer pPresentModes = allocator.mallocInt(pCount.get(0));
                    VulkanException.check(vkGetPhysicalDeviceSurfacePresentModesKHR(this.getPhysicalDevice().handle(), this.m_surfaceHandle, pCount, pPresentModes));
                    pPresentModes.get(presentModes, 0, presentModes.length);
                }
            }
            catch (VulkanException e)
            {
                if (formats != null) formats.free();
                capabilities.free();
                throw e;
            }

            this.m_physicalDeviceSurfaceProperties = new PhysicalDeviceSurfaceProperties(capabilities, formats, presentModes);
        }
    }

    @Override
    public void dispose()
    {
        vkDestroySurfaceKHR(this.getInstance(), this.m_surfaceHandle, null);
        if (this.m_physicalDeviceSurfaceProperties != null)
        {
            this.m_physicalDeviceSurfaceProperties.free();
            this.m_physicalDeviceSurfaceProperties = null;
        }
        super.dispose();
    }
}

package showoff;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan12Features;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import showoff.DefaultedRenderers.Vulkan.LogicalDevice;
import showoff.DefaultedRenderers.Vulkan.VulkanContext;
import showoff.DefaultedRenderers.Vulkan.ext.SurfaceHandle;
import showoff.DefaultedRenderers.Vulkan.ext.Swapchain;
import showoff.WindowContext.GLFWWindowProcessor;
import showoff.WindowContext.WindowProcessor;

import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSurface.*;

public class Main
{

    private static SurfaceHandle createSurface(VkInstance instance, VkPhysicalDevice physicalDevice, WindowProcessor windowProcessor)
    {
        try (ForeignStack stack = ForeignStack.pushConfined(Long.BYTES))
        {
            LongBuffer pVkDest = stack.mallocLong(1);
            if (windowProcessor.createVulkanSurface(instance, null, pVkDest) != VK_SUCCESS)
            {
                throw new RuntimeException("Could not create vulkan surface.");
            }
            return SurfaceHandle.create(physicalDevice, pVkDest.get(0));
        }
    }

    public static void main(String... args)
    {
        assert glfwInit() && glfwVulkanSupported();
        WindowProcessor windowProc = new GLFWWindowProcessor();
        windowProc.setWindowTitle("showoff");
        windowProc.createWindowContext(1280, 720);
        VulkanContext context = new VulkanContext();
        {
            PointerBuffer extensions = windowProc.getVulkanExtensions();
            String[] jext = new String[extensions.capacity()];
            for (int i = 0; i < jext.length; i++)
            {
                jext[i] = extensions.getStringASCII(i);
            }
            context.initialize(jext, true, (messageSeverity, messageTypes, pCallbackData, pUserData) ->
            {
                VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                if (messageSeverity >= VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT)
                {
                    System.err.println(callbackData.pMessageString());
                }
                else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) == 0)
                {
                    System.out.println(callbackData.pMessageString());
                }
                return VK_FALSE;
            });
        }
        context.detectPresentDevices();

        if (context.m_detectedDevices.isEmpty())
        {
            throw new RuntimeException("Failed to initialize vulkan dependencies.");
        }
        final VulkanContext.PhysicalDevice physicalDevice = context.m_detectedDevices.get(0);
        System.out.printf("Selected physical device: %s\n", physicalDevice.properties().deviceNameString());
        SurfaceHandle surface = createSurface(context.getVulkanInstance(), physicalDevice.handle(), windowProc);

        String[] deviceExtensions = new String[] {
                VK_KHR_SWAPCHAIN_EXTENSION_NAME
        };
        LogicalDevice logicalDevice = new LogicalDevice();
        LogicalDevice.QueueRequirements[] queueRequirementsList = new LogicalDevice.QueueRequirements[] {
                /*graphics*/new LogicalDevice.QueueRequirements(VK_QUEUE_GRAPHICS_BIT | VK_QUEUE_TRANSFER_BIT, new long[]{}, null, false),
                /*present*/new LogicalDevice.QueueRequirements(0x7FFFFFFF, new long[]{surface.handle()}, null, false),
                /*compute*/new LogicalDevice.QueueRequirements(VK_QUEUE_COMPUTE_BIT, new long[]{}, null, false)
        };
        try (ForeignStack stack = ForeignStack.pushConfined(1024))
        {
            VkPhysicalDeviceVulkan12Features vulkan12Features = VkPhysicalDeviceVulkan12Features.calloc(stack);
            vulkan12Features.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);
            vulkan12Features.pNext(MemoryUtil.NULL);
            vulkan12Features.descriptorBindingPartiallyBound(true);
            vulkan12Features.descriptorBindingVariableDescriptorCount(true);
            vulkan12Features.runtimeDescriptorArray(true);
            vulkan12Features.hostQueryReset(true);
            VkPhysicalDeviceFeatures physicalDeviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            physicalDeviceFeatures.geometryShader(true);
            physicalDeviceFeatures.depthClamp(true);
            physicalDeviceFeatures.fillModeNonSolid(true);
            physicalDeviceFeatures.samplerAnisotropy(true);
            physicalDeviceFeatures.fragmentStoresAndAtomics(true);
            logicalDevice.initialize(physicalDevice, queueRequirementsList, deviceExtensions, vulkan12Features.address(), physicalDeviceFeatures);
        }
        LogicalDevice.QueueProperties graphics_transfer_queue = logicalDevice.m_generatedQueues.get(0);
        LogicalDevice.QueueProperties present_queue = logicalDevice.m_generatedQueues.get(1);
        LogicalDevice.QueueProperties compute_queue = logicalDevice.m_generatedQueues.get(2);
        System.out.printf("Graphics and Transfer Queue f%s|i%s, Present Queue f%s|i%s, Compute Queue f%s|i%s\n",
                graphics_transfer_queue.family(), graphics_transfer_queue.index(),
                present_queue.family(), present_queue.index(),
                compute_queue.family(), compute_queue.index());

        Swapchain swapchain = new Swapchain();
        //swapchain.initialize()

        long prev_frame = System.currentTimeMillis();
        while (!windowProc.windowShouldClose())
        {
            windowProc.beginRenderStage();
            long c_frame = System.currentTimeMillis();
            if (c_frame - 1200 >= prev_frame)
            {
                prev_frame = c_frame;
                try (ForeignStack stack = ForeignStack.pushConfined(512))
                {
                    VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
                    surface.capabilities(physicalDevice.handle(), capabilities);
                    System.out.println(capabilities.currentExtent().width() + " " + capabilities.currentExtent().height());
                }
            }
            windowProc.endRenderStage();
        }

        vkDeviceWaitIdle(logicalDevice.getHandle());
        logicalDevice.dispose();
        vkDestroySurfaceKHR(context.getVulkanInstance(), surface.handle(), null);
        windowProc.endWindowContext();
        glfwTerminate();
        context.disposeContext();
        System.exit(0);
    }

}

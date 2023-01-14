package showoff;

import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import showoff.DefaultedRenderers.Vulkan.VulkanContext;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;

public class Main
{
    
    private static void test_main()
    {
        assert glfwInit() && glfwVulkanSupported();
        VulkanContext context = new VulkanContext();
        if (context.initialize(new String[] {}, true, (messageSeverity, messageTypes, pCallbackData, pUserData) ->
        {
            VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
            if (messageSeverity >= VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT)
            {
                System.err.println(callbackData.pMessageString());
            }
            else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT )!= 0)
            {
                System.out.println(callbackData.pMessageString());
            }
            return VK_FALSE;
        }) != VK_SUCCESS || context.m_detectedDevices.isEmpty())
        {
            throw new RuntimeException("Failed to initialize vulkan dependencies.");
        }
        final VulkanContext.PhysicalDevice physicalDevice = context.m_detectedDevices.get(0);
        System.out.printf("Selected physical device: %s\n", physicalDevice.properties().deviceNameString());
        glfwTerminate();
        context.disposeContext();
    }

    public static void main(String... args)
    {
        System.exit(0);
    }

}
